package cn.smu.edu.exam.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.exam.domain.dto.AnswerItemDTO;
import cn.smu.edu.exam.domain.dto.SubmitAnswerDTO;
import cn.smu.edu.exam.domain.entity.*;
import cn.smu.edu.exam.domain.vo.*;
import cn.smu.edu.exam.event.ExamSubmitEvent;
import cn.smu.edu.exam.domain.dto.SubjectiveAnswerContent;
import cn.smu.edu.exam.domain.entity.ExamAnswerAttachment;
import cn.smu.edu.exam.repository.*;
import cn.smu.edu.exam.service.AutoGradeService;
import cn.smu.edu.exam.service.SubmitService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmitServiceImpl implements SubmitService {

    private static final String TOPIC_SUBMIT      = "edu.exam.submit";
    private static final String KEY_IDEMPOTENT    = "exam:submit:%d:%d";   // TTL 30min
    private static final String KEY_ANSWER_CACHE  = "exam:answer:%d:%d";   // TTL 2h

    private final ExamPublishMapper publishMapper;
    private final ExamPaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final StudentAnswerMapper studentAnswerMapper;
    private final ExamMonitorMapper monitorMapper;
    private final ExamSubmitQueueMapper submitQueueMapper;
    private final ExamAnswerAttachmentMapper attachmentMapper;
    private final AutoGradeService autoGradeService;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // ── C2 三层容灾交卷入口 ──────────────────────────────────────────────────

    @Override
    public SubmitResultVO submit(Long publishId, Long studentId, SubmitAnswerDTO dto) {
        // 验证考试存在
        if (publishMapper.selectById(publishId) == null) {
            throw new BizException(ErrorCode.EXAM_NOT_FOUND);
        }

        // ① Redis SETNX 幂等键（TTL 30min）— 防止重复提交
        String idempotentKey = String.format(KEY_IDEMPOTENT, publishId, studentId);
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", Duration.ofMinutes(30));
        if (!Boolean.TRUE.equals(isNew)) {
            throw new BizException(ErrorCode.EXAM_ALREADY_SUBMITTED);
        }

        // ② 答案 JSON 存 Redis（TTL 2h，断电容灾备用）
        String answersJson;
        try {
            answersJson = objectMapper.writeValueAsString(dto.getAnswers());
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR.getCode(), "答案序列化失败: " + e.getMessage());
        }
        String answerKey = String.format(KEY_ANSWER_CACHE, publishId, studentId);
        redisTemplate.opsForValue().set(answerKey, answersJson, Duration.ofHours(2));

        // ③ 发送 Kafka 消息（edu.exam.submit），立即返回
        LocalDateTime clientSubmitAt = dto.getClientSubmitAt() != null
                ? dto.getClientSubmitAt() : LocalDateTime.now();
        String submitType = dto.getSubmitType() != null ? dto.getSubmitType() : "MANUAL";

        ExamSubmitEvent event = new ExamSubmitEvent(
                publishId, studentId, answersJson, submitType, clientSubmitAt);
        kafkaTemplate.send(TOPIC_SUBMIT, String.valueOf(publishId), event);

        // 更新监考状态为 SUBMITTED（快速响应，不等 Kafka 落库）
        ExamMonitor monitor = monitorMapper.selectByPublishAndStudent(publishId, studentId);
        if (monitor != null) {
            monitor.setSessionStatus("SUBMITTED");
            monitor.setSubmitTime(LocalDateTime.now());
            monitorMapper.updateById(monitor);
        }

        log.info("交卷已受理(Kafka): publishId={}, studentId={}, type={}, answers={}",
                publishId, studentId, submitType, dto.getAnswers().size());

        SubmitResultVO vo = new SubmitResultVO();
        vo.setPublishId(publishId);
        vo.setStudentId(studentId);
        vo.setSubmittedCount(dto.getAnswers().size());
        vo.setGradeResults(Collections.emptyList()); // 异步批改，稍后通过 WebSocket 通知
        return vo;
    }

    // ── Kafka Consumer 层（写入 exam_submit_queue） ──────────────────────────

    @Override
    @Transactional
    public void enqueueSubmit(Long publishId, Long studentId, String answersJson,
                              String submitType, LocalDateTime clientSubmitAt) {
        // 幂等：exam_submit_queue 有 uk_publish_student，重复忽略
        ExamSubmitQueue existing = submitQueueMapper.selectByPublishAndStudent(publishId, studentId);
        if (existing != null) {
            log.warn("exam_submit_queue 已存在记录，跳过: publishId={}, studentId={}", publishId, studentId);
            return;
        }

        ExamSubmitQueue queue = new ExamSubmitQueue();
        queue.setPublishId(publishId);
        queue.setStudentId(studentId);
        queue.setAnswersJson(answersJson);
        queue.setSubmitType(submitType != null ? submitType : "MANUAL");
        queue.setClientSubmitAt(clientSubmitAt != null ? clientSubmitAt : LocalDateTime.now());
        queue.setProcessStatus(0);
        queue.setRetryCount(0);
        submitQueueMapper.insert(queue);
        log.info("交卷已入队: publishId={}, studentId={}, queueId={}", publishId, studentId, queue.getId());
    }

    // ── XXL-Job 层（展开 student_answer + 自动批改） ─────────────────────────

    @Override
    @Transactional
    public void expandSubmitQueue(int batchSize) {
        List<ExamSubmitQueue> pending = submitQueueMapper.selectPending(batchSize);
        for (ExamSubmitQueue queue : pending) {
            // 标记处理中（防止并发重复展开）
            queue.setProcessStatus(1);
            submitQueueMapper.updateById(queue);

            try {
                expandOne(queue);
                queue.setProcessStatus(2);
                queue.setProcessedAt(LocalDateTime.now());
            } catch (Exception e) {
                log.error("展开交卷失败: queueId={}, error={}", queue.getId(), e.getMessage(), e);
                queue.setProcessStatus(3);
                queue.setRetryCount(queue.getRetryCount() + 1);
                queue.setErrorMsg(e.getMessage());
            } finally {
                submitQueueMapper.updateById(queue);
            }
        }
    }

    private void expandOne(ExamSubmitQueue queue) throws JsonProcessingException {
        List<AnswerItemDTO> answers = objectMapper.readValue(
                queue.getAnswersJson(), new TypeReference<List<AnswerItemDTO>>() {});

        ExamPublish publish = publishMapper.selectById(queue.getPublishId());
        if (publish == null) throw new IllegalStateException("exam_publish 不存在: " + queue.getPublishId());

        // 试卷题目分值 Map
        List<ExamPaperQuestion> paperQuestions = paperQuestionMapper.selectByPaperId(publish.getPaperId());
        Map<Long, BigDecimal> scoreMap = paperQuestions.stream()
                .collect(Collectors.toMap(ExamPaperQuestion::getQuestionId,
                        ExamPaperQuestion::getScore, (a, b) -> a));

        // 批量加载题目（获取类型和标准答案）
        Set<Long> questionIds = answers.stream()
                .map(AnswerItemDTO::getQuestionId).collect(Collectors.toSet());
        Map<Long, Question> questionMap = questionIds.isEmpty() ? Map.of()
                : questionMapper.selectBatchIds(questionIds).stream()
                        .collect(Collectors.toMap(Question::getId, q -> q));

        LocalDateTime submittedAt = queue.getClientSubmitAt();
        for (AnswerItemDTO item : answers) {
            Question question = questionMap.get(item.getQuestionId());
            if (question == null) continue;

            // 幂等：已存在跳过
            if (studentAnswerMapper.existsAnswer(queue.getPublishId(), item.getQuestionId(), queue.getStudentId()) > 0) {
                continue;
            }

            StudentAnswer answer = new StudentAnswer();
            answer.setPublishId(queue.getPublishId());
            answer.setQuestionId(item.getQuestionId());
            answer.setStudentId(queue.getStudentId());
            answer.setAnswerContent(item.getAnswerContent());
            answer.setReviewStatus(0);
            answer.setSubmittedAt(submittedAt);
            studentAnswerMapper.insert(answer);

            // 主观题附件：解析 answer_content JSON，提取 attachments 写入关联表
            saveAttachmentsIfPresent(answer, item.getAnswerContent(), queue);

            BigDecimal questionScore = scoreMap.getOrDefault(item.getQuestionId(), question.getScore());
            autoGradeService.grade(answer, question.getType(), question.getAnswer(), questionScore);
        }

        // 答案展开完成后清除 Redis 答案缓存（节省内存）
        String answerKey = String.format(KEY_ANSWER_CACHE, queue.getPublishId(), queue.getStudentId());
        redisTemplate.delete(answerKey);

        log.info("交卷展开完成: queueId={}, publishId={}, studentId={}, answers={}",
                queue.getId(), queue.getPublishId(), queue.getStudentId(), answers.size());
    }

    private void saveAttachmentsIfPresent(StudentAnswer answer, String answerContent, ExamSubmitQueue queue) {
        if (!SubjectiveAnswerContent.isJsonFormat(answerContent)) return;
        try {
            SubjectiveAnswerContent content = objectMapper.readValue(answerContent, SubjectiveAnswerContent.class);
            List<String> attachments = content.getAttachments();
            for (int i = 0; i < attachments.size(); i++) {
                ExamAnswerAttachment att = new ExamAnswerAttachment();
                att.setStudentAnswerId(answer.getId());
                att.setPublishId(queue.getPublishId());
                att.setStudentId(queue.getStudentId());
                att.setQuestionId(answer.getQuestionId());
                att.setFileKey(attachments.get(i));
                att.setSortOrder(i);
                attachmentMapper.insert(att);
            }
        } catch (JsonProcessingException e) {
            log.warn("解析主观题附件JSON失败，跳过附件关联: answerId={}, err={}", answer.getId(), e.getMessage());
        }
    }

    // ── 查询汇总 ─────────────────────────────────────────────────────────────

    @Override
    public ExamScoreSummaryVO getScoreSummary(Long publishId, Long studentId) {
        ExamPublish publish = publishMapper.selectById(publishId);
        if (publish == null) throw new BizException(ErrorCode.EXAM_NOT_FOUND);

        BigDecimal fullScore = paperQuestionMapper.sumScoreByPaperId(publish.getPaperId());
        List<StudentAnswer> answers = studentAnswerMapper.selectByPublishAndStudent(publishId, studentId);
        BigDecimal totalScore = studentAnswerMapper.sumScoreByPublishAndStudent(publishId, studentId);
        int graded  = studentAnswerMapper.countGraded(publishId, studentId);
        int correct = studentAnswerMapper.countCorrect(publishId, studentId);

        List<StudentAnswerVO> answerVOs = answers.stream().map(a -> {
            StudentAnswerVO vo = new StudentAnswerVO();
            vo.setId(a.getId());
            vo.setPublishId(a.getPublishId());
            vo.setQuestionId(a.getQuestionId());
            vo.setStudentId(a.getStudentId());
            vo.setAnswerContent(a.getAnswerContent());
            vo.setScore(a.getScore());
            vo.setIsCorrect(a.getIsCorrect());
            vo.setComment(a.getComment());
            vo.setReviewStatus(a.getReviewStatus());
            vo.setSubmittedAt(a.getSubmittedAt());
            // 主观题附件：从关联表加载 MinIO 路径列表
            if (a.getId() != null) {
                List<String> attachments = attachmentMapper
                        .selectByStudentAnswerId(a.getId()).stream()
                        .map(cn.smu.edu.exam.domain.entity.ExamAnswerAttachment::getFileKey)
                        .collect(Collectors.toList());
                if (!attachments.isEmpty()) vo.setAttachments(attachments);
            }
            return vo;
        }).collect(Collectors.toList());

        ExamScoreSummaryVO summary = new ExamScoreSummaryVO();
        summary.setPublishId(publishId);
        summary.setStudentId(studentId);
        summary.setTotalScore(totalScore);
        summary.setFullScore(fullScore);
        summary.setTotalQuestions(answers.size());
        summary.setGradedQuestions(graded);
        summary.setCorrectCount(correct);
        summary.setAnswers(answerVOs);
        return summary;
    }
}
