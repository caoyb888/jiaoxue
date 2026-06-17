package cn.smu.edu.notify.controller;

import cn.smu.edu.notify.service.LessonBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * STOMP 消息处理控制器（S3-05 课件翻页同步）
 *
 * 客户端发送：STOMP SEND /app/lesson/{id}/nextSlide
 * 服务端广播：/topic/lesson/{id}/slide
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class LessonMessageController {

    private final LessonBroadcastService broadcastService;

    /**
     * 课件翻页同步
     * 教师发送 {"slideIndex": 3} → 广播给所有订阅 /topic/lesson/{id}/slide 的学生
     */
    @MessageMapping("/lesson/{lessonId}/nextSlide")
    public void nextSlide(@DestinationVariable Long lessonId,
                          @Payload Map<String, Integer> payload) {
        Integer slideIndex = payload.getOrDefault("slideIndex", 0);
        log.info("课件翻页: lessonId={}, slideIndex={}", lessonId, slideIndex);
        broadcastService.broadcastSlideChange(lessonId, slideIndex);
    }

    /**
     * 签到人数推送（定期更新，也可由签到服务主动调用）
     */
    @MessageMapping("/lesson/{lessonId}/attend/count")
    public void attendCount(@DestinationVariable Long lessonId,
                            @Payload Map<String, Long> payload) {
        Long count = payload.getOrDefault("count", 0L);
        broadcastService.broadcastAttendCount(lessonId, count);
    }
}
