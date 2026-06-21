package cn.smu.edu.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通用通知事件 — 生产者：各业务服务（AI 任务完成等）；消费者：edu-notify。
 * Topic: edu.notice
 *
 * <p>路由规则（由 edu-notify NoticeConsumer 处理）：
 * <ul>
 *   <li>userId 非空   → 单播 /user/{userId}/queue/messages（如：通知教师批改完成）</li>
 *   <li>lessonId 非空 → 广播 /topic/lesson/{id}/ai（如：课堂摘要/思维导图完成）</li>
 * </ul>
 *
 * <p>type 枚举：AI_REVIEW_DONE / AI_SUMMARY_DONE / AI_MINDMAP_DONE / AI_GENERATE_DONE
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyEvent implements Serializable {

    private String type;
    private Long userId;
    private Long lessonId;
    private String message;
    private Map<String, Object> payload;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public static NotifyEvent toUser(Long userId, String type, String message, Map<String, Object> payload) {
        return NotifyEvent.builder().userId(userId).type(type).message(message).payload(payload).build();
    }

    public static NotifyEvent toLesson(Long lessonId, String type, String message, Map<String, Object> payload) {
        return NotifyEvent.builder().lessonId(lessonId).type(type).message(message).payload(payload).build();
    }
}
