package com.xxl.job.executor.service;

import com.xxl.job.core.context.XxlJobHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Python 脚本执行服务
 * <p>
 * 使用独立部署的解释器（不在 /usr/xxl-job 备份范围内，见 python.interpreter-path），
 * 通过 ProcessBuilder 调用，实时回显日志到 XxlJobHelper。
 */
@Service
public class PythonExecutorService {
    private static final Logger log = LoggerFactory.getLogger(PythonExecutorService.class);

    @Value("${python.interpreter-path}")
    private String interpreterPath;

    @Value("${python.script-timeout-seconds}")
    private long timeoutSeconds;

    /**
     * 执行 python 脚本
     *
     * @param scriptPath .py 绝对路径
     * @param scriptArgs 脚本参数（如分片序号、分片总数）
     */
    public void runScript(String scriptPath, String... scriptArgs) throws Exception {

        // valid interpreter
        File interpreter = new File(interpreterPath);
        if (!interpreter.isFile()) {
            throw new IllegalStateException("Python 解释器不存在: " + interpreterPath);
        }
        File scriptFile = new File(scriptPath);
        if (!scriptFile.isFile()) {
            throw new IllegalStateException("Python 脚本不存在: " + scriptPath);
        }

        // build command
        List<String> command = new ArrayList<>();
        command.add(interpreterPath);
        command.add(scriptPath);
        if (scriptArgs != null) {
            command.addAll(Arrays.asList(scriptArgs));
        }

        XxlJobHelper.log(">>>>>> 开始执行 Python 脚本: {}, interpreter: {}", scriptPath, interpreterPath);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);                       // stdout/stderr 合并，避免顺序错乱
        pb.directory(scriptFile.getParentFile());            // 工作目录设为脚本所在目录，便于脚本内相对路径引用同目录资源
        pb.environment().put("PYTHONIOENCODING", "UTF-8");   // 避免中文输出在部分环境下乱码/异常
        pb.environment().put("PYTHONUNBUFFERED", "1");       // 关闭输出缓冲，保证日志实时性

        Process process = null;
        Thread readerThread = null;
        try {
            process = pb.start();
            Process finalProcess = process;

            // 实时读取输出并写入 XxlJobHelper 日志文件
            readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(finalProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        XxlJobHelper.log(line);
                    }
                } catch (IOException e) {
                    // process 被销毁时读取流可能抛出异常，属预期情况，仅记录不上抛
                    log.debug("Python 脚本输出流读取结束: {}", e.getMessage());
                }
            });
            readerThread.setName("xxl-job, PythonExecutorService-stdout-reader");
            readerThread.start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Python 脚本执行超时(" + timeoutSeconds + "s)，路径: " + scriptPath);
            }

            // 等待输出读取线程收尾，避免日志截断
            readerThread.join(5000);

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("Python 脚本执行失败，exitCode=" + exitCode + "，路径: " + scriptPath);
            }
            XxlJobHelper.log(">>>>>> Python 脚本执行成功: {}", scriptPath);
        } catch (Exception e) {
            log.error("执行 Python 脚本 [{}] 抛出异常: {}", scriptPath, e.getMessage(), e);
            throw e;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            if (readerThread != null && readerThread.isAlive()) {
                readerThread.interrupt();
            }
        }
    }

}