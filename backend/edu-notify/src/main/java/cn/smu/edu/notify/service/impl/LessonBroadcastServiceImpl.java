package cn.smu.edu.notify.service.impl;

import cn.smu.edu.notify.service.LessonBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonBroadcastServiceImpl implements LessonBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcastSlideChange(Long lessonId, int slideIndex) {
        String topic = "/topic/lesson/" + lessonId + "/slide";
        messagingTemplate.convertAndSend(topic, Map.of("slideIndex", slideIndex));
        log.debug("翻页广播: lessonId={}, slideIndex={}", lessonId, slideIndex);
    }

    @Override
    public void broadcastAttendCount(Long lessonId, long count) {
        String topic = "/topic/lesson/" + lessonId + "/attend";
        messagingTemplate.convertAndSend(topic, Map.of("count", count));
    }

    @Override
    public void broadcastBarrage(Long lessonId, String content, String style) {
        String topic = "/topic/lesson/" + lessonId + "/barrage";
        messagingTemplate.convertAndSend(topic, Map.of("content", content, "style", style));
    }

    @Override
    public void broadcastRollCall(Long lessonId, Object rollCallResult) {
        String topic = "/topic/lesson/" + lessonId + "/roll-call";
        messagingTemplate.convertAndSend(topic, rollCallResult);
    }

    @Override
    public void broadcastAiDone(Long lessonId, String taskType, String message) {
        String topic = "/topic/lesson/" + lessonId + "/ai";
        messagingTemplate.convertAndSend(topic, Map.of("taskType", taskType, "message", message));
    }

    @Override
    public void broadcastQuestion(Long lessonId, Object questionPayload) {
        String topic = "/topic/lesson/" + lessonId + "/question";
        messagingTemplate.convertAndSend(topic, questionPayload);
        log.info("课堂发题广播: lessonId={}", lessonId);
    }

    @Override
    public void broadcastQuestionClosed(Long lessonId, Long lessonQuestionId) {
        String topic = "/topic/lesson/" + lessonId + "/question";
        messagingTemplate.convertAndSend(topic, Map.of(
                "event", "QUESTION_CLOSED",
                "lessonQuestionId", lessonQuestionId));
        log.info("课堂题目关闭广播: lessonId={}, lessonQuestionId={}", lessonId, lessonQuestionId);
    }

    @Override
    public void sendToUser(Long userId, String type, Object payload) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/messages",
                Map.of("type", type, "payload", payload)
        );
    }
}
