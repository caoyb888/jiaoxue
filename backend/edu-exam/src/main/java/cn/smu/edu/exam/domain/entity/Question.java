package cn.smu.edu.exam.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 题目主表，支持6种题型：
 * 1-单选 2-多选 3-判断 4-填空 5-主观 6-投票
 * 1/2/3/6 有 question_option 选项；4/5 只用 answer 字段
 */
@Data
@TableName("question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bankId;

    private Integer type;

    private String content;

    /** 客观题：正确选项标签如 "A,C"；主观题：参考答案文本 */
    private String answer;

    private String analysis;

    private BigDecimal score;

    /** 1-极易 2-易 3-中 4-难 5-极难 */
    private Integer difficulty;

    /** AI批改规则提示词（主观题，已过 PromptSecurityFilter）*/
    private String reviewRule;

    private Long creatorId;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
