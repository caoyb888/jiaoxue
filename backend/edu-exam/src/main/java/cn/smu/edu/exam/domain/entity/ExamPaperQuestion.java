package cn.smu.edu.exam.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("exam_paper_question")
public class ExamPaperQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long paperId;

    private Long questionId;

    /** 该题在本试卷中的分值（覆盖题目默认分值） */
    private BigDecimal score;

    private Integer sortOrder;

    /** A/B/C 卷组（随机组卷时有效） */
    private String paperGroup;

    /** 大题/章节名称，如"一、单选题" */
    private String section;
}
