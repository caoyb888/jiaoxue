package cn.smu.edu.auth.service;

import cn.smu.edu.common.constant.RedisKey;
import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final StringRedisTemplate redisTemplate;

    private static final String RATE_LIMIT_KEY = "sms:rate:%s";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RATE_TTL = Duration.ofMinutes(1);

    public void sendCode(String phone) {
        // 限频：1分钟内只能发送1次
        String rateKey = String.format(RATE_LIMIT_KEY, phone);
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(rateKey, "1", RATE_TTL);
        if (Boolean.FALSE.equals(isNew)) {
            throw new BizException(ErrorCode.SMS_SEND_TOO_FREQUENT);
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 999999));
        String codeKey = String.format(RedisKey.SMS_CODE, phone);
        redisTemplate.opsForValue().set(codeKey, code, CODE_TTL);

        // 实际项目中调用短信服务商 API（阿里云/腾讯云）
        // 日志脱敏：不打印完整手机号
        log.info("短信验证码已发送: phone={}****", phone.substring(0, 3));
    }
}
