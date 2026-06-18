package cn.smu.edu.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 异步任务事件 — 生产者：edu-course（课堂结束）；消费者：edu-ai（concurrency=3）
 * Topic: edu.ai.tasks
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskEvent implements Serializable {

    private String taskId;
    private Long lessonId;
    private Long teacherId;
    private Long classId;
    private String taskType;      // SUMMARY / MINDMAP / REVIEW / GENERATE
    private LocalDateTime triggerTime;

    public static AiTaskEvent lessonSummary(Long lessonId, Long teacherId, Long classId, String taskId) {
        return new AiTaskEvent(taskId, lessonId, teacherId, classId, "SUMMARY", LocalDateTime.now());
    }
}
