package cn.smu.edu.course.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("class_group_student")
public class ClassGroupStudent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;
    private Long studentId;
    private Long classId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime joinedAt;
}
