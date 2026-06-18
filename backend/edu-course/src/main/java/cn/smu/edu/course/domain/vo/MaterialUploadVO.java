package cn.smu.edu.course.domain.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialUploadVO {

    private String uploadId;
    private String presignedUrl;
    private int expiresIn;
    private String objectPath;
}
