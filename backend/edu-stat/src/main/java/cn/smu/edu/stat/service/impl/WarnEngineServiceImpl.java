package cn.smu.edu.stat.service.impl;

import cn.smu.edu.stat.domain.entity.WarnEvent;
import cn.smu.edu.stat.repository.WarnEventMapper;
import cn.smu.edu.stat.service.WarnEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link WarnEngineService} 的 ClickHouse + MySQL 实现（S7-06）。
 *
 * <p>读 ClickHouse {@code lesson_event_log}（查询 WHERE 均带 {@code stat_date} 分区键，§7.4），
 * 评估阈值后将命中预警按去重键批量 upsert 进 MySQL {@code warn_event}（禁止循环单条 INSERT）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarnEngineServiceImpl implements WarnEngineService {

    // ── 阈值（后续可迁 Nacos 配置）──────────────────────────────────────────
    /** 低考勤阈值：去重签到学生数低于此值（且 > 0）触发。 */
    static final int LOW_ATTEND_THRESHOLD = 15;
    /** 频繁缺席回溯窗口（天）。 */
    static final int ABSENCE_WINDOW_DAYS = 7;
    /** 频繁缺席最小课次：窗口内班级总课次达到此值才评估，避免课次过少误报。 */
    static final int ABSENCE_MIN_LESSONS = 3;
    /** 频繁缺席出勤率下限：出勤课次/总课次低于此比例触发。 */
    static final double ABSENCE_RATIO = 0.6;

    static final String TYPE_LOW_ATTEND = "LOW_ATTEND";
    static final String TYPE_ZERO_ACTIVE = "ZERO_ACTIVE";
    static final String TYPE_FREQUENT_ABSENCE = "FREQUENT_ABSENCE";
    static final String TARGET_LESSON = "LESSON";
    static final String TARGET_STUDENT = "STUDENT";

    private static final int UPSERT_BATCH = 500;

    /** 课堂级聚合：WHERE stat_date = ? 命中单分区。 */
    private static final String LESSON_AGG_SQL = """
            SELECT lesson_id,
                   any(class_id)                                                    AS class_id,
                   any(dept_id)                                                     AS dept_id,
                   any(teacher_id)                                                  AS teacher_id,
                   uniqExactIf(student_id, event_type = 'ATTEND' AND student_id > 0) AS attend_students,
                   countIf(event_type IN ('BARRAGE', 'QUESTION', 'SCORE', 'SLIDE')) AS active_count
            FROM lesson_event_log
            WHERE stat_date = ?
            GROUP BY lesson_id
            """;

    /** 窗口内各班级总课次（按签到事件计）。 */
    private static final String CLASS_TOTAL_SQL = """
            SELECT class_id, uniqExact(lesson_id) AS total_lessons
            FROM lesson_event_log
            WHERE stat_date >= ? AND stat_date <= ? AND event_type = 'ATTEND' AND class_id > 0
            GROUP BY class_id
            """;

    /** 窗口内各学生在各班级的出勤课次。 */
    private static final String STUDENT_ATTEND_SQL = """
            SELECT class_id, student_id,
                   any(dept_id)         AS dept_id,
                   uniqExact(lesson_id) AS attended_lessons
            FROM lesson_event_log
            WHERE stat_date >= ? AND stat_date <= ? AND event_type = 'ATTEND'
              AND student_id > 0 AND class_id > 0
            GROUP BY class_id, student_id
            """;

    private final JdbcTemplate clickHouseJdbcTemplate;
    private final WarnEventMapper warnEventMapper;

    @Override
    public int runCheck(LocalDate statDate) {
        List<WarnEvent> warnings = new ArrayList<>();
        warnings.addAll(lessonLevelWarnings(statDate));
        warnings.addAll(absenceWarnings(statDate));

        if (warnings.isEmpty()) {
            log.info("教学预警检查完成: statDate={}, 无命中", statDate);
            return 0;
        }
        int affected = 0;
        for (int i = 0; i < warnings.size(); i += UPSERT_BATCH) {
            affected += warnEventMapper.upsertBatch(
                    warnings.subList(i, Math.min(i + UPSERT_BATCH, warnings.size())));
        }
        log.info("教学预警检查完成: statDate={}, 命中预警={}, upsert受影响行={}",
                statDate, warnings.size(), affected);
        return affected;
    }

    /** 低考勤 + 零活跃（课堂级，针对 statDate 当天）。 */
    private List<WarnEvent> lessonLevelWarnings(LocalDate statDate) {
        List<WarnEvent> result = new ArrayList<>();
        clickHouseJdbcTemplate.query(LESSON_AGG_SQL, ps -> ps.setDate(1, Date.valueOf(statDate)),
                (RowCallbackHandler) rs -> {
            long lessonId = rs.getLong("lesson_id");
            long classId = rs.getLong("class_id");
            long deptId = rs.getLong("dept_id");
            long teacherId = rs.getLong("teacher_id");
            int attendStudents = rs.getInt("attend_students");
            long activeCount = rs.getLong("active_count");

            // 无人签到的课堂不评估（无意义）
            if (attendStudents <= 0) {
                return;
            }
            if (attendStudents < LOW_ATTEND_THRESHOLD) {
                result.add(lessonWarn(TYPE_LOW_ATTEND, lessonId, classId, deptId, teacherId, statDate,
                        attendStudents, LOW_ATTEND_THRESHOLD,
                        "课堂签到人数 " + attendStudents + " 低于阈值 " + LOW_ATTEND_THRESHOLD));
            }
            if (activeCount == 0) {
                result.add(lessonWarn(TYPE_ZERO_ACTIVE, lessonId, classId, deptId, teacherId, statDate,
                        0, 1, "课堂有签到但零互动（无弹幕/提问/加分/翻页）"));
            }
        });
        return result;
    }

    /** 频繁缺席（学生级，回溯 {@link #ABSENCE_WINDOW_DAYS} 天）。 */
    private List<WarnEvent> absenceWarnings(LocalDate statDate) {
        LocalDate from = statDate.minusDays(ABSENCE_WINDOW_DAYS - 1L);

        Map<Long, Integer> classTotal = new HashMap<>();
        clickHouseJdbcTemplate.query(CLASS_TOTAL_SQL, ps -> {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(statDate));
        }, (RowCallbackHandler) rs -> classTotal.put(rs.getLong("class_id"), rs.getInt("total_lessons")));

        List<WarnEvent> result = new ArrayList<>();
        clickHouseJdbcTemplate.query(STUDENT_ATTEND_SQL, ps -> {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(statDate));
        }, (RowCallbackHandler) rs -> {
            long classId = rs.getLong("class_id");
            long studentId = rs.getLong("student_id");
            long deptId = rs.getLong("dept_id");
            int attended = rs.getInt("attended_lessons");

            Integer total = classTotal.get(classId);
            if (total == null || total < ABSENCE_MIN_LESSONS) {
                return;
            }
            int required = (int) Math.ceil(total * ABSENCE_RATIO);
            if (attended < required) {
                WarnEvent w = WarnEvent.builder()
                        .warnType(TYPE_FREQUENT_ABSENCE)
                        .targetType(TARGET_STUDENT)
                        .targetId(studentId)
                        .classId(classId)
                        .deptId(deptId)
                        .statDate(statDate)
                        .metricValue(attended)
                        .thresholdValue(required)
                        .detail("近 " + ABSENCE_WINDOW_DAYS + " 天出勤 " + attended + "/" + total
                                + " 次，低于应到 " + required + " 次")
                        .build();
                result.add(w);
            }
        });
        return result;
    }

    private static WarnEvent lessonWarn(String type, long lessonId, long classId, long deptId,
                                        long teacherId, LocalDate statDate, int metric, int threshold,
                                        String detail) {
        return WarnEvent.builder()
                .warnType(type)
                .targetType(TARGET_LESSON)
                .targetId(lessonId)
                .lessonId(lessonId)
                .classId(classId)
                .deptId(deptId)
                .teacherId(teacherId)
                .statDate(statDate)
                .metricValue(metric)
                .thresholdValue(threshold)
                .detail(detail)
                .build();
    }
}
