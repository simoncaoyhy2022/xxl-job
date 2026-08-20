package com.xxl.job.executor.cdc.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface SalesOrdHdrMapper {
    int upsertSalesOrdHdr(@Param("rows") List<Map<String, Object>> rows);

    int deleteSalesOrdHdr(@Param("BP") String BP, @Param("ids") List<String> ids);

}
