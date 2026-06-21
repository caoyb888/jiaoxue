package cn.smu.edu.ai.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 待批改主观题作答项（student_answer JOIN question 的只读投影）
 */
@Data
public class ReviewItemDTO {

    private Long answerId;
    private Long questionId;
    private Long studentId;

    /** 题干 */
    private String questionContent;

    /** 参考答案 */
    private String referenceAnswer;

    /** 教师设定的 AI 批改规则提示词 */
    private String reviewRule;

    /** 学生作答内容 */
    private String studentAnswer;

    /** 该题满分 */
    private BigDecimal maxScore;
}
