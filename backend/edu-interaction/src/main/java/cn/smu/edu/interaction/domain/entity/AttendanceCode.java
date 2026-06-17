package cn.smu.edu.interaction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("attendance_code")
public class AttendanceCode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lessonId;

    private String code;

    private String qrToken;

    private LocalDateTime expireAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
