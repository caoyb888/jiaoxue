package cn.smu.edu.user.domain.dto;

import lombok.Data;

@Data
public class UserQueryDTO {
    private String keyword;
    private Integer userType;
    private Long deptId;
    private Integer status;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
