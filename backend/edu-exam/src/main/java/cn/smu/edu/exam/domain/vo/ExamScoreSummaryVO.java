package cn.smu.edu.exam.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 学生本次考试得分汇总 */
@Data
public class ExamScoreSummaryVO {
    private Long publishId;
    private Long studentId;
    /** 已批改题目的得分总和（NULL题目不计入） */
    private BigDecimal totalScore;
    /** 试卷满分 */
    private BigDecimal fullScore;
    private Integer totalQuestions;
    private Integer gradedQuestions;
    /** 自动批改的客观题中答对数量 */
    private Integer correctCount;
    /** 各题批改明细 */
    private List<StudentAnswerVO> answers;
}
