package com.xxl.job.executor.cdc.sync;

import com.xxl.job.executor.cdc.mapper.ProdOrdHdrMapper;
import com.xxl.job.executor.cdc.meta.CdcTableDef;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class ProdOrdHdrSyncHandler implements CdcTableSyncHandler {
    private final ProdOrdHdrMapper prodOrdHdrMapper;

    public ProdOrdHdrSyncHandler(ProdOrdHdrMapper prodOrdHdrMapper) {
        this.prodOrdHdrMapper = prodOrdHdrMapper;
    }

    @Override
    public CdcTableDef tableDef() {
        return CdcTableDef.PRODORDHDR;
    }

    @Override
    public int upsert(String bp, List<Map<String, Object>> rows) {
        List<Map<String, Object>> sortedRows = rows.stream()
                .sorted(Comparator.comparing(row -> String.valueOf(row.get("F_ID"))))
                .toList();
        prodOrdHdrMapper.upsertBatch(sortedRows);
        return sortedRows.size();
    }

    @Override
    public void delete(List<Map<String, Object>> rows) {
        prodOrdHdrMapper.deleteBatch(rows);
    }
}
