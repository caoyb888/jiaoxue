package cn.smu.edu.interaction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("random_call")
public class RandomCall {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lessonId;

    private Long teacherId;

    /** 被点到的学生 ID 列表（JSON 数组） */
    private String studentIds;

    /** random/spotlight/racing */
    private String style;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime calledAt;
}
