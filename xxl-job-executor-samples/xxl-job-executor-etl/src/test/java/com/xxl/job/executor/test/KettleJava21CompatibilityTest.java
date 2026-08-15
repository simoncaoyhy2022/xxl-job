package com.xxl.job.executor.test;

import com.xxl.job.executor.EtlExecutorApplication;
import com.xxl.job.executor.service.HopExecutorService;
import com.xxl.job.executor.service.KettleExecutorService;
import org.apache.hop.core.HopEnvironment;
import org.apache.hop.core.database.DatabasePluginType;
import org.apache.hop.core.plugins.IPlugin;
import org.apache.hop.core.plugins.PluginRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.pentaho.di.core.KettleEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = EtlExecutorApplication.class)
public class KettleJava21CompatibilityTest {

    @Autowired
    private KettleExecutorService kettleExecutorService;

    @Autowired
    private HopExecutorService hopExecutorService;


    @Test
    public void testRunHopWorkflow(){
        String testHwfPath = "C:\\dev\\kettle-hop\\CS\\FG\\FG_freight_dtl.hwf";
        try {
            hopExecutorService.runWorkflow(testHwfPath);
            System.out.println("✅ .hwf 脚本在 Java 21 下运行成功！");
        } catch (Exception e) {
            System.err.println("❌ .hwf 脚本运行失败");
            e.printStackTrace();
        }
    }



    @Test
    public void  testRunHopPipeline() {
        // 替换为你本地测试用的 .hpl 文件绝对路径
        // String testHplPath = "C:\\dev\\kettle-hop\\CS\\FG\\main_sync_freight_dtl.hpl";
        String testHplPath = "C:\\dev\\kettle-hop\\CS\\FG\\update_AddTypeFromCRM.hpl";
        try {
            hopExecutorService.runPipeline(testHplPath);
            System.out.println("✅ .hpl 脚本在 Java 21 下运行成功！");
        } catch (Exception e) {
            System.err.println("❌ .hpl 脚本运行失败");
            e.printStackTrace();
        }
    }



    @BeforeAll
    public static void initHop() throws Exception {
        // 1. 强制在任何代码前设置插件目录
        System.setProperty("HOP_PLUGIN_BASE_FOLDERS", "/usr/xxl-job/hop-plugins");

        // 2. 手动触发初始化
        if (!HopEnvironment.isInitialized()) {
            HopEnvironment.init();
        }

        // 3. 打印当前注册的所有数据库插件（关键排查代码！）
        // List<IPlugin> dbPlugins = PluginRegistry.getInstance().getPlugins(DatabasePluginType.class);
        // System.out.println("====== 当前已加载的数据库插件列表 ======");
        // for (IPlugin p : dbPlugins) {
        //     System.out.println("Plugin ID: " + p.getIds()[0] + ", Name: " + p.getName());
        // }
        // System.out.println("======================================");
    }


    /**
     * 测试 1：验证 Kettle 环境在 Java 21 下能否成功初始化
     */
    @Test
    public void testKettleInit() {
        try {
            System.out.println("Java Version: " + System.getProperty("java.version"));
            if (!KettleEnvironment.isInitialized()) {
                KettleEnvironment.init();
            }
            System.out.println("✅ Kettle 9.3 在 Java 21 下初始化成功！");
        } catch (Exception e) {
            System.err.println("❌ Kettle 初始化失败，请检查是否添加了 --add-opens 参数");
            e.printStackTrace();
        }
    }

    /**
     * 测试 2：验证真实 .ktr 脚本能否正常运行
     */
    @Test
    public void testRunKtr() {
        testKettleInit();
        // 替换为你本地测试用的 .ktr 文件绝对路径
        String testKtrPath = "C:\\Users\\bugxi\\OneDrive\\kettle\\CS\\FG\\test.ktr";
        try {
            kettleExecutorService.runKtr(testKtrPath);
            System.out.println("✅ .ktr 脚本在 Java 21 下运行成功！");
        } catch (Exception e) {
            System.err.println("❌ .ktr 脚本运行失败");
            e.printStackTrace();
        }
    }

    /**
     * 测试 3：验证真实 .job 脚本能否正常运行
     */
    @Test
    public void testRunJob() {
        testKettleInit();
        // 替换为你本地测试用的 .ktr 文件绝对路径
        String testKtrPath = "C:\\Users\\bugxi\\OneDrive\\kettle\\CS\\FG\\FG_freight_dtl.kjb";
        try {
            kettleExecutorService.runJob(testKtrPath);
            System.out.println("✅ .Job 脚本在 Java 21 下运行成功！");
        } catch (Exception e) {
            System.err.println("❌ .Job 脚本运行失败");
            e.printStackTrace();
        }
    }
}