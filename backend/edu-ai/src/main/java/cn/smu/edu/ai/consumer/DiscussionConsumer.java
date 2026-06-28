package cn.smu.edu.ai.consumer;

import cn.smu.edu.ai.service.DiscussionService;
import cn.smu.edu.common.constant.KafkaTopic;
import cn.smu.edu.common.event.DiscussionMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 分组讨论消息消费者（S8-04）。
 *
 * <p>消费 {@code edu.discussion.events}：{@code MESSAGE} 收集发言；{@code END} 触发该组
 * LLM 汇总。LLM 调用走 {@link DiscussionService}→{@code AiGatewayService}（C4 安全层），
 * 故 concurrency 沿用 AI 算力上限 3（CLAUDE.md §8.3）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiscussionConsumer {

    static final String ACTION_END = "END";

    private final DiscussionService discussionService;

    @KafkaListener(topics = KafkaTopic.DISCUSSION_EVENTS, groupId = "edu-ai-discussion", concurrency = "3")
    public void consume(DiscussionMessageEvent event) {
        if (event == null || event.getLessonId() == null || event.getGroupId() == null) {
            return;
        }
        if (ACTION_END.equals(event.getAction())) {
            discussionService.summarize(event.getLessonId(), event.getGroupId());
        } else {
            discussionService.appendMessage(event);
        }
    }
}
