package cn.smu.edu.interaction.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 签到队列项（Redis List 内存储的 JSON 对象）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendQueueItem {

    private Long lessonId;
    private Long studentId;
    private Long classId;
    private String method;
    private String ipAddress;
    private LocalDateTime attendedAt;
}
