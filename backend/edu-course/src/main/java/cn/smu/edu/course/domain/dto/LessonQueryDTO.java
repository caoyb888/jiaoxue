package cn.smu.edu.course.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LessonQueryDTO {

    @NotNull
    private Long classId;

    private Integer status;
    private int page = 1;
    private int size = 20;
}
