package cn.smu.edu.auth.service.impl;

import cn.smu.edu.auth.domain.vo.TokenVO;
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
public class JwtTokenService {

    private final PrivateKey jwtPrivateKey;
    private final StringRedisTemplate redisTemplate;

    static final long ACCESS_TOKEN_TTL_SEC  = 7200L;
    static final long REFRESH_TOKEN_TTL_SEC = 604800L;

    TokenVO issueToken(UserInfo user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId",   user.getId());
        claims.put("username", user.getUsername());
        claims.put("deptId",   user.getDeptId());
        claims.put("roles",    String.join(",", user.getRoles()));

        Date now = new Date();
        String accessToken = Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_TOKEN_TTL_SEC * 1000))
                .signWith(jwtPrivateKey, Jwts.SIG.RS256)
                .compact();

        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                "auth:refresh:" + refreshToken,
                String.valueOf(user.getId()),
                Duration.ofSeconds(REFRESH_TOKEN_TTL_SEC));

        return TokenVO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(ACCESS_TOKEN_TTL_SEC)
                .tokenType("Bearer")
                .build();
    }

    String validateRefreshToken(String refreshToken) {
        return redisTemplate.opsForValue().get("auth:refresh:" + refreshToken);
    }
}
