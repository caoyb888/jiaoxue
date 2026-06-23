package cn.smu.edu.stat.service.impl;

import cn.smu.edu.common.event.TeachingEvent;
import cn.smu.edu.stat.domain.vo.LessonRealtimeVO;
import cn.smu.edu.stat.domain.vo.RealtimeOverviewVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RealtimeStatServiceImplTest {

    @Mock
    StringRedisTemplate redis;

    @Mock
    ZSetOperations<String, String> zSet;

    RealtimeStatServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForZSet()).thenReturn(zSet);
        service = new RealtimeStatServiceImpl(redis);
    }

    @Test
    void record_shouldTouchLessonOnlineAndEventBuckets_whenStudentPresent() {
        TeachingEvent event = new TeachingEvent("BARRAGE", 100L, 9L, Map.of("studentId", 7));

        service.record(event, "BARRAGE");

        // 活跃课堂集合 + 单课堂事件桶 + 全局事件桶 + 全局在线 + 单课堂在线 = 5 个键 ZADD
        verify(zSet).add(eq("stat:rt:lessons"), eq("100"), anyDouble());
        verify(zSet).add(eq("stat:rt:online"), eq("7"), anyDouble());
        verify(zSet).add(eq("stat:rt:online:100"), eq("7"), anyDouble());
        verify(zSet).add(eq("stat:rt:events:BARRAGE"), any(), anyDouble());
        verify(zSet).add(eq("stat:rt:lesson:100:events:BARRAGE"), any(), anyDouble());
        // 每个键写入后都刷新 5min TTL
        verify(redis).expire(eq("stat:rt:lessons"), eq(Duration.ofMinutes(5)));
    }

    @Test
    void record_shouldSkipOnlineKeys_whenNoStudentId() {
        service.record(new TeachingEvent("QUESTION_PUBLISHED", 5L, 2L, Map.of()), "QUESTION");

        verify(zSet).add(eq("stat:rt:lessons"), eq("5"), anyDouble());
        verify(zSet, never()).add(eq("stat:rt:online"), any(), anyDouble());
        verify(zSet, never()).add(eq("stat:rt:online:5"), any(), anyDouble());
    }

    @Test
    void record_shouldDoNothing_whenBucketNull() {
        service.record(new TeachingEvent("ROLL_CALL", 1L, 2L, Map.of()), null);
        verify(zSet, never()).add(any(), any(), anyDouble());
    }

    @Test
    void overview_shouldPruneWindowAndReturnCardinalities() {
        when(zSet.zCard("stat:rt:lessons")).thenReturn(3L);
        when(zSet.zCard("stat:rt:online")).thenReturn(42L);
        when(zSet.zCard("stat:rt:events:BARRAGE")).thenReturn(15L);

        RealtimeOverviewVO vo = service.overview();

        assertThat(vo.windowMinutes()).isEqualTo(5);
        assertThat(vo.activeLessonCount()).isEqualTo(3L);
        assertThat(vo.onlineStudentCount()).isEqualTo(42L);
        assertThat(vo.eventVolume()).containsKeys("ATTEND", "BARRAGE", "QUESTION", "SCORE", "SLIDE");
        assertThat(vo.eventVolume().get("BARRAGE")).isEqualTo(15L);
        // 读取前剔除窗口外成员（score < now-window）
        verify(zSet).removeRangeByScore(eq("stat:rt:lessons"), eq(0.0), anyDouble());
    }

    @Test
    void overview_shouldTreatNullCardAsZero() {
        when(zSet.zCard(any())).thenReturn(null);
        RealtimeOverviewVO vo = service.overview();
        assertThat(vo.activeLessonCount()).isZero();
        assertThat(vo.onlineStudentCount()).isZero();
        assertThat(vo.eventVolume().values()).allMatch(v -> v == 0L);
    }

    @Test
    void lessonRealtime_shouldReadPerLessonKeys() {
        when(zSet.zCard("stat:rt:online:77")).thenReturn(8L);
        when(zSet.zCard("stat:rt:lesson:77:events:ATTEND")).thenReturn(8L);

        LessonRealtimeVO vo = service.lessonRealtime(77L);

        assertThat(vo.lessonId()).isEqualTo(77L);
        assertThat(vo.onlineStudentCount()).isEqualTo(8L);
        assertThat(vo.eventVolume().get("ATTEND")).isEqualTo(8L);
    }

    @Test
    void record_shouldSwallowRedisError() {
        when(zSet.add(any(), any(), anyDouble())).thenThrow(new RuntimeException("redis down"));
        // 旁路统计：异常不得冒泡影响事件主链路
        service.record(new TeachingEvent("BARRAGE", 1L, 2L, Map.of("studentId", 3)), "BARRAGE");
    }
}
