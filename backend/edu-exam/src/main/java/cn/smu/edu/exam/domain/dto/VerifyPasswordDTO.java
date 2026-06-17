package cn.smu.edu.exam.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyPasswordDTO {

    @NotBlank(message = "密码不能为空")
    private String password;
}
