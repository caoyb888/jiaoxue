package cn.smu.edu.ai.service;

import cn.smu.edu.ai.domain.document.AiLectureTranscript;
import cn.smu.edu.ai.domain.model.AiRequest;
import cn.smu.edu.ai.domain.model.ModelType;
import cn.smu.edu.ai.repository.AiLectureTranscriptRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * S6-05 课堂摘要生成。
 *
 * 流程：读取 S6-04 落库的课堂转写全文（ai_lecture_transcript.fullText）→ 经 AiGatewayService
 *      （C4 安全层）调用 GENERATION 模型 → 解析 JSON{summary, keyPoints} → 摘要截断 ≤500 字。
 * 持久化由调用方（AiTaskConsumer.handleSummary）写入 lesson_report。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LessonSummaryService {

    /** 摘要硬上限 500 字（CLAUDE 验收：摘要 ≤500字） */
    static final int SUMMARY_MAX_CHARS = 500;

    private static final Pattern JSON_BLOCK = Pattern.compile("\\{.*}", Pattern.DOTALL);

    private static final String SYSTEM_PROMPT =
            "你是一名教学助手，负责将课堂录音转写文本提炼为结构化讲稿摘要。"
            + "只输出 JSON，格式：{\"summary\": \"不超过500字的课堂讲稿摘要\", "
            + "\"keyPoints\": [\"关键点1\", \"关键点2\"]}，不要输出任何额外文字。";

    private final AiLectureTranscriptRepository transcriptRepository;
    private final AiGatewayService aiGatewayService;
    private final ObjectMapper objectMapper;

    /** 摘要结果：summary 已截断 ≤500 字；keyPointsJson 为 keyPoints 的 JSON 数组字符串 */
    public record SummaryResult(String summary, List<String> keyPoints, String keyPointsJson) {
    }

    /** 读取课堂转写全文；无转写记录或为空时返回 null */
    public String loadTranscript(Long lessonId) {
        return transcriptRepository.findByLessonId(lessonId)
                .map(AiLectureTranscript::getFullText)
                .filter(t -> t != null && !t.isBlank())
                .orElse(null);
    }

    /** 基于转写文本生成摘要 + 关键点 */
    public SummaryResult summarize(Long lessonId, String transcript) {
        String content = (transcript == null || transcript.isBlank())
                ? "（本节课暂无转写文本，请基于课堂主题给出通用学习建议。）"
                : transcript;

        AiRequest req = AiRequest.builder()
                .lessonId(lessonId)
                .modelType(ModelType.GENERATION)
                .systemPrompt(SYSTEM_PROMPT)
                .userPrompt("课堂转写文本如下：\n" + content)
                .build();

        String raw = aiGatewayService.chatSync(req);
        return parse(raw);
    }

    private SummaryResult parse(String raw) {
        try {
            Matcher m = JSON_BLOCK.matcher(raw == null ? "" : raw);
            if (!m.find()) {
                throw new IllegalArgumentException("未找到 JSON 块");
            }
            JsonNode node = objectMapper.readTree(m.group());
            String summary = truncate(text(node, "summary"));
            List<String> keyPoints = new ArrayList<>();
            JsonNode kp = node.get("keyPoints");
            if (kp != null && kp.isArray()) {
                for (JsonNode item : kp) {
                    if (!item.asText().isBlank()) {
                        keyPoints.add(item.asText());
                    }
                }
            }
            return new SummaryResult(summary, keyPoints, objectMapper.writeValueAsString(keyPoints));
        } catch (Exception e) {
            // LLM 未按 JSON 返回：把原文当摘要截断，关键点留空
            log.warn("摘要返回非 JSON，降级为纯文本摘要: raw={}", truncateForLog(raw));
            List<String> empty = List.of();
            try {
                return new SummaryResult(truncate(raw), empty, objectMapper.writeValueAsString(empty));
            } catch (Exception ignore) {
                return new SummaryResult(truncate(raw), empty, "[]");
            }
        }
    }

    private String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    /** 截断到 ≤500 字（按字符计） */
    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > SUMMARY_MAX_CHARS ? s.substring(0, SUMMARY_MAX_CHARS) : s;
    }

    private String truncateForLog(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 100 ? s.substring(0, 100) + "..." : s;
    }
}
