package com.xxl.job.executor.cdc.mapper;

import java.util.List;
import java.util.Map;

public interface SalesOrdHdrMapper {
    int upsertBatch(List<Map<String, Object>> list);

    int deleteBatch(List<Map<String, Object>> list);

}
