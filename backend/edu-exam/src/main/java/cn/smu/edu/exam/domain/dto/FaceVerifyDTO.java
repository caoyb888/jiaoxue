package cn.smu.edu.exam.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 人脸核验请求 DTO。
 * C6合规：livePhotoBase64 只在内存中处理，调用比对接口后立即丢弃，永远不持久化。
 */
@Data
public class FaceVerifyDTO {

    @NotBlank(message = "人脸照片不能为空")
    private String livePhotoBase64;
}
