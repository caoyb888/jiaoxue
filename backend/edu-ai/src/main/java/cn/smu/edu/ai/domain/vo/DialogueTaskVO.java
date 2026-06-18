package cn.smu.edu.ai.domain.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DialogueTaskVO {

    private String sessionId;
    private String topic;
    private String opening;
    private int maxTurns;
}
