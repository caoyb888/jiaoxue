package cn.smu.edu.grade.domain.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GradeRuleCreateDTO {

    @NotNull
    private Long classId;

    @NotBlank
    private String ruleName;

    @NotNull
    @Min(1) @Max(6)
    private Integer gradeType;

    @NotNull
    @DecimalMin("0.01") @DecimalMax("100.00")
    private BigDecimal weight;

    private String description;
}
