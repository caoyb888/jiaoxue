package cn.smu.edu.ai.domain.vo;

import cn.smu.edu.ai.domain.document.AiReviewResult;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResultVO {

    private Long answerId;
    private Long questionId;
    private Long studentId;
    private BigDecimal score;
    private BigDecimal maxScore;
    private String comment;
    private String errorReason;
    private boolean parsed;
    private LocalDateTime reviewedAt;

    public static ReviewResultVO from(AiReviewResult r) {
        return ReviewResultVO.builder()
                .answerId(r.getAnswerId())
                .questionId(r.getQuestionId())
                .studentId(r.getStudentId())
                .score(r.getScore())
                .maxScore(r.getMaxScore())
                .comment(r.getComment())
                .errorReason(r.getErrorReason())
                .parsed(r.isParsed())
                .reviewedAt(r.getReviewedAt())
                .build();
    }
}
