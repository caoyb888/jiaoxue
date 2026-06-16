package cn.smu.edu.auth.service.impl;

import cn.smu.edu.auth.domain.dto.SmsLoginDTO;
import cn.smu.edu.auth.domain.vo.TokenVO;
import cn.smu.edu.auth.service.AuthService;
import cn.smu.edu.common.constant.RedisKey;
import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final StringRedisTemplate redisTemplate;
    private final PrivateKey jwtPrivateKey;
    private final UserQueryPort userQueryPort; // 防腐层接口，调用 edu-user

    private static final long ACCESS_TOKEN_TTL_SEC  = 7200L;   // 2小时
    private static final long REFRESH_TOKEN_TTL_SEC = 604800L; // 7天

    @Override
    public TokenVO loginByPhone(SmsLoginDTO dto) {
        // 1. 校验短信验证码（Redis TTL 5min）
        String codeKey = String.format(RedisKey.SMS_CODE, dto.getPhone());
        String savedCode = redisTemplate.opsForValue().get(codeKey);
        if (savedCode == null || !savedCode.equals(dto.getCode())) {
            throw new BizException(ErrorCode.SMS_CODE_ERROR);
        }

        // 2. 查询用户（phone_cipher AES-256 加密存储，通过 edu-user 查询）
        UserInfo user = userQueryPort.findByPhone(dto.getPhone());
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        if (!user.isEnabled()) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 3. 删除验证码（单次有效）
        redisTemplate.delete(codeKey);

        // 4. 签发 JWT RS256
        return buildTokenPair(user);
    }

    @Override
    public TokenVO refreshToken(String refreshToken) {
        String userId = validateRefreshToken(refreshToken);
        UserInfo user = userQueryPort.findById(Long.parseLong(userId));
        if (user == null || !user.isEnabled()) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
        return buildTokenPair(user);
    }

    private TokenVO buildTokenPair(UserInfo user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("deptId", user.getDeptId());
        claims.put("roles", String.join(",", user.getRoles()));

        Date now = new Date();
        String accessToken = Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_TOKEN_TTL_SEC * 1000))
                .signWith(jwtPrivateKey, Jwts.SIG.RS256)
                .compact();

        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        // refresh token 存 Redis，TTL 7天
        redisTemplate.opsForValue().set(
                "auth:refresh:" + refreshToken,
                String.valueOf(user.getId()),
                Duration.ofSeconds(REFRESH_TOKEN_TTL_SEC)
        );

        log.info("用户登录成功: userId={}", user.getId());

        return TokenVO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(ACCESS_TOKEN_TTL_SEC)
                .tokenType("Bearer")
                .build();
    }

    private String validateRefreshToken(String refreshToken) {
        String userId = redisTemplate.opsForValue().get("auth:refresh:" + refreshToken);
        if (userId == null) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED);
        }
        return userId;
    }
}
