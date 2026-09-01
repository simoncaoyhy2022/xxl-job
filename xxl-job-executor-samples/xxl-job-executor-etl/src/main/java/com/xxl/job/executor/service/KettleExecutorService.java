package com.xxl.job.executor.service;

import com.xxl.job.core.context.XxlJobHelper;
import jakarta.annotation.Resource;
import org.pentaho.di.core.logging.KettleLogStore;
import org.pentaho.di.core.logging.LogLevel;
import org.pentaho.di.core.logging.LoggingObjectInterface;
import org.pentaho.di.core.logging.LoggingRegistry;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Kettle 核心脚本执行服务 (加固版: 深度资源释放与防泄漏)
 */
@Service
public class KettleExecutorService {
    private static final Logger log = LoggerFactory.getLogger(KettleExecutorService.class);

    private static final LogLevel LOG_LEVEL = LogLevel.BASIC;

    @Resource
    private EtlAlarmService etlAlarmService;

    /**
     * 执行 Kettle Job (.kjb)
     */
    public void runJob(String jobPath) throws Exception {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        XxlJobHelper.log(">>>>>> 开始执行 Kettle Job: {}", jobPath);
        Job job = null;
        String logChannelId = null;
        try {
            JobMeta jobMeta = new JobMeta(jobPath, null);
            job = new Job(null, jobMeta);
            logChannelId = job.getLogChannelId(); // 记录 Channel ID
            job.setLogLevel(LOG_LEVEL);
            job.setGatheringMetrics(false);

            job.start();
            job.waitUntilFinished();

            String kettleLogText = getKettleLog(job);
            XxlJobHelper.log("Kettle 运行日志:\n{}", kettleLogText);

            if (job.getErrors() > 0) {
                throw new RuntimeException("Kettle [Job] 执行失败，错误数: " + job.getErrors() + "，路径: " + jobPath);
            } else {
                XxlJobHelper.log(">>>>>> Kettle Job 执行成功: {}", jobPath);
            }
        } catch (Exception e) {
            log.error("执行 Kettle Job [{}] 抛出异常: {}", jobPath, e.getMessage(), e);
            throw e;
        } finally {
            // 1. 释放 Job 相关资源
            if (job != null) {
                job.eraseParameters(); // 清空参数
            }
            // 2. 彻底清理日志缓存与全局注册表 (断开 GC Root 引用)
            if (logChannelId != null) {
                KettleLogStore.discardLines(logChannelId, true);
                LoggingRegistry.getInstance().removeIncludingChildren(logChannelId);
            }
            // 3. 还原线程上下文类加载器
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    /**
     * 执行 Kettle Trans (.ktr)
     */
    public void runKtr(String ktrPath) throws Exception {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        XxlJobHelper.log(">>>>>> 开始执行 Kettle Trans: {}", ktrPath);
        Trans trans = null;
        String logChannelId = null;
        try {
            TransMeta transMeta = new TransMeta(ktrPath);
            trans = new Trans(transMeta);
            logChannelId = trans.getLogChannelId(); // 记录 Channel ID
            trans.setLogLevel(LOG_LEVEL);
            trans.setGatheringMetrics(false);

            trans.execute(null);
            trans.waitUntilFinished();

            String kettleLogText = getKettleLog(trans);
            XxlJobHelper.log("Kettle 运行日志:\n{}", kettleLogText);

            if (trans.getErrors() > 0) {
                throw new RuntimeException("Kettle [Trans] 执行失败，错误数: " + trans.getErrors() + "，路径: " + ktrPath);
            } else {
                XxlJobHelper.log(">>>>>> Kettle Trans 执行成功: {}", ktrPath);
            }
        } catch (Exception e) {
            log.error("执行 Kettle Ktr [{}] 抛出异常: {}", ktrPath, e.getMessage(), e);
            throw e;
        } finally {
            // 1. 释放 Trans 步骤与内部连接资源
            if (trans != null) {
                trans.cleanup();
                trans.eraseParameters();
            }
            // 2. 彻底清理日志缓存与全局注册表 (断开 GC Root 引用)
            if (logChannelId != null) {
                KettleLogStore.discardLines(logChannelId, true);
                LoggingRegistry.getInstance().removeIncludingChildren(logChannelId);
            }
            // 3. 还原线程上下文类加载器
            Thread.currentThread().setContextClassLoader(originalClassLoader);
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