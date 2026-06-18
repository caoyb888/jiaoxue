package cn.smu.edu.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 课堂实时事件 — 生产者：各业务服务；消费者：edu-notify（concurrency=5）
 * Topic: edu.teaching.events
 *
 * <p>eventType 枚举：
 * <ul>
 *   <li>QUESTION_PUBLISHED — 教师发布课堂题目（payload 含题目内容和选项）</li>
 *   <li>QUESTION_CLOSED    — 教师关闭题目作答</li>
 *   <li>SCORE_ADDED        — 教师给学生加分</li>
 *   <li>BARRAGE            — 弹幕广播</li>
 *   <li>ROLL_CALL          — 随机点名结果</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeachingEvent implements Serializable {

    private String eventType;
    private Long lessonId;
    private Long teacherId;
    /** 事件具体数据，由各业务方自定义 */
    private Map<String, Object> payload;
}
