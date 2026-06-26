package cn.smu.edu.stat.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 教学预警事件（{@code warn_event}，edu_db）。
 *
 * <p>由 edu-stat XXL-Job {@code teachingWarnCheck} 写入，去重键
 * {@code (warn_type, target_type, target_id, stat_date)} 保证幂等。
 */
@Data
@Builder
@TableName("warn_event")
public class WarnEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预警类型：LOW_ATTEND / ZERO_ACTIVE / FREQUENT_ABSENCE。 */
    private String warnType;

    /** 预警对象类型：LESSON / STUDENT。 */
    private String targetType;

    /** 对象 ID（lesson_id 或 student_id）。 */
    private Long targetId;

    private Long lessonId;
    private Long classId;
    private Long deptId;
    private Long teacherId;

    /** 统计日期。 */
    private LocalDate statDate;

    /** 触发指标实际值。 */
    private Integer metricValue;

    /** 阈值。 */
    private Integer thresholdValue;

    /** 预警详情（人类可读）。 */
    private String detail;

    /** 处理状态：0 未处理 / 1 已处理 / 2 忽略。 */
    private Integer status;

    private Long handledBy;
    private LocalDateTime handledAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
