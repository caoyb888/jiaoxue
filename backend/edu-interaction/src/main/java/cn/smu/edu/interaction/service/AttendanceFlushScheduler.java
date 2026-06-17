package cn.smu.edu.interaction.service;

import cn.smu.edu.common.constant.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 签到批量落库调度器（每 500ms 扫描所有活跃课堂的签到队列）
 * C1 约束：禁止签到直连 MySQL，必须走此批量路径
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceFlushScheduler {

    private final AttendanceService attendanceService;
    private final StringRedisTemplate redisTemplate;

    @Scheduled(fixedDelay = 500)
    public void flush() {
        // 扫描所有 attend:queue:* 键，对每个活跃课堂执行落库
        Set<String> keys = redisTemplate.keys("attend:queue:*");
        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            Long size = redisTemplate.opsForList().size(key);
            if (size == null || size == 0) continue;

            // 从 key 中解析 lessonId（格式：attend:queue:{lessonId}）
            String lessonIdStr = key.substring("attend:queue:".length());
            try {
                Long lessonId = Long.parseLong(lessonIdStr);
                attendanceService.flushQueueToDb(lessonId);
            } catch (Exception e) {
                log.error("签到队列落库调度异常: key={}", key, e);
            }
        }
    }
}
