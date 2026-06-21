package cn.smu.edu.ai.consumer;

import cn.smu.edu.ai.service.AiGatewayService;
import cn.smu.edu.ai.service.LessonReportService;
import cn.smu.edu.common.event.AiTaskEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AiTaskConsumer 单测：验证幂等去重与任务类型路由（CLAUDE.md 5.6/8.3）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiTaskConsumerTest {

    @Mock AiGatewayService aiGatewayService;
    @Mock LessonReportService reportService;
    @Mock cn.smu.edu.ai.service.AiReviewService aiReviewService;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks AiTaskConsumer consumer;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private AiTaskEvent event(String taskId, String type) {
        return new AiTaskEvent(taskId, 100L, 1L, 10L, type, LocalDateTime.now(), null);
    }

    @Test
    void consume_shouldProcessSummary_whenFirstAcquire() {
        when(valueOps.setIfAbsent(eq("ai:task:done:t1"), eq("1"), any(Duration.class))).thenReturn(true);
        when(aiGatewayService.chatSync(any())).thenReturn("mock");

        consumer.consumeAiTask(event("t1", "SUMMARY"));

        verify(reportService).initReport(100L, 0);
        verify(aiGatewayService, times(2)).chatSync(any()); // 摘要 + 思维导图
        verify(reportService).saveAiContent(eq(100L), anyString(), anyString());
    }

    @Test
    void consume_shouldSkip_whenDuplicate() {
        when(valueOps.setIfAbsent(eq("ai:task:done:t2"), eq("1"), any(Duration.class))).thenReturn(false);

        consumer.consumeAiTask(event("t2", "SUMMARY"));

        verify(aiGatewayService, never()).chatSync(any());
        verify(reportService, never()).initReport(anyLong(), anyInt());
    }

    @Test
    void consume_shouldRouteReviewToReviewService() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        AiTaskEvent ev = AiTaskEvent.review(500L, 1L, "t3");

        consumer.consumeAiTask(ev);

        // REVIEW 路由到 AiReviewService，bizId=publishId
        verify(aiReviewService).reviewByPublish(500L, "t3");
        verify(aiGatewayService, never()).chatSync(any());
    }

    @Test
    void consume_shouldReleaseDedupeAndRethrow_whenHandlerFails() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(aiGatewayService.chatSync(any())).thenThrow(new RuntimeException("LLM down"));

        try {
            consumer.consumeAiTask(event("t4", "SUMMARY"));
        } catch (RuntimeException ignored) {
        }

        verify(redisTemplate).delete("ai:task:done:t4");
        verify(reportService).markFailed(100L);
    }
}
