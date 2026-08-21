package com.xxl.job.executor.cdc.service;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class CdcWatermarkService {

    public byte[] getLastLsn(DataSource ds, String captureInstance) {
        String sql = "SELECT last_lsn FROM etl_cdc_watermark WHERE capture_instance = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, captureInstance);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBytes(1) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("读取 watermark 失败, captureInstance=" + captureInstance, e);
        }
    }

    public void upsertLsn(DataSource ds, String captureInstance, byte[] lsn) {
        // SQL Server 2008 支持 MERGE；WITH (HOLDLOCK) 避免并发写同一行时的插入冲突
        String sql = """
                MERGE etl_cdc_watermark WITH (HOLDLOCK) AS target
                USING (SELECT ? AS capture_instance, ? AS last_lsn) AS src
                ON target.capture_instance = src.capture_instance
                WHEN MATCHED THEN UPDATE SET last_lsn = src.last_lsn, last_sync_time = GETDATE()
                WHEN NOT MATCHED THEN INSERT (capture_instance, last_lsn, last_sync_time)
                    VALUES (src.capture_instance, src.last_lsn, GETDATE());
                """;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, captureInstance);
            ps.setBytes(2, lsn);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("写入 watermark 失败, captureInstance=" + captureInstance, e);
        }
    }
}