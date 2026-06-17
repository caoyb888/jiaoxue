package cn.smu.edu.exam.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class QuestionUpdateDTO {

    private String content;

    private String answer;

    private String analysis;

    @DecimalMin(value = "0.00", message = "分值不能为负")
    @DecimalMax(value = "999.99", message = "分值不超过999.99")
    private BigDecimal score;

    @Min(value = 1) @Max(value = 5)
    private Integer difficulty;

    private String reviewRule;

    /** 传入则全量替换该题的选项列表 */
    @Valid
    private List<QuestionOptionDTO> options;
}
