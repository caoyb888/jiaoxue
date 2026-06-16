package cn.smu.edu.user.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_dept")
public class SysDept {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deptCode;
    private String deptName;
    private Long parentId;
    private Integer deptType;
    private Integer level;
    private Integer sortOrder;

    @TableLogic
    private Integer isDeleted;

    private String jwxtDeptId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
