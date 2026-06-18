package cn.smu.edu.ai.controller;

import cn.smu.edu.ai.domain.entity.LessonReport;
import cn.smu.edu.ai.domain.vo.MindmapVO;
import cn.smu.edu.ai.service.LessonReportService;
import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai/mindmap")
@RequiredArgsConstructor
public class MindmapController {

    private final LessonReportService reportService;
    private final ObjectMapper objectMapper;

    @GetMapping("/{lessonId}")
    public Result<MindmapVO> getMindmap(@PathVariable Long lessonId) {
        LessonReport report = reportService.getByLessonId(lessonId);

        if (report == null) {
            return Result.ok(MindmapVO.builder()
                    .lessonId(lessonId)
                    .genStatus("PENDING")
                    .studentVisible(false)
                    .build());
        }

        String statusStr = switch (report.getGenStatus()) {
            case 0 -> "PENDING";
            case 1 -> "GENERATING";
            case 2 -> "DONE";
            case 3 -> "FAILED";
            default -> "UNKNOWN";
        };

        JsonNode markmapJson = null;
        if (report.getAiMindmapJson() != null) {
            try {
                markmapJson = objectMapper.readTree(report.getAiMindmapJson());
            } catch (Exception e) {
                log.warn("思维导图JSON解析失败: lessonId={}", lessonId);
            }
        }

        return Result.ok(MindmapVO.builder()
                .lessonId(lessonId)
                .genStatus(statusStr)
                .markmapJson(markmapJson)
                .studentVisible(report.getMindmapVisible() == 1)
                .build());
    }

    @OperationLog(module = "ai", operation = "重新生成思维导图")
    @PostMapping("/{lessonId}/regenerate")
    public Result<Void> regenerate(@PathVariable Long lessonId) {
        // 置为生成中，Kafka 重发（S6 完整实现）
        log.info("触发思维导图重新生成: lessonId={}", lessonId);
        return Result.ok();
    }

    @OperationLog(module = "ai", operation = "更新思维导图可见性")
    @PutMapping("/{lessonId}")
    public Result<Void> updateVisibility(@PathVariable Long lessonId,
                                         @RequestBody java.util.Map<String, Boolean> body) {
        log.info("更新思维导图可见性: lessonId={}, visible={}", lessonId, body.get("studentVisible"));
        // S6 阶段完整实现
        return Result.ok();
    }
}
