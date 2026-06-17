package cn.smu.edu.exam.domain.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddQuestionDTO {

    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    @NotNull(message = "分值不能为空")
    @DecimalMin(value = "0.00", message = "分值不能为负")
    @DecimalMax(value = "999.99", message = "分值不超过999.99")
    private BigDecimal score;

    /** 为 null 时自动追加到末尾 */
    private Integer sortOrder;

    /** 大题名称，如"一、单选题" */
    private String section;

    /** A/B/C 卷组，默认A */
    @Pattern(regexp = "[ABC]", message = "卷组只能是 A/B/C")
    private String paperGroup = "A";
}
