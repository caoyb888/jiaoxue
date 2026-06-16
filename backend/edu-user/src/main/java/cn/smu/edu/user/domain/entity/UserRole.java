package cn.smu.edu.user.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_role")
public class UserRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String roleCode;
    private Long deptId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
