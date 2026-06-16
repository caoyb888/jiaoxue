package cn.smu.edu.auth.service.impl;

import lombok.Data;
import java.util.List;

@Data
public class UserInfo {
    private Long id;
    private String username;
    private Long deptId;
    private boolean enabled;
    private List<String> roles;
}
