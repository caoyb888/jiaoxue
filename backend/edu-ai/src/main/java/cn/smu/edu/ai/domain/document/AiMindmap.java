package cn.smu.edu.ai.domain.document;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * AI 思维导图（MongoDB，collection: ai_mindmap）
 *
 * S6-06：LLM 基于课堂转写生成 Markmap 格式 JSON，存此处作为思维导图的权威存储；
 * 同时把 markmapJson 回写 lesson_report.ai_mindmap_json 供课堂报告页直接渲染。
 * 一节课一条（lessonId 唯一），重新生成时覆盖。
 */
@Data
@Builder
@Document(collection = "ai_mindmap")
public class AiMindmap {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long lessonId;

    private Long teacherId;

    /** Markmap 格式 JSON：{"title":..,"children":[{"content":..,"children":[]}]} */
    private String markmapJson;

    /** 是否解析为合法 JSON（false 表示降级占位） */
    private boolean parsed;

    /** 来源任务类型：SUMMARY（随摘要一并生成）/ MINDMAP（单独触发） */
    private String source;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
