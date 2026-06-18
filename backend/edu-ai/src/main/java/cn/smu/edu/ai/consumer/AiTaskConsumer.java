package cn.smu.edu.ai.consumer;

import cn.smu.edu.ai.domain.model.AiRequest;
import cn.smu.edu.ai.domain.model.ModelType;
import cn.smu.edu.ai.service.AiGatewayService;
import cn.smu.edu.ai.service.LessonReportService;
import cn.smu.edu.common.event.AiTaskEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * AI 任务队列消费者（C3约束：课堂结束后全部异步化）
 *
 * concurrency = "3"：GPU/算力限制，不得随意调大（CLAUDE.md 8.3节）
 * 幂等：taskId 在 Redis 去重（TTL 24h），防重复处理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiTaskConsumer {

    private final AiGatewayService aiGatewayService;
    private final LessonReportService reportService;

    @KafkaListener(topics = "edu.ai.tasks", groupId = "edu-ai-task",
            concurrency = "3")
    public void consumeAiTask(AiTaskEvent event) {
        log.info("AI任务开始处理: taskId={}, lessonId={}, type={}", event.getTaskId(), event.getLessonId(), event.getTaskType());

        try {
            switch (event.getTaskType()) {
                case "SUMMARY"  -> handleSummary(event);
                case "MINDMAP"  -> handleMindmap(event);
                default -> log.warn("未知AI任务类型: type={}", event.getTaskType());
            }
        } catch (Exception e) {
            log.error("AI任务处理失败: taskId={}, lessonId={}", event.getTaskId(), event.getLessonId(), e);
            reportService.markFailed(event.getLessonId());
            throw e;
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
