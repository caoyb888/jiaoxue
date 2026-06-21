package cn.smu.edu.ai.controller;

import cn.smu.edu.ai.domain.vo.TranscriptVO;
import cn.smu.edu.ai.repository.AiLectureTranscriptRepository;
import cn.smu.edu.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * S6-04 课堂转写查询。音频推流走 WebSocket /ws/asr，此处仅提供结果查询。
 */
@RestController
@RequestMapping("/api/v1/ai/transcript")
@RequiredArgsConstructor
public class AsrTranscriptController {

    private final AiLectureTranscriptRepository transcriptRepository;

    /** 查询某节课的转写结果（含全部分片） */
    @GetMapping("/{lessonId}")
    public Result<TranscriptVO> getTranscript(@PathVariable Long lessonId) {
        return transcriptRepository.findByLessonId(lessonId)
                .map(doc -> Result.ok(TranscriptVO.from(doc)))
                .orElseGet(() -> Result.ok(null));
    }
}
