package com.xxl.job.executor.cdc.config;

import com.alibaba.druid.pool.DruidDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CDC 源库连接池注册表。
 * 与 target 报表库（Spring 标准单数据源）分离管理：
 * source 侧只做原生 JDBC 查询，不接入 MyBatis SqlSessionFactory。
 */
@Component
public class SourceDataSourceRegistry {

    private static final Logger logger = LoggerFactory.getLogger(SourceDataSourceRegistry.class);

    @Resource
    private CdcSourceProperties properties;

    private final Map<String, DruidDataSource> pools = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        for (CdcSourceProperties.SourceConfig cfg : properties.getSources()) {
            if (!cfg.isEnabled()) {
                logger.info(">>>>>> CDC source [{}] disabled, skip.", cfg.getId());
                continue;
            }
            if (pools.containsKey(cfg.getId())) {
                throw new IllegalStateException("重复的 CDC 源库 id: " + cfg.getId());
            }
            pools.put(cfg.getId(), buildPool(cfg));
            logger.info(">>>>>> CDC source [{}] datasource ready.", cfg.getId());
        }
        if (pools.isEmpty()) {
            logger.warn(">>>>>> 未配置任何启用的 CDC 源库，请检查 etl.cdc.sources.* 配置。");
        }
    }

    private DruidDataSource buildPool(CdcSourceProperties.SourceConfig cfg) {
        CdcSourceProperties.PoolConfig poolCfg = properties.getPool();

        DruidDataSource ds = new DruidDataSource();
        ds.setName("cdc-source-" + cfg.getId());
        ds.setUrl(cfg.getJdbcUrl());
        ds.setUsername(cfg.getUsername());
        ds.setPassword(cfg.getPassword());
        ds.setDriverClassName(cfg.getDriverClassName());

        ds.setInitialSize(poolCfg.getInitialSize());
        ds.setMinIdle(poolCfg.getMinIdle());
        ds.setMaxActive(poolCfg.getMaxActive());
        ds.setMaxWait(poolCfg.getMaxWait());
        ds.setValidationQuery(poolCfg.getValidationQuery());
        ds.setTestWhileIdle(true);
        ds.setTestOnBorrow(false);
        ds.setTestOnReturn(false);

        return ds;
    }

    /** 获取指定源库的数据源，不存在或未启用返回 null */
    public DataSource get(String sourceId) {
        return pools.get(sourceId);
    }

    /** 遍历所有已启用源库的 id，供编排器逐个提交任务 */
    public List<String> listSourceIds() {
        return List.copyOf(pools.keySet());
    }

    public Map<String, DruidDataSource> snapshot() {
        return Collections.unmodifiableMap(pools);
    }

    @PreDestroy
    public void close() {
        pools.forEach((id, ds) -> {
            try {
                ds.close();
            } catch (Exception e) {
                logger.warn(">>>>>> close CDC source [{}] datasource error: {}", id, e.getMessage());
            }
        });
    }
}