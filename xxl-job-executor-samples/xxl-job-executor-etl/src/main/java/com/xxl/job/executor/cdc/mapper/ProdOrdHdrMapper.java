package com.xxl.job.executor.cdc.mapper;

import java.util.List;
import java.util.Map;

public interface ProdOrdHdrMapper {
    int upsertProdOrdHdr(List<Map<String, Object>> rows);

    int deleteProdOrdHdr(String sourceId, List<String> ids);
}
