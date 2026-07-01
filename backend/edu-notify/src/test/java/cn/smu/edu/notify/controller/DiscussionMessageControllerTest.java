package cn.smu.edu.notify.controller;

import cn.smu.edu.common.constant.KafkaTopic;
import cn.smu.edu.common.event.DiscussionMessageEvent;
import cn.smu.edu.notify.service.LessonBroadcastService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DiscussionMessageControllerTest {

    @Mock
    LessonBroadcastService broadcastService;
    @Mock
    KafkaTemplate<String, Object> kafkaTemplate;

    private DiscussionMessageController controller() {
        return new DiscussionMessageController(broadcastService, kafkaTemplate);
    }

    private SimpMessageHeaderAccessor accessorWith(Long userId, String username) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        Map<String, Object> attrs = new HashMap<>();
        if (userId != null) attrs.put("userId", userId);
        if (username != null) attrs.put("username", username);
        accessor.setSessionAttributes(attrs);
        return accessor;
    }

    @Test
    void discuss_shouldBroadcastAndSendKafkaMessageWithSessionIdentity() {
        controller().discuss(5L, 2L,
                Map.of("content", "我认为方案A更优", "groupName", "第一组"),
                accessorWith(1001L, "张三"));

        // 广播（身份取自会话，非 payload）
        ArgumentCaptor<Object> bc = ArgumentCaptor.forClass(Object.class);
        verify(broadcastService).broadcastDiscussion(eq(5L), eq(2L), bc.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> msg = (Map<String, Object>) bc.getValue();
        assertThat(msg.get("action")).isEqualTo("MESSAGE");
        assertThat(msg.get("userId")).isEqualTo(1001L);
        assertThat(msg.get("userName")).isEqualTo("张三");
        assertThat(msg.get("content")).isEqualTo("我认为方案A更优");

        // Kafka MESSAGE
        ArgumentCaptor<Object> ev = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(KafkaTopic.DISCUSSION_EVENTS), eq("5:2"), ev.capture());
        DiscussionMessageEvent event = (DiscussionMessageEvent) ev.getValue();
        assertThat(event.getAction()).isEqualTo("MESSAGE");
        assertThat(event.getUserId()).isEqualTo(1001L);
        assertThat(event.getContent()).isEqualTo("我认为方案A更优");
        assertThat(event.getGroupName()).isEqualTo("第一组");
    }

    @Test
    void discuss_blankContent_shouldDoNothing() {
        controller().discuss(5L, 2L, Map.of("content", "  "), accessorWith(1L, "a"));

        verify(broadcastService, never()).broadcastDiscussion(any(), any(), any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void end_shouldBroadcastEndAndSendKafkaEnd() {
        controller().end(5L, 2L, Map.of("groupName", "第一组"), accessorWith(9L, "老师"));

        verify(broadcastService).broadcastDiscussion(eq(5L), eq(2L), eq(Map.of("action", "END")));

        ArgumentCaptor<Object> ev = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(KafkaTopic.DISCUSSION_EVENTS), eq("5:2"), ev.capture());
        DiscussionMessageEvent event = (DiscussionMessageEvent) ev.getValue();
        assertThat(event.getAction()).isEqualTo("END");
        assertThat(event.getGroupName()).isEqualTo("第一组");
    }

    @Test
    void discuss_missingSessionIdentity_shouldFallbackToAnonymous() {
        controller().discuss(5L, 2L, Map.of("content", "hi"), accessorWith(null, null));

        ArgumentCaptor<Object> ev = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(KafkaTopic.DISCUSSION_EVENTS), any(), ev.capture());
        DiscussionMessageEvent event = (DiscussionMessageEvent) ev.getValue();
        assertThat(event.getUserId()).isNull();
        assertThat(event.getUserName()).isEqualTo("匿名");
    }
}
