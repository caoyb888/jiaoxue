package cn.smu.edu.exam.service;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.exam.converter.QuestionConverter;
import cn.smu.edu.exam.domain.dto.ExamEnterDTO;
import cn.smu.edu.exam.domain.entity.*;
import cn.smu.edu.exam.domain.vo.ExamEnterVO;
import cn.smu.edu.exam.domain.vo.ExamQuestionPageVO;
import cn.smu.edu.exam.repository.*;
import cn.smu.edu.exam.service.impl.ExamEnterServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamEnterServiceTest {

    @Mock private ExamPublishMapper publishMapper;
    @Mock private ExamMonitorMapper monitorMapper;
    @Mock private ExamPaperQuestionMapper paperQuestionMapper;
    @Mock private QuestionMapper questionMapper;
    @Mock private QuestionOptionMapper questionOptionMapper;
    @Mock private QuestionConverter questionConverter;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private ExamEnterServiceImpl service;

    private static final Long PUBLISH_ID = 1L;
    private static final Long STUDENT_ID = 100L;

    private ExamPublish activePublish;

    @BeforeEach
    void setUp() {
        activePublish = new ExamPublish();
        activePublish.setId(PUBLISH_ID);
        activePublish.setPaperId(10L);
        activePublish.setStartTime(LocalDateTime.now().minusMinutes(30));
        activePublish.setEndTime(LocalDateTime.now().plusMinutes(60));
        activePublish.setDurationMin(90);
        activePublish.setEnableMonitor(1);
        activePublish.setFaceVerifyType(0);
        activePublish.setAllowCopy(0);
        activePublish.setShuffleQuestion(0);
        activePublish.setShuffleOption(0);
    }

    // ── enter ─────────────────────────────────────────────────────────────

    @Test
    void enter_shouldThrowNotActive_whenExamNotStarted() {
        activePublish.setStartTime(LocalDateTime.now().plusHours(1));
        when(publishMapper.selectById(PUBLISH_ID)).thenReturn(activePublish);
        assertThatThrownBy(() -> service.enter(PUBLISH_ID, STUDENT_ID, null))
                .isInstanceOf(BizException.class);
    }

    @Test
    void enter_shouldThrowPasswordError_whenWrongPassword() {
        activePublish.setPasswordHash("$2a$hash");
        when(publishMapper.selectById(PUBLISH_ID)).thenReturn(activePublish);
        when(passwordEncoder.matches("wrong", "$2a$hash")).thenReturn(false);

        ExamEnterDTO dto = new ExamEnterDTO();
        dto.setPassword("wrong");

        assertThatThrownBy(() -> service.enter(PUBLISH_ID, STUDENT_ID, dto))
                .isInstanceOf(BizException.class);
    }

    @Test
    void enter_shouldThrowAlreadySubmitted_whenSessionSubmitted() {
        when(publishMapper.selectById(PUBLISH_ID)).thenReturn(activePublish);
        ExamMonitor submitted = new ExamMonitor();
        submitted.setSessionStatus("SUBMITTED");
        when(monitorMapper.selectByPublishAndStudent(PUBLISH_ID, STUDENT_ID)).thenReturn(submitted);

        assertThatThrownBy(() -> service.enter(PUBLISH_ID, STUDENT_ID, null))
                .isInstanceOf(BizException.class);
    }

    @Test
    void enter_shouldCreateMonitorWithAnswering_whenNoFaceVerify() {
        when(publishMapper.selectById(PUBLISH_ID)).thenReturn(activePublish);
        when(monitorMapper.selectByPublishAndStudent(PUBLISH_ID, STUDENT_ID)).thenReturn(null);
        when(paperQuestionMapper.selectByPaperId(10L)).thenReturn(Collections.emptyList());
        when(paperQuestionMapper.selectCount(any())).thenReturn(0L);

        ExamEnterVO vo = service.enter(PUBLISH_ID, STUDENT_ID, null);

        ArgumentCaptor<ExamMonitor> cap = ArgumentCaptor.forClass(ExamMonitor.class);
        verify(monitorMapper).insert((ExamMonitor) cap.capture());
        assertThat(cap.getValue().getSessionStatus()).isEqualTo("ANSWERING");
        assertThat(vo.getFirstEnter()).isTrue();
    }

    @Test
    void enter_shouldCreateMonitorWithVerifying_whenFaceVerifyRequired() {
        activePublish.setFaceVerifyType(1);
        when(publishMapper.selectById(PUBLISH_ID)).thenReturn(activePublish);
        when(monitorMapper.selectByPublishAndStudent(PUBLISH_ID, STUDENT_ID)).thenReturn(null);
        when(paperQuestionMapper.selectByPaperId(10L)).thenReturn(Collections.emptyList());
        when(paperQuestionMapper.selectCount(any())).thenReturn(0L);

        service.enter(PUBLISH_ID, STUDENT_ID, null);

        ArgumentCaptor<ExamMonitor> cap = ArgumentCaptor.forClass(ExamMonitor.class);
        verify(monitorMapper).insert((ExamMonitor) cap.capture());
        assertThat(cap.getValue().getSessionStatus()).isEqualTo("VERIFYING");
    }

    @Test
    void enter_shouldMarkFirstEnterFalse_whenReconnecting() {
        when(publishMapper.selectById(PUBLISH_ID)).thenReturn(activePublish);
        ExamMonitor existing = new ExamMonitor();
        existing.setSessionStatus("ANSWERING");
        when(monitorMapper.selectByPublishAndStudent(PUBLISH_ID, STUDENT_ID)).thenReturn(existing);
        when(paperQuestionMapper.selectByPaperId(10L)).thenReturn(Collections.emptyList());
        when(paperQuestionMapper.selectCount(any())).thenReturn(0L);

        ExamEnterVO vo = service.enter(PUBLISH_ID, STUDENT_ID, null);

        verify(monitorMapper, never()).insert((ExamMonitor) any());
        assertThat(vo.getFirstEnter()).isFalse();
    }

    @Test
    void enter_shouldReturnPaginatedQuestions_notAllAtOnce() {
        when(publishMapper.selectById(PUBLISH_ID)).thenReturn(activePublish);
        when(monitorMapper.selectByPublishAndStudent(PUBLISH_ID, STUDENT_ID)).thenReturn(null);

        // 25道题，每页10题
        List<ExamPaperQuestion> relations = buildRelations(25);
        when(paperQuestionMapper.selectByPaperId(10L)).thenReturn(relations);
        when(paperQuestionMapper.selectCount(any())).thenReturn(25L);
        when(questionMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());

        ExamEnterVO vo = service.enter(PUBLISH_ID, STUDENT_ID, null);

        assertThat(vo.getTotalQuestions()).isEqualTo(25);
        assertThat(vo.getTotalPages()).isEqualTo(3);   // ceil(25/10)
        assertThat(vo.getCurrentPage()).isEqualTo(1);
        assertThat(vo.getQuestions()).hasSize(10);     // 第一页最多10题
    }

    // ── getQuestionsPage ──────────────────────────────────────────────────

    @Test
    void getQuestionsPage_shouldThrowSessionNotFound_whenNoMonitorRecord() {
        when(publishMapper.selectById(PUBLISH_ID)).thenReturn(activePublish);
        when(monitorMapper.selectByPublishAndStudent(PUBLISH_ID, STUDENT_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.getQuestionsPage(PUBLISH_ID, STUDENT_ID, 2))
                .isInstanceOf(BizException.class);
    }

    @Test
    void getQuestionsPage_shouldReturnSecondPage() {
        when(publishMapper.selectById(PUBLISH_ID)).thenReturn(activePublish);
        ExamMonitor monitor = new ExamMonitor();
        monitor.setSessionStatus("ANSWERING");
        when(monitorMapper.selectByPublishAndStudent(PUBLISH_ID, STUDENT_ID)).thenReturn(monitor);

        List<ExamPaperQuestion> relations = buildRelations(25);
        when(paperQuestionMapper.selectByPaperId(10L)).thenReturn(relations);
        when(paperQuestionMapper.selectCount(any())).thenReturn(25L);
        when(questionMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());

        ExamQuestionPageVO vo = service.getQuestionsPage(PUBLISH_ID, STUDENT_ID, 2);

        assertThat(vo.getCurrentPage()).isEqualTo(2);
        assertThat(vo.getQuestions()).hasSize(10); // page 2: items 11-20
    }

    @Test
    void getQuestionsPage_shouldReturnLastPageWithRemainder() {
        when(publishMapper.selectById(PUBLISH_ID)).thenReturn(activePublish);
        ExamMonitor monitor = new ExamMonitor();
        monitor.setSessionStatus("ANSWERING");
        when(monitorMapper.selectByPublishAndStudent(PUBLISH_ID, STUDENT_ID)).thenReturn(monitor);

        List<ExamPaperQuestion> relations = buildRelations(25);
        when(paperQuestionMapper.selectByPaperId(10L)).thenReturn(relations);
        when(paperQuestionMapper.selectCount(any())).thenReturn(25L);
        when(questionMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());

        ExamQuestionPageVO vo = service.getQuestionsPage(PUBLISH_ID, STUDENT_ID, 3);

        assertThat(vo.getCurrentPage()).isEqualTo(3);
        assertThat(vo.getQuestions()).hasSize(5); // page 3: items 21-25 (只剩5题)
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private List<ExamPaperQuestion> buildRelations(int count) {
        List<ExamPaperQuestion> list = new java.util.ArrayList<>();
        for (int i = 1; i <= count; i++) {
            ExamPaperQuestion r = new ExamPaperQuestion();
            r.setId((long) i);
            r.setPaperId(10L);
            r.setQuestionId((long) i);
            r.setSortOrder(i);
            r.setPaperGroup("A");
            list.add(r);
        }
        return list;
    }
}
