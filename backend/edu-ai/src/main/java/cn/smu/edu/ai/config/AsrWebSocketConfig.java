package cn.smu.edu.ai.config;

import cn.smu.edu.ai.service.AsrTranscriptionService;
import cn.smu.edu.ai.ws.AsrWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 注册课堂转写 WebSocket 端点 /ws/asr。
 * 允许跨域（开发期，生产由网关统一鉴权与同源策略管控）。
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class AsrWebSocketConfig implements WebSocketConfigurer {

    private final AsrTranscriptionService transcriptionService;
    private final ObjectMapper objectMapper;

    @Bean
    public AsrWebSocketHandler asrWebSocketHandler() {
        return new AsrWebSocketHandler(transcriptionService, objectMapper);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(asrWebSocketHandler(), "/ws/asr")
                .setAllowedOriginPatterns("*");
    }
}
