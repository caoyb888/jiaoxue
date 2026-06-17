package cn.smu.edu.interaction.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttendanceItemVO {

    private Long studentId;

    private String studentName;

    private String studentNo;

    /** 0-缺勤 1-正常签到 2-迟到 3-请假 */
    private Integer status;

    private String statusLabel;

    /** QR/CODE/MANUAL */
    private String method;

    private LocalDateTime attendedAt;

    private Integer isModified;
}
