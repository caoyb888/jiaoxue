package cn.smu.edu.ai.asr;

/**
 * ASR 识别落定的一个句段。seq 由上层会话统一分配，此处不含。
 *
 * @param text     识别文本
 * @param beginMs  相对会话起点的起始毫秒
 * @param endMs    相对会话起点的结束毫秒
 * @param finalFlag 是否落定（true 落定，false 中间态）
 */
public record AsrSegment(String text, long beginMs, long endMs, boolean finalFlag) {
}
