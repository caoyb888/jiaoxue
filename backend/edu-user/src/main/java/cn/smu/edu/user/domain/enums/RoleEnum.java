package cn.smu.edu.user.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 系统角色定义。
 *
 * <p>本系统无 sys_role 表，角色为固定枚举；{@code roleCode} 与
 * {@code user_role.role_code} 及 JWT roles 声明一致——均带 Spring Security 的
 * {@code ROLE_} 前缀（实际数据/令牌使用 ROLE_ADMIN / ROLE_TEACHER / ROLE_STUDENT）。
 *
 * <p>{@code id} 仅供前端角色多选回显/分配用（与 /v1/roles 返回的 id 一致），
 * 无外部持久化依赖。
 */
@Getter
public enum RoleEnum {

    ADMIN(1L, "ROLE_ADMIN", "系统管理员"),
    TEACHER(2L, "ROLE_TEACHER", "教师"),
    STUDENT(3L, "ROLE_STUDENT", "学生");

    private final Long id;
    private final String roleCode;
    private final String roleName;

    RoleEnum(Long id, String roleCode, String roleName) {
        this.id = id;
        this.roleCode = roleCode;
        this.roleName = roleName;
    }

    /** 按 id 解析角色，未知 id 返回 null。 */
    public static RoleEnum fromId(Long id) {
        return Arrays.stream(values()).filter(r -> r.id.equals(id)).findFirst().orElse(null);
    }
}
