package cn.smu.edu.ai.consumer;

import cn.smu.edu.ai.domain.model.AiRequest;
import cn.smu.edu.ai.domain.model.ModelType;
import cn.smu.edu.ai.service.AiGatewayService;
import cn.smu.edu.ai.service.AiNotifyPublisher;
import cn.smu.edu.ai.service.AiReviewService;
import cn.smu.edu.ai.service.LessonReportService;
import cn.smu.edu.common.event.AiTaskEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * AI 任务队列消费者（C3约束：课堂结束后全部异步化）
 *
 * concurrency = "3"：GPU/算力限制，不得随意调大（CLAUDE.md 8.3节）
 * 幂等：taskId 在 Redis 去重（ai:task:done:{taskId}，TTL 24h），防重复处理（CLAUDE.md 5.6节）
 *
 * 支持的任务类型（AiTaskEvent.taskType）：
 *   SUMMARY  — 课堂讲稿摘要 + 思维导图（S6-05/S6-06）
 *   MINDMAP  — 仅思维导图（S6-06）
 *   REVIEW   — 主观题智能批改（S6-02/S6-03）
 *   GENERATE — 一键 AI 出题（S6-07）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiTaskConsumer {

    private static final String DEDUPE_KEY_PREFIX = "ai:task:done:";
    private static final Duration DEDUPE_TTL = Duration.ofHours(24);

    private final AiGatewayService aiGatewayService;
    private final LessonReportService reportService;
    private final AiReviewService aiReviewService;
    private final AiNotifyPublisher notifyPublisher;
    private final StringRedisTemplate redisTemplate;

    @KafkaListener(topics = "edu.ai.tasks", groupId = "edu-ai-task", concurrency = "3")
    public void consumeAiTask(AiTaskEvent event) {
        log.info("AI任务收到: taskId={}, lessonId={}, type={}", event.getTaskId(), event.getLessonId(), event.getTaskType());

        // 幂等去重：首次处理才占位，重复消息直接跳过（CLAUDE.md 5.6/8.3）
        if (!tryAcquire(event.getTaskId())) {
            log.info("AI任务重复消息，已跳过: taskId={}", event.getTaskId());
            return;
        }

        try {
            switch (event.getTaskType()) {
                case "SUMMARY"  -> handleSummary(event);
                case "MINDMAP"  -> handleMindmap(event);
                case "REVIEW"   -> handleReview(event);
                case "GENERATE" -> handleGenerate(event);
                default -> log.warn("未知AI任务类型: type={}", event.getTaskType());
            }
        } catch (Exception e) {
            log.error("AI任务处理失败: taskId={}, lessonId={}", event.getTaskId(), event.getLessonId(), e);
            // 处理失败时释放去重键，允许重投递重试
            release(event.getTaskId());
            reportService.markFailed(event.getLessonId());
            throw e;
        }
    }

    /** 占位成功返回 true（首次），已存在返回 false（重复） */
    private boolean tryAcquire(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return true; // 无 taskId 的任务不做去重，直接处理
        }
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(DEDUPE_KEY_PREFIX + taskId, "1", DEDUPE_TTL);
        return Boolean.TRUE.equals(first);
    }

    private void release(String taskId) {
        if (taskId != null && !taskId.isBlank()) {
            redisTemplate.delete(DEDUPE_KEY_PREFIX + taskId);
        }
    }

    private void handleSummary(AiTaskEvent event) {
        reportService.initReport(event.getLessonId(), 0);

        AiRequest req = AiRequest.builder()
                .lessonId(event.getLessonId())
                .userId(event.getTeacherId())
                .modelType(ModelType.GENERATION)
                .systemPrompt("你是一名教学助手，擅长将课堂录音转写为结构化讲稿摘要。")
                .userPrompt(buildSummaryPrompt(event.getLessonId()))
                .build();

        String summary = aiGatewayService.chatSync(req);

        // 思维导图也一并生成（SUMMARY 任务包含两部分）
        AiRequest mindmapReq = AiRequest.builder()
                .lessonId(event.getLessonId())
                .userId(event.getTeacherId())
                .modelType(ModelType.ANALYSIS)
                .systemPrompt("你是一名教学助手，擅长将课堂内容转换为Markmap格式的思维导图JSON。只输出JSON，不要包含其他文字。")
                .userPrompt(buildMindmapPrompt(event.getLessonId()))
                .build();

        String mindmapJson = aiGatewayService.chatSync(mindmapReq);

        reportService.saveAiContent(event.getLessonId(), summary, mindmapJson);
        log.info("AI课堂摘要 + 思维导图生成完成: lessonId={}", event.getLessonId());
    }

    private void handleMindmap(AiTaskEvent event) {
        reportService.initReport(event.getLessonId(), 0);

        AiRequest req = AiRequest.builder()
                .lessonId(event.getLessonId())
                .userId(event.getTeacherId())
                .modelType(ModelType.ANALYSIS)
                .systemPrompt("你是一名教学助手，擅长将课堂内容转换为Markmap格式的思维导图JSON。只输出JSON，不要包含其他文字。")
                .userPrompt(buildMindmapPrompt(event.getLessonId()))
                .build();

        String mindmapJson = aiGatewayService.chatSync(req);
        reportService.saveAiContent(event.getLessonId(), null, mindmapJson);
        log.info("AI思维导图生成完成: lessonId={}", event.getLessonId());
    }

    /** 主观题智能批改（S6-02 批改 + S6-03 写回/通知）：bizId 携带 publishId */
    private void handleReview(AiTaskEvent event) {
        int count = aiReviewService.reviewByPublish(event.getBizId(), event.getTaskId());
        log.info("AI批改任务完成: publishId={}, 批改题数={}, taskId={}",
                event.getBizId(), count, event.getTaskId());
        // S6-03：WebSocket 单播通知教师批改完成
        notifyPublisher.notifyUser(event.getTeacherId(), "AI_REVIEW_DONE",
                "AI批改完成，共 " + count + " 题",
                java.util.Map.of("publishId", event.getBizId() == null ? 0L : event.getBizId(), "count", count));
    }

    /** 一键 AI 出题 — 业务逻辑在 S6-07 接入 AiQuestionGenerateService */
    private void handleGenerate(AiTaskEvent event) {
        log.info("AI出题任务受理（待 S6-07 出题服务接入）: lessonId={}, taskId={}",
                event.getLessonId(), event.getTaskId());
    }

    private String buildSummaryPrompt(Long lessonId) {
        return String.format(
                "请根据课堂ID %d 的录音转写内容，生成一份结构化的课堂讲稿摘要，包括：" +
                "1. 本节课的核心知识点（3-5个要点）；" +
                "2. 重点概念解释；" +
                "3. 例题或案例总结。要求语言简洁、条理清晰。", lessonId);
    }

    private String buildMindmapPrompt(Long lessonId) {
        return String.format(
                "请根据课堂ID %d 的内容，生成Markmap格式的思维导图JSON。" +
                "格式示例：{\"title\":\"主题\",\"children\":[{\"content\":\"子节点\",\"children\":[]}]}。" +
                "只输出JSON，不要有任何额外文字。", lessonId);
    }
}
