package com.xxl.job.executor.cdc.jobhandler;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.executor.cdc.config.SourceDataSourceRegistry;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class CdcSourceHealthCheckHandler {

    @Resource
    private SourceDataSourceRegistry registry;

    @XxlJob("cdcSourceHealthCheck")
    public void check() {
        for (String id : registry.listSourceIds()) {
            try (var conn = registry.get(id).getConnection()) {
                XxlJobHelper.log("source[{}] OK, catalog={}", id, conn.getCatalog());
            } catch (Exception e) {
                XxlJobHelper.log("source[{}] FAIL: {}", id, e.getMessage());
            }
        }
    }
}