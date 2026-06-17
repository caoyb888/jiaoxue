package cn.smu.edu.interaction.service.impl;

import cn.smu.edu.common.constant.RedisKey;
import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.interaction.domain.entity.AttendanceCode;
import cn.smu.edu.interaction.domain.vo.AttendCodeVO;
import cn.smu.edu.interaction.repository.AttendanceCodeMapper;
import cn.smu.edu.interaction.service.AttendanceCodeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceCodeServiceImpl implements AttendanceCodeService {

    private static final int CODE_TTL_SECONDS = 300; // 5min
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 去掉易混淆字符 0/O/1/I
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AttendanceCodeMapper attendanceCodeMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public AttendCodeVO generateCode(Long lessonId, Long teacherId) {
        String code = randomCode(6);
        String qrToken = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expireAt = LocalDateTime.now().plusSeconds(CODE_TTL_SECONDS);

        // 写 Redis（attend:code:{lessonId}，值为 code|qrToken 方便原子读取）
        String redisKey = String.format(RedisKey.ATTEND_CODE, lessonId);
        String redisVal = code + "|" + qrToken;
        redisTemplate.opsForValue().set(redisKey, redisVal, CODE_TTL_SECONDS, TimeUnit.SECONDS);

        // 写 attendance_code 表（ON DUPLICATE KEY 幂等更新，因为有 UNIQUE KEY uk_lesson_active）
        AttendanceCode entity = new AttendanceCode();
        entity.setLessonId(lessonId);
        entity.setCode(code);
        entity.setQrToken(qrToken);
        entity.setExpireAt(expireAt);

        // 先尝试删除旧记录（UNIQUE KEY 限制一课堂一条），再插入
        attendanceCodeMapper.delete(new LambdaQueryWrapper<AttendanceCode>()
                .eq(AttendanceCode::getLessonId, lessonId));
        attendanceCodeMapper.insert(entity);

        log.info("签到码已生成: lessonId={}, code={}, expireAt={}", lessonId, code, expireAt);

        return AttendCodeVO.builder()
                .lessonId(lessonId)
                .code(code)
                .qrToken(qrToken)
                .expireAt(expireAt)
                .remainSeconds(CODE_TTL_SECONDS)
                .build();
    }

    @Override
    public AttendCodeVO getCurrentCode(Long lessonId) {
        String redisKey = String.format(RedisKey.ATTEND_CODE, lessonId);
        String redisVal = redisTemplate.opsForValue().get(redisKey);

        if (redisVal == null) {
            throw new BizException(ErrorCode.ATTEND_CODE_INVALID);
        }

        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        String[] parts = redisVal.split("\\|");
        String code = parts[0];
        String qrToken = parts[1];

        return AttendCodeVO.builder()
                .lessonId(lessonId)
                .code(code)
                .qrToken(qrToken)
                .expireAt(LocalDateTime.now().plusSeconds(ttl != null ? ttl : 0))
                .remainSeconds(ttl != null ? ttl : 0)
                .build();
    }

    private String randomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
