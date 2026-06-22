package cn.smu.edu.ai.domain.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * AI 对话会话（MongoDB，collection: ai_dialogue_session）
 *
 * S6-08：创建对话任务时落库，记录主题/开场白/最大轮次；每次学生发言 turnCount+1。
 * 对话消息明细见 {@link AiDialogueMessage}。
 */
@Data
@Document(collection = "ai_dialogue_session")
public class AiDialogueSession {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sessionId;

    private Long userId;
    private Long lessonId;
    private String topic;
    private String opening;

    /** 最大对话轮次（学生发言次数上限） */
    private int maxTurns;

    /** 已发生的学生发言轮次 */
    private int turnCount;

    /** 使用的模型类型（ModelType 名称） */
    private String modelType;

    /** ACTIVE / CLOSED */
    private String status;

    @Indexed
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CLOSED = "CLOSED";
}
