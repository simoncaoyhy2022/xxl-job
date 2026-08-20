package com.xxl.job.executor.cdc.service;

import com.xxl.job.executor.cdc.meta.CdcTableDef;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL Server CDC 净变更（net changes）抽取。
 * <p>
 * 使用 net changes 而非 all changes：同一主键在 [fromLsn, toLsn] 区间内的多次变更，
 * 会被 SQL Server 自动合并为一条最终结果（insert+update 合并为 insert，多次 update 合并为一次），
 * __$operation 只会是 1=delete、2=insert、4=update，不会出现 3（update 前镜像，net changes 不返回）。
 */
@Component
public class CdcExtractService {

    /**
     * 抽取 (fromLsn, toLsn] 区间内的净变更。
     *
     * @param fromLsn 上次同步水位；首次同步传 null，内部会 fallback 到该表的 min_lsn
     * @param toLsn   本次同步截止水位，通常是本次任务开始时取的 sys.fn_cdc_get_max_lsn()
     */
    public List<Map<String, Object>> fetchNetChanges(DataSource ds, CdcTableDef def,
                                                     byte[] fromLsn, byte[] toLsn) {
        byte[] effectiveFrom = fromLsn;
        try (Connection c = ds.getConnection()) {

            // 首次同步：没有水位记录，从该 capture instance 的最小可用 LSN 开始
            if (effectiveFrom == null) {
                effectiveFrom = getMinLsn(c, def.getCaptureInstance());
                if (effectiveFrom == null) {
                    // 该 capture instance 尚无任何可用变更（刚开启 CDC 或日志已被清理），本次无数据
                    return new ArrayList<>();
                }
            }

            // from > to：说明 toLsn 是在极短时间窗口内取的、日志还没推进，直接返回空，避免触发函数报错
            if (Arrays.compare(effectiveFrom, toLsn) > 0) {
                return new ArrayList<>();
            }

            String cols = String.join(",", def.getColumns());
            String sql = "SELECT [__$operation], " + cols +
                    " FROM cdc.fn_cdc_get_net_changes_" + def.getCaptureInstance() +
                    "(?, ?, 'all with mask')";

            List<Map<String, Object>> result = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setBytes(1, effectiveFrom);
                ps.setBytes(2, toLsn);
                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= meta.getColumnCount(); i++) {
                            row.put(meta.getColumnLabel(i), rs.getObject(i));
                        }
                        result.add(row);
                    }
                }
            }
            return result;

        } catch (SQLException e) {
            throw new RuntimeException("抽取净变更失败, captureInstance=" + def.getCaptureInstance(), e);
        }
    }

    /**
     * 指定 capture instance 当前可用的最小 LSN（该表 CDC 变更日志的起点，随日志清理任务向前推进）。
     * 返回 null 表示该 capture instance 尚无任何变更记录。
     */
    public byte[] getMinLsn(DataSource ds, String captureInstance) {
        try (Connection c = ds.getConnection()) {
            return getMinLsn(c, captureInstance);
        } catch (SQLException e) {
            throw new RuntimeException("获取 min_lsn 失败, captureInstance=" + captureInstance, e);
        }
    }

    private byte[] getMinLsn(Connection c, String captureInstance) throws SQLException {
        String sql = "SELECT sys.fn_cdc_get_min_lsn(?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, captureInstance);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBytes(1) : null;
            }
        }
    }

    /**
     * 整个数据库当前的最大 LSN（跨所有 capture instance 共用，日志越晚越大）。
     * 通常在任务开始时取一次，作为本轮同步的 toLsn。
     */
    public byte[] getMaxLsn(DataSource ds) {
        String sql = "SELECT sys.fn_cdc_get_max_lsn()";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getBytes(1) : null;
        } catch (SQLException e) {
            throw new RuntimeException("获取 max_lsn 失败", e);
        }
    }
}