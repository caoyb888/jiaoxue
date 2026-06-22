package cn.smu.edu.ai.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 一键出题生成结果预览项（S6-13），映射 question 表读取列。
 */
@Data
public class GeneratedQuestionVO {

    private Long id;
    /** 1-单选 2-多选 3-判断 4-填空 5-主观 */
    private Integer type;
    private String content;
    private String answer;
    private String analysis;
    private BigDecimal score;
    private Integer difficulty;
}
