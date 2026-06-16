package cn.smu.edu.user.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String studentNo;
    private String username;
    private String realName;
    private String phoneCipher;
    private String email;
    private String passwordHash;
    private Integer userType;
    private Long deptId;
    private String openId;
    private String unionId;
    private String avatarUrl;
    private Integer status;

    @TableLogic
    private Integer isDeleted;

    private LocalDateTime lastLoginAt;
    private String jwxtUid;
    private String archivePhotoPath;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
