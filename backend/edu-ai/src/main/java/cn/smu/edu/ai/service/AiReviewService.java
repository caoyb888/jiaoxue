package cn.smu.edu.ai.service;

import cn.smu.edu.ai.domain.document.AiReviewResult;
import cn.smu.edu.ai.domain.dto.ReviewItemDTO;
import cn.smu.edu.ai.domain.model.AiRequest;
import cn.smu.edu.ai.domain.model.ModelType;
import cn.smu.edu.ai.repository.AiReviewResultRepository;
import cn.smu.edu.ai.repository.ReviewQueryMapper;
import cn.smu.edu.ai.repository.ReviewWritebackMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * S6-02 AI 智能批改服务
 *
 * 流程：拉取某次发布下未批改的主观题 → 逐题调用 REVIEW 模型（C4：经 AiGatewayService 过安全层）
 *      → 解析 LLM 返回 JSON(score/comment/errorReason) → 落库 MongoDB ai_review_result。
 *
 * 写回 student_answer + WebSocket 通知教师在 S6-03 实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiReviewService {

    private static final Pattern JSON_BLOCK = Pattern.compile("\\{.*}", Pattern.DOTALL);

    private final ReviewQueryMapper reviewQueryMapper;
    private final AiReviewResultRepository reviewResultRepository;
    private final ReviewWritebackMapper reviewWritebackMapper;
    private final AiGatewayService aiGatewayService;
    private final ObjectMapper objectMapper;

    /**
     * 批改某次试卷发布下的全部待批改主观题，并写回 student_answer。
     * @return 实际批改的题数（解析成功且已写回）
     */
    public int reviewByPublish(Long publishId, String taskId) {
        if (publishId == null) {
            log.warn("AI批改任务缺少 publishId，跳过: taskId={}", taskId);
            return 0;
        }
        List<ReviewItemDTO> items = reviewQueryMapper.selectPendingSubjective(publishId);
        log.info("AI批改开始: publishId={}, 待批改题数={}, taskId={}", publishId, items.size(), taskId);

        int done = 0;
        for (ReviewItemDTO item : items) {
            try {
                AiReviewResult result = reviewOne(item, publishId, taskId);
                reviewResultRepository.save(result);
                // S6-03：解析成功才写回 student_answer（review_status=1）；非法降级题保留 0 待人工
                if (result.isParsed() && result.getScore() != null) {
                    reviewWritebackMapper.writeBack(result.getAnswerId(), result.getScore(), result.getComment());
                }
                done++;
            } catch (Exception e) {
                log.error("单题批改失败: answerId={}, publishId={}", item.getAnswerId(), publishId, e);
            }
        }
        log.info("AI批改完成: publishId={}, 成功={}/{}, taskId={}", publishId, done, items.size(), taskId);
        return done;
    }

    private AiReviewResult reviewOne(ReviewItemDTO item, Long publishId, String taskId) {
        AiRequest req = AiRequest.builder()
                .userId(item.getStudentId())
                .modelType(ModelType.REVIEW)
                .systemPrompt(buildSystemPrompt(item))
                .userPrompt(buildUserPrompt(item))
                .build();

        String raw = aiGatewayService.chatSync(req);
        return parseResult(raw, item, publishId, taskId);
    }

    private String buildSystemPrompt(ReviewItemDTO item) {
        String rule = item.getReviewRule() != null && !item.getReviewRule().isBlank()
                ? item.getReviewRule()
                : "按照参考答案的要点完整度、准确性与表达逻辑综合评分。";
        return "你是一名严谨的阅卷老师，负责主观题批改。" + rule
                + "\n请只输出 JSON，格式：{\"score\": 数字, \"comment\": \"评语\", \"errorReason\": \"错因分析\"}，"
                + "score 取值范围 0 ~ " + item.getMaxScore() + "，不要输出任何额外文字。";
    }

    private String buildUserPrompt(ReviewItemDTO item) {
        return "题目：" + nullToEmpty(item.getQuestionContent()) + "\n"
                + "参考答案：" + nullToEmpty(item.getReferenceAnswer()) + "\n"
                + "满分：" + item.getMaxScore() + "\n"
                + "学生作答：" + nullToEmpty(item.getStudentAnswer());
    }

    /** 解析 LLM 返回；非法 JSON 时降级为待人工复核（parsed=false） */
    private AiReviewResult parseResult(String raw, ReviewItemDTO item, Long publishId, String taskId) {
        AiReviewResult.AiReviewResultBuilder b = AiReviewResult.builder()
                .publishId(publishId)
                .answerId(item.getAnswerId())
                .questionId(item.getQuestionId())
                .studentId(item.getStudentId())
                .maxScore(item.getMaxScore())
                .taskId(taskId)
                .reviewedAt(LocalDateTime.now());

        try {
            Matcher m = JSON_BLOCK.matcher(raw == null ? "" : raw);
            if (!m.find()) {
                throw new IllegalArgumentException("未找到 JSON 块");
            }
            JsonNode node = objectMapper.readTree(m.group());
            BigDecimal score = node.hasNonNull("score")
                    ? clampScore(new BigDecimal(node.get("score").asText()), item.getMaxScore())
                    : null;
            return b.score(score)
                    .comment(text(node, "comment"))
                    .errorReason(text(node, "errorReason"))
                    .parsed(true)
                    .build();
        } catch (Exception e) {
            log.warn("AI批改返回非法JSON，降级人工复核: answerId={}, raw={}", item.getAnswerId(), truncate(raw));
            return b.score(null)
                    .comment("AI返回格式异常，待人工复核")
                    .errorReason(null)
                    .parsed(false)
                    .build();
        }
    }

    private BigDecimal clampScore(BigDecimal score, BigDecimal max) {
        if (score.signum() < 0) return BigDecimal.ZERO;
        if (max != null && score.compareTo(max) > 0) return max;
        return score;
    }

    private String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String truncate(String s) {
        if (s == null) return "";
        return s.length() > 100 ? s.substring(0, 100) + "..." : s;
    }
}
