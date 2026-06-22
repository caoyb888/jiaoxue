package cn.smu.edu.ai.service;

import cn.smu.edu.ai.domain.document.AiQuestionTask;
import cn.smu.edu.ai.domain.dto.QuestionGenerateDTO;
import cn.smu.edu.ai.domain.entity.GeneratedQuestion;
import cn.smu.edu.ai.domain.model.AiRequest;
import cn.smu.edu.ai.domain.model.ModelType;
import cn.smu.edu.ai.domain.vo.GeneratedQuestionVO;
import cn.smu.edu.ai.repository.AiQuestionTaskRepository;
import cn.smu.edu.ai.repository.QuestionReadMapper;
import cn.smu.edu.ai.repository.QuestionWriteMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * S6-07 一键 AI 出题。
 *
 * createTask：落 ai_question_task(PENDING) 并返回 taskId（Controller 随后发 Kafka GENERATE）。
 * generate：Consumer 异步调用 → LLM（GENERATION，经 C4 安全层）返回题目 JSON 数组
 *          → 解析入 question 表（批量）→ 任务置 DONE → WebSocket 通知教师。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQuestionGenerateService {

    private static final Pattern JSON_ARRAY = Pattern.compile("\\[.*]", Pattern.DOTALL);
    private static final BigDecimal DEFAULT_SCORE = new BigDecimal("5");

    private static final String SYSTEM_PROMPT =
            "你是一名命题专家，根据给定知识点、题型、难度生成高质量题目。"
            + "只输出 JSON 数组，每个元素格式："
            + "{\"type\": 题型数字, \"content\": \"题干\", \"answer\": \"标准答案\", "
            + "\"analysis\": \"解析\", \"difficulty\": 难度数字, \"score\": 分值}。"
            + "题型：1-单选 2-多选 3-判断 4-填空 5-主观。不要输出任何额外文字。";

    private final AiQuestionTaskRepository taskRepository;
    private final QuestionWriteMapper questionWriteMapper;
    private final QuestionReadMapper questionReadMapper;
    private final AiGatewayService aiGatewayService;
    private final AiNotifyPublisher notifyPublisher;
    private final ObjectMapper objectMapper;

    /** 创建出题任务（PENDING），返回 taskId */
    public String createTask(QuestionGenerateDTO dto, Long creatorId) {
        String taskId = UUID.randomUUID().toString();
        AiQuestionTask task = new AiQuestionTask();
        task.setTaskId(taskId);
        task.setBankId(dto.getBankId());
        task.setCreatorId(creatorId);
        task.setTopic(dto.getTopic());
        task.setTypes(dto.getTypes());
        task.setCount(dto.getCount());
        task.setDifficulty(dto.getDifficulty());
        task.setStatus(AiQuestionTask.STATUS_PENDING);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        log.info("AI出题任务已创建: taskId={}, bankId={}, count={}", taskId, dto.getBankId(), dto.getCount());
        return taskId;
    }

    /** 查询某出题任务已生成入库的题目（预览） */
    public List<GeneratedQuestionVO> getGeneratedQuestions(String taskId) {
        AiQuestionTask task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null || task.getQuestionIds() == null || task.getQuestionIds().isEmpty()) {
            return List.of();
        }
        return questionReadMapper.selectByIds(task.getQuestionIds());
    }

    /** 异步执行出题（由 Consumer 调用） */
    public void generate(String taskId) {
        AiQuestionTask task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) {
            log.warn("AI出题任务不存在，跳过: taskId={}", taskId);
            return;
        }
        markStatus(task, AiQuestionTask.STATUS_GENERATING, null);

        try {
            String raw = aiGatewayService.chatSync(buildRequest(task));
            List<GeneratedQuestion> questions = parse(raw, task);
            if (questions.isEmpty()) {
                fail(task, "AI 未返回可用题目");
                return;
            }
            questionWriteMapper.insertBatch(questions);
            List<Long> ids = questions.stream().map(GeneratedQuestion::getId).collect(Collectors.toList());

            task.setStatus(AiQuestionTask.STATUS_DONE);
            task.setGeneratedCount(questions.size());
            task.setQuestionIds(ids);
            task.setUpdatedAt(LocalDateTime.now());
            taskRepository.save(task);

            notifyPublisher.notifyUser(task.getCreatorId(), "AI_QUESTION_DONE",
                    "AI出题完成，共 " + questions.size() + " 道",
                    Map.of("taskId", taskId, "count", questions.size(), "bankId", task.getBankId()));
            log.info("AI出题完成: taskId={}, 入库题数={}", taskId, questions.size());
        } catch (Exception e) {
            log.error("AI出题失败: taskId={}", taskId, e);
            fail(task, e.getMessage());
            throw e; // 交由 Consumer 释放幂等键重试
        }
    }

    private AiRequest buildRequest(AiQuestionTask task) {
        String typeNames = task.getTypes().stream().map(this::typeName).collect(Collectors.joining("、"));
        String userPrompt = "知识点/主题：" + task.getTopic() + "\n"
                + "题型：" + typeNames + "\n"
                + "题目数量：" + task.getCount() + "\n"
                + "难度（1易~5难）：" + task.getDifficulty();
        return AiRequest.builder()
                .userId(task.getCreatorId())
                .modelType(ModelType.GENERATION)
                .systemPrompt(SYSTEM_PROMPT)
                .userPrompt(userPrompt)
                .build();
    }

    /** 解析 LLM 返回的题目 JSON 数组；忽略无题干的元素 */
    private List<GeneratedQuestion> parse(String raw, AiQuestionTask task) {
        List<GeneratedQuestion> result = new ArrayList<>();
        Matcher m = JSON_ARRAY.matcher(raw == null ? "" : raw);
        if (!m.find()) {
            log.warn("AI出题返回无 JSON 数组: taskId={}", task.getTaskId());
            return result;
        }
        try {
            JsonNode arr = objectMapper.readTree(m.group());
            if (!arr.isArray()) {
                return result;
            }
            for (JsonNode node : arr) {
                String content = text(node, "content");
                if (content == null || content.isBlank()) {
                    continue;
                }
                GeneratedQuestion q = new GeneratedQuestion();
                q.setBankId(task.getBankId());
                q.setCreatorId(task.getCreatorId());
                q.setType(intOr(node, "type", 1));
                q.setContent(content);
                q.setAnswer(text(node, "answer"));
                q.setAnalysis(text(node, "analysis"));
                q.setDifficulty(clampDifficulty(intOr(node, "difficulty", task.getDifficulty())));
                q.setScore(node.hasNonNull("score")
                        ? new BigDecimal(node.get("score").asText()) : DEFAULT_SCORE);
                result.add(q);
            }
        } catch (Exception e) {
            log.warn("AI出题 JSON 解析失败: taskId={}, err={}", task.getTaskId(), e.getMessage());
        }
        return result;
    }

    private void fail(AiQuestionTask task, String msg) {
        markStatus(task, AiQuestionTask.STATUS_FAILED, msg);
        notifyPublisher.notifyUser(task.getCreatorId(), "AI_QUESTION_FAILED",
                "AI出题失败", Map.of("taskId", task.getTaskId()));
    }

    private void markStatus(AiQuestionTask task, String status, String errorMsg) {
        task.setStatus(status);
        task.setErrorMsg(errorMsg);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
    }

    private int clampDifficulty(int d) {
        if (d < 1) return 1;
        if (d > 5) return 5;
        return d;
    }

    private String typeName(int type) {
        return switch (type) {
            case 1 -> "单选题";
            case 2 -> "多选题";
            case 3 -> "判断题";
            case 4 -> "填空题";
            case 5 -> "主观题";
            default -> "题目";
        };
    }

    private String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private int intOr(JsonNode node, String field, int def) {
        return node.hasNonNull(field) ? node.get(field).asInt(def) : def;
    }
}
