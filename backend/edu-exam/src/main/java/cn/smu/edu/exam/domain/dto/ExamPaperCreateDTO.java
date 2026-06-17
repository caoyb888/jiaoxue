package cn.smu.edu.exam.domain.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExamPaperCreateDTO {

    @NotBlank(message = "试卷标题不能为空")
    @Size(max = 200, message = "试卷标题不超过200字符")
    private String title;

    @DecimalMin(value = "0.01", message = "总分必须大于0")
    @DecimalMax(value = "9999.99", message = "总分不超过9999.99")
    private BigDecimal totalScore = new BigDecimal("100.00");

    /** 0-固定组卷 1-随机抽题 */
    private Integer isRandom = 0;

    /** A/B/C 卷型，默认A */
    @Pattern(regexp = "[ABC]", message = "卷型只能是 A/B/C")
    private String paperType = "A";

    private String description;
}
