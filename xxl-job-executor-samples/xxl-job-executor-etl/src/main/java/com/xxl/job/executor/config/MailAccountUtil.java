package com.xxl.job.executor.config;

import cn.hutool.extra.mail.MailAccount;
import org.springframework.stereotype.Component;

@Component
public class MailAccountUtil {
    // hutool mail account
    public MailAccount getMailAccount() {
        return new MailAccount()
                .setHost("smtp.mxhichina.com")
                .setPort(465)                                     // 使用 SSL 端口
                .setAuth(true)                                    // 必须开启认证
                .setFrom("******")                                // 发件人邮箱
                .setUser("******")                                // 登录用户名（完整邮箱）
                .setPass("******************")                    // 客户端授权码/密码
                .setSslEnable(true)                               // 启用 SSL 加密
                .setStarttlsEnable(false);                        // SSL 和 STARTTLS 二选一
    }
}
