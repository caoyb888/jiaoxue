package cn.smu.edu.notify.controller;

import cn.smu.edu.common.constant.KafkaTopic;
import cn.smu.edu.common.event.DiscussionMessageEvent;
import cn.smu.edu.notify.service.LessonBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 分组讨论 STOMP 消息处理（S8-12）。
 *
 * <p>客户端发言：SEND {@code /app/lesson/{lessonId}/group/{groupId}/discuss}，body {@code {content}}。
 * 服务端两路分发：① 实时广播 {@code /topic/lesson/{lessonId}/group/{groupId}/discussion}（组内学生 +
 * 教师即时可见）；② 发 Kafka {@code edu.discussion.events}（MESSAGE 收集 / END 触发 edu-ai LLM 汇总）。
 *
 * <p>发言人身份取自 WebSocket 握手会话属性（{@code JwtHandshakeInterceptor} 注入的 userId/username），
 * 不信任客户端 payload 中的身份字段。
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DiscussionMessageController {

    private final LessonBroadcastService broadcastService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** 学生发言：广播 + Kafka MESSAGE（收集入 edu-ai）。 */
    @MessageMapping("/lesson/{lessonId}/group/{groupId}/discuss")
    public void discuss(@DestinationVariable Long lessonId,
                        @DestinationVariable Long groupId,
                        @Payload Map<String, String> payload,
                        SimpMessageHeaderAccessor accessor) {
        String content = payload.get("content");
        if (!StringUtils.hasText(content)) {
            return;
        }
        Identity id = identity(accessor);
        String groupName = payload.get("groupName");
        LocalDateTime now = LocalDateTime.now();

        Map<String, Object> broadcast = new HashMap<>(); // 允许 userId 为 null（匿名兜底）
        broadcast.put("action", "MESSAGE");
        broadcast.put("userId", id.userId());
        broadcast.put("userName", id.userName());
        broadcast.put("content", content);
        broadcast.put("sentAt", now.toString());
        broadcastService.broadcastDiscussion(lessonId, groupId, broadcast);

        kafkaTemplate.send(KafkaTopic.DISCUSSION_EVENTS, lessonId + ":" + groupId,
                DiscussionMessageEvent.builder()
                        .action("MESSAGE").lessonId(lessonId).groupId(groupId).groupName(groupName)
                        .userId(id.userId()).userName(id.userName()).content(content).sentAt(now)
                        .build());
        log.debug("讨论发言: lessonId={}, groupId={}, userId={}", lessonId, groupId, id.userId());
    }

    /** 结束讨论：广播结束标记 + Kafka END（触发 edu-ai LLM 汇总）。 */
    @MessageMapping("/lesson/{lessonId}/group/{groupId}/discuss/end")
    public void end(@DestinationVariable Long lessonId,
                    @DestinationVariable Long groupId,
                    @Payload(required = false) Map<String, String> payload,
                    SimpMessageHeaderAccessor accessor) {
        Identity id = identity(accessor);
        String groupName = payload == null ? null : payload.get("groupName");

        broadcastService.broadcastDiscussion(lessonId, groupId, Map.of("action", "END"));

        kafkaTemplate.send(KafkaTopic.DISCUSSION_EVENTS, lessonId + ":" + groupId,
                DiscussionMessageEvent.builder()
                        .action("END").lessonId(lessonId).groupId(groupId).groupName(groupName)
                        .userId(id.userId()).userName(id.userName()).sentAt(LocalDateTime.now())
                        .build());
        log.info("讨论结束触发汇总: lessonId={}, groupId={}", lessonId, groupId);
    }

    private Identity identity(SimpMessageHeaderAccessor accessor) {
        Map<String, Object> attrs = accessor.getSessionAttributes();
        Long userId = attrs == null ? null : (Long) attrs.get("userId");
        Object name = attrs == null ? null : attrs.get("username");
        return new Identity(userId, name == null ? "匿名" : name.toString());
    }

    private record Identity(Long userId, String userName) {
    }
}
