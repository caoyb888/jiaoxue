package cn.smu.edu.course.domain.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseCreateDTO {

    @NotBlank
    @Size(max = 30)
    private String courseCode;

    @NotBlank
    @Size(max = 100)
    private String courseName;

    @NotNull
    private Long deptId;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal credit;

    private Integer courseType = 1;

    @NotBlank
    private String semester;

    private String description;
}
