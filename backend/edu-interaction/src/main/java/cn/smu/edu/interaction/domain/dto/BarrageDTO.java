package cn.smu.edu.interaction.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BarrageDTO {

    @NotBlank
    @Size(max = 500)
    private String content;

    /** roll/top/bottom，默认 roll */
    private String style = "roll";
}
