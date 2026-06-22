package cn.smu.edu.ai.consumer;

import cn.smu.edu.ai.service.AiNotifyPublisher;
import cn.smu.edu.ai.service.AiReviewService;
import cn.smu.edu.ai.service.LessonReportService;
import cn.smu.edu.ai.service.LessonSummaryService;
import cn.smu.edu.ai.service.MindmapService;
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

    private final LessonReportService reportService;
    private final LessonSummaryService lessonSummaryService;
    private final MindmapService mindmapService;
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

        // S6-05：读取 S6-04 课堂转写全文 → 摘要(≤500字) + key_points 抽取
        String transcript = lessonSummaryService.loadTranscript(event.getLessonId());
        LessonSummaryService.SummaryResult summary =
                lessonSummaryService.summarize(event.getLessonId(), transcript);

        // 思维导图也一并生成（SUMMARY 任务包含两部分）—— 基于同一份转写文本，存 Mongo ai_mindmap
        String mindmapJson = mindmapService.generate(
                event.getLessonId(), event.getTeacherId(), transcript, "SUMMARY");

        reportService.saveAiContent(event.getLessonId(),
                summary.summary(), summary.keyPointsJson(), mindmapJson);
        // 通知教师课堂报告已生成
        notifyPublisher.notifyLesson(event.getLessonId(), "AI_SUMMARY_DONE",
                "课堂摘要已生成", java.util.Map.of("keyPointCount", summary.keyPoints().size()));
        log.info("AI课堂摘要 + 思维导图生成完成: lessonId={}, 关键点数={}",
                event.getLessonId(), summary.keyPoints().size());
    }

    private void handleMindmap(AiTaskEvent event) {
        reportService.initReport(event.getLessonId(), 0);

        String transcript = lessonSummaryService.loadTranscript(event.getLessonId());
        // 生成并存 Mongo ai_mindmap
        String mindmapJson = mindmapService.generate(
                event.getLessonId(), event.getTeacherId(), transcript, "MINDMAP");
        // 仅更新思维导图列，避免覆盖已生成的摘要/关键点
        reportService.saveMindmap(event.getLessonId(), mindmapJson);
        notifyPublisher.notifyLesson(event.getLessonId(), "AI_MINDMAP_DONE",
                "思维导图已生成", java.util.Map.of("lessonId", event.getLessonId()));
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

}
