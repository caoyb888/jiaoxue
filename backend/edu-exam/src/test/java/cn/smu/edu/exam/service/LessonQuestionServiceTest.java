package cn.smu.edu.exam.service;

import cn.smu.edu.common.event.TeachingEvent;
import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.exam.domain.dto.PublishLessonQuestionDTO;
import cn.smu.edu.exam.domain.entity.LessonQuestion;
import cn.smu.edu.exam.domain.entity.Question;
import cn.smu.edu.exam.domain.entity.QuestionOption;
import cn.smu.edu.exam.domain.vo.LessonQuestionVO;
import cn.smu.edu.exam.repository.LessonQuestionMapper;
import cn.smu.edu.exam.repository.QuestionMapper;
import cn.smu.edu.exam.repository.QuestionOptionMapper;
import cn.smu.edu.exam.service.impl.LessonQuestionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonQuestionServiceTest {

    @Mock private LessonQuestionMapper lessonQuestionMapper;
    @Mock private QuestionMapper questionMapper;
    @Mock private QuestionOptionMapper questionOptionMapper;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks private LessonQuestionServiceImpl service;

    private static final Long LESSON_ID  = 1L;
    private static final Long TEACHER_ID = 99L;
    private static final Long QUESTION_ID = 100L;

    private Question singleChoiceQuestion;

    @BeforeEach
    void setUp() {
        singleChoiceQuestion = new Question();
        singleChoiceQuestion.setId(QUESTION_ID);
        singleChoiceQuestion.setType(1);
        singleChoiceQuestion.setContent("哪个是正确答案？");
    }

    // ── publish ───────────────────────────────────────────────────────────────

    @Test
    void publish_shouldInsertLessonQuestion_andBroadcastViaKafka() {
        when(questionMapper.selectById(QUESTION_ID)).thenReturn(singleChoiceQuestion);
        when(lessonQuestionMapper.closeAllByLesson(LESSON_ID)).thenReturn(0);
        when(lessonQuestionMapper.insert((LessonQuestion) any())).thenReturn(1);

        QuestionOption opt = new QuestionOption();
        opt.setId(1L); opt.setOptionLabel("A"); opt.setContent("选项A"); opt.setSortOrder(1);
        when(questionOptionMapper.selectByQuestionId(QUESTION_ID)).thenReturn(List.of(opt));

        PublishLessonQuestionDTO dto = new PublishLessonQuestionDTO();
        dto.setQuestionId(QUESTION_ID);

        LessonQuestionVO result = service.publish(LESSON_ID, TEACHER_ID, dto);

        assertThat(result.getQuestionId()).isEqualTo(QUESTION_ID);
        assertThat(result.getQuestionType()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(0);
        assertThat(result.getOptions()).hasSize(1);
        // 课堂发题时 isCorrect 必须为 null（防泄题）
        assertThat(result.getOptions().get(0).getIsCorrect()).isNull();

        // 验证关闭旧题 + 插入新题
        verify(lessonQuestionMapper).closeAllByLesson(LESSON_ID);
        verify(lessonQuestionMapper).insert((LessonQuestion) any());

        // 验证 Kafka 广播
        ArgumentCaptor<TeachingEvent> kafkaCaptor = ArgumentCaptor.forClass(TeachingEvent.class);
        verify(kafkaTemplate).send(eq("edu.teaching.events"), eq(LESSON_ID.toString()), kafkaCaptor.capture());
        assertThat(kafkaCaptor.getValue().getEventType()).isEqualTo("QUESTION_PUBLISHED");
        assertThat(kafkaCaptor.getValue().getLessonId()).isEqualTo(LESSON_ID);
    }

    @Test
    void publish_shouldThrow_whenQuestionNotFound() {
        when(questionMapper.selectById(999L)).thenReturn(null);
        PublishLessonQuestionDTO dto = new PublishLessonQuestionDTO();
        dto.setQuestionId(999L);

        assertThatThrownBy(() -> service.publish(LESSON_ID, TEACHER_ID, dto))
                .isInstanceOf(BizException.class);
    }

    @Test
    void publish_nonChoiceQuestion_shouldNotLoadOptions() {
        Question essayQ = new Question();
        essayQ.setId(200L);
        essayQ.setType(5); // 主观题，无选项
        essayQ.setContent("请简述...");
        when(questionMapper.selectById(200L)).thenReturn(essayQ);
        when(lessonQuestionMapper.closeAllByLesson(LESSON_ID)).thenReturn(0);
        when(lessonQuestionMapper.insert((LessonQuestion) any())).thenReturn(1);

        PublishLessonQuestionDTO dto = new PublishLessonQuestionDTO();
        dto.setQuestionId(200L);

        LessonQuestionVO result = service.publish(LESSON_ID, TEACHER_ID, dto);

        assertThat(result.getOptions()).isNull();
        verify(questionOptionMapper, never()).selectByQuestionId(any());
    }

    // ── close ─────────────────────────────────────────────────────────────────

    @Test
    void close_shouldUpdateStatusAndBroadcast() {
        LessonQuestion lq = new LessonQuestion();
        lq.setId(10L);
        lq.setLessonId(LESSON_ID);
        lq.setTeacherId(TEACHER_ID);
        lq.setStatus(0);
        when(lessonQuestionMapper.selectById(10L)).thenReturn(lq);
        when(lessonQuestionMapper.updateById((LessonQuestion) any())).thenReturn(1);

        service.close(10L, TEACHER_ID);

        ArgumentCaptor<LessonQuestion> captor = ArgumentCaptor.forClass(LessonQuestion.class);
        verify(lessonQuestionMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(captor.getValue().getClosedAt()).isNotNull();

        verify(kafkaTemplate).send(eq("edu.teaching.events"), eq(LESSON_ID.toString()), any(TeachingEvent.class));
    }

    @Test
    void close_alreadyClosed_shouldBeIdempotent() {
        LessonQuestion lq = new LessonQuestion();
        lq.setId(10L); lq.setTeacherId(TEACHER_ID); lq.setStatus(1); // already closed
        when(lessonQuestionMapper.selectById(10L)).thenReturn(lq);

        service.close(10L, TEACHER_ID);

        verify(lessonQuestionMapper, never()).updateById((LessonQuestion) any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void close_shouldThrow_whenCalledByOtherTeacher() {
        LessonQuestion lq = new LessonQuestion();
        lq.setId(10L); lq.setTeacherId(TEACHER_ID); lq.setStatus(0);
        when(lessonQuestionMapper.selectById(10L)).thenReturn(lq);

        assertThatThrownBy(() -> service.close(10L, 999L))
                .isInstanceOf(BizException.class);
    }

    // ── getCurrent ────────────────────────────────────────────────────────────

    @Test
    void getCurrent_shouldReturnCurrentOpenQuestion() {
        LessonQuestion lq = new LessonQuestion();
        lq.setId(10L); lq.setLessonId(LESSON_ID); lq.setQuestionId(QUESTION_ID); lq.setStatus(0);
        when(lessonQuestionMapper.selectCurrentByLesson(LESSON_ID)).thenReturn(lq);
        when(questionMapper.selectById(QUESTION_ID)).thenReturn(singleChoiceQuestion);
        when(questionOptionMapper.selectByQuestionId(QUESTION_ID)).thenReturn(List.of());

        LessonQuestionVO result = service.getCurrent(LESSON_ID);

        assertThat(result).isNotNull();
        assertThat(result.getQuestionId()).isEqualTo(QUESTION_ID);
        assertThat(result.getStatus()).isEqualTo(0);
    }

    @Test
    void getCurrent_shouldReturnNull_whenNoOpenQuestion() {
        when(lessonQuestionMapper.selectCurrentByLesson(LESSON_ID)).thenReturn(null);

        assertThat(service.getCurrent(LESSON_ID)).isNull();
    }

    // ── getHistory ─────────────────────────────────────────────────────────────

    @Test
    void getHistory_shouldReturnAllPublishedQuestions() {
        LessonQuestion lq1 = new LessonQuestion();
        lq1.setId(1L); lq1.setLessonId(LESSON_ID); lq1.setQuestionId(100L); lq1.setStatus(1);
        LessonQuestion lq2 = new LessonQuestion();
        lq2.setId(2L); lq2.setLessonId(LESSON_ID); lq2.setQuestionId(200L); lq2.setStatus(0);

        when(lessonQuestionMapper.selectByLesson(LESSON_ID)).thenReturn(List.of(lq1, lq2));

        Question q1 = new Question(); q1.setId(100L); q1.setType(1); q1.setContent("题1");
        Question q2 = new Question(); q2.setId(200L); q2.setType(3); q2.setContent("题2");
        when(questionMapper.selectBatchIds(anyCollection())).thenReturn(List.of(q1, q2));
        when(questionOptionMapper.selectByQuestionId(100L)).thenReturn(List.of());

        List<LessonQuestionVO> history = service.getHistory(LESSON_ID);

        assertThat(history).hasSize(2);
    }

    @Test
    void getHistory_shouldReturnEmpty_whenNoQuestions() {
        when(lessonQuestionMapper.selectByLesson(LESSON_ID)).thenReturn(List.of());
        assertThat(service.getHistory(LESSON_ID)).isEmpty();
    }
}
