package cn.smu.edu.notify.consumer;

import cn.smu.edu.common.constant.KafkaTopic;
import cn.smu.edu.common.event.NotifyEvent;
import cn.smu.edu.notify.service.LessonBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用通知消费者：消费 edu.notice，按目标路由到 STOMP 单播或课堂广播。
 * 复用于 AI 批改完成（单播教师）、课堂摘要/思维导图完成（课堂广播）等。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeConsumer {

    private final LessonBroadcastService broadcastService;

    @KafkaListener(topics = KafkaTopic.NOTICE,
                   groupId = "notify-notice",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consume(NotifyEvent event) {
        if (event == null || event.getType() == null) {
            log.warn("收到空的 NotifyEvent，已忽略");
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        if (event.getPayload() != null) payload.putAll(event.getPayload());
        payload.put("message", event.getMessage());

        if (event.getUserId() != null) {
            broadcastService.sendToUser(event.getUserId(), event.getType(), payload);
            log.info("通知单播: userId={}, type={}", event.getUserId(), event.getType());
        }
        if (event.getLessonId() != null) {
            broadcastService.broadcastAiDone(event.getLessonId(), event.getType(),
                    event.getMessage() == null ? "" : event.getMessage());
            log.info("通知课堂广播: lessonId={}, type={}", event.getLessonId(), event.getType());
        }
    }
}
