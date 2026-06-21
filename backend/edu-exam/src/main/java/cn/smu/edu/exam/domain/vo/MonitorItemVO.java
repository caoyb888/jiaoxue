package cn.smu.edu.exam.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 单个学生的监考状态概览（用于大屏学生列表）。
 */
@Data
public class MonitorItemVO {

    private Long studentId;
    private String sessionStatus;
    private LocalDateTime lastHeartbeatAt;
    private Integer tabSwitchCount;
    private Integer screenshotCount;
    private Integer copyCount;
    private Integer abnormalFlag;
    private Integer faceVerifyPassed;
    private BigDecimal faceVerifyScore;
    private LocalDateTime submitTime;
}
