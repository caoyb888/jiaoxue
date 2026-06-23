package cn.smu.edu.stat.service;

import cn.smu.edu.stat.domain.entity.LessonEventLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LessonEventWriterTest {

    @Mock
    JdbcTemplate clickHouseJdbcTemplate;

    @InjectMocks
    LessonEventWriter writer;

    private LessonEventLog sample() {
        return LessonEventLog.builder()
                .statDate(LocalDate.now())
                .lessonId(1L)
                .eventType("BARRAGE")
                .studentId(2L)
                .eventValue("hi")
                .build();
    }

    @Test
    void offer_shouldNotFlush_whenBelowBatchSize() {
        for (int i = 0; i < LessonEventWriter.BATCH_SIZE - 1; i++) {
            writer.offer(sample());
        }
        verify(clickHouseJdbcTemplate, never()).batchUpdate(anyString(), any(List.class), anyInt(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void offer_shouldFlushOneBatch_whenReachingBatchSize() {
        for (int i = 0; i < LessonEventWriter.BATCH_SIZE; i++) {
            writer.offer(sample());
        }
        verify(clickHouseJdbcTemplate, times(1)).batchUpdate(
                anyString(),
                (List<LessonEventLog>) any(List.class),
                eq(LessonEventWriter.BATCH_SIZE),
                (ParameterizedPreparedStatementSetter<LessonEventLog>) any());
    }

    @Test
    void scheduledFlush_shouldWriteRemainder_belowBatchSize() {
        writer.offer(sample());
        writer.offer(sample());
        writer.scheduledFlush();
        verify(clickHouseJdbcTemplate, times(1)).batchUpdate(anyString(), any(List.class), anyInt(), any());
    }

    @Test
    void scheduledFlush_shouldDoNothing_whenEmpty() {
        writer.scheduledFlush();
        verify(clickHouseJdbcTemplate, never()).batchUpdate(anyString(), any(List.class), anyInt(), any());
    }
}
