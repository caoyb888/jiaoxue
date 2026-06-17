package cn.smu.edu.interaction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("class_score")
public class ClassScore {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lessonId;

    private Long studentId;

    private Long classId;

    private BigDecimal score;

    private String reason;

    private Long operatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
