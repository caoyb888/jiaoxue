package cn.smu.edu.course.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class MaterialUploadDTO {

    @NotBlank
    private String fileName;

    @NotBlank
    @Pattern(regexp = "pptx|pdf|docx|mp4", message = "fileType 只支持 pptx/pdf/docx/mp4")
    private String fileType;

    @NotNull
    @Positive
    private Integer fileSizeKb;
}
