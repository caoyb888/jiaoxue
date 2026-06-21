package cn.smu.edu.exam.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 考试异常行为 Kafka 事件（发往 edu.exam.abnormal）。
 * edu-notify 消费后通过 WebSocket 推送给教师端监考大屏。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamAbnormalEvent {

    private Long publishId;
    private Long studentId;
    private String eventType;
    private int eventCount;
    private boolean abnormal;
    private LocalDateTime occurredAt;
}
