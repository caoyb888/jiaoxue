package cn.smu.edu.exam.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Kafka edu.exam.submit 消息体（C2 交卷容灾第一层→第二层的桥梁）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamSubmitEvent {
    private Long publishId;
    private Long studentId;
    /** 全量答案 JSON（AnswerItemDTO 列表序列化后的字符串） */
    private String answersJson;
    /** MANUAL / AUTO / FORCE */
    private String submitType;
    /** 客户端实际触发时间（学号打散后的时间） */
    private LocalDateTime clientSubmitAt;
}
