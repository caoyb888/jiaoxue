package cn.smu.edu.interaction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("barrage")
public class Barrage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lessonId;

    /** 后台实名（不向前台暴露） */
    private Long studentId;

    private String content;

    /** roll/top/bottom */
    private String style;

    /** 0-正常 1-屏蔽 */
    private Integer isBlocked;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
