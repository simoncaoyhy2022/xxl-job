package com.xxl.job.executor.cdc.meta;

import java.util.List;

/**
 * CDC 内置表定义注册表。
 */
public final class CdcTableRegistry {
    public static final CdcTableDef PRODORDHDR = new CdcTableDef(
            "dbo_t_prodordhdr",
            new String[]{"F_ID", "F_DOCDATE", "F_DOCTIME", "F_DUEDATE", "F_SALESID", "F_ITEMID", "F_PARTID", "F_DOCQTY", "F_DUEQTY",
                    "F_SHEETQTY", "F_RESQTY", "F_FREEQTY", "F_PAPERWID", "F_SHEETLEN", "F_SHEETWID", "F_LCUT", "F_WCUT", "F_JOINSHEET",
                    "F_EDITBY", "F_CONFBY", "F_ISCONF", "F_ISCLOSE", "F_CLOSETYPE", "F_CLOSEDATE", "F_RMRK", "F_UNITAREA", "F_ISOUT",
                    "F_UPCUT", "F_LOWCUT", "F_UPWID", "F_LOWWID", "F_PAPER", "F_FLUTEID", "F_BIANLIAO", "F_BUSHU", "F_BUSHUREASON",
                    "F_BPINV", "F_TRANSPORTEDTIME", "F_BOARDAREA", "F_LAYERTYPE", "F_ScoreType", "F_BIANLIAOPRODID", "F_BIANLIAOCOUNT",
                    "F_TechFlowID", "F_RMRK_CONVERTPROD", "F_PRODIDBYINVSET", "F_FeiLiao", "F_ReWork", "F_ReWorkProdID", "F_ExceedProdID",
                    "F_DIENUM", "F_CreatedBY", "F_TECHFLOWMAINID", "F_ScoreIsControledByMinVal", "F_WIPLen", "F_WIPWidth", "F_FLOWID",
                    "F_IsBPCorrugatorAccepted", "F_BPCorrugatorAcceptBy", "F_BPCorrugatorUploaded"},
            new String[]{"F_ID"},
            "t_prodordhdr");

    public static final CdcTableDef SALESORDHDR = new CdcTableDef(
            "dbo_t_salesordhdr",
            new String[]{"F_ID", "F_CUSTORDID", "F_DOCDATE", "CAST(F_DOCDATE AS DATE) AS F_Date", "F_CUSTID", "F_CreatedBY", "F_EDITBY", "F_CONFBY", "F_CloseBy", "F_ISCONF",
                    "F_ISCLOSE", "F_ITEMID", "F_DOCQTY", "F_TRDTYPE", "F_PRNVER", "F_TAXRATE", "F_CURRTYPE", "F_DRAWER", "F_LEVELID", "F_CUSTLEVELID"},
            new String[]{"F_ID"},
            "fact_sales_order_hdr");

    public static final CdcTableDef SALESORDDTL = new CdcTableDef(
            "dbo_t_salesorddtl",
            new String[]{"F_SALESID", "F_ITEMID", "F_DOCQTY", "F_UNITAMT", "F_BOARDLEN", "F_BOARDWID", "F_PRICEBOARDWID", "F_BPINV", "F_ISINV", "F_INVQTY"},
            new String[]{"F_SALESID", "F_ITEMID"},
            "fact_sales_order_dtl");

    // 工单和订单
    private static final List<CdcTableDef> ALL = List.of(PRODORDHDR, SALESORDHDR, SALESORDDTL);

    // todo:pmc数据库的几张维度表

    private CdcTableRegistry() {
    }

    public static List<CdcTableDef> all() {
        return ALL;
    }
}
