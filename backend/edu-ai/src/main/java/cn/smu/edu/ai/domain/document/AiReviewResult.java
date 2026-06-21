package cn.smu.edu.ai.domain.document;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 主观题批改结果（MongoDB，collection: ai_review_result）
 *
 * 由 AiReviewService 解析 LLM 返回的 JSON 后落库；
 * 批改结果写回 student_answer 由 S6-03 完成。
 */
@Data
@Builder
@Document(collection = "ai_review_result")
public class AiReviewResult {

    @Id
    private String id;

    /** 试卷发布ID（一次批改任务的范围） */
    @Indexed
    private Long publishId;

    /** student_answer 主键，写回时使用 */
    @Indexed
    private Long answerId;

    private Long questionId;
    private Long studentId;

    /** AI 评分（0 ~ maxScore） */
    private BigDecimal score;
    private BigDecimal maxScore;

    /** 批改评语 */
    private String comment;

    /** 错因分析 */
    private String errorReason;

    /** 是否解析成功（false 表示 LLM 返回非法 JSON，需人工复核） */
    private boolean parsed;

    /** 触发批改的任务ID */
    private String taskId;

    private LocalDateTime reviewedAt;
}
