package cn.smu.edu.exam.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生作答记录（每题一条）。
 * review_status: 0-未批改 1-自动批改完成 2-教师已批改
 * is_correct: NULL-未批改 0-错误 1-正确
 */
@Data
@TableName("student_answer")
public class StudentAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long publishId;
    private Long questionId;
    private Long studentId;

    private String answerContent;
    private BigDecimal score;

    /** NULL=未批改 0=错误 1=正确（仅客观题有值） */
    private Integer isCorrect;

    private String comment;

    /** 0-未批改 1-自动批改完成 2-教师已批改 */
    private Integer reviewStatus;

    private LocalDateTime submittedAt;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
