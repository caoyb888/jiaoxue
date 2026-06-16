package cn.smu.edu.user.domain.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserUpdateDTO {

    @Size(max = 50)
    private String realName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private String email;

    private String avatarUrl;

    @Min(0) @Max(2)
    private Integer status;
}
