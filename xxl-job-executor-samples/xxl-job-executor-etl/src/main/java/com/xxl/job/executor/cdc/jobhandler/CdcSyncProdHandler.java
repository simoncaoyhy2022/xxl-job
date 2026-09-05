package com.xxl.job.executor.cdc.jobhandler;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.executor.cdc.sync.CdcSyncOrchestrator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * CDC 增量同步 XxlJob 入口
 * 这里的Prod表示生产型的源库，区别于PMC等虚拟BP系统的的源库
 * <p>
 * 在 XXL-JOB 调度中心配置 JobHandler 名称为: cdcSyncHandler
 *
 */
@Component
public class CdcSyncProdHandler {

    @Resource
    private CdcSyncOrchestrator cdcSyncOrchestrator;

    @XxlJob("cdcSyncProdHandler")
    public void sync() {
        // 无参
        // String param = XxlJobHelper.getJobParam();
        try {
            XxlJobHelper.log(">>>>>> 开始 CDC 增量同步 (全部源库 x 全部表)");
            cdcSyncOrchestrator.syncAllProd();
            XxlJobHelper.handleSuccess("CDC 增量同步执行完毕");
        } catch (Exception e) {
            XxlJobHelper.handleFail("CDC 增量同步异常: " + e.getMessage());
        }
    }

}