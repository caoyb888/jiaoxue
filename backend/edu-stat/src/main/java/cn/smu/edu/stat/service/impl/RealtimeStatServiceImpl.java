package cn.smu.edu.stat.service.impl;

import cn.smu.edu.common.event.TeachingEvent;
import cn.smu.edu.stat.domain.vo.LessonRealtimeVO;
import cn.smu.edu.stat.domain.vo.RealtimeOverviewVO;
import cn.smu.edu.stat.service.RealtimeStatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 基于 Redis ZSET 滑动窗口的实时统计聚合实现。
 *
 * <p><b>窗口模型：</b>每个指标是一个 ZSET，{@code member} 为去重维度（课堂/学生 ID 或事件唯一令牌），
 * {@code score} 为事件发生的 epochMillis。写入时 {@code ZADD} 并刷新键 TTL（{@link #TTL}）；
 * 读取时先 {@code ZREMRANGEBYSCORE} 剔除窗口外（早于 now-5min）的成员，再 {@code ZCARD} 取窗口内基数。
 * 这样既保证 5 分钟"当前态"语义，又靠 TTL 兜底回收无活动的键。
 *
 * <p>用 {@link StringRedisTemplate}（纯字符串读写，避免 JSON 序列化器与键空间污染），库 db=7。
 *
 * <p>架构约束：本聚合与签到削峰（C1）无关——这里消费的是已落 Kafka 的课堂事件，
 * 不在签到主链路上，属旁路统计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeStatServiceImpl implements RealtimeStatService {

    private static final Duration TTL = Duration.ofMinutes(WINDOW_MINUTES);
    private static final long WINDOW_MS = Duration.ofMinutes(WINDOW_MINUTES).toMillis();

    /** 活跃课堂集合（member=lessonId）。 */
    private static final String K_LESSONS = "stat:rt:lessons";
    /** 全局在线学生集合（member=studentId）。 */
    private static final String K_ONLINE = "stat:rt:online";
    /** 单课堂在线学生集合前缀（+lessonId）。 */
    private static final String K_ONLINE_LESSON = "stat:rt:online:";
    /** 全局事件桶发生量前缀（+bucket）。 */
    private static final String K_EVENTS = "stat:rt:events:";

    private final StringRedisTemplate redis;

    @Override
    public void record(TeachingEvent event, String bucket) {
        if (event == null || bucket == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Long lessonId = event.getLessonId();
        if (lessonId != null) {
            touch(K_LESSONS, String.valueOf(lessonId), now);
            touch(lessonEventKey(lessonId, bucket), token(), now);
        }
        touch(K_EVENTS + bucket, token(), now);

        Long studentId = studentId(event);
        if (studentId != null) {
            touch(K_ONLINE, String.valueOf(studentId), now);
            if (lessonId != null) {
                touch(K_ONLINE_LESSON + lessonId, String.valueOf(studentId), now);
            }
        }
    }

    @Override
    public RealtimeOverviewVO overview() {
        long now = System.currentTimeMillis();
        Map<String, Long> volume = new LinkedHashMap<>();
        for (String bucket : EVENT_BUCKETS) {
            volume.put(bucket, windowCount(K_EVENTS + bucket, now));
        }
        return new RealtimeOverviewVO(
                WINDOW_MINUTES,
                windowCount(K_LESSONS, now),
                windowCount(K_ONLINE, now),
                volume);
    }

    @Override
    public LessonRealtimeVO lessonRealtime(Long lessonId) {
        long now = System.currentTimeMillis();
        Map<String, Long> volume = new LinkedHashMap<>();
        for (String bucket : EVENT_BUCKETS) {
            volume.put(bucket, windowCount(lessonEventKey(lessonId, bucket), now));
        }
        return new LessonRealtimeVO(
                lessonId,
                WINDOW_MINUTES,
                windowCount(K_ONLINE_LESSON + lessonId, now),
                volume);
    }

    /** 写入一个窗口成员并刷新键 TTL。窗口外成员留待读取时惰性清理。 */
    private void touch(String key, String member, long now) {
        try {
            redis.opsForZSet().add(key, member, now);
            redis.expire(key, TTL);
        } catch (Exception ex) {
            // 实时统计为旁路能力，Redis 抖动不得影响事件主链路
            log.warn("实时统计写入失败: key={}", key, ex);
        }
    }

    /** 剔除窗口外成员后返回窗口内基数。 */
    private long windowCount(String key, long now) {
        try {
            redis.opsForZSet().removeRangeByScore(key, 0, now - WINDOW_MS);
            Long card = redis.opsForZSet().zCard(key);
            return card == null ? 0L : card;
        } catch (Exception ex) {
            log.warn("实时统计读取失败: key={}", key, ex);
            return 0L;
        }
    }

    private static String lessonEventKey(Long lessonId, String bucket) {
        return "stat:rt:lesson:" + lessonId + ":events:" + bucket;
    }

    private static String token() {
        return UUID.randomUUID().toString();
    }

    private static Long studentId(TeachingEvent event) {
        Map<String, Object> payload = event.getPayload();
        if (payload == null) {
            return null;
        }
        return payload.get("studentId") instanceof Number n ? n.longValue() : null;
    }
}
