package cn.smu.edu.exam.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class QuestionCreateDTO {

    @NotNull(message = "题库ID不能为空")
    private Long bankId;

    /** 1-单选 2-多选 3-判断 4-填空 5-主观 6-投票 */
    @NotNull(message = "题型不能为空")
    @Min(value = 1, message = "题型值范围1-6")
    @Max(value = 6, message = "题型值范围1-6")
    private Integer type;

    @NotBlank(message = "题干内容不能为空")
    private String content;

    /** 客观题：正确选项标签如 "A,C"；主观题：参考答案 */
    private String answer;

    private String analysis;

    @DecimalMin(value = "0.00", message = "分值不能为负")
    @DecimalMax(value = "999.99", message = "分值不超过999.99")
    private BigDecimal score = BigDecimal.ZERO;

    /** 1-极易 2-易 3-中 4-难 5-极难 */
    @Min(value = 1) @Max(value = 5)
    private Integer difficulty = 3;

    /** 主观题AI批改规则提示词（将经过 PromptSecurityFilter） */
    private String reviewRule;

    /** 客观题选项（单选/多选/判断/投票必填） */
    @Valid
    private List<QuestionOptionDTO> options;
}
