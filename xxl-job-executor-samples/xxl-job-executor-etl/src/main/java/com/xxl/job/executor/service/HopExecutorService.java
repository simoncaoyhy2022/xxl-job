package com.xxl.job.executor.service;

import com.xxl.job.core.context.XxlJobHelper;
import org.apache.hop.core.Result;
import org.apache.hop.core.encryption.HopTwoWayPasswordEncoder;
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
import org.springframework.stereotype.Service;

@Service
public class HopExecutorService {
    private static final Logger log = LoggerFactory.getLogger(HopExecutorService.class);

    private static final LogLevel LOG_LEVEL = LogLevel.ERROR;

    /**
     * 执行 Hop Pipeline (.hpl)
     */
    public void runPipeline(String pipelinePath) throws Exception {
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        XxlJobHelper.log(">>>>>> 开始执行 Hop Pipeline: {}", pipelinePath);

        IVariables variables = Variables.getADefaultVariableSpace();

        IHopMetadataProvider metadataProvider = buildMetadataProvider(variables);

        PipelineMeta pipelineMeta = new PipelineMeta(pipelinePath, metadataProvider, variables);

        // ✅ 改为直接实例化 LocalPipelineEngine，不再依赖 metadataProvider 里的运行配置
        LocalPipelineEngine pipeline = new LocalPipelineEngine(pipelineMeta);
        pipeline.setMetadataProvider(metadataProvider);
        pipeline.initializeFrom(variables);
        pipeline.setLogLevel(LOG_LEVEL);

        try {
            pipeline.prepareExecution();
            pipeline.startThreads();
            pipeline.waitUntilFinished();

            Result result = pipeline.getResult();
            XxlJobHelper.log("Hop Pipeline 运行结果: nrErrors={}, status={}",
                    result.getNrErrors(), pipeline.getStatusDescription());

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

            XxlJobHelper.log("Hop Workflow 运行结果: nrErrors={}, status={}",
                    result.getNrErrors(), workflow.getStatusDescription());

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
        String projectMetadataFolder = "C:\\Dev\\kettle-hop\\metadata"; // 建议改为可配置项
        return new JsonMetadataProvider(
                new HopTwoWayPasswordEncoder(),
                projectMetadataFolder,
                variables
        );
    }
}