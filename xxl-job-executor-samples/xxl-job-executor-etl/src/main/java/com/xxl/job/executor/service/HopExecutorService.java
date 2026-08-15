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

    // Hop 元数据目录配置，数据库配置信息存放在此目录中，这与kettle不同
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

        // ✅ 直接实例化 LocalPipelineEngine，跳过了按 Run Configuration 选择/创建引擎的逻辑（不再需要预先在 metadata 目录配置 run-config），
        // 但 metadataProvider 仍用于解析数据库连接等其他元数据。
        LocalPipelineEngine pipeline = new LocalPipelineEngine(pipelineMeta);
        pipeline.setMetadataProvider(metadataProvider);
        pipeline.initializeFrom(variables);
        pipeline.setLogLevel(LOG_LEVEL);

        try {
            pipeline.prepareExecution();
            pipeline.startThreads();
            pipeline.waitUntilFinished();

            Result result = pipeline.getResult();
            // 抓取 Hop 内存中的运行日志并输出到 XXL-JOB 日志
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

        // 1. 构造 WorkflowMeta (参数顺序: variables, filename, metadataProvider)
        WorkflowMeta workflowMeta = new WorkflowMeta(variables, workflowPath, metadataProvider);

        // 2. 直接实例化 LocalWorkflowEngine 并注入依赖
        IWorkflowEngine<WorkflowMeta> workflow = new LocalWorkflowEngine(workflowMeta);
        workflow.setMetadataProvider(metadataProvider);
        workflow.initializeFrom(variables); // 继承/合并变量空间
        workflow.setLogLevel(LOG_LEVEL);

        try {
            // 3. startExecution 阻塞执行并直接返回 Result
            Result result = workflow.startExecution();

            // 抓取 Hop 内存中的运行日志并输出到 XXL-JOB 日志
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
        }
    }

    private IHopMetadataProvider buildMetadataProvider(IVariables variables) {
        return new JsonMetadataProvider(
                new HopTwoWayPasswordEncoder(),
                metadataFolder,
                variables
        );
    }

    /**
     * 获取 Hop 内存缓冲区中的运行日志
     */
    private String getHopLog(ILoggingObject loggingObject) {
        if (loggingObject == null) {
            return "";
        }
        String logChannelId = loggingObject.getLogChannelId();
        // 从 HopLogStore 的 Appender 中获取指定 Channel 的日志内容
        return HopLogStore.getAppender().getBuffer(logChannelId, false).toString();
    }
}