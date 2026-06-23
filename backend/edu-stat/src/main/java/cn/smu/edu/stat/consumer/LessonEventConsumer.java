package cn.smu.edu.stat.consumer;

import cn.smu.edu.common.constant.KafkaTopic;
import cn.smu.edu.common.event.TeachingEvent;
import cn.smu.edu.stat.domain.entity.LessonEventLog;
import cn.smu.edu.stat.service.LessonEventWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/**
 * 课堂事件消费者 → ClickHouse 明细层 {@code lesson_event_log}。
 *
 * <p>消费 {@code edu.teaching.events}（concurrency=5，CLAUDE.md 限定），
 * 将业务事件类型归一为 ClickHouse 事件桶（ATTEND/BARRAGE/QUESTION/SCORE/SLIDE），
 * 入 {@link LessonEventWriter} 缓冲队列批量落库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LessonEventConsumer {

    /** 业务事件类型 → ClickHouse 归一事件桶。未列出的事件忽略（不入库）。 */
    private static final Map<String, String> EVENT_TYPE_MAP = Map.of(
            "ATTEND_COUNT", "ATTEND",
            "BARRAGE", "BARRAGE",
            "QUESTION_PUBLISHED", "QUESTION",
            "QUESTION_CLOSED", "QUESTION",
            "SCORE_ADDED", "SCORE",
            "SLIDE_CHANGED", "SLIDE");

    private static final int MAX_VALUE_LEN = 512;

    private final LessonEventWriter writer;

    @KafkaListener(topics = KafkaTopic.TEACHING_EVENTS,
                   groupId = "stat-lesson-event",
                   concurrency = "5",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consume(TeachingEvent event) {
        if (event == null || event.getEventType() == null) {
            return;
        }
        String bucket = EVENT_TYPE_MAP.get(event.getEventType());
        if (bucket == null) {
            log.debug("事件类型 {} 无对应 ClickHouse 桶，忽略", event.getEventType());
            return;
        }
        Map<String, Object> payload = event.getPayload();
        LessonEventLog row = LessonEventLog.builder()
                .statDate(LocalDate.now())
                .lessonId(event.getLessonId())
                .classId(asLong(payload, "classId"))
                .deptId(asLong(payload, "deptId"))
                .teacherId(event.getTeacherId())
                .eventType(bucket)
                .studentId(asLong(payload, "studentId"))
                .eventValue(truncate(payload))
                .build();
        writer.offer(row);
    }

    private static Long asLong(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object v = payload.get(key);
        return v instanceof Number n ? n.longValue() : null;
    }

    private static String truncate(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "";
        }
        String s = payload.toString();
        return s.length() > MAX_VALUE_LEN ? s.substring(0, MAX_VALUE_LEN) : s;
    }
}
