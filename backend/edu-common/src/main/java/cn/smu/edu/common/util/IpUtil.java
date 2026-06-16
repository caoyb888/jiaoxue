package cn.smu.edu.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public class IpUtil {

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    private IpUtil() {}
}
