package com.xxl.job.executor.test;

import com.xxl.job.executor.EtlExecutorApplication;
import com.xxl.job.executor.service.HopExecutorService;
import com.xxl.job.executor.service.KettleExecutorService;
import org.junit.jupiter.api.Test;
import org.pentaho.di.core.KettleEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = EtlExecutorApplication.class)
public class KettleJava21CompatibilityTest {

    @Autowired
    private KettleExecutorService kettleExecutorService;


    @Autowired
    private HopExecutorService hopExecutorService;



    @Test
    public void  testRunHopPipeline() {
        // 替换为你本地测试用的 .hpl 文件绝对路径
        String testHplPath = "C:\\dev\\kettle-hop\\CS\\FG\\main_sync_freight_dtl.hpl";
        try {
            hopExecutorService.runPipeline(testHplPath);
            System.out.println("✅ .hpl 脚本在 Java 21 下运行成功！");
        } catch (Exception e) {
            System.err.println("❌ .hpl 脚本运行失败");
            e.printStackTrace();
        }
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