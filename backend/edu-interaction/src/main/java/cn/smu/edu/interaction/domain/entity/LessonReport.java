package cn.smu.edu.interaction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("lesson_report")
public class LessonReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lessonId;

    private Integer attendCount;

    private Integer absentCount;

    private BigDecimal attendRate;

    private Integer interactCount;

    private Integer quizCount;

    private Integer durationMin;

    private Integer slideCount;

    private String aiSummary;

    private String aiMindmapJson;

    /** 0-不开放 1-开放 */
    private Integer mindmapVisible;

    /** 0-不开放 1-开放 */
    private Integer summaryVisible;

    /** 0-待生成 1-生成中 2-已完成 3-生成失败 */
    private Integer genStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
