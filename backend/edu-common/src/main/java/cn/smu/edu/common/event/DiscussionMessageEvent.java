package cn.smu.edu.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分组讨论消息事件（S8-04）。
 *
 * <p>生产者：edu-interaction/edu-notify（学生 WebSocket 讨论输入 → Kafka）；
 * 消费者：edu-ai {@code DiscussionConsumer}。Topic：{@code edu.discussion.events}。
 *
 * <p>{@code action=MESSAGE} 为一条讨论发言（收集）；{@code action=END} 表示讨论结束，
 * 触发该组 LLM 汇总分析。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionMessageEvent implements Serializable {

    /** MESSAGE：讨论发言；END：讨论结束触发汇总。 */
    private String action;

    private Long lessonId;
    private Long groupId;
    private String groupName;

    private Long userId;
    private String userName;

    /** 发言内容（action=MESSAGE 时有值）。 */
    private String content;

    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();
}
