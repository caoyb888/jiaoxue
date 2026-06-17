package cn.smu.edu.interaction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("attendance")
public class Attendance {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lessonId;

    private Long studentId;

    private Long classId;

    /** 0-缺勤 1-正常签到 2-迟到 3-请假 */
    private Integer status;

    /** QR-扫码 CODE-口令 MANUAL-教师手动补签 */
    private String method;

    private LocalDateTime attendedAt;

    private String ipAddress;

    /** 0-原始 1-已修改 */
    private Integer isModified;

    private Long modifierId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
