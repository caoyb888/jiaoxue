package cn.smu.edu.auth.service.impl;

import lombok.Data;
import java.util.List;

@Data
public class UserInfo {
    private Long id;
    private String username;
    private String realName;
    private Long deptId;
    private Integer status;    // 1=启用 0=禁用，来自 UserVO
    private List<String> roles;

    public boolean isEnabled() {
        return Integer.valueOf(1).equals(status);
    }
}
