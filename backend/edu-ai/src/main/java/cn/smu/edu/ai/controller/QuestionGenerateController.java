package cn.smu.edu.ai.controller;

import cn.smu.edu.ai.domain.dto.QuestionGenerateDTO;
import cn.smu.edu.ai.domain.vo.QuestionTaskVO;
import cn.smu.edu.ai.repository.AiQuestionTaskRepository;
import cn.smu.edu.ai.service.AiQuestionGenerateService;
import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.constant.KafkaTopic;
import cn.smu.edu.common.event.AiTaskEvent;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * S6-07 一键 AI 出题。
 *
 * C3 约束：触发接口仅落任务 + 发 Kafka 后立即返回 taskId，不同步等待 LLM。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/question/ai-generate")
@RequiredArgsConstructor
public class QuestionGenerateController {

    private final AiQuestionGenerateService questionGenerateService;
    private final AiQuestionTaskRepository taskRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** 触发一键出题（异步），返回 taskId 供轮询 */
    @OperationLog(module = "ai", operation = "一键AI出题")
    @PostMapping
    public Result<String> generate(@Valid @RequestBody QuestionGenerateDTO dto) {
        Long creatorId = UserContext.getUserId();
        String taskId = questionGenerateService.createTask(dto, creatorId);
        // bizId 携带 bankId，taskType=GENERATE
        AiTaskEvent event = new AiTaskEvent(taskId, null, creatorId, null,
                "GENERATE", LocalDateTime.now(), dto.getBankId());
        kafkaTemplate.send(KafkaTopic.AI_TASKS, taskId, event);
        log.info("AI出题任务已入队: taskId={}, bankId={}", taskId, dto.getBankId());
        return Result.ok(taskId);
    }

    /** 查询出题任务进度/结果 */
    @GetMapping("/{taskId}")
    public Result<QuestionTaskVO> status(@PathVariable String taskId) {
        return taskRepository.findByTaskId(taskId)
                .map(t -> Result.ok(QuestionTaskVO.from(t)))
                .orElseGet(() -> Result.ok(null));
    }
}
