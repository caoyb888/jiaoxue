package cn.smu.edu.ai.domain.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 汇报点评结果（MongoDB，collection: ai_presentation_review，S8-05）。
 *
 * <p>学生汇报录音经 ASR 转写为 {@code transcript}，LLM 按可配置维度多维评分，
 * 加权得 {@code totalScore}。一节课每个学生一条（lessonId+studentId 唯一）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "uk_lesson_student", def = "{'lessonId': 1, 'studentId': 1}", unique = true)
@Document(collection = "ai_presentation_review")
public class AiPresentationReview {

    @Id
    private String id;

    private Long lessonId;
    private Long studentId;
    private String studentName;

    /** ASR 转写文本。 */
    private String transcript;

    /** 各维度得分。 */
    @Builder.Default
    private List<DimensionScore> dimensions = new ArrayList<>();

    /** 加权总分。 */
    private Double totalScore;

    /** 总体评语。 */
    private String overallComment;

    /** LLM 输出是否成功解析为评分 JSON（false 表示降级）。 */
    private boolean parsed;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionScore {
        private String name;
        private double weight;
        private double maxScore;
        private double score;
        private String comment;
    }
}
