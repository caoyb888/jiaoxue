package cn.smu.edu.ai.domain.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MindmapVO {

    private Long lessonId;
    /** PENDING / GENERATING / DONE / FAILED */
    private String genStatus;
    private JsonNode markmapJson;
    private Boolean studentVisible;
}
