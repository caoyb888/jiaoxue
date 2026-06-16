package cn.smu.edu.common.util;

/**
 * 从请求上下文（Header 或 ThreadLocal）中获取当前用户信息
 * 网关解析 JWT 后将 userId/username/deptId/roles 写入请求头，各服务读取
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<Long> DEPT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLES = new ThreadLocal<>();

    public static void setUserId(Long userId) { USER_ID.set(userId); }
    public static Long getUserId() { return USER_ID.get(); }

    public static void setUsername(String username) { USERNAME.set(username); }
    public static String getUsername() { return USERNAME.get(); }

    public static void setDeptId(Long deptId) { DEPT_ID.set(deptId); }
    public static Long getDeptId() { return DEPT_ID.get(); }

    public static void setRoles(String roles) { ROLES.set(roles); }
    public static String getRoles() { return ROLES.get(); }

    public static void clear() {
        USER_ID.remove();
        USERNAME.remove();
        DEPT_ID.remove();
        ROLES.remove();
    }
}
