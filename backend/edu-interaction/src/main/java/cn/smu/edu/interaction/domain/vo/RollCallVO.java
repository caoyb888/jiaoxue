package cn.smu.edu.interaction.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RollCallVO {

    private Long lessonId;

    private List<Long> studentIds;

    /** 被点到学生的姓名/学号（与 studentIds 对应） */
    private List<StudentBriefVO> students;

    /** random/spotlight/racing */
    private String style;

    private String message;
}
