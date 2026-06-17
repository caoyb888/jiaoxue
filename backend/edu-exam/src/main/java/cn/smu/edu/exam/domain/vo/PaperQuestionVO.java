package cn.smu.edu.exam.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 试卷中的题目（含位置/分值/卷组信息，以及题目本体） */
@Data
public class PaperQuestionVO {
    private Long id;
    private Long paperId;
    private Long questionId;
    private BigDecimal score;
    private Integer sortOrder;
    private String paperGroup;
    private String section;
    /** 题目详情（含选项） */
    private QuestionVO question;
}
