package cn.smu.edu.interaction.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendResultVO {

    private Long lessonId;

    private Long studentId;

    /** 是否首次签到（false=重复签到） */
    private boolean firstAttend;

    /** 当前签到总人数（实时计数，从 Redis attend:count 读取） */
    private long totalCount;

    private String message;
}
