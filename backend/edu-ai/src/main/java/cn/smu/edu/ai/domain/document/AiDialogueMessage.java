package cn.smu.edu.ai.domain.document;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * AI 对话消息（MongoDB，collection: ai_dialogue_message）
 *
 * S6-08：按 sessionId + seq 顺序存储 user/assistant 消息。
 * createdAt 作为 S6-15 TTL 索引锚点（对话消息过期自动清理）。
 */
@Data
@Builder
@Document(collection = "ai_dialogue_message")
public class AiDialogueMessage {

    @Id
    private String id;

    @Indexed
    private String sessionId;

    private Long userId;

    /** user / assistant */
    private String role;

    private String content;

    /** 会话内顺序号（从 0 起） */
    private int seq;

    /** 是否被 Prompt 安全层拦截（C4：违规输入落库标记，不调用 LLM） */
    @Field("is_filtered")
    private boolean filtered;

    @Indexed
    private LocalDateTime createdAt;

    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
}
