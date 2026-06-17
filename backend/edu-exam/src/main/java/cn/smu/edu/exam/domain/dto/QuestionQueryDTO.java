package cn.smu.edu.exam.domain.dto;

import lombok.Data;

@Data
public class QuestionQueryDTO {

    /** 按题库过滤（必填） */
    private Long bankId;

    /** 1-单选 2-多选 3-判断 4-填空 5-主观 6-投票 */
    private Integer type;

    /** 1-极易 2-易 3-中 4-难 5-极难 */
    private Integer difficulty;

    /** 全文检索关键词（对应 FULLTEXT INDEX ft_content） */
    private String keyword;

    private int page = 1;
    private int size = 20;
}
