package cn.smu.edu.interaction.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RollCallVO {

    private Long lessonId;

    private List<Long> studentIds;

    /** random/spotlight/racing */
    private String style;

    private String message;
}
