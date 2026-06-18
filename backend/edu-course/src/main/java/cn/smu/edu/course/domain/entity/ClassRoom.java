package cn.smu.edu.course.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("class_room")
public class ClassRoom {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;
    private Long teacherId;
    private String className;
    private String classCode;
    private String semester;
    private Integer studentCount;
    private Long deptId;
    private Integer status;
    private String jwxtClassId;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
