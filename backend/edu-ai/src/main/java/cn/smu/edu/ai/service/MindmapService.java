package cn.smu.edu.ai.service;

import cn.smu.edu.ai.domain.document.AiMindmap;
import cn.smu.edu.ai.domain.model.AiRequest;
import cn.smu.edu.ai.domain.model.ModelType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * S6-06 AI 思维导图生成。
 *
 * 基于课堂转写文本经 GENERATION/ANALYSIS 模型（C4 安全层）生成 Markmap JSON，
 * 校验后存 MongoDB ai_mindmap（lessonId 唯一，覆盖式 upsert），并返回 JSON 供调用方
 * 回写 lesson_report.ai_mindmap_json。LLM 返回非法 JSON 时降级为占位导图。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MindmapService {

    private static final Pattern JSON_BLOCK = Pattern.compile("\\{.*}", Pattern.DOTALL);

    private static final String SYSTEM_PROMPT =
            "你是一名教学助手，擅长将课堂内容转换为 Markmap 格式的思维导图 JSON。"
            + "只输出 JSON，格式：{\"title\":\"主题\",\"children\":[{\"content\":\"子节点\",\"children\":[]}]}，"
            + "不要包含任何额外文字。";

    private static final String FALLBACK_JSON =
            "{\"title\":\"思维导图生成失败，请重新生成\",\"children\":[]}";

    private final AiGatewayService aiGatewayService;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 生成思维导图并存 Mongo，返回校验后的 Markmap JSON。
     *
     * @param source 来源任务类型（SUMMARY / MINDMAP）
     */
    public String generate(Long lessonId, Long teacherId, String transcript, String source) {
        String content = (transcript == null || transcript.isBlank())
                ? "（本节课暂无转写文本，请基于课堂主题给出通用知识结构。）"
                : transcript;

        AiRequest req = AiRequest.builder()
                .lessonId(lessonId)
                .userId(teacherId)
                .modelType(ModelType.ANALYSIS)
                .systemPrompt(SYSTEM_PROMPT)
                .userPrompt("课堂转写文本如下：\n" + content)
                .build();

        String raw = aiGatewayService.chatSync(req);
        Parsed parsed = extractJson(raw);
        saveToMongo(lessonId, teacherId, parsed, source);
        return parsed.json();
    }

    /** 校验结果 */
    private record Parsed(String json, boolean ok) {
    }

    /** 抽取并校验 JSON 块；非法时降级占位 */
    private Parsed extractJson(String raw) {
        try {
            Matcher m = JSON_BLOCK.matcher(raw == null ? "" : raw);
            if (!m.find()) {
                throw new IllegalArgumentException("未找到 JSON 块");
            }
            String block = m.group();
            objectMapper.readTree(block); // 仅校验可解析
            return new Parsed(block, true);
        } catch (Exception e) {
            log.warn("思维导图返回非法JSON，降级占位: raw={}", truncate(raw));
            return new Parsed(FALLBACK_JSON, false);
        }
    }

    private void saveToMongo(Long lessonId, Long teacherId, Parsed parsed, String source) {
        LocalDateTime now = LocalDateTime.now();
        mongoTemplate.upsert(
                new Query(Criteria.where("lessonId").is(lessonId)),
                new Update()
                        .set("teacherId", teacherId)
                        .set("markmapJson", parsed.json())
                        .set("parsed", parsed.ok())
                        .set("source", source)
                        .set("updatedAt", now)
                        .setOnInsert("createdAt", now),
                AiMindmap.class);
        log.info("思维导图已存 Mongo ai_mindmap: lessonId={}, parsed={}, source={}",
                lessonId, parsed.ok(), source);
    }

    private String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 100 ? s.substring(0, 100) + "..." : s;
    }
}
