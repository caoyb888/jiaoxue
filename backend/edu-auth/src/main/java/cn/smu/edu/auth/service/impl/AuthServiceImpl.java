package cn.smu.edu.auth.service.impl;

import cn.smu.edu.auth.domain.dto.SmsLoginDTO;
import cn.smu.edu.auth.domain.vo.TokenVO;
import cn.smu.edu.auth.service.AuthService;
import cn.smu.edu.common.constant.RedisKey;
import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final StringRedisTemplate redisTemplate;
    private final UserQueryPort userQueryPort;
    private final JwtTokenService jwtTokenService;

    @Override
    public TokenVO loginByPhone(SmsLoginDTO dto) {
        String codeKey = String.format(RedisKey.SMS_CODE, dto.getPhone());
        String savedCode = redisTemplate.opsForValue().get(codeKey);
        if (savedCode == null || !savedCode.equals(dto.getCode())) {
            throw new BizException(ErrorCode.SMS_CODE_ERROR);
        }

        UserInfo user = userQueryPort.findByPhone(dto.getPhone());
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        if (!user.isEnabled()) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }

        redisTemplate.delete(codeKey);
        log.info("手机号登录成功: userId={}", user.getId());
        return jwtTokenService.issueToken(user);
    }

    @Override
    public TokenVO refreshToken(String refreshToken) {
        String userId = jwtTokenService.validateRefreshToken(refreshToken);
        if (userId == null) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED);
        }
        UserInfo user = userQueryPort.findById(Long.parseLong(userId));
        if (user == null || !user.isEnabled()) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
        return jwtTokenService.issueToken(user);
    }
}
