package cn.smu.edu.notify.config;

import cn.smu.edu.notify.handler.JwtHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * STOMP over WebSocket 配置
 *
 * Topic 规范：
 *   /topic/lesson/{id}/slide      — 课件翻页广播（教师→全班）
 *   /topic/lesson/{id}/attend     — 签到人数广播（实时计数）
 *   /topic/lesson/{id}/barrage    — 弹幕广播（前台匿名）
 *   /topic/lesson/{id}/roll-call  — 随机点名结果广播
 *   /topic/lesson/{id}/quiz       — 教师发题广播
 *   /topic/lesson/{id}/ai         — AI 任务完成通知（摘要/思维导图）
 *   /queue/user/{userId}          — 个人消息（单播，考试告警）
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*")
                .withSockJS()                          // 兼容不支持 WebSocket 的环境
                .setStreamBytesLimit(512 * 1024)
                .setHttpMessageCacheSize(1000)
                .setDisconnectDelay(30 * 1000);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 客户端订阅前缀
        registry.enableSimpleBroker("/topic", "/queue");
        // 客户端发送消息前缀（服务端 @MessageMapping 处理）
        registry.setApplicationDestinationPrefixes("/app");
        // 用户专属消息前缀（convertAndSendToUser 时自动加此前缀）
        registry.setUserDestinationPrefix("/user");
    }
}
