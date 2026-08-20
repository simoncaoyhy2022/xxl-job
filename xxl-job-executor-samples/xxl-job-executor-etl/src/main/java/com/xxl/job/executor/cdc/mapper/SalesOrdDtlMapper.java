package com.xxl.job.executor.cdc.mapper;

import java.util.List;
import java.util.Map;

public interface SalesOrdDtlMapper {
    int upsertBatch(List<Map<String, Object>> list);

    int deleteSalesOrdDtl(List<Map<String, Object>> list);

}
