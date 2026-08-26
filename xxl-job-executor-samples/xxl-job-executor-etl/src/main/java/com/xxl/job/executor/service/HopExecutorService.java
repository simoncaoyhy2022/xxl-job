package com.xxl.job.executor.service;

import com.xxl.job.core.context.XxlJobHelper;
import org.apache.hop.core.Result;
import org.apache.hop.core.encryption.HopTwoWayPasswordEncoder;
import org.apache.hop.core.logging.HopLogStore;
import org.apache.hop.core.logging.ILoggingObject;
import org.apache.hop.core.logging.LogLevel;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.variables.Variables;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.metadata.serializer.json.JsonMetadataProvider;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.engines.local.LocalPipelineEngine;
import org.apache.hop.workflow.WorkflowMeta;
import org.apache.hop.workflow.engine.IWorkflowEngine;
import org.apache.hop.workflow.engines.local.LocalWorkflowEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HopExecutorService {
    private static final Logger log = LoggerFactory.getLogger(HopExecutorService.class);

    private static final LogLevel LOG_LEVEL = LogLevel.BASIC;

    @Value("${hop.metadata.base-folders}")
    private String metadataFolder;

    /**
     * 执行 Hop Pipeline (.hpl)
     */
    public void runPipeline(String pipelinePath) throws Exception {
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        XxlJobHelper.log(">>>>>> 开始执行 Hop Pipeline: {}", pipelinePath);

        IVariables variables = Variables.getADefaultVariableSpace();
        IHopMetadataProvider metadataProvider = buildMetadataProvider(variables);
        PipelineMeta pipelineMeta = new PipelineMeta(pipelinePath, metadataProvider, variables);

        LocalPipelineEngine pipeline = new LocalPipelineEngine(pipelineMeta);
        pipeline.setMetadataProvider(metadataProvider);
        pipeline.initializeFrom(variables);
        pipeline.setLogLevel(LOG_LEVEL);

        String logChannelId = null;
        try {
            logChannelId = pipeline.getLogChannelId();
            pipeline.prepareExecution();
            pipeline.startThreads();
            pipeline.waitUntilFinished();

            Result result = pipeline.getResult();
            // 获取日志并输出
            String hopLogText = getHopLog(pipeline);
            XxlJobHelper.log("Hop 运行日志:\n{}", hopLogText);

            if (result.getNrErrors() > 0) {
                throw new RuntimeException("Hop [Pipeline] 执行失败，错误数: " + result.getNrErrors() + "，路径: " + pipelinePath);
            } else {
                XxlJobHelper.log(">>>>>> Hop Pipeline 执行成功: {}", pipelinePath);
            }
        } catch (Exception e) {
            log.error("执行 Hop Pipeline [{}] 抛出异常: {}", pipelinePath, e.getMessage(), e);
            throw e;
        } finally {
            // 1. 释放 Pipeline 内部算子和行集资源
            try {
                pipeline.cleanup();
            } catch (Throwable t) {
                log.warn("释放 Hop Pipeline 资源异常（已忽略）: {}", t.getMessage());
            }
            // 2. 独立清除全局日志单例缓存，防止静态 Map 内存泄漏
            if (logChannelId != null) {
                try {
                    HopLogStore.discardLines(logChannelId, true);
                } catch (Throwable t) {
                    log.warn("清除 Hop LogChannel 缓存失败（已忽略）: {}", t.getMessage());
                }
            }
        }
    }

    /**
     * 执行 Hop Workflow (.hwf)
     */
    public void runWorkflow(String workflowPath) throws Exception {
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        XxlJobHelper.log(">>>>>> 开始执行 Hop Workflow: {}", workflowPath);

        IVariables variables = Variables.getADefaultVariableSpace();
        IHopMetadataProvider metadataProvider = buildMetadataProvider(variables);

        WorkflowMeta workflowMeta = new WorkflowMeta(variables, workflowPath, metadataProvider);

        IWorkflowEngine<WorkflowMeta> workflow = new LocalWorkflowEngine(workflowMeta);
        workflow.setMetadataProvider(metadataProvider);
        workflow.initializeFrom(variables);
        workflow.setLogLevel(LOG_LEVEL);

        String logChannelId = null;
        try {
            logChannelId = workflow.getLogChannelId();
            Result result = workflow.startExecution();

            // 获取日志并输出
            String hopLogText = getHopLog(workflow);
            XxlJobHelper.log("Hop 运行日志:\n{}", hopLogText);

            if (result.getNrErrors() > 0) {
                throw new RuntimeException("Hop [Workflow] 执行失败，错误数: " + result.getNrErrors() + "，路径: " + workflowPath);
            } else {
                XxlJobHelper.log(">>>>>> Hop Workflow 执行成功: {}", workflowPath);
            }
        } catch (Exception e) {
            log.error("执行 Hop Workflow [{}] 抛出异常: {}", workflowPath, e.getMessage(), e);
            throw e;
        } finally {
            // Workflow 核心清理：清除日志通道缓存，防止内存泄漏
            if (logChannelId != null) {
                try {
                    HopLogStore.discardLines(logChannelId, true);
                } catch (Throwable t) {
                    log.warn("清除 Hop LogChannel 缓存失败（已忽略）: {}", t.getMessage());
                }
            }
        }
    }

    private IHopMetadataProvider buildMetadataProvider(IVariables variables) {
        return new JsonMetadataProvider(
                new HopTwoWayPasswordEncoder(),
                metadataFolder,
                variables
        );
    }

    private String getHopLog(ILoggingObject loggingObject) {
        if (loggingObject == null) {
            return "";
        }
        String logChannelId = loggingObject.getLogChannelId();
        return HopLogStore.getAppender().getBuffer(logChannelId, false).toString();
    }
}