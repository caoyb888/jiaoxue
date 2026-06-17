package cn.smu.edu.exam.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuestionVO {
    private Long id;
    private Long bankId;
    /** 1-单选 2-多选 3-判断 4-填空 5-主观 6-投票 */
    private Integer type;
    private String content;
    private String answer;
    private String analysis;
    private BigDecimal score;
    private Integer difficulty;
    private String reviewRule;
    private Long creatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** 选项列表（单选/多选/判断/投票题有值） */
    private List<QuestionOptionVO> options;
}
