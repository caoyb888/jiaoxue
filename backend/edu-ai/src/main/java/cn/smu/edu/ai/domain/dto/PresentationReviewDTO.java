package cn.smu.edu.ai.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 汇报点评请求（S8-05）。
 *
 * <p>{@code transcript} 为汇报录音 ASR 转写结果（上游已转写）；{@code dimensions}
 * 可选，覆盖默认点评规则（维度 + 权重 + 满分）。
 */
@Data
public class PresentationReviewDTO {

    @NotNull
    private Long lessonId;

    @NotNull
    private Long studentId;

    private String studentName;

    @NotNull
    private String transcript;

    /** 自定义点评维度（为空则用默认规则）。 */
    private List<DimensionRule> dimensions;

    @Data
    public static class DimensionRule {
        private String name;
        private double weight;
        private double maxScore;
    }
}
