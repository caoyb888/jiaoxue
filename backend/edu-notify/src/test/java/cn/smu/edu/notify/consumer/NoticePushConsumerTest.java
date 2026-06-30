package cn.smu.edu.notify.consumer;

import cn.smu.edu.common.event.NoticePublishEvent;
import cn.smu.edu.notify.domain.entity.Notice;
import cn.smu.edu.notify.repository.NoticeMapper;
import cn.smu.edu.notify.service.NoticeTargetResolver;
import cn.smu.edu.notify.service.WechatPushService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticePushConsumerTest {

    @Mock
    NoticeMapper noticeMapper;
    @Mock
    NoticeTargetResolver targetResolver;
    @Mock
    WechatPushService wechatPushService;
    @Mock
    StringRedisTemplate redisTemplate;
    @Mock
    ValueOperations<String, String> valueOps;
    @InjectMocks
    NoticePushConsumer consumer;

    private NoticePublishEvent event() {
        return NoticePublishEvent.builder().noticeId(99L).title("通知").scope("SCHOOL").build();
    }

    @Test
    void consume_firstTime_shouldResolveAndPush() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("notify:notice:push:99"), eq("1"), any(Duration.class)))
                .thenReturn(true);
        Notice notice = Notice.builder().id(99L).scope("SCHOOL").build();
        when(noticeMapper.selectById(99L)).thenReturn(notice);
        when(targetResolver.resolve(notice)).thenReturn(List.of(1L, 2L));

        consumer.consume(event());

        verify(wechatPushService).pushNotice(notice, List.of(1L, 2L));
    }

    @Test
    void consume_duplicate_shouldSkip() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        consumer.consume(event());

        verify(noticeMapper, never()).selectById(any());
        verify(wechatPushService, never()).pushNotice(any(), any());
    }

    @Test
    void consume_nullEvent_shouldIgnore() {
        consumer.consume(null);
        verify(redisTemplate, never()).opsForValue();
    }
}
