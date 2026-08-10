package com.xxl.job.executor.config;

import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.plugins.PluginFolder;
import org.pentaho.di.core.plugins.StepPluginType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Kettle 环境全局初始化配置
 * 插件目录以 zip 形式打包在 jar 内部，首次启动时自动解压到本地磁盘，
 * 后续启动检测到已解压则直接复用，避免每次都重复解压。
 */
@Component
public class KettleInitializer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(KettleInitializer.class);

    // classpath 下打包的插件 zip
    private static final String PLUGIN_ZIP_CLASSPATH = "kettle-plugins/pdi-core-plugins.zip";
    // 解压后标记文件，用于判断是否已经解压完成
    private static final String EXTRACT_MARKER = ".extracted";

    // 解压目标目录，默认放在系统临时目录下；可通过配置覆盖到指定路径
    @Value("${kettle.plugin.extract-dir:${java.io.tmpdir}/xxl-job-kettle-plugins}")
    private String extractBaseDir;

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            if (!KettleEnvironment.isInitialized()) {
                log.info(">>>>>> 正在初始化 Kettle 环境...");

                File pluginDir = ensurePluginExtracted();
                if (pluginDir != null) {
                    StepPluginType.getInstance().getPluginFolders()
                            .add(new PluginFolder(pluginDir.getAbsolutePath(), false, true));
                    log.info(">>>>>> 已注册 Kettle 插件目录: {}", pluginDir.getAbsolutePath());
                } else {
                    log.warn(">>>>>> 未能准备 Kettle 插件目录，部分特殊步骤(如阻塞步骤)可能无法运行");
                }

                KettleEnvironment.init();
                log.info(">>>>>> Kettle 环境初始化完成");
            }
        } catch (Exception e) {
            log.error(">>>>>> Kettle 环境初始化失败", e);
            throw e;
        }
    }

    /**
     * 确保插件已解压到本地目录，返回解压后 pdi-core-plugins 文件夹路径
     */
    private File ensurePluginExtracted() {
        Path targetDir = Paths.get(extractBaseDir, "pdi-core-plugins");
        Path markerFile = targetDir.resolve(EXTRACT_MARKER);

        if (Files.exists(markerFile)) {
            log.info(">>>>>> 检测到 Kettle 插件已解压，直接复用: {}", targetDir);
            return targetDir.toFile();
        }

        ClassPathResource resource = new ClassPathResource(PLUGIN_ZIP_CLASSPATH);
        if (!resource.exists()) {
            log.error(">>>>>> classpath 下未找到插件包: {}", PLUGIN_ZIP_CLASSPATH);
            return null;
        }

        log.info(">>>>>> 首次运行，正在解压 Kettle 插件到: {}", targetDir);
        try {
            Files.createDirectories(targetDir);
            try (InputStream is = resource.getInputStream();
                 ZipInputStream zis = new ZipInputStream(is)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    // zip 内部第一层目录就是 pdi-core-plugins/，去掉这一层前缀直接解到 targetDir 下
                    String entryName = stripLeadingFolder(entry.getName());
                    if (entryName.isEmpty()) {
                        continue;
                    }
                    Path outPath = targetDir.resolve(entryName).normalize();

                    // 防止 zip slip 攻击：确保解压路径在目标目录内
                    if (!outPath.startsWith(targetDir)) {
                        throw new IOException("非法的压缩包条目: " + entry.getName());
                    }

                    if (entry.isDirectory()) {
                        Files.createDirectories(outPath);
                    } else {
                        Files.createDirectories(outPath.getParent());
                        Files.copy(zis, outPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                    zis.closeEntry();
                }
            }
            // 写入标记文件，表示解压完成
            Files.createFile(markerFile);
            log.info(">>>>>> Kettle 插件解压完成: {}", targetDir);
            return targetDir.toFile();
        } catch (IOException e) {
            log.error(">>>>>> 解压 Kettle 插件失败", e);
            return null;
        }
    }

    /**
     * 去掉 zip 条目名里的第一层目录前缀（即压缩包里最外层的 pdi-core-plugins/）
     */
    private String stripLeadingFolder(String entryName) {
        String normalized = entryName.replace('\\', '/');
        int idx = normalized.indexOf('/');
        if (idx < 0) {
            return "";
        }
        return normalized.substring(idx + 1);
    }
}