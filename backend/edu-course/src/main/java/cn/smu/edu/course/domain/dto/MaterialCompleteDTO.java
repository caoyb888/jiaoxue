package cn.smu.edu.course.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MaterialCompleteDTO {

    @NotBlank
    private String uploadId;

    @NotBlank
    private String title;
}
