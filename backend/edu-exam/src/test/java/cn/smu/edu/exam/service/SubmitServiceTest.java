package cn.smu.edu.exam.service;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.exam.domain.dto.AnswerItemDTO;
import cn.smu.edu.exam.domain.dto.SubmitAnswerDTO;
import cn.smu.edu.exam.domain.entity.*;
import cn.smu.edu.exam.domain.vo.*;
import cn.smu.edu.exam.repository.*;
import cn.smu.edu.exam.service.impl.SubmitServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmitServiceTest {

    @Mock private ExamPublishMapper publishMapper;
    @Mock private ExamPaperQuestionMapper paperQuestionMapper;
    @Mock private QuestionMapper questionMapper;
    @Mock private StudentAnswerMapper studentAnswerMapper;
    @Mock private AutoGradeService autoGradeService;

    @InjectMocks private SubmitServiceImpl service;

    private ExamPublish publish;
    private ExamPaperQuestion pq;
    private Question question1;  // single choice
    private Question question5;  // essay

    @BeforeEach
    void setUp() {
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

        question5 = new Question();
        question5.setId(200L);
        question5.setType(5);
        question5.setAnswer("参考答案");
        question5.setScore(new BigDecimal("10.00"));
    }

    // ── submit ────────────────────────────────────────────────────────────────

    @Test
    void submit_shouldInsertAndGrade_forSingleChoiceQuestion() {
        when(publishMapper.selectById(10L)).thenReturn(publish);
        when(paperQuestionMapper.selectByPaperId(20L)).thenReturn(List.of(pq));
        when(questionMapper.selectBatchIds(anyCollection())).thenReturn(List.of(question1));
        when(studentAnswerMapper.existsAnswer(10L, 100L, 99L)).thenReturn(0);
        when(studentAnswerMapper.insert((StudentAnswer) any())).thenReturn(1);

        GradeResultVO gradeVO = new GradeResultVO(100L, 1, new BigDecimal("4.00"), 1, 1);
        when(autoGradeService.grade(any(), eq(1), eq("A"), eq(new BigDecimal("4.00"))))
                .thenReturn(gradeVO);

        SubmitAnswerDTO dto = new SubmitAnswerDTO();
        dto.setAnswers(List.of(answerItem(100L, "A")));

        SubmitResultVO result = service.submit(10L, 99L, dto);

        assertThat(result.getSubmittedCount()).isEqualTo(1);
        assertThat(result.getGradeResults()).hasSize(1);
        assertThat(result.getGradeResults().get(0).getIsCorrect()).isEqualTo(1);
        verify(studentAnswerMapper).insert((StudentAnswer) any());
        verify(autoGradeService).grade(any(), eq(1), eq("A"), any());
    }

    @Test
    void submit_shouldSkipGrade_forEssayQuestion() {
        ExamPaperQuestion pq5 = new ExamPaperQuestion();
        pq5.setQuestionId(200L);
        pq5.setScore(new BigDecimal("10.00"));

        when(publishMapper.selectById(10L)).thenReturn(publish);
        when(paperQuestionMapper.selectByPaperId(20L)).thenReturn(List.of(pq5));
        when(questionMapper.selectBatchIds(anyCollection())).thenReturn(List.of(question5));
        when(studentAnswerMapper.existsAnswer(10L, 200L, 99L)).thenReturn(0);
        when(studentAnswerMapper.insert((StudentAnswer) any())).thenReturn(1);

        GradeResultVO skipVO = new GradeResultVO(200L, 5, null, null, 0);
        when(autoGradeService.grade(any(), eq(5), any(), any())).thenReturn(skipVO);

        SubmitAnswerDTO dto = new SubmitAnswerDTO();
        dto.setAnswers(List.of(answerItem(200L, "牛顿第一定律...")));

        SubmitResultVO result = service.submit(10L, 99L, dto);

        assertThat(result.getSubmittedCount()).isEqualTo(1);
        GradeResultVO grade = result.getGradeResults().get(0);
        assertThat(grade.getReviewStatus()).isEqualTo(0); // 待人工批改
        assertThat(grade.getScore()).isNull();
    }

    @Test
    void submit_shouldSkipDuplicate_whenAnswerAlreadyExists() {
        when(publishMapper.selectById(10L)).thenReturn(publish);
        when(paperQuestionMapper.selectByPaperId(20L)).thenReturn(List.of(pq));
        when(questionMapper.selectBatchIds(anyCollection())).thenReturn(List.of(question1));
        // 幂等：已有作答
        when(studentAnswerMapper.existsAnswer(10L, 100L, 99L)).thenReturn(1);

        SubmitAnswerDTO dto = new SubmitAnswerDTO();
        dto.setAnswers(List.of(answerItem(100L, "A")));

        SubmitResultVO result = service.submit(10L, 99L, dto);

        assertThat(result.getSubmittedCount()).isEqualTo(0); // 跳过重复，已提交数为0
        verify(studentAnswerMapper, never()).insert((StudentAnswer) any());
        verify(autoGradeService, never()).grade(any(), anyInt(), any(), any());
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
    void submit_shouldIgnoreUnknownQuestion() {
        when(publishMapper.selectById(10L)).thenReturn(publish);
        when(paperQuestionMapper.selectByPaperId(20L)).thenReturn(List.of(pq));
        // questionMapper 返回空（题目不存在）
        when(questionMapper.selectBatchIds(anyCollection())).thenReturn(List.of());

        SubmitAnswerDTO dto = new SubmitAnswerDTO();
        dto.setAnswers(List.of(answerItem(999L, "A")));

        SubmitResultVO result = service.submit(10L, 99L, dto);

        assertThat(result.getSubmittedCount()).isEqualTo(0);
        verify(studentAnswerMapper, never()).insert((StudentAnswer) any());
    }

    @Test
    void submit_scoreMap_shouldPreferPaperScoreOverDefaultScore() {
        // paper 里该题得 8 分，题库默认 4 分，应使用 8 分
        pq.setScore(new BigDecimal("8.00"));

        when(publishMapper.selectById(10L)).thenReturn(publish);
        when(paperQuestionMapper.selectByPaperId(20L)).thenReturn(List.of(pq));
        when(questionMapper.selectBatchIds(anyCollection())).thenReturn(List.of(question1));
        when(studentAnswerMapper.existsAnswer(any(), any(), any())).thenReturn(0);
        when(studentAnswerMapper.insert((StudentAnswer) any())).thenReturn(1);

        ArgumentCaptor<BigDecimal> scoreCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        when(autoGradeService.grade(any(), anyInt(), any(), scoreCaptor.capture()))
                .thenReturn(new GradeResultVO(100L, 1, new BigDecimal("8.00"), 1, 1));

        SubmitAnswerDTO dto = new SubmitAnswerDTO();
        dto.setAnswers(List.of(answerItem(100L, "A")));

        service.submit(10L, 99L, dto);

        assertThat(scoreCaptor.getValue()).isEqualByComparingTo("8.00");
    }

    // ── getScoreSummary ───────────────────────────────────────────────────────

    @Test
    void getScoreSummary_shouldReturnProperVO() {
        when(publishMapper.selectById(10L)).thenReturn(publish);
        when(paperQuestionMapper.sumScoreByPaperId(20L)).thenReturn(new BigDecimal("100.00"));

        StudentAnswer a = new StudentAnswer();
        a.setId(1L); a.setPublishId(10L); a.setQuestionId(100L); a.setStudentId(99L);
        a.setAnswerContent("A"); a.setScore(new BigDecimal("4.00"));
        a.setIsCorrect(1); a.setReviewStatus(1);

        when(studentAnswerMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(List.of(a));
        when(studentAnswerMapper.sumScoreByPublishAndStudent(10L, 99L))
                .thenReturn(new BigDecimal("4.00"));
        when(studentAnswerMapper.countGraded(10L, 99L)).thenReturn(1);
        when(studentAnswerMapper.countCorrect(10L, 99L)).thenReturn(1);

        ExamScoreSummaryVO summary = service.getScoreSummary(10L, 99L);

        assertThat(summary.getPublishId()).isEqualTo(10L);
        assertThat(summary.getStudentId()).isEqualTo(99L);
        assertThat(summary.getFullScore()).isEqualByComparingTo("100.00");
        assertThat(summary.getTotalScore()).isEqualByComparingTo("4.00");
        assertThat(summary.getTotalQuestions()).isEqualTo(1);
        assertThat(summary.getGradedQuestions()).isEqualTo(1);
        assertThat(summary.getCorrectCount()).isEqualTo(1);
        assertThat(summary.getAnswers()).hasSize(1);
    }

    @Test
    void getScoreSummary_shouldThrow_whenPublishNotFound() {
        when(publishMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.getScoreSummary(99L, 1L))
                .isInstanceOf(BizException.class);
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private AnswerItemDTO answerItem(Long questionId, String content) {
        AnswerItemDTO item = new AnswerItemDTO();
        item.setQuestionId(questionId);
        item.setAnswerContent(content);
        return item;
    }
}
