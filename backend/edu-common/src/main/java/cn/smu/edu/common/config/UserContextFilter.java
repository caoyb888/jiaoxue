package cn.smu.edu.common.config;

import cn.smu.edu.common.util.UserContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * 从网关透传的请求头中提取用户信息，注入 UserContext
 */
@Component
@Order(1)
public class UserContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        try {
            String userId = request.getHeader("X-User-Id");
            String username = request.getHeader("X-Username");
            String deptId = request.getHeader("X-Dept-Id");
            String roles = request.getHeader("X-Roles");

            if (StringUtils.hasText(userId)) {
                UserContext.setUserId(Long.parseLong(userId));
            }
            if (StringUtils.hasText(username)) {
                UserContext.setUsername(username);
            }
            if (StringUtils.hasText(deptId)) {
                UserContext.setDeptId(Long.parseLong(deptId));
            }
            if (StringUtils.hasText(roles)) {
                UserContext.setRoles(roles);
            }
            chain.doFilter(req, res);
        } finally {
            UserContext.clear();
        }
    }
}
