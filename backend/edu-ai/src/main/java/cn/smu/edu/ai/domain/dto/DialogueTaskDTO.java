package cn.smu.edu.ai.domain.dto;

import cn.smu.edu.ai.domain.model.ModelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DialogueTaskDTO {

    @NotNull
    private Long lessonId;

    @NotBlank
    @Size(max = 200)
    private String topic;

    @Size(max = 500)
    private String opening;

    private int maxTurns = 5;

    private ModelType modelType = ModelType.ANALYSIS;
}
