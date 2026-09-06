package com.xxl.job.executor.cdc.meta;

/**
 * CDC 表元数据定义。
 */
public class CdcTableDef {
    private final String captureInstance;
    private final String[] columns;
    private final String[] pkColumns;
    private final String targetTable;

    public CdcTableDef(String captureInstance, String[] columns, String[] pkColumns, String targetTable) {
        this.captureInstance = captureInstance;
        this.columns = columns;
        this.pkColumns = pkColumns;
        this.targetTable = targetTable;
    }

    public String getCaptureInstance() {
        return captureInstance;
    }

    public String[] getColumns() {
        return columns;
    }

    public String[] getPkColumns() {
        return pkColumns;
    }

    public String getTargetTable() {
        return targetTable;
    }
}
