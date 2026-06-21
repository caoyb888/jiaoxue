package cn.smu.edu.ai.domain.vo;

import cn.smu.edu.ai.domain.document.AiLectureTranscript;
import cn.smu.edu.ai.domain.document.TranscriptChunk;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课堂转写查询响应。
 */
@Data
public class TranscriptVO {

    private Long lessonId;
    private String status;
    private List<TranscriptChunk> chunks;
    private String fullText;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    public static TranscriptVO from(AiLectureTranscript doc) {
        TranscriptVO vo = new TranscriptVO();
        vo.setLessonId(doc.getLessonId());
        vo.setStatus(doc.getStatus());
        vo.setChunks(doc.getChunks());
        vo.setFullText(doc.getFullText());
        vo.setStartedAt(doc.getStartedAt());
        vo.setEndedAt(doc.getEndedAt());
        return vo;
    }
}
