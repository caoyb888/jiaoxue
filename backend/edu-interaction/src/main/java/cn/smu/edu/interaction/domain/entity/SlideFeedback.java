package cn.smu.edu.interaction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("slide_feedback")
public class SlideFeedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lessonId;

    private Long studentId;

    private Integer slidePage;

    private String keyword;

    /** 1-疑问 2-关键词标注 3-重点标记 */
    private Integer feedbackType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
