package cn.smu.edu.course.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClassRoomCreateDTO {

    @NotNull
    private Long courseId;

    @NotBlank
    @Size(max = 100)
    private String className;

    @NotBlank
    @Size(max = 30)
    private String classCode;

    @NotBlank
    private String semester;

    private Long deptId;
}
