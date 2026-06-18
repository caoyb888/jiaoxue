package cn.smu.edu.course.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("class_group")
public class ClassGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long classId;
    private Long lessonId;
    private String groupName;
    private Integer groupNo;
    private String groupType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
