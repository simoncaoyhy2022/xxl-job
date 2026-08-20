// xxl-job-executor-samples/xxl-job-executor-etl/src/main/java/com/xxl/job/executor/cdc/jobhandler/CdcSyncJobHandler.java
package com.xxl.job.executor.cdc.jobhandler;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.executor.cdc.sync.CdcSyncOrchestrator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * CDC 增量同步 XxlJob 入口
 *
 * 在 XXL-JOB 调度中心配置 JobHandler 名称为: cdcSyncHandler
 * 任务参数(executorParam)：
 *   空                          -> 同步全部启用源库 x 全部表
 *   sourceId                    -> 同步指定源库的全部表           如: szbp
 *   sourceId:captureInstance    -> 同步指定源库的指定表           如: szbp:dbo_t_prodordhdr
 */
@Component
public class CdcSyncJobHandler {

    @Resource
    private CdcSyncOrchestrator cdcSyncOrchestrator;

    @XxlJob("cdcSyncHandler")
    public void sync() {
        String param = XxlJobHelper.getJobParam();

        try {
            if (param == null || param.trim().isEmpty()) {
                XxlJobHelper.log(">>>>>> 开始 CDC 增量同步 (全部源库 x 全部表)");
                cdcSyncOrchestrator.syncAllProd();
            } else {
                String value = param.trim();
                int idx = value.indexOf(':');
                if (idx < 0) {
                    XxlJobHelper.log(">>>>>> 开始 CDC 增量同步, BP={}", value);
                    cdcSyncOrchestrator.syncSource(value);
                } else {
                    String sourceId = value.substring(0, idx).trim();
                    String captureInstance = value.substring(idx + 1).trim();
                    XxlJobHelper.log(">>>>>> 开始 CDC 增量同步, BP={}, captureInstance={}", sourceId, captureInstance);
                    cdcSyncOrchestrator.syncTable(sourceId, captureInstance);
                }
            }
            XxlJobHelper.handleSuccess("CDC 增量同步执行完毕");
        } catch (Exception e) {
            XxlJobHelper.handleFail("CDC 增量同步异常: " + e.getMessage());
        }
    }

}