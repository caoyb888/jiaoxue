package cn.smu.edu.ai.service.impl;

import cn.smu.edu.ai.domain.document.AiPresentationReview;
import cn.smu.edu.ai.domain.document.AiPresentationReview.DimensionScore;
import cn.smu.edu.ai.domain.dto.PresentationReviewDTO;
import cn.smu.edu.ai.domain.dto.PresentationReviewDTO.DimensionRule;
import cn.smu.edu.ai.domain.model.AiRequest;
import cn.smu.edu.ai.domain.model.ModelType;
import cn.smu.edu.ai.repository.AiPresentationReviewRepository;
import cn.smu.edu.ai.service.AiGatewayService;
import cn.smu.edu.ai.service.PresentationReviewService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link PresentationReviewService} 实现。LLM 评分走 {@link AiGatewayService}（C4 安全层）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresentationReviewServiceImpl implements PresentationReviewService {

    /** 默认点评规则：维度名 → {权重, 满分}。权重之和为 1。 */
    private static final List<DimensionRule> DEFAULT_RULES = List.of(
            rule("内容质量", 0.4, 100),
            rule("逻辑结构", 0.3, 100),
            rule("语言表达", 0.2, 100),
            rule("时间控制", 0.1, 100));

    private final AiPresentationReviewRepository repository;
    private final AiGatewayService aiGatewayService;
    private final ObjectMapper objectMapper;

    @Override
    public AiPresentationReview review(PresentationReviewDTO dto) {
        List<DimensionRule> rules = CollectionUtils.isEmpty(dto.getDimensions())
                ? DEFAULT_RULES : dto.getDimensions();

        String raw = aiGatewayService.chatSync(AiRequest.builder()
                .systemPrompt(buildSystemPrompt(rules))
                .userPrompt(dto.getTranscript())
                .modelType(ModelType.REVIEW)
                .lessonId(dto.getLessonId())
                .build());

        AiPresentationReview review = repository
                .findByLessonIdAndStudentId(dto.getLessonId(), dto.getStudentId())
                .orElseGet(() -> AiPresentationReview.builder()
                        .lessonId(dto.getLessonId()).studentId(dto.getStudentId())
                        .createdAt(LocalDateTime.now()).build());
        review.setStudentName(dto.getStudentName());
        review.setTranscript(dto.getTranscript());
        review.setUpdatedAt(LocalDateTime.now());

        applyScores(review, rules, raw);
        repository.save(review);
        log.info("汇报点评完成: lessonId={}, studentId={}, total={}, parsed={}",
                dto.getLessonId(), dto.getStudentId(), review.getTotalScore(), review.isParsed());
        return review;
    }

    @Override
    public AiPresentationReview getReview(Long lessonId, Long studentId) {
        return repository.findByLessonIdAndStudentId(lessonId, studentId).orElse(null);
    }

    /** 解析 LLM 评分 JSON；失败则降级（各维度 0 分，原文存入评语）。 */
    private void applyScores(AiPresentationReview review, List<DimensionRule> rules, String raw) {
        Map<String, JsonNode> scored = new HashMap<>();
        String overall = null;
        try {
            JsonNode root = objectMapper.readTree(extractJson(raw));
            JsonNode dims = root.path("dimensions");
            if (dims.isArray() && !dims.isEmpty()) {
                for (JsonNode d : dims) {
                    scored.put(d.path("name").asText(), d);
                }
                overall = root.path("overallComment").asText(null);
            }
        } catch (Exception e) {
            log.warn("汇报点评 LLM 输出解析失败，降级: {}", e.getMessage());
        }

        boolean parsed = !scored.isEmpty();
        List<DimensionScore> result = new ArrayList<>();
        double weighted = 0;
        for (DimensionRule r : rules) {
            JsonNode node = scored.get(r.getName());
            double score = node == null ? 0 : clamp(node.path("score").asDouble(0), r.getMaxScore());
            String comment = node == null ? null : node.path("comment").asText(null);
            result.add(DimensionScore.builder()
                    .name(r.getName()).weight(r.getWeight()).maxScore(r.getMaxScore())
                    .score(score).comment(comment).build());
            weighted += score * r.getWeight();
        }
        review.setDimensions(result);
        review.setTotalScore(round2(weighted));
        review.setOverallComment(parsed ? overall : raw);
        review.setParsed(parsed);
    }

    private static String buildSystemPrompt(List<DimensionRule> rules) {
        StringBuilder dims = new StringBuilder();
        for (DimensionRule r : rules) {
            dims.append("- ").append(r.getName())
                    .append("（满分 ").append((int) r.getMaxScore()).append("）\n");
        }
        return """
                你是汇报点评助手。请仅基于以下汇报转写文本，按下列维度逐项打分并给出简短评语，
                维度：
                %s
                只输出 JSON，格式：
                {"dimensions":[{"name":"维度名","score":数字,"comment":"评语"}],"overallComment":"总体评语"}
                不得编造转写中未出现的内容。
                """.formatted(dims.toString().trim());
    }

    /** 容错提取 LLM 输出中的 JSON 主体（去除 ``` 包裹/前后说明）。 */
    private static String extractJson(String raw) {
        if (raw == null) {
            return "{}";
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        return start >= 0 && end > start ? raw.substring(start, end + 1) : "{}";
    }

    private static double clamp(double v, double max) {
        return Math.max(0, Math.min(v, max));
    }

    private static double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static DimensionRule rule(String name, double weight, double maxScore) {
        DimensionRule r = new DimensionRule();
        r.setName(name);
        r.setWeight(weight);
        r.setMaxScore(maxScore);
        return r;
    }
}
