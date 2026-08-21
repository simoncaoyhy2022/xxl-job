// xxl-job-executor-samples/xxl-job-executor-etl/src/main/java/com/xxl/job/executor/cdc/sync/CdcTableSyncHandler.java
package com.xxl.job.executor.cdc.sync;

import java.util.List;
import java.util.Map;

/**
 * 单张 CDC 表的落库处理器：将净变更行分发到对应 Mapper 的 upsert / delete
 */
public interface CdcTableSyncHandler {

    void upsert(String bp, List<Map<String, Object>> rows);

    void delete(List<Map<String, Object>> rows);

}