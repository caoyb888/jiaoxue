package cn.smu.edu.ai.domain.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRequest {

    private String userPrompt;
    private String systemPrompt;

    @Builder.Default
    private ModelType modelType = ModelType.ANALYSIS;

    private Long lessonId;
    private Long userId;
}
