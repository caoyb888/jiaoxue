package cn.smu.edu.exam.service;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.exam.converter.ExamPaperConverter;
import cn.smu.edu.exam.converter.QuestionConverter;
import cn.smu.edu.exam.domain.dto.*;
import cn.smu.edu.exam.domain.entity.*;
import cn.smu.edu.exam.domain.vo.*;
import cn.smu.edu.exam.repository.*;
import cn.smu.edu.exam.service.impl.ExamPaperServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamPaperServiceTest {

    @Mock private ExamPaperMapper examPaperMapper;
    @Mock private ExamPaperQuestionMapper paperQuestionMapper;
    @Mock private QuestionMapper questionMapper;
    @Mock private QuestionOptionMapper questionOptionMapper;
    @Mock private ExamPaperConverter converter;
    @Mock private QuestionConverter questionConverter;

    @InjectMocks
    private ExamPaperServiceImpl service;

    private static final Long CREATOR_ID = 1L;
    private static final Long OTHER_ID   = 2L;
    private static final Long PAPER_ID   = 100L;
    private static final Long QUESTION_ID = 200L;

    private ExamPaper paper;

    @BeforeEach
    void setUp() {
        paper = new ExamPaper();
        paper.setId(PAPER_ID);
        paper.setCreatorId(CREATOR_ID);
        paper.setTitle("期中考试A卷");
        paper.setTotalScore(new BigDecimal("100.00"));
        paper.setIsRandom(0);
        paper.setPaperType("A");
        paper.setCreatedAt(LocalDateTime.now());
        paper.setUpdatedAt(LocalDateTime.now());
    }

    // ── create ─────────────────────────────────────────────────────────────

    @Test
    void create_shouldSetCreatorIdAndInsert() {
        ExamPaperCreateDTO dto = new ExamPaperCreateDTO();
        dto.setTitle("期中考试A卷");

        when(converter.toEntity(dto)).thenReturn(new ExamPaper());
        when(converter.toVO(any())).thenReturn(new ExamPaperVO());

        service.create(dto, CREATOR_ID);

        ArgumentCaptor<ExamPaper> cap = ArgumentCaptor.forClass(ExamPaper.class);
        verify(examPaperMapper).insert(cap.capture());
        assertThat(cap.getValue().getCreatorId()).isEqualTo(CREATOR_ID);
    }

    // ── update ─────────────────────────────────────────────────────────────

    @Test
    void update_shouldSucceed_forOwner() {
        when(examPaperMapper.selectById(PAPER_ID)).thenReturn(paper);
        when(converter.toVO(any())).thenReturn(new ExamPaperVO());
        doNothing().when(converter).updateEntity(any(), any());

        assertThatNoException().isThrownBy(
                () -> service.update(PAPER_ID, new ExamPaperUpdateDTO(), CREATOR_ID));
        verify(examPaperMapper).updateById(paper);
    }

    @Test
    void update_shouldThrowForbidden_forNonOwner() {
        when(examPaperMapper.selectById(PAPER_ID)).thenReturn(paper);
        assertThatThrownBy(() -> service.update(PAPER_ID, new ExamPaperUpdateDTO(), OTHER_ID))
                .isInstanceOf(BizException.class);
    }

    // ── delete ─────────────────────────────────────────────────────────────

    @Test
    void delete_shouldRemovePaperAndRelations() {
        when(examPaperMapper.selectById(PAPER_ID)).thenReturn(paper);

        service.delete(PAPER_ID, CREATOR_ID);

        verify(examPaperMapper).deleteById(PAPER_ID);
        verify(paperQuestionMapper).deleteAllByPaperId(PAPER_ID);
    }

    @Test
    void delete_shouldThrowForbidden_forNonOwner() {
        when(examPaperMapper.selectById(PAPER_ID)).thenReturn(paper);
        assertThatThrownBy(() -> service.delete(PAPER_ID, OTHER_ID))
                .isInstanceOf(BizException.class);
    }

    // ── addQuestions ───────────────────────────────────────────────────────

    @Test
    void addQuestions_shouldSkipDuplicate() {
        when(examPaperMapper.selectById(PAPER_ID)).thenReturn(paper);
        when(paperQuestionMapper.existsRelation(PAPER_ID, QUESTION_ID, "A")).thenReturn(1);
        when(paperQuestionMapper.selectByPaperId(PAPER_ID)).thenReturn(Collections.emptyList());
        when(converter.toVO(any())).thenReturn(new ExamPaperVO());

        AddQuestionDTO addDto = new AddQuestionDTO();
        addDto.setQuestionId(QUESTION_ID);
        addDto.setScore(new BigDecimal("5.00"));
        addDto.setPaperGroup("A");

        BatchAddQuestionsDTO batch = new BatchAddQuestionsDTO();
        batch.setQuestions(List.of(addDto));

        service.addQuestions(PAPER_ID, batch, CREATOR_ID);

        verify(paperQuestionMapper, never()).insert(any(ExamPaperQuestion.class));
    }

    @Test
    void addQuestions_shouldInsertRelation_whenNotDuplicate() {
        when(examPaperMapper.selectById(PAPER_ID)).thenReturn(paper);
        when(paperQuestionMapper.existsRelation(PAPER_ID, QUESTION_ID, "A")).thenReturn(0);
        when(questionMapper.selectById(QUESTION_ID)).thenReturn(new Question());
        when(paperQuestionMapper.maxSortOrder(PAPER_ID, "A")).thenReturn(0);
        when(paperQuestionMapper.selectByPaperId(PAPER_ID)).thenReturn(Collections.emptyList());
        when(converter.toVO(any())).thenReturn(new ExamPaperVO());

        AddQuestionDTO addDto = new AddQuestionDTO();
        addDto.setQuestionId(QUESTION_ID);
        addDto.setScore(new BigDecimal("5.00"));
        addDto.setPaperGroup("A");

        BatchAddQuestionsDTO batch = new BatchAddQuestionsDTO();
        batch.setQuestions(List.of(addDto));

        service.addQuestions(PAPER_ID, batch, CREATOR_ID);

        ArgumentCaptor<ExamPaperQuestion> cap = ArgumentCaptor.forClass(ExamPaperQuestion.class);
        verify(paperQuestionMapper).insert(cap.capture());
        assertThat(cap.getValue().getQuestionId()).isEqualTo(QUESTION_ID);
        assertThat(cap.getValue().getScore()).isEqualByComparingTo("5.00");
        assertThat(cap.getValue().getSortOrder()).isEqualTo(1);
    }

    // ── removeQuestion ─────────────────────────────────────────────────────

    @Test
    void removeQuestion_shouldSucceed() {
        when(examPaperMapper.selectById(PAPER_ID)).thenReturn(paper);
        when(paperQuestionMapper.deleteByPaperAndQuestion(PAPER_ID, QUESTION_ID, "A")).thenReturn(1);

        assertThatNoException().isThrownBy(
                () -> service.removeQuestion(PAPER_ID, QUESTION_ID, "A", CREATOR_ID));
    }

    @Test
    void removeQuestion_shouldThrowNotFound_whenRelationAbsent() {
        when(examPaperMapper.selectById(PAPER_ID)).thenReturn(paper);
        when(paperQuestionMapper.deleteByPaperAndQuestion(PAPER_ID, QUESTION_ID, "A")).thenReturn(0);

        assertThatThrownBy(() -> service.removeQuestion(PAPER_ID, QUESTION_ID, "A", CREATOR_ID))
                .isInstanceOf(BizException.class);
    }

    // ── scoreCheck ─────────────────────────────────────────────────────────

    @Test
    void checkScore_shouldReturnMatched_whenSumEqualsTotal() {
        when(examPaperMapper.selectById(PAPER_ID)).thenReturn(paper);
        when(paperQuestionMapper.sumScoreByPaperId(PAPER_ID)).thenReturn(new BigDecimal("100.00"));
        when(paperQuestionMapper.selectCount(any())).thenReturn(20L);

        ScoreCheckVO result = service.checkScore(PAPER_ID, CREATOR_ID);

        assertThat(result.getMatched()).isTrue();
        assertThat(result.getDiff()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getQuestionCount()).isEqualTo(20);
    }

    @Test
    void checkScore_shouldReturnNotMatched_whenSumDiffers() {
        when(examPaperMapper.selectById(PAPER_ID)).thenReturn(paper);
        when(paperQuestionMapper.sumScoreByPaperId(PAPER_ID)).thenReturn(new BigDecimal("95.00"));
        when(paperQuestionMapper.selectCount(any())).thenReturn(19L);

        ScoreCheckVO result = service.checkScore(PAPER_ID, CREATOR_ID);

        assertThat(result.getMatched()).isFalse();
        assertThat(result.getDiff()).isEqualByComparingTo("-5.00");
    }

    // ── randomCompose ──────────────────────────────────────────────────────

    @Test
    void randomCompose_shouldThrowParamError_whenNotEnoughQuestions() {
        when(examPaperMapper.selectById(PAPER_ID)).thenReturn(paper);

        // 没有符合条件的题目
        when(questionMapper.selectList(any())).thenReturn(Collections.emptyList());

        RandomPickRuleDTO rule = new RandomPickRuleDTO();
        rule.setBankId(1L);
        rule.setCount(5);
        rule.setScorePerQuestion(new BigDecimal("2.00"));
        rule.setPaperGroup("A");

        RandomCompositionDTO dto = new RandomCompositionDTO();
        dto.setRules(List.of(rule));

        assertThatThrownBy(() -> service.randomCompose(PAPER_ID, dto, CREATOR_ID))
                .isInstanceOf(BizException.class);
    }
}
