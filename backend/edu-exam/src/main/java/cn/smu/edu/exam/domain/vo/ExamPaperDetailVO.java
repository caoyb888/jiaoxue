package cn.smu.edu.exam.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExamPaperDetailVO extends ExamPaperVO {
    private List<PaperQuestionVO> questions;
    private Integer totalQuestions;
    /** 各题分值之和（用于前端提示是否与 totalScore 匹配） */
    private BigDecimal actualScore;
}
