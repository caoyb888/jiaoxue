package cn.smu.edu.exam.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_paper")
public class ExamPaper {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long creatorId;

    private String title;

    private BigDecimal totalScore;

    /** 0-固定组卷 1-随机抽题 */
    private Integer isRandom;

    /** A/B/C 卷型 */
    private String paperType;

    private String description;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
