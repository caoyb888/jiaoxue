package cn.smu.edu.user.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVO {
    private Long id;
    private String studentNo;
    private String username;
    private String realName;
    private String email;
    private Integer userType;
    private Long deptId;
    private String deptName;
    private String avatarUrl;
    private Integer status;
    private List<String> roles;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
