package cn.smu.edu.ai.controller;

import cn.smu.edu.ai.domain.vo.ReviewResultVO;
import cn.smu.edu.ai.repository.AiReviewResultRepository;
import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.constant.KafkaTopic;
import cn.smu.edu.common.event.AiTaskEvent;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * S6-02 AI 智能批改：异步触发 + 结果查询。
 *
 * C3 约束：触发接口仅发 Kafka 消息后立即返回"处理中"，不同步等待 LLM。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/review")
@RequiredArgsConstructor
public class AiReviewController {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AiReviewResultRepository reviewResultRepository;

    /** 触发某次试卷发布的主观题批量批改（异步） */
    @OperationLog(module = "ai", operation = "触发AI智能批改")
    @PostMapping("/{publishId}")
    public Result<String> triggerReview(@PathVariable Long publishId) {
        Long teacherId = UserContext.getUserId();
        String taskId = UUID.randomUUID().toString();
        AiTaskEvent event = AiTaskEvent.review(publishId, teacherId, taskId);
        kafkaTemplate.send(KafkaTopic.AI_TASKS, taskId, event);
        log.info("AI批改任务已入队: publishId={}, taskId={}", publishId, taskId);
        return Result.ok(taskId);
    }

    /** 查询某次发布的批改结果 */
    @GetMapping("/{publishId}")
    public Result<List<ReviewResultVO>> listResults(@PathVariable Long publishId) {
        List<ReviewResultVO> list = reviewResultRepository.findByPublishId(publishId)
                .stream().map(ReviewResultVO::from).toList();
        return Result.ok(list);
    }
}
