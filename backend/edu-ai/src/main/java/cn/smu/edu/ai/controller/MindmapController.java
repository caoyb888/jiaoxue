package cn.smu.edu.ai.controller;

import cn.smu.edu.ai.domain.entity.LessonReport;
import cn.smu.edu.ai.domain.vo.MindmapVO;
import cn.smu.edu.ai.service.LessonReportService;
import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.constant.KafkaTopic;
import cn.smu.edu.common.event.AiTaskEvent;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai/mindmap")
@RequiredArgsConstructor
public class MindmapController {

    private final LessonReportService reportService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
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

    /** 重新生成思维导图：异步发 Kafka MINDMAP 任务，置 gen_status=生成中 */
    @OperationLog(module = "ai", operation = "重新生成思维导图")
    @PostMapping("/{lessonId}/regenerate")
    public Result<String> regenerate(@PathVariable Long lessonId) {
        Long teacherId = UserContext.getUserId();
        String taskId = UUID.randomUUID().toString();
        AiTaskEvent event = new AiTaskEvent(taskId, lessonId, teacherId, null,
                "MINDMAP", LocalDateTime.now(), null);
        kafkaTemplate.send(KafkaTopic.AI_TASKS, taskId, event);
        reportService.markGenerating(lessonId);
        log.info("触发思维导图重新生成: lessonId={}, taskId={}", lessonId, taskId);
        return Result.ok(taskId);
    }

    @OperationLog(module = "ai", operation = "更新思维导图可见性")
    @PutMapping("/{lessonId}")
    public Result<Void> updateVisibility(@PathVariable Long lessonId,
                                         @RequestBody java.util.Map<String, Boolean> body) {
        boolean visible = Boolean.TRUE.equals(body.get("studentVisible"));
        reportService.updateMindmapVisible(lessonId, visible);
        log.info("更新思维导图可见性: lessonId={}, visible={}", lessonId, visible);
        return Result.ok();
    }
}
