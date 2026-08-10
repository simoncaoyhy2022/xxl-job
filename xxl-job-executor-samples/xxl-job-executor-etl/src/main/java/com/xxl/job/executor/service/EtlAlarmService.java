package com.xxl.job.executor.service;

import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import com.xxl.job.executor.config.MailAccountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ETL 告警邮件服务
 */
@Service
public class EtlAlarmService {
    private static final Logger log = LoggerFactory.getLogger(EtlAlarmService.class);
    private static final String ALARM_RECIPIENT = "Simon_Cao@pmpgc.com";

    @Autowired
    private MailAccountUtil mailAccountUtil;

    /**
     * 构建基本信息 Map
     */
    public Map<String, String> getMailBasicInfo(String filePath) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("文件路径", filePath);
        return details;
    }

    /**
     * 异步发送 HTML 报警邮件
     */
    @Async
    public void sendAlarmMail(String subject, String title, Map<String, String> details, String logText) {
        try {
            StringBuilder content = new StringBuilder();
            content.append("<h2>").append(title).append("</h2>");

            if (details != null) {
                for (Map.Entry<String, String> entry : details.entrySet()) {
                    content.append("<p><strong>").append(entry.getKey()).append("：</strong>")
                            .append(entry.getValue()).append("</p>");
                }
            }

            content.append("<hr/>");
            content.append("<h3>运行日志详情：</h3>");
            content.append("<pre style='background-color:#ffebeb; color:#a30000; padding:10px; overflow-x:auto;'>")
                    .append(logText)
                    .append("</pre>");

            MailAccount mailAccount = mailAccountUtil.getMailAccount();
            MailUtil.send(mailAccount, ALARM_RECIPIENT, subject, content.toString(), true);
            log.info("告警邮件发送成功，接收人: {}", ALARM_RECIPIENT);
        } catch (Exception e) {
            log.error("告警邮件发送失败: {}", e.getMessage(), e);
        }
    }
}