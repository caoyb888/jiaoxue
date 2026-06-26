package cn.smu.edu.stat.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-Job 执行器装配。
 *
 * <p>edu-stat 含 {@code teachingWarnCheck} handler（教学预警引擎，S7-06）；缺少
 * {@link XxlJobSpringExecutor} bean 时 handler 不会向调度中心注册、定时任务永不执行
 * （历史上 XXL-Job 执行器多次因漏装配而失效）。本类补齐装配。
 *
 * <p>调度中心地址/Token 来自 Nacos/环境变量，需在 18160 admin 配置 cron（如每日 23:30）
 * 触发 {@code teachingWarnCheck}。
 */
@Slf4j
@Configuration
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses:}")
    private String adminAddresses;

    @Value("${xxl.job.access-token:}")
    private String accessToken;

    @Value("${xxl.job.executor.appname:edu-stat}")
    private String appname;

    @Value("${xxl.job.executor.address:}")
    private String address;

    @Value("${xxl.job.executor.ip:}")
    private String ip;

    @Value("${xxl.job.executor.port:0}")
    private int port;

    @Value("${xxl.job.executor.logpath:/tmp/edu-xxl-job/jobhandler}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays:7}")
    private int logRetentionDays;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info(">>>>>>>>>>> xxl-job executor init: admin={}, appname={}, port={}",
                adminAddresses, appname, port);
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAccessToken(accessToken);
        executor.setAppname(appname);
        executor.setAddress(address);
        executor.setIp(ip);
        executor.setPort(port);
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(logRetentionDays);
        return executor;
    }
}
