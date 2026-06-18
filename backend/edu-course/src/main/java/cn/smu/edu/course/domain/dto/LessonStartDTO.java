package cn.smu.edu.course.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LessonStartDTO {

    @NotNull
    private Long classId;

    private Long materialId;
    private String title;
    private String chapter;
    private String liveMode = "SLIDE_ONLY";
}
