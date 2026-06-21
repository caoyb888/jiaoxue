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

    /** 业务主键：REVIEW=publishId(试卷发布)，GENERATE=bankId(题库)，SUMMARY/MINDMAP 可空 */
    private Long bizId;

    public static AiTaskEvent lessonSummary(Long lessonId, Long teacherId, Long classId, String taskId) {
        return new AiTaskEvent(taskId, lessonId, teacherId, classId, "SUMMARY", LocalDateTime.now(), null);
    }

    /** 主观题智能批改任务：bizId=publishId（试卷发布ID） */
    public static AiTaskEvent review(Long publishId, Long teacherId, String taskId) {
        return new AiTaskEvent(taskId, null, teacherId, null, "REVIEW", LocalDateTime.now(), publishId);
    }
}
