package cn.smu.edu.stat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * ClickHouse 连接配置。
 *
 * <p>ClickHouse 仅用于统计分析（OLAP），与主库 MySQL 完全隔离。
 *
 * <p><b>注意：</b>这里<em>不</em>暴露 {@code DataSource} 类型的 Bean——否则
 * Spring Boot 的 {@code DataSourceAutoConfiguration}（{@code @ConditionalOnMissingBean(DataSource.class)}）
 * 会回退，导致 MyBatis-Plus 依赖的主库 MySQL 数据源不再自动装配。
 * 因此 ClickHouse 的 {@link DriverManagerDataSource} 在 {@link #clickHouseJdbcTemplate()}
 * 内部构造，仅以 {@code clickHouseJdbcTemplate} 这一个 Bean 对外提供。
 */
@Configuration
public class ClickHouseConfig {

    @Value("${clickhouse.url}")
    private String url;

    @Value("${clickhouse.username:default}")
    private String username;

    @Value("${clickhouse.password:}")
    private String password;

    @Bean(name = "clickHouseJdbcTemplate")
    public JdbcTemplate clickHouseJdbcTemplate() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        return new JdbcTemplate(ds);
    }
}
