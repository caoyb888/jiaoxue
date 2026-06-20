package cn.smu.edu.notify.consumer;

import cn.smu.edu.common.event.TeachingEvent;
import cn.smu.edu.common.constant.KafkaTopic;
import cn.smu.edu.notify.service.LessonBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 课堂教学事件消费者（concurrency=5，由 CLAUDE.md 限定）。
 * 消费 edu.teaching.events 并通过 STOMP 广播给在线学生。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TeachingEventConsumer {

    private final LessonBroadcastService broadcastService;

    @KafkaListener(topics = KafkaTopic.TEACHING_EVENTS,
                   groupId = "notify-teaching",
                   concurrency = "5",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consume(TeachingEvent event) {
        if (event == null || event.getEventType() == null) {
            log.warn("收到空的 TeachingEvent，已忽略");
            return;
        }
        log.debug("处理 TeachingEvent: type={}, lessonId={}", event.getEventType(), event.getLessonId());

        switch (event.getEventType()) {
            case "QUESTION_PUBLISHED" -> handleQuestionPublished(event);
            case "QUESTION_CLOSED"    -> handleQuestionClosed(event);
            case "BARRAGE"            -> handleBarrage(event);
            case "ROLL_CALL"          -> handleRollCall(event);
            case "ATTEND_COUNT"       -> handleAttendCount(event);
            default -> log.debug("未知事件类型: {}", event.getEventType());
        }
    }

    private void handleBarrage(TeachingEvent event) {
        Map<String, Object> payload = event.getPayload();
        if (payload == null) return;
        Object content = payload.get("content");
        Object style = payload.get("style");
        broadcastService.broadcastBarrage(
                event.getLessonId(),
                content == null ? "" : content.toString(),
                style == null ? "roll" : style.toString());
    }

    private void handleRollCall(TeachingEvent event) {
        broadcastService.broadcastRollCall(event.getLessonId(), event.getPayload());
    }

    private void handleAttendCount(TeachingEvent event) {
        Map<String, Object> payload = event.getPayload();
        if (payload == null) return;
        Object count = payload.get("count");
        broadcastService.broadcastAttendCount(
                event.getLessonId(),
                count instanceof Number n ? n.longValue() : 0L);
    }

    private void handleQuestionPublished(TeachingEvent event) {
        Object questionPayload = event.getPayload() != null
                ? event.getPayload().get("question")
                : null;
        broadcastService.broadcastQuestion(event.getLessonId(), questionPayload);
    }

    private void handleQuestionClosed(TeachingEvent event) {
        Map<String, Object> payload = event.getPayload();
        if (payload == null) return;
        Object idObj = payload.get("lessonQuestionId");
        if (idObj instanceof Number id) {
            broadcastService.broadcastQuestionClosed(event.getLessonId(), id.longValue());
        }
    }
}
