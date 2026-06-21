package cn.smu.edu.ai.domain.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 课堂转写分片（嵌入 ai_lecture_transcript.chunks 数组）。
 *
 * ASR 流式识别每输出一个落定的句段即生成一个分片，按 seq 递增追加。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptChunk {

    /** 分片序号（会话内自增，从 0 起） */
    private int seq;

    /** 识别文本 */
    private String text;

    /** 相对会话起点的起始毫秒 */
    private long beginMs;

    /** 相对会话起点的结束毫秒 */
    private long endMs;

    /** 是否为落定结果（false 为中间态，本系统仅落库落定结果） */
    private boolean finalFlag;

    private LocalDateTime createdAt;
}
