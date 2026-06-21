package cn.smu.edu.user.domain.enums;

import lombok.Getter;

/**
 * 系统角色定义。
 *
 * <p>本系统无 sys_role 表，角色为固定枚举；{@code roleCode} 与
 * {@code user_role.role_code} 一致（不含 Spring Security 的 ROLE_ 前缀）。
 */
@Getter
public enum RoleEnum {

    SUPER_ADMIN(1L, "SUPER_ADMIN", "超级管理员"),
    DEPT_ADMIN(2L, "DEPT_ADMIN", "院系管理员"),
    TEACHER(3L, "TEACHER", "教师"),
    STUDENT(4L, "STUDENT", "学生");

    private final Long id;
    private final String roleCode;
    private final String roleName;

    RoleEnum(Long id, String roleCode, String roleName) {
        this.id = id;
        this.roleCode = roleCode;
        this.roleName = roleName;
    }
}
