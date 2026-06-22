package cn.smu.edu.ai.domain.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 生成并待入库的题目（映射 question 表的插入字段）。
 * 批量插入后 id 由 useGeneratedKeys 回填。
 */
@Data
public class GeneratedQuestion {

    private Long id;
    private Long bankId;
    /** 1-单选 2-多选 3-判断 4-填空 5-主观 */
    private Integer type;
    private String content;
    private String answer;
    private String analysis;
    private BigDecimal score;
    private Integer difficulty;
    private Long creatorId;
}
