package com.xxl.job.executor.service;

import com.xxl.job.core.context.XxlJobHelper;
import org.pentaho.di.core.logging.KettleLogStore;
import org.pentaho.di.core.logging.LogLevel;
import org.pentaho.di.core.logging.LoggingObjectInterface;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Kettle 核心脚本执行服务 (纯净版: 只保留 runJob 和 runKtr)
 */
@Service
public class KettleExecutorService {
    private static final Logger log = LoggerFactory.getLogger(KettleExecutorService.class);

    private static final LogLevel LOG_LEVEL = LogLevel.BASIC; // 可修改 MINIMAL。。。

    @Autowired
    private EtlAlarmService etlAlarmService;

    /**
     * 执行 Kettle Job (.kjb)
     */
    public void runJob(String jobPath) throws Exception {
        // 确保使用当前线程的 ClassLoader，避免 Spring Boot 打包后找不到 Kettle 内部类
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        XxlJobHelper.log(">>>>>> 开始执行 Kettle Job: {}", jobPath);
        Job job = null;
        try {
            JobMeta jobMeta = new JobMeta(jobPath, null);
            job = new Job(null, jobMeta);
            job.setLogLevel(LOG_LEVEL);
            job.setGatheringMetrics(false);

            job.start();
            job.waitUntilFinished();

            String kettleLogText = getKettleLog(job);
            XxlJobHelper.log("Kettle 运行日志:\n{}", kettleLogText);

            if (job.getErrors() > 0) {
                // Map<String, String> details = etlAlarmService.getMailBasicInfo(jobPath);
                // details.put("错误数量", String.valueOf(job.getErrors()));
                // etlAlarmService.sendAlarmMail("XXL-JOB-Kettle-Job-Error", "Kettle [Job] 执行失败", details, kettleLogText);

                throw new RuntimeException("Kettle [Job] 执行失败，错误数: " + job.getErrors() + "，路径: " + jobPath);
            } else {
                XxlJobHelper.log(">>>>>> Kettle Job 执行成功: {}", jobPath);
            }
        } catch (Exception e) {
            log.error("执行 Kettle Job [{}] 抛出异常: {}", jobPath, e.getMessage(), e);
            throw e;
        } finally {
            if (job != null) {
                // 清理 Kettle 日志缓冲区，防止 JVM 内存泄漏
                KettleLogStore.discardLines(job.getLogChannelId(), true);
            }
        }
    }

    /**
     * 执行 Kettle Trans (.ktr)
     */
    public void runKtr(String ktrPath) throws Exception {
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        XxlJobHelper.log(">>>>>> 开始执行 Kettle Trans: {}", ktrPath);
        Trans trans = null;
        try {
            TransMeta transMeta = new TransMeta(ktrPath);
            trans = new Trans(transMeta);
            trans.setLogLevel(LOG_LEVEL);
            trans.setGatheringMetrics(false);

            trans.execute(null);
            trans.waitUntilFinished();

            String kettleLogText = getKettleLog(trans);

            // 2. 打印到 Spring Boot 控制台 (SLF4J)
            // log.info("""
            //         ==================== Kettle 运行日志开始 ====================
            //         {}
            //         ==================== Kettle 运行日志结束 ====================""", kettleLogText);

            XxlJobHelper.log("Kettle 运行日志:\n{}", kettleLogText);

            if (trans.getErrors() > 0) {
                // Map<String, String> details = etlAlarmService.getMailBasicInfo(ktrPath);
                // details.put("错误数量", String.valueOf(trans.getErrors()));
                // etlAlarmService.sendAlarmMail("XXL-JOB-Kettle-Trans-Error", "Kettle [Trans] 执行失败", details, kettleLogText);

                throw new RuntimeException("Kettle [Trans] 执行失败，错误数: " + trans.getErrors() + "，路径: " + ktrPath);
            } else {
                XxlJobHelper.log(">>>>>> Kettle Trans 执行成功: {}", ktrPath);
            }
        } catch (Exception e) {
            log.error("执行 Kettle Ktr [{}] 抛出异常: {}", ktrPath, e.getMessage(), e);
            throw e;
        } finally {
            if (trans != null) {
                trans.cleanup();
                // 清理 Kettle 日志缓冲区，防止 JVM 内存泄漏
                KettleLogStore.discardLines(trans.getLogChannelId(), true);
            }
        }
    }

    /**
     * 获取 Kettle 内存缓冲区中的运行日志
     */
    private String getKettleLog(LoggingObjectInterface loggingObject) {
        if (loggingObject == null) return "";
        String logChannelId = loggingObject.getLogChannelId();
        return KettleLogStore.getAppender().getBuffer(logChannelId, false).toString();
    }
}