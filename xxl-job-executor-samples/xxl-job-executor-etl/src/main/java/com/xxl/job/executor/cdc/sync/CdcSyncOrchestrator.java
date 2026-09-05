package com.xxl.job.executor.cdc.sync;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.executor.cdc.config.CdcSourceProperties;
import com.xxl.job.executor.cdc.config.SourceDataSourceRegistry;
import com.xxl.job.executor.cdc.meta.CdcTableDef;
import com.xxl.job.executor.cdc.service.CdcExtractService;
import com.xxl.job.executor.cdc.service.CdcWatermarkService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/** Coordinates the common CDC extraction, batching, and watermark workflow. */
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
    private CdcTableSyncHandlerRegistry handlerRegistry;

    public void syncAllProd() {
        List<String> bps = cdcSourceProperties.getSources().stream()
                .filter(c -> c.isProd() || "pmc".equals(c.getId()))
                .map(CdcSourceProperties.SourceConfig::getId)
                .toList();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<? extends Future<?>> futures = bps.stream()
                    .map(bp -> executor.submit(() -> {
                        for (CdcTableDef def : CdcTableDef.ALL) {
                            if ("pmc".equals(bp) && def == CdcTableDef.PRODORDHDR) {
                                // 跳过pmc的工单表（pmc只同步订单表）
                                continue;
                            }
                            syncOne(bp, def);
                        }
                    }))
                    .toList();

            List<Throwable> errors = new ArrayList<>();
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    errors.add(e.getCause() != null ? e.getCause() : e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("CDC task interrupted", e);
                }
            }

            if (!errors.isEmpty()) {
                String errorMsg = errors.stream()
                        .map(Throwable::getMessage)
                        .collect(Collectors.joining("; "));
                throw new RuntimeException("CDC synchronization contains failed tasks: " + errorMsg);
            }
        }
    }

    public void syncBP(String bp, CdcTableDef def) {
        syncOne(bp, def);
    }

    private void syncOne(String bp, CdcTableDef def) {
        DataSource ds = registry.get(bp);
        if (ds == null) {
            log("source[{}] is disabled or does not exist; skip captureInstance={}", bp, def.getCaptureInstance());
            return;
        }

        CdcTableSyncHandler handler = handlerRegistry.get(def);
        if (handler == null) {
            log("No sync handler registered for captureInstance={}; skip", def.getCaptureInstance());
            return;
        }

        try {
            byte[] fromLsn = watermarkService.getLastLsn(ds, def.getCaptureInstance());
            byte[] toLsn = extractService.getMaxLsn(ds);
            if (toLsn == null) {
                log("source[{}] captureInstance={} returned an empty max_lsn; skip", bp, def.getCaptureInstance());
                return;
            }
            if (fromLsn != null && Arrays.compareUnsigned(fromLsn, toLsn) >= 0) {
                log("source[{}] captureInstance={} has no new changes; skip", bp, def.getCaptureInstance());
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
                    int op = opObj instanceof Number ? ((Number) opObj).intValue() : -1;
                    if (op == 1) {
                        deletes.add(row);
                    } else if (op == 2 || op == 4) {
                        upserts.add(row);
                    } else {
                        log("source[{}] captureInstance={} has unknown operation={}; skip row", bp, def.getCaptureInstance(), op);
                    }
                }

                int batchSize = batchSizeFor(def);
                for (List<Map<String, Object>> batch : partition(upserts, batchSize)) {
                    upsertCount += handler.upsert(bp, batch);
                }
                for (List<Map<String, Object>> batch : partition(deletes, batchSize)) {
                    handler.delete(batch);
                }
                deleteCount = deletes.size();
            }

            watermarkService.upsertLsn(ds, def.getCaptureInstance(), toLsn);
            log("BP[{}] captureInstance={} synchronized, upsert={}, delete={}",
                    bp, def.getCaptureInstance(), upsertCount, deleteCount);
        } catch (Exception e) {
            log("source[{}] captureInstance={} synchronization failed: {}", bp, def.getCaptureInstance(), e.getMessage());
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
        int paramsPerRow = def.getColumns().length + 2;
        return Math.max(50, MAX_PARAMS_PER_BATCH / paramsPerRow);
    }

    private void log(String pattern, Object... args) {
        XxlJobHelper.log(pattern, args);
        logger.info(pattern, args);
    }
}
