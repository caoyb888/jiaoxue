package cn.smu.edu.grade.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成绩组成规则，一个教学班（class_id）下所有规则的 weight 之和应等于 100。
 * grade_type：1-期末考试 2-平时作业 3-实验报告 4-项目实践 5-出勤 6-其他
 */
@Data
@TableName("grade_rule")
public class GradeRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long classId;
    private Long teacherId;

    private String ruleName;

    /** 1-期末 2-平时 3-实验 4-项目 5-出勤 6-其他 */
    private Integer gradeType;

    /** 权重百分比，合计必须=100（同一 classId 下）*/
    private BigDecimal weight;

    private String description;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
