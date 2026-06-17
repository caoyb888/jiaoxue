package cn.smu.edu.exam.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/** 单题批改结果 */
@Data
@AllArgsConstructor
public class GradeResultVO {
    private Long questionId;
    private Integer questionType;
    private BigDecimal score;
    /** NULL=未批改/投票 0=错误 1=正确 */
    private Integer isCorrect;
    /** 0-未批改 1-自动批改完成 2-教师已批改 */
    private Integer reviewStatus;
}
