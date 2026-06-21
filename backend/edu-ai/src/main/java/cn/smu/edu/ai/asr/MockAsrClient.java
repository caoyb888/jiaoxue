package cn.smu.edu.ai.asr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Mock ASR 客户端（默认）。无需真实科大讯飞 key，本地/CI 即可联调转写链路。
 *
 * 行为：每收到一帧非空音频即同步回调一个落定句段（占位文本），endStream 时补一段收尾文本。
 * 时间戳按 40ms/帧 累加，模拟真实流式节奏。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.asr.mock-mode", havingValue = "true", matchIfMissing = true)
public class MockAsrClient implements AsrClient {

    /** 每帧约 40ms（16k/16bit/单声道，1280 字节） */
    private static final long FRAME_MS = 40;

    @Override
    public AsrSession open(String sessionId, Consumer<AsrSegment> onSegment) {
        log.info("[MockASR] 会话开启: sessionId={}", sessionId);
        return new MockSession(sessionId, onSegment);
    }

    private static final class MockSession implements AsrSession {
        private final String sessionId;
        private final Consumer<AsrSegment> onSegment;
        private long cursorMs = 0;
        private int frameCount = 0;

        MockSession(String sessionId, Consumer<AsrSegment> onSegment) {
            this.sessionId = sessionId;
            this.onSegment = onSegment;
        }

        @Override
        public void sendAudio(byte[] frame) {
            if (frame == null || frame.length == 0) {
                return;
            }
            long begin = cursorMs;
            cursorMs += FRAME_MS;
            frameCount++;
            onSegment.accept(new AsrSegment(
                    "（模拟转写）第 " + frameCount + " 段课堂语音内容。",
                    begin, cursorMs, true));
        }

        @Override
        public void endStream() {
            onSegment.accept(new AsrSegment("（本节课转写结束）", cursorMs, cursorMs, true));
        }

        @Override
        public void close() {
            log.info("[MockASR] 会话关闭: sessionId={}, 帧数={}", sessionId, frameCount);
        }
    }
}
