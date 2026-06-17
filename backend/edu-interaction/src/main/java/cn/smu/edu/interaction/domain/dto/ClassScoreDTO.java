package cn.smu.edu.interaction.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ClassScoreDTO {

    @NotNull
    private Long studentId;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal score;

    private String reason;
}
