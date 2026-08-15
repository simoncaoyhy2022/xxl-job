package com.xxl.job.executor.config;

import org.apache.hop.core.HopEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Apache Hop 环境初始化
 *
 * 与 KettleInitializer 不同：Hop 的 transform/action 均以独立插件 jar 存在，
 * 不能像 Kettle 那样从 classpath zip 解压后注册，fat jar 内嵌插件会导致
 * PluginRegistry 扫描失败。
 *
 * 部署要求：从官方发行包 apache-hop-client-x.x.x.zip 中取出 plugins/ 目录，
 * 整体拷贝到宿主机固定路径（如 /usr/xxl-job/hop-plugins），并在启动脚本中
 * 设置环境变量 HOP_PLUGIN_BASE_FOLDERS 指向该目录（多个目录用逗号分隔）。
 *
 * 例如 start-executor.sh 中：
 *   export HOP_PLUGIN_BASE_FOLDERS=/usr/xxl-job/hop-plugins
 */
@Component
public class HopInitializer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(HopInitializer.class);

    @Value("${hop.plugin.base-folders:}")
    private String pluginBaseFoldersProp;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (HopEnvironment.isInitialized()) {
            return;
        }

        String pluginBaseFolders = resolvePluginBaseFolders();
        if (pluginBaseFolders == null || pluginBaseFolders.isBlank()) {
            throw new IllegalStateException(
                    "Hop 插件目录未配置：请设置环境变量 HOP_PLUGIN_BASE_FOLDERS 或配置项 hop.plugin.base-folders，"
                            + "指向从官方发行包解压出的 plugins 目录");
        }
        for (String folder : pluginBaseFolders.split(",")) {
            File dir = new File(folder.trim());
            if (!dir.isDirectory()) {
                log.warn(">>>>>> Hop 插件目录不存在，请检查部署: {}", dir.getAbsolutePath());
            }
        }

        // Hop 在 init() 时从该环境变量/系统属性读取插件扫描目录
        if (System.getenv("HOP_PLUGIN_BASE_FOLDERS") == null) {
            System.setProperty("HOP_PLUGIN_BASE_FOLDERS", pluginBaseFolders);
        }

        log.info(">>>>>> 正在初始化 Apache Hop 环境, pluginBaseFolders={}", pluginBaseFolders);
        try {
            HopEnvironment.init();
        } catch (Exception e) {
            log.error(">>>>>> Apache Hop 环境初始化失败，请确认 HOP_PLUGIN_BASE_FOLDERS 指向的是"
                    + "完整的官方 plugins 目录（而非源码/空目录）", e);
            throw e;
        }
        log.info(">>>>>> Apache Hop 环境初始化完成");
    }

    private String resolvePluginBaseFolders() {
        String env = System.getenv("HOP_PLUGIN_BASE_FOLDERS");
        if (env != null && !env.isBlank()) {
            return env;
        }
        return pluginBaseFoldersProp;
    }
}