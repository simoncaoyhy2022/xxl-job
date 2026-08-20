// xxl-job-executor-samples/xxl-job-executor-etl/src/main/java/com/xxl/job/executor/cdc/sync/CdcSyncOrchestrator.java
package com.xxl.job.executor.cdc.sync;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.executor.cdc.config.CdcSourceProperties;
import com.xxl.job.executor.cdc.config.SourceDataSourceRegistry;
import com.xxl.job.executor.cdc.mapper.ProdOrdHdrMapper;
import com.xxl.job.executor.cdc.mapper.SalesOrdDtlMapper;
import com.xxl.job.executor.cdc.mapper.SalesOrdHdrMapper;
import com.xxl.job.executor.cdc.meta.CdcTableDef;
import com.xxl.job.executor.cdc.service.CdcExtractService;
import com.xxl.job.executor.cdc.service.CdcWatermarkService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * CDC 增量同步编排器
 * <p>
 * 单表流程：
 * 1、取水位 fromLsn（首次为 null，由 CdcExtractService 内部回退到 min_lsn）
 * 2、取本轮截止 toLsn = sys.fn_cdc_get_max_lsn()
 * 3、fromLsn >= toLsn 时判定无新变更，直接跳过（不重复扫描/不推进水位以外的开销）
 * 4、拉取净变更，按 __$operation 拆分 upsert(2/4) 与 delete(1)，分批落库
 * 5、无论本轮是否有变更，均将水位推进到 toLsn（避免下次重复扫描空区间）
 */
@Component
public class CdcSyncOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(CdcSyncOrchestrator.class);
    private static final int MAX_PARAMS_PER_BATCH = 3000;

    @Resource
    private SourceDataSourceRegistry registry;
    @Resource
    private CdcSourceProperties cdcSourceProperties;
    @Resource
    private CdcExtractService extractService;
    @Resource
    private CdcWatermarkService watermarkService;

    @Resource
    private ProdOrdHdrMapper prodOrdHdrMapper;
    @Resource
    private SalesOrdHdrMapper salesOrdHdrMapper;
    @Resource
    private SalesOrdDtlMapper salesOrdDtlMapper;

    private final Map<String, CdcTableSyncHandler> handlerMap = new HashMap<>();

    @PostConstruct
    public void init() {
        handlerMap.put(CdcTableDef.PRODORDHDR.getCaptureInstance(), new CdcTableSyncHandler() {
            @Override
            public void upsert(List<Map<String, Object>> rows) {
                prodOrdHdrMapper.upsertBatch(rows);
            }

            @Override
            public void delete(List<Map<String, Object>> rows) {
                prodOrdHdrMapper.deleteBatch(rows);
            }
        });

        handlerMap.put(CdcTableDef.SALESORDHDR.getCaptureInstance(), new CdcTableSyncHandler() {
            @Override
            public void upsert(List<Map<String, Object>> rows) {
                salesOrdHdrMapper.upsertBatch(rows);
            }

            @Override
            public void delete(List<Map<String, Object>> rows) {
                salesOrdHdrMapper.deleteBatch(rows);
            }
        });

        handlerMap.put(CdcTableDef.SALESORDDTL.getCaptureInstance(), new CdcTableSyncHandler() {
            @Override
            public void upsert(List<Map<String, Object>> rows) {
                salesOrdDtlMapper.upsertBatch(rows);
            }

            @Override
            public void delete(List<Map<String, Object>> rows) {
                salesOrdDtlMapper.deleteBatch(rows);
            }
        });
    }

    /**
     * 同步所有生产厂数据库 x 全部表
     */
    public void syncAllProd() {
        List<String> bps = cdcSourceProperties.getSources().stream()
                .filter(CdcSourceProperties.SourceConfig::isProd)
                .map(CdcSourceProperties.SourceConfig::getId)
                .toList();

        // 使用虚拟线程池并行同步每个源库的全部表，避免单线程顺序执行过慢
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (String bp : bps) {
                executor.submit(() -> {
                    for (CdcTableDef def : CdcTableDef.ALL) {
                        syncOne(bp, def);
                    }
                });
            }
        } // 退出 try 块时自动阻塞等待所有 bp 的虚拟线程全部执行完成


    }

    /**
     * 同步指定源库的全部表
     */
    public void syncSource(String bp) {
        for (CdcTableDef def : CdcTableDef.ALL) {
            syncOne(bp, def);
        }
    }

    /**
     * 同步指定源库的指定表（按 captureInstance 匹配）
     */
    public void syncTable(String bp, String captureInstance) {
        CdcTableDef def = CdcTableDef.ALL.stream()
                .filter(d -> d.getCaptureInstance().equals(captureInstance))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的 captureInstance: " + captureInstance));
        syncOne(bp, def);
    }

    private void syncOne(String bp, CdcTableDef def) {
        DataSource ds = registry.get(bp);
        if (ds == null) {
            log("source[{}] 未启用或不存在, 跳过 captureInstance={}", bp, def.getCaptureInstance());
            return;
        }

        CdcTableSyncHandler handler = handlerMap.get(def.getCaptureInstance());
        if (handler == null) {
            log("captureInstance={} 未注册同步处理器, 跳过", def.getCaptureInstance());
            return;
        }

        try {
            byte[] fromLsn = watermarkService.getLastLsn(ds, def.getCaptureInstance());
            byte[] toLsn = extractService.getMaxLsn(ds);
            if (toLsn == null) {
                log("source[{}] captureInstance={} 获取 max_lsn 为空, 跳过", bp, def.getCaptureInstance());
                return;
            }
            if (fromLsn != null && Arrays.compare(fromLsn, toLsn) >= 0) {
                log("source[{}] captureInstance={} 无新变更(fromLsn>=toLsn), 跳过", bp, def.getCaptureInstance());
                return;
            }

            List<Map<String, Object>> changes = extractService.fetchNetChanges(ds, def, fromLsn, toLsn);

            int upsertCount = 0;
            int deleteCount = 0;

            if (!changes.isEmpty()) {
                List<Map<String, Object>> upserts = new ArrayList<>();
                List<Map<String, Object>> deletes = new ArrayList<>();

                for (Map<String, Object> row : changes) {
                    Object opObj = row.get("__$operation");
                    int op = (opObj instanceof Number) ? ((Number) opObj).intValue() : -1;
                    if (op == 1) {
                        deletes.add(row);
                    } else if (op == 2 || op == 4) {
                        upserts.add(row);
                    } else {
                        log("source[{}] captureInstance={} 未知 operation={}, 跳过该行", bp, def.getCaptureInstance(), op);
                    }
                }

                int size = batchSizeFor(def);// 计算每批次大小，避免 SQL 参数过多

                for (List<Map<String, Object>> batch : partition(upserts, size)) {
                    handler.upsert(batch);
                }
                for (List<Map<String, Object>> batch : partition(deletes, size)) {
                    handler.delete(batch);
                }

                upsertCount = upserts.size();
                deleteCount = deletes.size();
            }

            // 推进水位（即便本轮无变更，也要推进，避免下次重复扫描空区间）
            watermarkService.upsertLsn(ds, def.getCaptureInstance(), toLsn);

            log("source[{}] captureInstance={} 同步完成, upsert={}, delete={}",
                    bp, def.getCaptureInstance(), upsertCount, deleteCount);
        } catch (Exception e) {
            log("source[{}] captureInstance={} 同步失败: {}", bp, def.getCaptureInstance(), e.getMessage());
            logger.error("CDC sync error, source={}, captureInstance={}", bp, def.getCaptureInstance(), e);
            throw e;
        }
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }

    private int batchSizeFor(CdcTableDef def) {
        int paramsPerRow = def.getColumns().length + 2; // + BP + dataSource/updateTime 等固定列
        return Math.max(50, MAX_PARAMS_PER_BATCH / paramsPerRow);
    }

    private void log(String pattern, Object... args) {
        XxlJobHelper.log(pattern, args);
        logger.info(pattern, args);
    }

}