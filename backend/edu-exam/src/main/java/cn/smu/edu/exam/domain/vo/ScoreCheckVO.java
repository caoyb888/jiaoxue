package cn.smu.edu.exam.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ScoreCheckVO {
    private BigDecimal totalScore;
    private BigDecimal actualScore;
    private Integer questionCount;
    /** true = 实际分值之和等于设定总分 */
    private Boolean matched;
    private BigDecimal diff;
}
