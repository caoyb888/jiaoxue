package cn.smu.edu.stat.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * ClickHouse {@code lesson_event_log} 明细行（非 MyBatis 实体，由 JdbcTemplate 批量写入）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonEventLog {

    private LocalDate statDate;
    private Long lessonId;
    private Long classId;
    private Long deptId;
    private Long teacherId;
    /** ATTEND / BARRAGE / QUESTION / SCORE / SLIDE */
    private String eventType;
    private Long studentId;
    private String eventValue;
}
