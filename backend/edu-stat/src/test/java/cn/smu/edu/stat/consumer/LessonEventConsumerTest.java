package cn.smu.edu.stat.consumer;

import cn.smu.edu.common.event.TeachingEvent;
import cn.smu.edu.stat.domain.entity.LessonEventLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import cn.smu.edu.stat.service.LessonEventWriter;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LessonEventConsumerTest {

    @Mock
    LessonEventWriter writer;

    @InjectMocks
    LessonEventConsumer consumer;

    @Test
    void consume_shouldMapBarrageToBucket_andExtractStudentId() {
        TeachingEvent event = new TeachingEvent("BARRAGE", 100L, 9L,
                Map.of("content", "hi", "studentId", 7));

        consumer.consume(event);

        ArgumentCaptor<LessonEventLog> cap = ArgumentCaptor.forClass(LessonEventLog.class);
        verify(writer).offer(cap.capture());
        LessonEventLog row = cap.getValue();
        assertThat(row.getEventType()).isEqualTo("BARRAGE");
        assertThat(row.getLessonId()).isEqualTo(100L);
        assertThat(row.getStudentId()).isEqualTo(7L);
    }

    @Test
    void consume_shouldMapQuestionPublishedToQuestionBucket() {
        consumer.consume(new TeachingEvent("QUESTION_PUBLISHED", 1L, 2L, Map.of()));
        ArgumentCaptor<LessonEventLog> cap = ArgumentCaptor.forClass(LessonEventLog.class);
        verify(writer).offer(cap.capture());
        assertThat(cap.getValue().getEventType()).isEqualTo("QUESTION");
    }

    @Test
    void consume_shouldIgnoreUnknownEventType() {
        consumer.consume(new TeachingEvent("ROLL_CALL", 1L, 2L, Map.of()));
        verify(writer, never()).offer(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void consume_shouldIgnoreNullEvent() {
        consumer.consume(null);
        verify(writer, never()).offer(org.mockito.ArgumentMatchers.any());
    }
}
