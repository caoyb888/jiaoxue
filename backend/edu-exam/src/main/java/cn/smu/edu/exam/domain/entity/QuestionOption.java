package cn.smu.edu.exam.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("question_option")
public class QuestionOption {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long questionId;

    /** A/B/C/D/E */
    private String optionLabel;

    private String content;

    /** 0-错误 1-正确 */
    private Integer isCorrect;

    private Integer sortOrder;
}
