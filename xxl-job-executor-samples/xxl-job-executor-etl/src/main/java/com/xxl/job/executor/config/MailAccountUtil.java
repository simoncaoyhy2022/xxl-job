package com.xxl.job.executor.config;

import cn.hutool.extra.mail.MailAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MailAccountUtil {

    @Value("${etl.mail.host}")
    private String host;

    @Value("${etl.mail.port}")
    private int port;

    @Value("${etl.mail.from}")
    private String from;

    @Value("${etl.mail.user}")
    private String user;

    @Value("${etl.mail.pass}")
    private String pass;

    @Value("${etl.mail.ssl-enable:true}")
    private boolean sslEnable;

    @Value("${etl.mail.starttls-enable:false}")
    private boolean starttlsEnable;

    // hutool mail account
    public MailAccount getMailAccount() {
        return new MailAccount()
                .setHost(host)
                .setPort(port)
                .setAuth(true)                       // 必须开启认证
                .setFrom(from)                       // 发件人邮箱
                .setUser(user)                       // 登录用户名（完整邮箱）
                .setPass(pass)                       // 客户端授权码/密码
                .setSslEnable(sslEnable)             // 启用 SSL 加密
                .setStarttlsEnable(starttlsEnable);  // SSL 和 STARTTLS 二选一
    }
}