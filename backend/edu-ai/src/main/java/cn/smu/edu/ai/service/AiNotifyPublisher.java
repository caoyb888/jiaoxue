package cn.smu.edu.ai.service;

import cn.smu.edu.common.constant.KafkaTopic;
import cn.smu.edu.common.event.NotifyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * AI 任务完成后的 WebSocket 通知发布器（经 edu.notice → edu-notify → STOMP）。
 * 复用于批改完成（单播教师）、课堂摘要/思维导图完成（课堂广播）等。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiNotifyPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** 单播通知某用户（如批改完成通知教师） */
    public void notifyUser(Long userId, String type, String message, Map<String, Object> payload) {
        if (userId == null) return;
        kafkaTemplate.send(KafkaTopic.NOTICE, NotifyEvent.toUser(userId, type, message, payload));
        log.info("AI通知已发送(单播): userId={}, type={}", userId, type);
    }

    /** 课堂广播通知（如课堂摘要/思维导图完成） */
    public void notifyLesson(Long lessonId, String type, String message, Map<String, Object> payload) {
        if (lessonId == null) return;
        kafkaTemplate.send(KafkaTopic.NOTICE, NotifyEvent.toLesson(lessonId, type, message, payload));
        log.info("AI通知已发送(课堂广播): lessonId={}, type={}", lessonId, type);
    }
}
