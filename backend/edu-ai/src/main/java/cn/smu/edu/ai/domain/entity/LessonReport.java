package cn.smu.edu.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("lesson_report")
public class LessonReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lessonId;
    private Integer attendCount;
    private Integer absentCount;
    private java.math.BigDecimal attendRate;
    private Integer interactCount;
    private Integer quizCount;
    private Integer durationMin;
    private Integer slideCount;

    private String aiSummary;
    private String aiMindmapJson;

    private Integer mindmapVisible;
    private Integer summaryVisible;

    /** 0-待生成 1-生成中 2-已完成 3-生成失败 */
    private Integer genStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
