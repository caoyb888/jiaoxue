package cn.smu.edu.exam.service;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.exam.converter.ExamPublishConverter;
import cn.smu.edu.exam.converter.QuestionConverter;
import cn.smu.edu.exam.domain.dto.ExamPublishCreateDTO;
import cn.smu.edu.exam.domain.dto.ExamPublishUpdateDTO;
import cn.smu.edu.exam.domain.entity.ExamPaper;
import cn.smu.edu.exam.domain.entity.ExamPublish;
import cn.smu.edu.exam.domain.vo.ExamPublishVO;
import cn.smu.edu.exam.repository.*;
import cn.smu.edu.exam.domain.vo.StudentExamListVO;
import cn.smu.edu.exam.service.impl.ExamPublishServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamPublishServiceTest {

    @Mock private ExamPublishMapper publishMapper;
    @Mock private ExamPaperMapper paperMapper;
    @Mock private ExamPaperQuestionMapper paperQuestionMapper;
    @Mock private QuestionMapper questionMapper;
    @Mock private QuestionOptionMapper questionOptionMapper;
    @Mock private ExamMonitorMapper monitorMapper;
    @Mock private ExamPublishConverter converter;
    @Mock private QuestionConverter questionConverter;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private ExamPublishServiceImpl service;

    private static final Long TEACHER_ID  = 1L;
    private static final Long OTHER_ID    = 2L;
    private static final Long PAPER_ID    = 10L;
    private static final Long PUBLISH_ID  = 100L;

    private final LocalDateTime FUTURE_START = LocalDateTime.now().plusHours(1);
    private final LocalDateTime FUTURE_END   = LocalDateTime.now().plusHours(3);
    private final LocalDateTime PAST_START   = LocalDateTime.now().minusHours(2);
    private final LocalDateTime PAST_END     = LocalDateTime.now().minusHours(1);
    private final LocalDateTime ACTIVE_START = LocalDateTime.now().minusMinutes(30);
    private final LocalDateTime ACTIVE_END   = LocalDateTime.now().plusMinutes(30);

    private ExamPublish scheduledPublish;
    private ExamPublish activePublish;
    private ExamPublish endedPublish;

    @BeforeEach
    void setUp() {
        scheduledPublish = buildPublish(0, FUTURE_START, FUTURE_END, null);
        activePublish    = buildPublish(1, ACTIVE_START, ACTIVE_END, null);
        endedPublish     = buildPublish(2, PAST_START, PAST_END, null);
    }

    private ExamPublish buildPublish(int status, LocalDateTime start, LocalDateTime end, String pwHash) {
        ExamPublish p = new ExamPublish();
        p.setId(PUBLISH_ID);
        p.setTeacherId(TEACHER_ID);
        p.setPaperId(PAPER_ID);
        p.setClassId(200L);
        p.setStartTime(start);
        p.setEndTime(end);
        p.setDurationMin(60);
        p.setPasswordHash(pwHash);
        p.setEnableMonitor(0);
        p.setFaceVerifyType(0);
        p.setAllowCopy(0);
        p.setShuffleQuestion(0);
        p.setShuffleOption(0);
        p.setStatus(status);
        return p;
    }

    // ── computeStatus (static) ────────────────────────────────────────────

    @Test
    void computeStatus_shouldReturn0_whenBeforeStart() {
        assertThat(ExamPublishServiceImpl.computeStatus(FUTURE_START, FUTURE_END)).isEqualTo(0);
    }

    @Test
    void computeStatus_shouldReturn1_whenActive() {
        assertThat(ExamPublishServiceImpl.computeStatus(ACTIVE_START, ACTIVE_END)).isEqualTo(1);
    }

    @Test
    void computeStatus_shouldReturn2_whenAfterEnd() {
        assertThat(ExamPublishServiceImpl.computeStatus(PAST_START, PAST_END)).isEqualTo(2);
    }

    // ── publish ───────────────────────────────────────────────────────────

    @Test
    void publish_shouldRejectInvalidTimeRange() {
        ExamPublishCreateDTO dto = new ExamPublishCreateDTO();
        dto.setPaperId(PAPER_ID);
        dto.setClassId(200L);
        dto.setStartTime(FUTURE_END);     // start > end
        dto.setEndTime(FUTURE_START);

        assertThatThrownBy(() -> service.publish(dto, TEACHER_ID))
                .isInstanceOf(BizException.class);
    }

    @Test
    void publish_shouldHashPassword_whenProvided() {
        ExamPublishCreateDTO dto = new ExamPublishCreateDTO();
        dto.setPaperId(PAPER_ID);
        dto.setClassId(200L);
        dto.setStartTime(FUTURE_START);
        dto.setEndTime(FUTURE_END);
        dto.setPassword("secret123");

        when(paperMapper.selectById(PAPER_ID)).thenReturn(new ExamPaper());
        when(converter.toEntity(dto)).thenReturn(new ExamPublish());
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$hash");
        when(converter.toVO(any())).thenReturn(new ExamPublishVO());

        service.publish(dto, TEACHER_ID);

        ArgumentCaptor<ExamPublish> cap = ArgumentCaptor.forClass(ExamPublish.class);
        verify(publishMapper).insert(cap.capture());
        assertThat(cap.getValue().getPasswordHash()).isEqualTo("$2a$hash");
        verify(passwordEncoder).encode("secret123");
    }

    @Test
    void publish_shouldNotHashPassword_whenNull() {
        ExamPublishCreateDTO dto = new ExamPublishCreateDTO();
        dto.setPaperId(PAPER_ID);
        dto.setClassId(200L);
        dto.setStartTime(FUTURE_START);
        dto.setEndTime(FUTURE_END);
        dto.setPassword(null);

        when(paperMapper.selectById(PAPER_ID)).thenReturn(new ExamPaper());
        when(converter.toEntity(dto)).thenReturn(new ExamPublish());
        when(converter.toVO(any())).thenReturn(new ExamPublishVO());

        service.publish(dto, TEACHER_ID);

        verify(passwordEncoder, never()).encode(any());
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    void update_shouldThrowForbidden_forNonTeacher() {
        when(publishMapper.selectById(PUBLISH_ID)).thenReturn(scheduledPublish);
        assertThatThrownBy(() -> service.update(PUBLISH_ID, new ExamPublishUpdateDTO(), OTHER_ID))
                .isInstanceOf(BizException.class);
    }

    @Test
    void update_shouldThrowError_whenExamActive() {
        when(publishMapper.selectById(PUBLISH_ID)).thenReturn(activePublish);
        assertThatThrownBy(() -> service.update(PUBLISH_ID, new ExamPublishUpdateDTO(), TEACHER_ID))
                .isInstanceOf(BizException.class);
    }

    @Test
    void update_shouldClearPassword_whenEmptyStringProvided() {
        when(publishMapper.selectById(PUBLISH_ID)).thenReturn(scheduledPublish);
        doNothing().when(converter).updateEntity(any(), any());
        when(converter.toVO(any())).thenReturn(new ExamPublishVO());

        ExamPublishUpdateDTO dto = new ExamPublishUpdateDTO();
        dto.setPassword("");  // 空串=取消密码

        service.update(PUBLISH_ID, dto, TEACHER_ID);

        assertThat(scheduledPublish.getPasswordHash()).isNull();
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void update_shouldUpdatePassword_whenNonEmptyProvided() {
        when(publishMapper.selectById(PUBLISH_ID)).thenReturn(scheduledPublish);
        doNothing().when(converter).updateEntity(any(), any());
        when(passwordEncoder.encode("newpw")).thenReturn("$2a$newhash");
        when(converter.toVO(any())).thenReturn(new ExamPublishVO());

        ExamPublishUpdateDTO dto = new ExamPublishUpdateDTO();
        dto.setPassword("newpw");

        service.update(PUBLISH_ID, dto, TEACHER_ID);

        assertThat(scheduledPublish.getPasswordHash()).isEqualTo("$2a$newhash");
    }

    // ── cancel ────────────────────────────────────────────────────────────

    @Test
    void cancel_shouldThrowError_whenExamActive() {
        when(publishMapper.selectById(PUBLISH_ID)).thenReturn(activePublish);
        assertThatThrownBy(() -> service.cancel(PUBLISH_ID, TEACHER_ID))
                .isInstanceOf(BizException.class);
    }

    @Test
    void cancel_shouldSucceed_forScheduledExam() {
        when(publishMapper.selectById(PUBLISH_ID)).thenReturn(scheduledPublish);
        assertThatNoException().isThrownBy(() -> service.cancel(PUBLISH_ID, TEACHER_ID));
        verify(publishMapper).updateById((ExamPublish) any());
        verify(publishMapper).deleteById(PUBLISH_ID);
    }

    // ── verifyPassword ────────────────────────────────────────────────────

    @Test
    void verifyPassword_shouldReturnTrue_whenNoPasswordSet() {
        when(publishMapper.selectById(PUBLISH_ID)).thenReturn(scheduledPublish); // no hash
        assertThat(service.verifyPassword(PUBLISH_ID, "anything")).isTrue();
    }

    @Test
    void verifyPassword_shouldReturnTrue_whenCorrectPassword() {
        ExamPublish withPw = buildPublish(0, FUTURE_START, FUTURE_END, "$2a$hash");
        when(publishMapper.selectById(PUBLISH_ID)).thenReturn(withPw);
        when(passwordEncoder.matches("correct", "$2a$hash")).thenReturn(true);

        assertThat(service.verifyPassword(PUBLISH_ID, "correct")).isTrue();
    }

    @Test
    void verifyPassword_shouldReturnFalse_whenWrongPassword() {
        ExamPublish withPw = buildPublish(0, FUTURE_START, FUTURE_END, "$2a$hash");
        when(publishMapper.selectById(PUBLISH_ID)).thenReturn(withPw);
        when(passwordEncoder.matches("wrong", "$2a$hash")).thenReturn(false);

        assertThat(service.verifyPassword(PUBLISH_ID, "wrong")).isFalse();
    }

    // ── getStudentView ────────────────────────────────────────────────────

    @Test
    void getStudentView_shouldThrowPasswordError_whenWrongPassword() {
        ExamPublish withPw = buildPublish(1, ACTIVE_START, ACTIVE_END, "$2a$hash");
        when(publishMapper.selectById(PUBLISH_ID)).thenReturn(withPw);
        when(passwordEncoder.matches("bad", "$2a$hash")).thenReturn(false);

        assertThatThrownBy(() -> service.getStudentView(PUBLISH_ID, 99L, "bad"))
                .isInstanceOf(BizException.class);
    }

    @Test
    void getStudentView_shouldHideAnswers_whenAnswerShowAtNotReached() {
        ExamPublish pub = buildPublish(2, PAST_START, PAST_END, null);
        pub.setAnswerShowAt(LocalDateTime.now().plusDays(1)); // 明天才公布答案
        when(publishMapper.selectById(PUBLISH_ID)).thenReturn(pub);
        when(paperQuestionMapper.selectByPaperId(any())).thenReturn(java.util.Collections.emptyList());

        var vo = service.getStudentView(PUBLISH_ID, 99L, null);
        assertThat(vo.getAnswerVisible()).isFalse();
    }

    @Test
    void getStudentView_shouldShowAnswers_whenAnswerShowAtIsNull() {
        ExamPublish pub = buildPublish(2, PAST_START, PAST_END, null);
        pub.setAnswerShowAt(null); // null = 立即可查
        when(publishMapper.selectById(PUBLISH_ID)).thenReturn(pub);
        when(paperQuestionMapper.selectByPaperId(any())).thenReturn(java.util.Collections.emptyList());

        var vo = service.getStudentView(PUBLISH_ID, 99L, null);
        assertThat(vo.getAnswerVisible()).isTrue();
    }

    // ── listForStudent ────────────────────────────────────────────────────

    @Test
    void listForStudent_shouldMarkEnteredTrue_whenMonitorRecordExists() {
        ExamPublish pub = buildPublish(1, ACTIVE_START, ACTIVE_END, null);
        pub.setClassId(200L);
        when(publishMapper.selectByClassId(200L)).thenReturn(java.util.List.of(pub));

        cn.smu.edu.exam.domain.entity.ExamMonitor monitor = new cn.smu.edu.exam.domain.entity.ExamMonitor();
        monitor.setPublishId(PUBLISH_ID);
        monitor.setStudentId(99L);
        monitor.setSessionStatus("ANSWERING");
        when(monitorMapper.selectList(any())).thenReturn(java.util.List.of(monitor));

        List<StudentExamListVO> result = service.listForStudent(200L, 99L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEntered()).isTrue();
        assertThat(result.get(0).getSubmitted()).isFalse();
    }

    @Test
    void listForStudent_shouldMarkSubmittedTrue_whenSessionStatusIsSubmitted() {
        ExamPublish pub = buildPublish(2, PAST_START, PAST_END, null);
        pub.setClassId(200L);
        when(publishMapper.selectByClassId(200L)).thenReturn(java.util.List.of(pub));

        cn.smu.edu.exam.domain.entity.ExamMonitor monitor = new cn.smu.edu.exam.domain.entity.ExamMonitor();
        monitor.setPublishId(PUBLISH_ID);
        monitor.setStudentId(99L);
        monitor.setSessionStatus("SUBMITTED");
        when(monitorMapper.selectList(any())).thenReturn(java.util.List.of(monitor));

        List<StudentExamListVO> result = service.listForStudent(200L, 99L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSubmitted()).isTrue();
    }

    // ── syncExamStatus ─────────────────────────────────────────────────────

    @Test
    void syncExamStatus_shouldUpdateStatus_whenComputedStatusDiffers() {
        ExamPublish pub = buildPublish(0, ACTIVE_START, ACTIVE_END, null); // DB中=0，实际=1
        when(publishMapper.selectActiveOrPending()).thenReturn(java.util.List.of(pub));

        service.syncExamStatus();

        // 应调用 updateById 更新状态到 1
        ArgumentCaptor<ExamPublish> cap = ArgumentCaptor.forClass(ExamPublish.class);
        verify(publishMapper).updateById(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(1);
    }

    @Test
    void syncExamStatus_shouldNotUpdate_whenStatusMatches() {
        ExamPublish pub = buildPublish(1, ACTIVE_START, ACTIVE_END, null); // DB=1，实际也=1
        when(publishMapper.selectActiveOrPending()).thenReturn(java.util.List.of(pub));

        service.syncExamStatus();

        verify(publishMapper, never()).updateById((ExamPublish) any());
    }
}
