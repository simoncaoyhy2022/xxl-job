package com.xxl.job.executor.cdc.sync;

import com.xxl.job.executor.cdc.mapper.SalesOrdHdrMapper;
import com.xxl.job.executor.cdc.meta.CdcTableDef;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class SalesOrdHdrSyncHandler implements CdcTableSyncHandler {
    private final SalesOrdHdrMapper salesOrdHdrMapper;

    public SalesOrdHdrSyncHandler(SalesOrdHdrMapper salesOrdHdrMapper) {
        this.salesOrdHdrMapper = salesOrdHdrMapper;
    }

    @Override
    public CdcTableDef tableDef() {
        return CdcTableDef.SALESORDHDR;
    }

    @Override
    public int upsert(String bp, List<Map<String, Object>> rows) {
        List<Map<String, Object>> filteredRows;
        if ("pmc".equals(bp)) {
            filteredRows = rows.stream()
                    .filter(row -> "TPS".equals(row.get("F_CUSTLEVELID")))
                    .peek(row -> row.put("BP", String.valueOf(row.get("F_LEVELID")).substring(0, 2).concat("BP")))
                    .sorted(Comparator.comparing(row -> String.valueOf(row.get("F_ID"))))
                    .toList();
        } else {
            String levelId = bp.substring(0, 2).toUpperCase() + "*";
            filteredRows = rows.stream()
                    .filter(row -> !"TPS".equals(row.get("F_CUSTLEVELID"))
                            && Objects.equals(levelId, row.get("F_LEVELID")))
                    .sorted(Comparator.comparing(row -> String.valueOf(row.get("F_ID"))))
                    .toList();
        }
        if (filteredRows.isEmpty()) {
            return 0;
        }
        salesOrdHdrMapper.upsertBatch(filteredRows);
        return filteredRows.size();
    }

    @Override
    public void delete(List<Map<String, Object>> rows) {
        salesOrdHdrMapper.deleteBatch(rows);
    }
}
