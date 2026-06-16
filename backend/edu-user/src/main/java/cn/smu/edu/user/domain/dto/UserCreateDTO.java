package cn.smu.edu.user.domain.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserCreateDTO {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Size(max = 50)
    private String realName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotNull
    @Min(1) @Max(4)
    private Integer userType;

    @NotNull
    private Long deptId;
}
