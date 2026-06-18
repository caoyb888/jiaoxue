package cn.smu.edu.exam.service;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.exam.domain.dto.ReviewAnswerDTO;
import cn.smu.edu.exam.domain.entity.ExamPublish;
import cn.smu.edu.exam.domain.entity.StudentAnswer;
import cn.smu.edu.exam.domain.vo.StudentAnswerVO;
import cn.smu.edu.exam.repository.ExamAnswerAttachmentMapper;
import cn.smu.edu.exam.repository.ExamPublishMapper;
import cn.smu.edu.exam.repository.StudentAnswerMapper;
import cn.smu.edu.exam.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewServiceTest {

    @Mock private StudentAnswerMapper studentAnswerMapper;
    @Mock private ExamPublishMapper publishMapper;
    @Mock private ExamAnswerAttachmentMapper attachmentMapper;

    @InjectMocks private ReviewServiceImpl service;

    private StudentAnswer answer;
    private ExamPublish publish;

    @BeforeEach
    void setUp() {
        answer = new StudentAnswer();
        answer.setId(1L);
        answer.setPublishId(10L);
        answer.setQuestionId(100L);
        answer.setStudentId(99L);
        answer.setAnswerContent("这是主观题答案");
        answer.setReviewStatus(1); // 自动批改完成

        publish = new ExamPublish();
        publish.setId(10L);
        publish.setTeacherId(50L);

        when(attachmentMapper.selectByStudentAnswerId(anyLong())).thenReturn(Collections.emptyList());
    }

    // ── review ────────────────────────────────────────────────────────────────

    @Test
    void review_shouldUpdateScoreAndStatusToTeacherReviewed() {
        when(studentAnswerMapper.selectById(1L)).thenReturn(answer);
        when(publishMapper.selectById(10L)).thenReturn(publish);

        ReviewAnswerDTO dto = new ReviewAnswerDTO();
        dto.setScore(new BigDecimal("8.00"));
        dto.setIsCorrect(1);
        dto.setComment("答案基本正确，结构清晰");

        StudentAnswerVO result = service.review(1L, 50L, dto);

        assertThat(answer.getScore()).isEqualByComparingTo("8.00");
        assertThat(answer.getIsCorrect()).isEqualTo(1);
        assertThat(answer.getComment()).isEqualTo("答案基本正确，结构清晰");
        assertThat(answer.getReviewStatus()).isEqualTo(2);
        assertThat(result.getReviewStatus()).isEqualTo(2);
        verify(studentAnswerMapper).updateById((StudentAnswer) answer);
    }

    @Test
    void review_shouldAllowZeroScore() {
        when(studentAnswerMapper.selectById(1L)).thenReturn(answer);
        when(publishMapper.selectById(10L)).thenReturn(publish);

        ReviewAnswerDTO dto = new ReviewAnswerDTO();
        dto.setScore(BigDecimal.ZERO);
        dto.setIsCorrect(0);

        StudentAnswerVO result = service.review(1L, 50L, dto);

        assertThat(answer.getScore()).isEqualByComparingTo("0");
        assertThat(result.getScore()).isEqualByComparingTo("0");
    }

    @Test
    void review_shouldThrow_whenAnswerNotFound() {
        when(studentAnswerMapper.selectById(999L)).thenReturn(null);
        ReviewAnswerDTO dto = new ReviewAnswerDTO();
        dto.setScore(BigDecimal.TEN);
        assertThatThrownBy(() -> service.review(999L, 50L, dto))
                .isInstanceOf(BizException.class);
        verify(studentAnswerMapper, never()).updateById((StudentAnswer) any());
    }

    @Test
    void review_shouldThrow_whenTeacherNotOwner() {
        when(studentAnswerMapper.selectById(1L)).thenReturn(answer);
        when(publishMapper.selectById(10L)).thenReturn(publish);

        ReviewAnswerDTO dto = new ReviewAnswerDTO();
        dto.setScore(BigDecimal.TEN);

        // 用不同 teacherId（99L != 50L）
        assertThatThrownBy(() -> service.review(1L, 99L, dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("无权批改");
    }

    @Test
    void review_shouldAllowRereview_whenAlreadyTeacherReviewed() {
        answer.setReviewStatus(2); // 已批改，再次修改
        when(studentAnswerMapper.selectById(1L)).thenReturn(answer);
        when(publishMapper.selectById(10L)).thenReturn(publish);

        ReviewAnswerDTO dto = new ReviewAnswerDTO();
        dto.setScore(new BigDecimal("9.00"));

        service.review(1L, 50L, dto);

        assertThat(answer.getScore()).isEqualByComparingTo("9.00");
        assertThat(answer.getReviewStatus()).isEqualTo(2);
    }

    // ── listAnswers ───────────────────────────────────────────────────────────

    @Test
    void listAnswers_shouldReturnAllAnswersForStudent() {
        when(studentAnswerMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(List.of(answer));
        List<StudentAnswerVO> result = service.listAnswers(10L, 99L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStudentId()).isEqualTo(99L);
    }

    @Test
    void listAnswers_shouldReturnEmpty_whenNoAnswers() {
        when(studentAnswerMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(List.of());
        List<StudentAnswerVO> result = service.listAnswers(10L, 99L);
        assertThat(result).isEmpty();
    }
}
