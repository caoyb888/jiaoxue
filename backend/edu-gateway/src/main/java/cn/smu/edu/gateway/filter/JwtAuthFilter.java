package cn.smu.edu.gateway.filter;

import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper;
    private final PublicKey jwtPublicKey;

    // 白名单：无需 JWT 的路径
    private static final List<String> WHITE_LIST = List.of(
            "/api/v1/auth/login/**",
            "/api/v1/auth/sms/**",
            "/api/v1/auth/wechat/**",
            "/actuator/**",
            "/doc.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (WHITE_LIST.stream().anyMatch(p -> pathMatcher.match(p, path))) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange.getRequest());
        if (token == null) {
            return unauthorized(exchange, ErrorCode.UNAUTHORIZED);
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtPublicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 将用户信息注入请求头，下游服务通过 UserContextFilter 读取
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", String.valueOf(claims.get("userId")))
                    .header("X-Username", String.valueOf(claims.get("username")))
                    .header("X-Dept-Id", String.valueOf(claims.get("deptId")))
                    .header("X-Roles", String.valueOf(claims.get("roles")))
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (JwtException e) {
            log.warn("JWT 验证失败: path={}, reason={}", path, e.getMessage());
            return unauthorized(exchange, ErrorCode.TOKEN_INVALID);
        }
    }

    private String extractToken(ServerHttpRequest request) {
        String bearer = request.getHeaders().getFirst("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, ErrorCode errorCode) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(Result.fail(errorCode));
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
