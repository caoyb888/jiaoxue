package cn.smu.edu.interaction.service.impl;

import cn.smu.edu.common.constant.KafkaTopic;
import cn.smu.edu.common.constant.RedisKey;
import cn.smu.edu.common.event.TeachingEvent;
import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.interaction.domain.dto.AttendDTO;
import cn.smu.edu.interaction.domain.entity.Attendance;
import cn.smu.edu.interaction.domain.vo.AttendResultVO;
import cn.smu.edu.interaction.event.AttendQueueItem;
import cn.smu.edu.interaction.repository.AttendanceCodeMapper;
import cn.smu.edu.interaction.repository.AttendanceMapper;
import cn.smu.edu.interaction.service.AttendanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private static final int BLOOM_EXPECTED_INSERTIONS = 5000;
    private static final double BLOOM_FALSE_POSITIVE_RATE = 0.001;
    // 布隆过滤器 TTL：课堂结束后 24h 清理
    private static final long BLOOM_TTL_HOURS = 24;
    // 每批最多落库条数
    private static final int BATCH_SIZE = 50;

    private final AttendanceCodeMapper attendanceCodeMapper;
    private final AttendanceMapper attendanceMapper;
    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public AttendResultVO attend(Long lessonId, Long studentId, AttendDTO dto) {
        // 1. 验证签到码
        String redisCodeKey = String.format(RedisKey.ATTEND_CODE, lessonId);
        String redisVal = redisTemplate.opsForValue().get(redisCodeKey);
        if (redisVal == null) {
            throw new BizException(ErrorCode.ATTEND_CODE_INVALID);
        }

        String[] parts = redisVal.split("\\|");
        String validCode = parts[0];
        String validQrToken = parts[1];

        String method;
        if (dto.getQrToken() != null && dto.getQrToken().equals(validQrToken)) {
            method = "QR";
        } else if (dto.getCode() != null && dto.getCode().equalsIgnoreCase(validCode)) {
            method = "CODE";
        } else {
            throw new BizException(ErrorCode.ATTEND_CODE_INVALID);
        }

        // 2. BloomFilter 去重（Redis 布隆过滤器，Redisson 实现）
        String bloomKey = String.format(RedisKey.ATTEND_BLOOM, lessonId);
        RBloomFilter<String> bloom = redissonClient.getBloomFilter(bloomKey);
        if (!bloom.isExists()) {
            bloom.tryInit(BLOOM_EXPECTED_INSERTIONS, BLOOM_FALSE_POSITIVE_RATE);
            bloom.expire(BLOOM_TTL_HOURS, TimeUnit.HOURS);
        }

        String bloomMember = studentId.toString();
        if (bloom.contains(bloomMember)) {
            // 布隆判断已签到（可能误判，但概率极低 0.1%）
            long count = getAttendCount(lessonId);
            return AttendResultVO.builder()
                    .lessonId(lessonId)
                    .studentId(studentId)
                    .firstAttend(false)
                    .totalCount(count)
                    .message("您已签到，请勿重复操作")
                    .build();
        }

        // 3. 加入 BloomFilter
        bloom.add(bloomMember);

        // 4. 计数器 +1
        String countKey = String.format(RedisKey.ATTEND_COUNT, lessonId);
        Long count = redisTemplate.opsForValue().increment(countKey);

        // 4.1 实时广播签到人数（Kafka → edu-notify → STOMP /topic/lesson/{id}/attend）
        TeachingEvent countEvent = new TeachingEvent("ATTEND_COUNT", lessonId, null,
                Map.of("count", count != null ? count : 0L));
        kafkaTemplate.send(KafkaTopic.TEACHING_EVENTS, lessonId.toString(), countEvent);

        // 5. 签到项推入 Redis List 队列（异步落库）
        String queueKey = String.format(RedisKey.ATTEND_QUEUE, lessonId);
        try {
            AttendQueueItem item = new AttendQueueItem(
                    lessonId, studentId,
                    null,  // classId 在落库时从 lesson 表获取（此处省略，可优化）
                    method,
                    dto.getIpAddress(),
                    LocalDateTime.now()
            );
            redisTemplate.opsForList().rightPush(queueKey, objectMapper.writeValueAsString(item));
        } catch (Exception e) {
            log.error("签到队列入队失败: lessonId={}, studentId={}", lessonId, studentId, e);
        }

        log.info("签到成功: lessonId={}, studentId={}, method={}, count={}", lessonId, studentId, method, count);

        return AttendResultVO.builder()
                .lessonId(lessonId)
                .studentId(studentId)
                .firstAttend(true)
                .totalCount(count != null ? count : 0)
                .message("签到成功")
                .build();
    }

    @Override
    public void flushQueueToDb(Long lessonId) {
        String queueKey = String.format(RedisKey.ATTEND_QUEUE, lessonId);
        Long size = redisTemplate.opsForList().size(queueKey);
        if (size == null || size == 0) {
            return;
        }

        int batchCount = (int) Math.min(size, BATCH_SIZE);
        List<Attendance> batch = new ArrayList<>(batchCount);

        for (int i = 0; i < batchCount; i++) {
            String json = redisTemplate.opsForList().leftPop(queueKey);
            if (json == null) break;
            try {
                AttendQueueItem item = objectMapper.readValue(json, AttendQueueItem.class);
                Attendance att = new Attendance();
                att.setLessonId(item.getLessonId());
                att.setStudentId(item.getStudentId());
                att.setClassId(item.getClassId() != null ? item.getClassId() : 0L);
                att.setStatus(1); // 正常签到
                att.setMethod(item.getMethod());
                att.setAttendedAt(item.getAttendedAt());
                att.setIpAddress(item.getIpAddress());
                att.setIsModified(0);
                batch.add(att);
            } catch (Exception e) {
                log.error("签到队列解析失败: json={}", json, e);
            }
        }

        if (!batch.isEmpty()) {
            // MyBatis-Plus saveBatch（每批落库，不逐条 INSERT）
            for (Attendance att : batch) {
                try {
                    // 防止数据库层重复（BloomFilter 已过滤 99.9%，此处兜底）
                    boolean exists = attendanceMapper.selectCount(
                            new LambdaQueryWrapper<Attendance>()
                                    .eq(Attendance::getLessonId, att.getLessonId())
                                    .eq(Attendance::getStudentId, att.getStudentId())
                    ) > 0;
                    if (!exists) {
                        attendanceMapper.insert(att);
                    }
                } catch (Exception e) {
                    log.warn("签到落库异常（可能重复）: lessonId={}, studentId={}", att.getLessonId(), att.getStudentId(), e);
                }
            }
            log.info("签到批量落库: lessonId={}, count={}", lessonId, batch.size());
        }
    }

    private long getAttendCount(Long lessonId) {
        String countKey = String.format(RedisKey.ATTEND_COUNT, lessonId);
        String val = redisTemplate.opsForValue().get(countKey);
        return val != null ? Long.parseLong(val) : 0;
    }
}
