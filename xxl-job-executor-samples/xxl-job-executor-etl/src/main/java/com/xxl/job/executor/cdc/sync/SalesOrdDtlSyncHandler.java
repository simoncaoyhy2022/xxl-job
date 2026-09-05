package com.xxl.job.executor.cdc.sync;

import com.xxl.job.executor.cdc.mapper.SalesOrdDtlMapper;
import com.xxl.job.executor.cdc.meta.CdcTableDef;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class SalesOrdDtlSyncHandler implements CdcTableSyncHandler {
    private final SalesOrdDtlMapper salesOrdDtlMapper;

    public SalesOrdDtlSyncHandler(SalesOrdDtlMapper salesOrdDtlMapper) {
        this.salesOrdDtlMapper = salesOrdDtlMapper;
    }

    @Override
    public CdcTableDef tableDef() {
        return CdcTableDef.SALESORDDTL;
    }

    @Override
    public int upsert(String bp, List<Map<String, Object>> rows) {
        List<Map<String, Object>> filteredRows;
        if ("pmc".equals(bp)) {
            filteredRows = rows.stream()
                    .filter(row -> String.valueOf(row.get("F_SALESID")).startsWith("TPS"))
                    .peek(row -> row.put("F_ITEMID", String.valueOf(row.get("F_ITEMID")).trim().toUpperCase()))
                    .sorted(Comparator.comparing((Map<String, Object> row) -> String.valueOf(row.get("F_SALESID")))
                            .thenComparing(row -> String.valueOf(row.get("F_ITEMID"))))
                    .toList();
        } else {
            String sourceBp = bp.substring(0, 2).toUpperCase();
            filteredRows = rows.stream()
                    .filter(row -> Objects.equals(sourceBp, row.get("F_BPINV"))
                            && !String.valueOf(row.get("F_SALESID")).startsWith("TPS"))
                    .peek(row -> row.put("F_ITEMID", String.valueOf(row.get("F_ITEMID")).trim().toUpperCase()))
                    .sorted(Comparator.comparing((Map<String, Object> row) -> String.valueOf(row.get("F_SALESID")))
                            .thenComparing(row -> String.valueOf(row.get("F_ITEMID"))))
                    .toList();
        }
        if (filteredRows.isEmpty()) {
            return 0;
        }
        salesOrdDtlMapper.upsertBatch(filteredRows);
        return filteredRows.size();
    }

    @Override
    public void delete(List<Map<String, Object>> rows) {
        salesOrdDtlMapper.deleteBatch(rows);
    }
}
