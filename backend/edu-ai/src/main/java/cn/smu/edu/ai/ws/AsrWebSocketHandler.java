package cn.smu.edu.ai.ws;

import cn.smu.edu.ai.domain.document.TranscriptChunk;
import cn.smu.edu.ai.service.AsrTranscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 课堂转写 WebSocket 端点：教师端推流音频 → ASR 转写 → 实时回传字幕。
 *
 * 连接：/ws/asr?lessonId={lessonId}&teacherId={teacherId}
 *   - 二进制帧：课堂音频（16k/16bit/单声道 PCM）
 *   - 文本帧 {"action":"stop"}：主动结束（也可直接断开）
 *   - 服务端回传文本帧：落定分片 JSON {seq,text,beginMs,endMs}
 */
@Slf4j
@RequiredArgsConstructor
public class AsrWebSocketHandler extends AbstractWebSocketHandler {

    private final AsrTranscriptionService transcriptionService;
    private final ObjectMapper objectMapper;

    /** session.id -> lessonId，断开时定位会话 */
    private final Map<String, Long> sessionLesson = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        Long lessonId = longParam(session, "lessonId");
        Long teacherId = longParam(session, "teacherId");
        if (lessonId == null) {
            log.warn("ASR WS 缺少 lessonId，关闭连接: sessionId={}", session.getId());
            silentClose(session, CloseStatus.BAD_DATA);
            return;
        }
        sessionLesson.put(session.getId(), lessonId);
        // 并发安全包装：识别回调可能与音频帧不同线程
        WebSocketSession safe = new ConcurrentWebSocketSessionDecorator(session, 5000, 1024 * 1024);
        Consumer<TranscriptChunk> sink = chunk -> sendChunk(safe, chunk);
        transcriptionService.start(lessonId, teacherId, sink);
    }

    @Override
    protected void handleBinaryMessage(@NonNull WebSocketSession session, @NonNull BinaryMessage message) {
        Long lessonId = sessionLesson.get(session.getId());
        if (lessonId != null) {
            byte[] frame = new byte[message.getPayload().remaining()];
            message.getPayload().get(frame);
            transcriptionService.onAudio(lessonId, frame);
        }
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        if (message.getPayload().contains("stop")) {
            stop(session);
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        stop(session);
    }

    private void stop(WebSocketSession session) {
        Long lessonId = sessionLesson.remove(session.getId());
        if (lessonId != null) {
            transcriptionService.stop(lessonId);
        }
    }

    private void sendChunk(WebSocketSession session, TranscriptChunk chunk) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(chunk)));
        } catch (IOException e) {
            log.warn("ASR 字幕回传失败: sessionId={}", session.getId(), e);
        }
    }

    private Long longParam(WebSocketSession session, String name) {
        if (session.getUri() == null) {
            return null;
        }
        String v = UriComponentsBuilder.fromUri(session.getUri()).build()
                .getQueryParams().getFirst(name);
        try {
            return v == null ? null : Long.parseLong(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void silentClose(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException ignore) {
            // 关闭失败无需处理
        }
    }
}
