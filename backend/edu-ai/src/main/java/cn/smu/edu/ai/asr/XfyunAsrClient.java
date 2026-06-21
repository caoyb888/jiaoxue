package cn.smu.edu.ai.asr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * 科大讯飞实时语音转写（RTASR）客户端。
 * 仅当 {@code ai.asr.mock-mode=false} 时启用，需配置 appId / apiKey。
 *
 * 握手鉴权（RTASR 规范）：
 *   ts    = 当前秒级时间戳
 *   md5   = MD5(appId + ts)
 *   signa = Base64(HmacSHA1(md5, apiKey))
 *   wss://rtasr.xfyun.cn/v1/ws?appid={appId}&ts={ts}&signa={urlencode(signa)}
 *
 * 音频要求：16k/16bit/单声道 PCM；结束时发送 {"end": true}。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.asr.mock-mode", havingValue = "false")
public class XfyunAsrClient implements AsrClient {

    private final String hostUrl;
    private final String appId;
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public XfyunAsrClient(
            @Value("${ai.asr.xfyun.host-url:wss://rtasr.xfyun.cn/v1/ws}") String hostUrl,
            @Value("${ai.asr.xfyun.app-id:}") String appId,
            @Value("${ai.asr.xfyun.api-key:}") String apiKey) {
        this.hostUrl = hostUrl;
        this.appId = appId;
        this.apiKey = apiKey;
        log.info("[XfyunASR] 已启用真实转写，appId={}", appId);
    }

    @Override
    public AsrSession open(String sessionId, Consumer<AsrSegment> onSegment) {
        try {
            URI uri = URI.create(buildAuthUrl());
            WebSocket ws = httpClient.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .buildAsync(uri, new RtasrListener(sessionId, onSegment))
                    .join();
            log.info("[XfyunASR] 会话开启: sessionId={}", sessionId);
            return new XfyunSession(sessionId, ws);
        } catch (Exception e) {
            throw new IllegalStateException("科大讯飞 RTASR 连接失败: " + e.getMessage(), e);
        }
    }

    private String buildAuthUrl() throws Exception {
        String ts = String.valueOf(System.currentTimeMillis() / 1000);
        String baseString = appId + ts;
        String md5 = md5Hex(baseString);
        String signa = base64HmacSha1(md5, apiKey);
        return hostUrl
                + "?appid=" + appId
                + "&ts=" + ts
                + "&signa=" + URLEncoder.encode(signa, StandardCharsets.UTF_8);
    }

    private static String md5Hex(String s) throws Exception {
        byte[] digest = MessageDigest.getInstance("MD5").digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String base64HmacSha1(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(raw);
    }

    /** 解析 RTASR 返回的 result.data，拼接落定（type=0）句段文本 */
    private void handleMessage(String payload, Consumer<AsrSegment> onSegment) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode actionNode = root.get("action");
            if (actionNode == null || !"result".equals(actionNode.asText())) {
                return; // started / error 等控制消息忽略
            }
            JsonNode data = objectMapper.readTree(root.get("data").asText());
            JsonNode cn = data.path("cn").path("st");
            boolean isFinal = "0".equals(cn.path("type").asText());
            long begin = cn.path("bg").asLong(0);
            long end = cn.path("ed").asLong(0);
            StringBuilder text = new StringBuilder();
            for (JsonNode rt : cn.path("rt")) {
                for (JsonNode wsNode : rt.path("ws")) {
                    JsonNode cw = wsNode.path("cw");
                    if (cw.isArray() && !cw.isEmpty()) {
                        text.append(cw.get(0).path("w").asText());
                    }
                }
            }
            if (isFinal && !text.isEmpty()) {
                onSegment.accept(new AsrSegment(text.toString(), begin, end, true));
            }
        } catch (Exception e) {
            log.warn("[XfyunASR] 结果解析失败: {}", e.getMessage());
        }
    }

    /** WebSocket 监听器：累积分片消息并解析 */
    private final class RtasrListener implements WebSocket.Listener {
        private final String sessionId;
        private final Consumer<AsrSegment> onSegment;
        private final StringBuilder buffer = new StringBuilder();

        RtasrListener(String sessionId, Consumer<AsrSegment> onSegment) {
            this.sessionId = sessionId;
            this.onSegment = onSegment;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                handleMessage(buffer.toString(), onSegment);
                buffer.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.error("[XfyunASR] 连接异常: sessionId={}", sessionId, error);
        }
    }

    private static final class XfyunSession implements AsrSession {
        private final String sessionId;
        private final WebSocket ws;

        XfyunSession(String sessionId, WebSocket ws) {
            this.sessionId = sessionId;
            this.ws = ws;
        }

        @Override
        public void sendAudio(byte[] frame) {
            if (frame == null || frame.length == 0) {
                return;
            }
            ws.sendBinary(ByteBuffer.wrap(frame), true);
        }

        @Override
        public void endStream() {
            ws.sendText("{\"end\": true}", true);
        }

        @Override
        public void close() {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "done");
            log.info("[XfyunASR] 会话关闭: sessionId={}", sessionId);
        }
    }
}
