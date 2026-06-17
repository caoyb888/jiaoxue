package cn.smu.edu.notify.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器：从请求参数或 Header 提取 JWT/UserId，注入 WebSocket Session 属性
 *
 * 网关已验证 JWT 并将用户信息注入 Header（X-User-Id, X-Username 等）。
 * 此拦截器只从 Header 或 Query 参数中读取这些已解析的用户信息。
 */
@Slf4j
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // 从网关透传 Header 读取用户信息
        String userId = request.getHeaders().getFirst("X-User-Id");
        String username = request.getHeaders().getFirst("X-Username");

        if (!StringUtils.hasText(userId)) {
            // SockJS 握手通过 URL Query 参数传递（兜底）
            String query = request.getURI().getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] kv = param.split("=");
                    if (kv.length == 2) {
                        if ("userId".equals(kv[0])) userId = kv[1];
                        if ("username".equals(kv[0])) username = kv[1];
                    }
                }
            }
        }

        if (!StringUtils.hasText(userId)) {
            log.warn("WebSocket 握手拒绝：缺少用户身份信息, uri={}", request.getURI());
            return false;
        }

        attributes.put("userId", Long.parseLong(userId));
        if (StringUtils.hasText(username)) {
            attributes.put("username", username);
        }

        log.debug("WebSocket 握手成功: userId={}", userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
