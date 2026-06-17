package cn.smu.edu.exam.service;

import cn.smu.edu.exam.domain.entity.StudentAnswer;
import cn.smu.edu.exam.domain.vo.GradeResultVO;
import cn.smu.edu.exam.repository.StudentAnswerMapper;
import cn.smu.edu.exam.service.impl.AutoGradeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoGradeServiceTest {

    @Mock
    private StudentAnswerMapper studentAnswerMapper;

    @InjectMocks
    private AutoGradeServiceImpl service;

    private static final BigDecimal SCORE_4 = new BigDecimal("4.00");

    private StudentAnswer answer(String content) {
        StudentAnswer a = new StudentAnswer();
        a.setId(1L);
        a.setQuestionId(100L);
        a.setAnswerContent(content);
        a.setReviewStatus(0);
        return a;
    }

    // ── 静态归一化方法 ─────────────────────────────────────────────────────

    @Test
    void normalize_shouldTrimAndUppercase() {
        assertThat(AutoGradeServiceImpl.normalize("  a  ")).isEqualTo("A");
        assertThat(AutoGradeServiceImpl.normalize(null)).isEqualTo("");
    }

    @Test
    void normalizeMultiChoice_shouldSortAndUppercase() {
        assertThat(AutoGradeServiceImpl.normalizeMultiChoice("C,A,B")).isEqualTo("A,B,C");
        assertThat(AutoGradeServiceImpl.normalizeMultiChoice("a, c, b")).isEqualTo("A,B,C");
        assertThat(AutoGradeServiceImpl.normalizeMultiChoice(null)).isEqualTo("");
    }

    @ParameterizedTest
    @CsvSource({"true,T", "True,T", "T,T", "1,T", "对,T", "是,T", "正确,T",
                "false,F", "False,F", "F,F", "0,F", "错,F", "否,F", "错误,F"})
    void normalizeTrueFalse_shouldCoverAllVariants(String input, String expected) {
        assertThat(AutoGradeServiceImpl.normalizeTrueFalse(input)).isEqualTo(expected);
    }

    @Test
    void normalizeTrueFalse_shouldReturnNull_forUnrecognized() {
        assertThat(AutoGradeServiceImpl.normalizeTrueFalse("maybe")).isNull();
        assertThat(AutoGradeServiceImpl.normalizeTrueFalse("")).isNull();
        assertThat(AutoGradeServiceImpl.normalizeTrueFalse(null)).isNull();
    }

    // ── 单选题（type=1）──────────────────────────────────────────────────────

    @Test
    void gradeSingleChoice_correct() {
        GradeResultVO result = service.grade(answer("a"), 1, "A", SCORE_4);
        assertThat(result.getIsCorrect()).isEqualTo(1);
        assertThat(result.getScore()).isEqualByComparingTo(SCORE_4);
        assertThat(result.getReviewStatus()).isEqualTo(1);
        verify(studentAnswerMapper).updateById((StudentAnswer) any());
    }

    @Test
    void gradeSingleChoice_wrong() {
        GradeResultVO result = service.grade(answer("B"), 1, "A", SCORE_4);
        assertThat(result.getIsCorrect()).isEqualTo(0);
        assertThat(result.getScore()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void gradeSingleChoice_caseInsensitive() {
        GradeResultVO result = service.grade(answer("  a  "), 1, "A", SCORE_4);
        assertThat(result.getIsCorrect()).isEqualTo(1);
    }

    // ── 多选题（type=2）──────────────────────────────────────────────────────

    @Test
    void gradeMultipleChoice_correct_differentOrder() {
        GradeResultVO result = service.grade(answer("C,A,B"), 2, "A,B,C", SCORE_4);
        assertThat(result.getIsCorrect()).isEqualTo(1);
        assertThat(result.getScore()).isEqualByComparingTo(SCORE_4);
    }

    @Test
    void gradeMultipleChoice_partialIsWrong() {
        GradeResultVO result = service.grade(answer("A,B"), 2, "A,B,C", SCORE_4);
        assertThat(result.getIsCorrect()).isEqualTo(0);
        assertThat(result.getScore()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void gradeMultipleChoice_extraOptionIsWrong() {
        GradeResultVO result = service.grade(answer("A,B,C,D"), 2, "A,B,C", SCORE_4);
        assertThat(result.getIsCorrect()).isEqualTo(0);
    }

    @Test
    void gradeMultipleChoice_emptyAnswerIsWrong() {
        GradeResultVO result = service.grade(answer(""), 2, "A,B,C", SCORE_4);
        assertThat(result.getIsCorrect()).isEqualTo(0);
    }

    // ── 判断题（type=3）──────────────────────────────────────────────────────

    @Test
    void gradeTrueFalse_correct_withChineseVariant() {
        GradeResultVO result = service.grade(answer("对"), 3, "true", SCORE_4);
        assertThat(result.getIsCorrect()).isEqualTo(1);
        assertThat(result.getScore()).isEqualByComparingTo(SCORE_4);
    }

    @Test
    void gradeTrueFalse_wrong() {
        GradeResultVO result = service.grade(answer("错"), 3, "true", SCORE_4);
        assertThat(result.getIsCorrect()).isEqualTo(0);
        assertThat(result.getScore()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void gradeTrueFalse_unrecognizedAnswerSkipsGrade() {
        GradeResultVO result = service.grade(answer("maybe"), 3, "true", SCORE_4);
        assertThat(result.getReviewStatus()).isEqualTo(0); // 无法判断，不批改
        assertThat(result.getScore()).isNull();
        verify(studentAnswerMapper, never()).updateById((StudentAnswer) any());
    }

    // ── 填空/主观题（type=4/5）─────────────────────────────────────────────

    @Test
    void gradeEssay_shouldSkipGrade() {
        GradeResultVO result = service.grade(answer("some text"), 5, null, SCORE_4);
        assertThat(result.getReviewStatus()).isEqualTo(0);
        assertThat(result.getScore()).isNull();
        verify(studentAnswerMapper, never()).updateById((StudentAnswer) any());
    }

    @Test
    void gradeFillBlank_shouldSkipGrade() {
        GradeResultVO result = service.grade(answer("Newton"), 4, "Newton's law", SCORE_4);
        assertThat(result.getReviewStatus()).isEqualTo(0);
        verify(studentAnswerMapper, never()).updateById((StudentAnswer) any());
    }

    // ── 投票题（type=6）──────────────────────────────────────────────────────

    @Test
    void gradeVote_shouldSetZeroScoreAndNullCorrect() {
        GradeResultVO result = service.grade(answer("A"), 6, null, SCORE_4);
        assertThat(result.getScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getIsCorrect()).isNull();
        assertThat(result.getReviewStatus()).isEqualTo(1);
        verify(studentAnswerMapper).updateById((StudentAnswer) any());
    }
}
