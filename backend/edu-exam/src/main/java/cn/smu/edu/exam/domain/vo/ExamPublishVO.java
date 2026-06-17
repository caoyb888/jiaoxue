package cn.smu.edu.exam.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师视角的发布 VO（含全部配置，不含 passwordHash）。
 * 学生视角用 ExamPublishStudentVO。
 */
@Data
public class ExamPublishVO {
    private Long id;
    private Long paperId;
    private Long classId;
    private Long teacherId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMin;
    /** true = 设有密码（不返回明文或散列） */
    private Boolean hasPassword;
    private Integer enableMonitor;
    private Integer faceVerifyType;
    private LocalDateTime answerShowAt;
    private Integer allowCopy;
    private Integer shuffleQuestion;
    private Integer shuffleOption;
    /** 实时计算状态（由当前时间与 start/end 推导） */
    private Integer status;
    private String statusLabel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
