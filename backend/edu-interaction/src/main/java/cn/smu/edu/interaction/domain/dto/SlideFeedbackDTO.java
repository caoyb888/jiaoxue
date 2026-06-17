package cn.smu.edu.interaction.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SlideFeedbackDTO {

    @Min(1)
    private Integer slidePage;

    @NotBlank
    private String keyword;

    /** 1-疑问 2-关键词标注 3-重点标记 */
    private Integer feedbackType = 1;
}
