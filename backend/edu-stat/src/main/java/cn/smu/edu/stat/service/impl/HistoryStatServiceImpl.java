package cn.smu.edu.stat.service.impl;

import cn.smu.edu.stat.domain.vo.ClassDailyStatVO;
import cn.smu.edu.stat.domain.vo.ClassHistoryVO;
import cn.smu.edu.stat.domain.vo.DeptHistoryVO;
import cn.smu.edu.stat.domain.vo.DeptPeriodStatVO;
import cn.smu.edu.stat.service.HistoryStatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * {@link HistoryStatService} 的 ClickHouse 实现（S7-04）。
 *
 * <p><b>数据源选择：</b>聚合表 {@code lesson_stat_daily} 目前没有写入方
 * （S7-01 的消费者只写明细表 {@code lesson_event_log}），因此本实现直接对明细表
 * 按 {@code stat_date} 做逐日聚合。ClickHouse 适合这类列式聚合，且查询 WHERE 子句
 * 强制带分区键 {@code stat_date}（CLAUDE.md §7.4），命中分区裁剪、避免全表扫描。
 * 待后续补充 明细→日聚合 的定时任务后，可平滑切换到读 {@code lesson_stat_daily}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryStatServiceImpl implements HistoryStatService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * 逐日聚合：WHERE 第一个条件即分区键 stat_date 范围，命中分区裁剪。
     * countIf 按归一事件桶分别统计；uniqExactIf 统计去重活跃学生（排除 student_id=0 占位）。
     */
    private static final String DAILY_SQL = """
            SELECT toString(stat_date)                                AS stat_date,
                   uniqExact(lesson_id)                               AS lesson_count,
                   countIf(event_type = 'ATTEND')                     AS attend_count,
                   countIf(event_type = 'BARRAGE')                    AS barrage_count,
                   countIf(event_type = 'QUESTION')                   AS question_count,
                   countIf(event_type = 'SCORE')                      AS score_count,
                   countIf(event_type = 'SLIDE')                      AS slide_count,
                   uniqExactIf(student_id, student_id > 0)            AS active_student_cnt
            FROM lesson_event_log
            WHERE stat_date >= ? AND stat_date <= ? AND class_id = ?
            GROUP BY stat_date
            ORDER BY stat_date
            """;

    private final JdbcTemplate clickHouseJdbcTemplate;

    @Override
    public ClassHistoryVO classHistory(long classId, int days) {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(days - 1L);

        List<ClassDailyStatVO> daily = clickHouseJdbcTemplate.query(
                DAILY_SQL,
                ps -> {
                    ps.setDate(1, Date.valueOf(from));
                    ps.setDate(2, Date.valueOf(today));
                    ps.setLong(3, classId);
                },
                (rs, rowNum) -> new ClassDailyStatVO(
                        rs.getString("stat_date"),
                        rs.getLong("lesson_count"),
                        rs.getLong("attend_count"),
                        rs.getLong("barrage_count"),
                        rs.getLong("question_count"),
                        rs.getLong("score_count"),
                        rs.getLong("slide_count"),
                        rs.getLong("active_student_cnt")));

        log.info("班级历史统计查询完成: classId={}, days={}, 命中天数={}", classId, days, daily.size());
        return new ClassHistoryVO(classId, from.format(DATE_FMT), today.format(DATE_FMT), daily);
    }

    /**
     * period → ClickHouse 时间桶表达式。<b>白名单映射</b>：返回值是固定字面量、绝不含
     * 用户输入，可安全拼入 SQL（GROUP BY 无法参数化）。非法 period 兜底为 day。
     */
    private static String bucketExpr(String period) {
        return switch (period) {
            case "week" -> "toMonday(stat_date)";
            case "month" -> "toStartOfMonth(stat_date)";
            default -> "stat_date";
        };
    }

    @Override
    public DeptHistoryVO deptHistory(long deptId, String period, int days) {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(days - 1L);
        String bucket = bucketExpr(period);

        // bucket 来自白名单（bucketExpr），无注入风险；stat_date 范围作为分区键命中裁剪（§7.4）
        String sql = """
                SELECT toString(%1$s)                                  AS period_start,
                       uniqExact(lesson_id)                            AS lesson_count,
                       uniqExact(class_id)                             AS class_count,
                       countIf(event_type = 'ATTEND')                  AS attend_count,
                       countIf(event_type = 'BARRAGE')                 AS barrage_count,
                       countIf(event_type = 'QUESTION')                AS question_count,
                       countIf(event_type = 'SCORE')                   AS score_count,
                       countIf(event_type = 'SLIDE')                   AS slide_count,
                       uniqExactIf(student_id, student_id > 0)         AS active_student_cnt
                FROM lesson_event_log
                WHERE stat_date >= ? AND stat_date <= ? AND dept_id = ?
                GROUP BY period_start
                ORDER BY period_start
                """.formatted(bucket);

        List<DeptPeriodStatVO> buckets = clickHouseJdbcTemplate.query(
                sql,
                ps -> {
                    ps.setDate(1, Date.valueOf(from));
                    ps.setDate(2, Date.valueOf(today));
                    ps.setLong(3, deptId);
                },
                (rs, rowNum) -> new DeptPeriodStatVO(
                        rs.getString("period_start"),
                        rs.getLong("lesson_count"),
                        rs.getLong("class_count"),
                        rs.getLong("attend_count"),
                        rs.getLong("barrage_count"),
                        rs.getLong("question_count"),
                        rs.getLong("score_count"),
                        rs.getLong("slide_count"),
                        rs.getLong("active_student_cnt")));

        log.info("院系历史统计查询完成: deptId={}, period={}, days={}, 命中桶数={}",
                deptId, period, days, buckets.size());
        return new DeptHistoryVO(deptId, period, from.format(DATE_FMT), today.format(DATE_FMT), buckets);
    }
}
