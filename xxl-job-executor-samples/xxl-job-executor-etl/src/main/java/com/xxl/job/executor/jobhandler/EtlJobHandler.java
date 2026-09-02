package com.xxl.job.executor.jobhandler;

import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.executor.service.HopExecutorService;
import com.xxl.job.executor.service.KettleExecutorService;
import com.xxl.job.executor.service.PythonExecutorService;
import com.xxl.job.executor.service.ScriptArtifactService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Kettle / Hop XXL-JOB 执行器 Handler
 */
@Component
public class EtlJobHandler {

    @Resource
    private KettleExecutorService kettleExecutorService;
    @Resource
    private HopExecutorService hopExecutorService;
    @Resource
    private ScriptArtifactService scriptArtifactService;
    @Resource
    private PythonExecutorService pythonExecutorService;

    /**
     * 1. 执行 Kettle Job (.kjb)
     * 在 XXL-JOB 调度中心配置 JobHandler 名称为: kettleJobHandler
     * 任务参数填入: .kjb 文件的绝对路径 (如: /data/kettle/jobs/test.kjb)
     */
    @XxlJob("kettleJobHandler")
    public void executeJob() {
        String jobPath = XxlJobHelper.getJobParam();
        if (jobPath == null || jobPath.trim().isEmpty()) {
            XxlJobHelper.handleFail("参数错误：Job 脚本路径不能为空！");
            return;
        }

        try {
            kettleExecutorService.runJob(resolveScript(jobPath, "KJB"));
            XxlJobHelper.handleSuccess("Kettle Job 执行完毕！");
        } catch (Exception e) {
            XxlJobHelper.handleFail("Kettle Job 执行异常: " + e.getMessage());
        }
    }

    /**
     * 2. 执行 Kettle Trans (.ktr)
     * 在 XXL-JOB 调度中心配置 JobHandler 名称为: kettleTransHandler
     * 任务参数填入: .ktr 文件的绝对路径 (如: /data/kettle/trans/test.ktr)
     */
    @XxlJob("kettleTransHandler")
    public void executeTrans() {
        String ktrPath = XxlJobHelper.getJobParam();
        if (ktrPath == null || ktrPath.trim().isEmpty()) {
            XxlJobHelper.handleFail("参数错误：Trans 脚本路径不能为空！");
            return;
        }

        try {
            kettleExecutorService.runKtr(resolveScript(ktrPath, "KTR"));
            XxlJobHelper.handleSuccess("Kettle Trans 执行完毕！");
        } catch (Exception e) {
            XxlJobHelper.handleFail("Kettle Trans 执行异常: " + e.getMessage());
        }
    }

    /**
     * 3. 执行 Hop Pipeline (.hpl)
     * 在 XXL-JOB 调度中心配置 JobHandler 名称为: hopPipelineHandler
     * 任务参数填入: .hpl 文件的绝对路径，或 scriptId=123
     */
    @XxlJob("hopPipelineHandler")
    public void executePipeline() {
        String pipelinePath = XxlJobHelper.getJobParam();
        if (pipelinePath == null || pipelinePath.trim().isEmpty()) {
            XxlJobHelper.handleFail("参数错误：Pipeline 脚本路径不能为空！");
            return;
        }

        try {
            hopExecutorService.runPipeline(resolveScript(pipelinePath, "HPL"));
            XxlJobHelper.handleSuccess("Hop Pipeline 执行完毕！");
        } catch (Exception e) {
            XxlJobHelper.handleFail("Hop Pipeline 执行异常: " + e.getMessage());
        }
    }

    /**
     * 4. 执行 Hop Workflow (.hwf)
     * 在 XXL-JOB 调度中心配置 JobHandler 名称为: hopWorkflowHandler
     * 任务参数填入: .hwf 文件的绝对路径，或 scriptId=123
     */
    @XxlJob("hopWorkflowHandler")
    public void executeWorkflow() {
        String workflowPath = XxlJobHelper.getJobParam();
        if (workflowPath == null || workflowPath.trim().isEmpty()) {
            XxlJobHelper.handleFail("参数错误：Workflow 脚本路径不能为空！");
            return;
        }

        try {
            hopExecutorService.runWorkflow(resolveScript(workflowPath, "HWF"));
            XxlJobHelper.handleSuccess("Hop Workflow 执行完毕！");
        } catch (Exception e) {
            XxlJobHelper.handleFail("Hop Workflow 执行异常: " + e.getMessage());
        }
    }

    /**
     * 5. 执行 Python 脚本
     * 在 XXL-JOB 调度中心配置 JobHandler 名称为: pythonJobHandler
     * 任务参数填入: scriptId=123（推荐），或 .py 文件的绝对路径（兼容旧任务）
     */
    @XxlJob("pythonJobHandler")
    public void executePython() {
        String pyPath = XxlJobHelper.getJobParam();
        if (pyPath == null || pyPath.trim().isEmpty()) {
            XxlJobHelper.handleFail("参数错误：Python 脚本路径不能为空！");
            return;
        }

        try {
            String resolved = resolveScript(pyPath, "PY");
            pythonExecutorService.runScript(
                    resolved,
                    String.valueOf(XxlJobContext.getXxlJobContext().getShardIndex()),
                    String.valueOf(XxlJobContext.getXxlJobContext().getShardTotal())
            );
            XxlJobHelper.handleSuccess("Python 脚本执行完毕！");
        } catch (Exception e) {
            XxlJobHelper.handleFail("Python 脚本执行异常: " + e.getMessage());
        }
    }

    /**
     * Preferred parameter: scriptId=123. A legacy absolute path is retained only for existing jobs.
     */
    private String resolveScript(String parameter, String subtype) throws Exception {
        String value = parameter.trim();
        if (value.startsWith("scriptId=")) {
            int scriptId = Integer.parseInt(value.substring("scriptId=".length()).trim());
            Path localFile = scriptArtifactService.resolve(scriptId, subtype);
            XxlJobHelper.log(">>>>>> 已同步脚本目录，入口脚本 {} -> {}", scriptId, localFile);
            return localFile.toString();
        }
        return value;
    }
}