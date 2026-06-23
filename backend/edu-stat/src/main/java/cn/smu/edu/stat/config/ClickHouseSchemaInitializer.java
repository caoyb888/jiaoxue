package cn.smu.edu.stat.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 启动时幂等创建 ClickHouse 三张统计表（CREATE TABLE IF NOT EXISTS）。
 *
 * <p>失败不阻断启动：dev/CI 环境若 ClickHouse 不可达，仅打印 WARN，
 * 业务消费者会在写入时再次报错并重试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClickHouseSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate clickHouseJdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            String sql = StreamUtils.copyToString(
                    new ClassPathResource("clickhouse/schema.sql").getInputStream(),
                    StandardCharsets.UTF_8);
            // 先逐行剔除 SQL 注释（-- 开头），再按 ; 切分，避免注释把整条语句吞掉
            String stripped = sql.lines()
                    .filter(line -> !line.trim().startsWith("--"))
                    .reduce("", (a, b) -> a + "\n" + b);
            Arrays.stream(stripped.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(stmt -> {
                        clickHouseJdbcTemplate.execute(stmt);
                        log.debug("ClickHouse DDL 执行成功: {}", stmt.lines().findFirst().orElse(""));
                    });
            log.info("ClickHouse 统计表初始化完成");
        } catch (Exception e) {
            log.warn("ClickHouse 统计表初始化失败（不阻断启动），稍后写入时将重试: {}", e.getMessage());
        }
    }
}
