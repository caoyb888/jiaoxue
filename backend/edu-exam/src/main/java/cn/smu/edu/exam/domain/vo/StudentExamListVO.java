package cn.smu.edu.exam.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 学生端考试列表视图（不含题目内容）。 */
@Data
public class StudentExamListVO {
    private Long publishId;
    private Long paperId;
    private String paperTitle;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMin;
    private Boolean hasPassword;
    private Integer enableMonitor;
    private Integer faceVerifyType;
    private Integer status;
    private String statusLabel;
    /** 学生是否已进入（有 exam_monitor 记录） */
    private Boolean entered;
    /** 学生是否已交卷（session_status=SUBMITTED） */
    private Boolean submitted;
}
