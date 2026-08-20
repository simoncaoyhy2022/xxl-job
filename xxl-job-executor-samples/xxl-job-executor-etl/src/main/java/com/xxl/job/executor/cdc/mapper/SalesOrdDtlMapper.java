package com.xxl.job.executor.cdc.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface SalesOrdDtlMapper {
    int upsertSalesOrdDtl(@Param("rows") List<Map<String, Object>> rows);

    int deleteSalesOrdDtl(@Param("BP") String BP, @Param("ids") List<String> ids);

}
