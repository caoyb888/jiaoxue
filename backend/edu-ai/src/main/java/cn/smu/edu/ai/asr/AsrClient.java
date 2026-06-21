package cn.smu.edu.ai.asr;

import java.util.function.Consumer;

/**
 * ASR 厂商客户端抽象。默认 {@link MockAsrClient}（无 key 即可跑、CI 友好），
 * 配置 {@code ai.asr.mock-mode=false} 时切换为 {@link XfyunAsrClient}（科大讯飞实时转写）。
 */
public interface AsrClient {

    /**
     * 打开一个识别会话。
     *
     * @param sessionId 会话标识（用于日志/排障）
     * @param onSegment 落定句段回调，由实现方在识别到结果时调用
     * @return 可发送音频的会话句柄
     */
    AsrSession open(String sessionId, Consumer<AsrSegment> onSegment);
}
