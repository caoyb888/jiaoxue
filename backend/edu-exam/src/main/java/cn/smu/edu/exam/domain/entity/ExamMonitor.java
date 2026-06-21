package cn.smu.edu.exam.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 监考状态表（每学生一行，实时反映考试会话状态与异常行为累计）。
 * session_status: VERIFYING/ANSWERING/SUBMITTED/OFFLINE/ABNORMAL
 */
@Data
@TableName("exam_monitor")
public class ExamMonitor {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long publishId;
    private Long studentId;

    private String sessionStatus;
    private LocalDateTime lastHeartbeatAt;

    private Integer faceVerifyPassed;
    private BigDecimal faceVerifyScore;

    private Integer tabSwitchCount;
    private Integer screenshotCount;
    private Integer copyCount;
    private Integer abnormalFlag;
    private String snapshotUrl;
    private LocalDateTime submitTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
