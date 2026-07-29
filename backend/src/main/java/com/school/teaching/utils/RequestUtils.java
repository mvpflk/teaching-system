package com.school.teaching.utils;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestUtils {

    /** 从请求中提取客户端真实 IP，支持反向代理头 */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) return realIp;
        return request.getRemoteAddr();
    }

    private RequestUtils() {}
}
