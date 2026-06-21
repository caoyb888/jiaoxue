package cn.smu.edu.exam.service;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.exam.domain.dto.AnswerItemDTO;
import cn.smu.edu.exam.domain.dto.SubmitAnswerDTO;
import cn.smu.edu.exam.domain.entity.*;
import cn.smu.edu.exam.domain.vo.*;
import cn.smu.edu.exam.event.ExamSubmitEvent;
import cn.smu.edu.exam.repository.*;

import java.util.Collections;
import cn.smu.edu.exam.service.impl.SubmitServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubmitServiceTest {

    @Mock private ExamPublishMapper publishMapper;
    @Mock private ExamPaperQuestionMapper paperQuestionMapper;
    @Mock private QuestionMapper questionMapper;
    @Mock private StudentAnswerMapper studentAnswerMapper;
    @Mock private ExamMonitorMapper monitorMapper;
    @Mock private ExamSubmitQueueMapper submitQueueMapper;
    @Mock private ExamAnswerAttachmentMapper attachmentMapper;
    @Mock private AutoGradeService autoGradeService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks private SubmitServiceImpl service;

    private ExamPublish publish;
    private ExamPaperQuestion pq;
    private Question question1;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // 通过反射注入 ObjectMapper（@InjectMocks 只注入 Mocks，需手动注入真实对象）
        try {
            var field = SubmitServiceImpl.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(service, objectMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        publish = new ExamPublish();
        publish.setId(10L);
        publish.setPaperId(20L);

        pq = new ExamPaperQuestion();
        pq.setQuestionId(100L);
        pq.setScore(new BigDecimal("4.00"));

        question1 = new Question();
        question1.setId(100L);
        question1.setType(1);
        question1.setAnswer("A");
        question1.setScore(new BigDecimal("4.00"));

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ── submit (C2 三层容灾) ─────────────────────────────────────────────────

    @Test
    void submit_shouldSendKafkaAndReturnImmediately_whenFirstSubmit() {
        when(publishMapper.selectById(10L)).thenReturn(publish);
        when(valueOps.setIfAbsent(anyString(), eq("1"), any())).thenReturn(true);

        SubmitAnswerDTO dto = new SubmitAnswerDTO();
        dto.setAnswers(List.of(answerItem(100L, "A")));

        SubmitResultVO result = service.submit(10L, 99L, dto);

        assertThat(result.getPublishId()).isEqualTo(10L);
        assertThat(result.getStudentId()).isEqualTo(99L);
        assertThat(result.getSubmittedCount()).isEqualTo(1);
        // C2：立即返回，gradeResults 为空（异步批改）
        assertThat(result.getGradeResults()).isEmpty();
        // 验证 Kafka 消息已发送
        verify(kafkaTemplate).send(eq("edu.exam.submit"), eq("10"), any(ExamSubmitEvent.class));
        // 验证答案已存入 Redis
        verify(valueOps).set(contains("exam:answer:"), anyString(), any());
    }

    @Test
    void submit_shouldThrowAlreadySubmitted_whenIdempotentKeyExists() {
        when(publishMapper.selectById(10L)).thenReturn(publish);
        when(valueOps.setIfAbsent(anyString(), eq("1"), any())).thenReturn(false);

        SubmitAnswerDTO dto = new SubmitAnswerDTO();
        dto.setAnswers(List.of(answerItem(100L, "A")));

        assertThatThrownBy(() -> service.submit(10L, 99L, dto))
                .isInstanceOf(BizException.class);
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void submit_shouldThrow_whenPublishNotFound() {
        when(publishMapper.selectById(99L)).thenReturn(null);
        SubmitAnswerDTO dto = new SubmitAnswerDTO();
        dto.setAnswers(List.of());
        assertThatThrownBy(() -> service.submit(99L, 1L, dto))
                .isInstanceOf(BizException.class);
    }

    @Test
    void submit_shouldUpdateMonitorStatusToSubmitted_whenMonitorExists() {
        when(publishMapper.selectById(10L)).thenReturn(publish);
        when(valueOps.setIfAbsent(anyString(), eq("1"), any())).thenReturn(true);

        ExamMonitor monitor = new ExamMonitor();
        monitor.setSessionStatus("ANSWERING");
        when(monitorMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(monitor);

        SubmitAnswerDTO dto = new SubmitAnswerDTO();
        dto.setAnswers(List.of(answerItem(100L, "A")));
        service.submit(10L, 99L, dto);

        assertThat(monitor.getSessionStatus()).isEqualTo("SUBMITTED");
        verify(monitorMapper).updateById(monitor);
    }

    // ── enqueueSubmit ─────────────────────────────────────────────────────────

    @Test
    void enqueueSubmit_shouldInsertQueue_whenFirstTime() {
        when(submitQueueMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(null);

        service.enqueueSubmit(10L, 99L, "[{\"questionId\":100}]", "MANUAL", LocalDateTime.now());

        ArgumentCaptor<ExamSubmitQueue> cap = ArgumentCaptor.forClass(ExamSubmitQueue.class);
        verify(submitQueueMapper).insert((ExamSubmitQueue) cap.capture());
        assertThat(cap.getValue().getPublishId()).isEqualTo(10L);
        assertThat(cap.getValue().getStudentId()).isEqualTo(99L);
        assertThat(cap.getValue().getProcessStatus()).isEqualTo(0);
    }

    @Test
    void enqueueSubmit_shouldSkip_whenAlreadyQueued() {
        ExamSubmitQueue existing = new ExamSubmitQueue();
        when(submitQueueMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(existing);

        service.enqueueSubmit(10L, 99L, "[]", "MANUAL", LocalDateTime.now());

        verify(submitQueueMapper, never()).insert((ExamSubmitQueue) any());
    }

    // ── expandSubmitQueue ─────────────────────────────────────────────────────

    @Test
    void expandSubmitQueue_shouldExpandAnswersToStudentAnswer() throws Exception {
        ExamSubmitQueue queue = buildQueue(10L, 99L, "[{\"questionId\":100,\"answerContent\":\"A\"}]");
        when(submitQueueMapper.selectPending(200)).thenReturn(List.of(queue));
        when(publishMapper.selectById(10L)).thenReturn(publish);
        when(paperQuestionMapper.selectByPaperId(20L)).thenReturn(List.of(pq));
        when(questionMapper.selectBatchIds(anyCollection())).thenReturn(List.of(question1));
        when(studentAnswerMapper.existsAnswer(10L, 100L, 99L)).thenReturn(0);
        when(studentAnswerMapper.insert((StudentAnswer) any())).thenReturn(1);
        when(autoGradeService.grade(any(), anyInt(), any(), any()))
                .thenReturn(new GradeResultVO(100L, 1, new BigDecimal("4.00"), 1, 1));
        when(redisTemplate.delete(anyString())).thenReturn(true);

        service.expandSubmitQueue(200);

        verify(studentAnswerMapper).insert((StudentAnswer) any());
        assertThat(queue.getProcessStatus()).isEqualTo(2); // 已完成
    }

    @Test
    void expandSubmitQueue_shouldMarkFailed_onExpandError() {
        ExamSubmitQueue queue = buildQueue(10L, 99L, "INVALID_JSON{{{");
        when(submitQueueMapper.selectPending(200)).thenReturn(List.of(queue));
        when(publishMapper.selectById(10L)).thenReturn(publish);
        when(paperQuestionMapper.selectByPaperId(20L)).thenReturn(List.of(pq));

        service.expandSubmitQueue(200);

        assertThat(queue.getProcessStatus()).isEqualTo(3); // 处理失败
        assertThat(queue.getErrorMsg()).isNotNull();
    }

    @Test
    void expandSubmitQueue_shouldSkipDuplicate_whenAnswerExists() throws Exception {
        ExamSubmitQueue queue = buildQueue(10L, 99L, "[{\"questionId\":100,\"answerContent\":\"A\"}]");
        when(submitQueueMapper.selectPending(200)).thenReturn(List.of(queue));
        when(publishMapper.selectById(10L)).thenReturn(publish);
        when(paperQuestionMapper.selectByPaperId(20L)).thenReturn(List.of(pq));
        when(questionMapper.selectBatchIds(anyCollection())).thenReturn(List.of(question1));
        when(studentAnswerMapper.existsAnswer(10L, 100L, 99L)).thenReturn(1); // 已存在
        when(redisTemplate.delete(anyString())).thenReturn(true);

        service.expandSubmitQueue(200);

        verify(studentAnswerMapper, never()).insert((StudentAnswer) any());
        assertThat(queue.getProcessStatus()).isEqualTo(2);
    }

    // ── getScoreSummary ───────────────────────────────────────────────────────

    @Test
    void getScoreSummary_shouldReturnProperVO() {
        when(publishMapper.selectById(10L)).thenReturn(publish);
        when(paperQuestionMapper.sumScoreByPaperId(20L)).thenReturn(new BigDecimal("100.00"));

        StudentAnswer a = new StudentAnswer();
        a.setId(1L); a.setPublishId(10L); a.setQuestionId(100L); a.setStudentId(99L);
        a.setAnswerContent("A"); a.setScore(new BigDecimal("4.00")); a.setIsCorrect(1); a.setReviewStatus(1);

        when(studentAnswerMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(List.of(a));
        when(studentAnswerMapper.sumScoreByPublishAndStudent(10L, 99L)).thenReturn(new BigDecimal("4.00"));
        when(studentAnswerMapper.countGraded(10L, 99L)).thenReturn(1);
        when(studentAnswerMapper.countCorrect(10L, 99L)).thenReturn(1);
        when(attachmentMapper.selectByStudentAnswerId(1L)).thenReturn(Collections.emptyList());

        ExamScoreSummaryVO summary = service.getScoreSummary(10L, 99L);

        assertThat(summary.getFullScore()).isEqualByComparingTo("100.00");
        assertThat(summary.getTotalScore()).isEqualByComparingTo("4.00");
        assertThat(summary.getAnswers()).hasSize(1);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private AnswerItemDTO answerItem(Long questionId, String content) {
        AnswerItemDTO item = new AnswerItemDTO();
        item.setQuestionId(questionId);
        item.setAnswerContent(content);
        return item;
    }

    private ExamSubmitQueue buildQueue(Long publishId, Long studentId, String answersJson) {
        ExamSubmitQueue q = new ExamSubmitQueue();
        q.setId(1L);
        q.setPublishId(publishId);
        q.setStudentId(studentId);
        q.setAnswersJson(answersJson);
        q.setSubmitType("MANUAL");
        q.setClientSubmitAt(LocalDateTime.now());
        q.setProcessStatus(0);
        q.setRetryCount(0);
        return q;
    }
}
