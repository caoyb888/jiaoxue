package cn.smu.edu.ai.asr;

/**
 * 一次 ASR 流式识别会话。上层每收到一帧课堂音频调用 {@link #sendAudio}，
 * 识别结果通过 {@link AsrClient#open} 注册的回调异步/同步回传；
 * 音频结束时调用 {@link #endStream} 触发尾包落定，最后 {@link #close} 释放连接。
 */
public interface AsrSession extends AutoCloseable {

    /** 发送一帧音频（建议 16k/16bit/单声道 PCM，约 40ms/帧） */
    void sendAudio(byte[] frame);

    /** 通知上游音频已结束，等待最后的落定结果 */
    void endStream();

    @Override
    void close();
}
