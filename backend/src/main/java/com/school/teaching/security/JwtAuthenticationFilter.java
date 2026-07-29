package com.school.teaching.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.common.R;
import com.school.teaching.sse.SseTicketStore;
import com.school.teaching.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final SseTicketStore ticketStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationFilter(JwtUtils jwtUtils, SseTicketStore ticketStore) {
        this.jwtUtils = jwtUtils;
        this.ticketStore = ticketStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            // EventSource 不支持自定义 Header，从 URL 查询参数获取
            token = request.getParameter("token");
        }
        if (token == null || token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 尝试 SSE ticket 认证（短期一次性令牌）
        SseTicketStore.TicketEntry ticketEntry = ticketStore.validateAndConsume(token);
        if (ticketEntry != null) {
            // ticket 有效 — 使用ticket中存储的真实角色
            String ticketRole = ticketEntry.role() != null ? ticketEntry.role() : "STUDENT";
            CustomUserDetails details = new CustomUserDetails(
                ticketEntry.userId(), "sse-ticket", ticketRole, null, null, null, null);
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authToken);
            filterChain.doFilter(request, response);
            return;
        }

        // 非 ticket — 按原有 JWT 逻辑处理
        if (jwtUtils.isBlacklisted(token)) {
            writeError(response, 401, "Token已失效，请重新登录");
            return;
        }

        Long userId;
        String role;
        String username;
        String jti;
        Long schoolId = null;
        Long stageId = null;
        try {
            var claims = jwtUtils.parseToken(token);
            userId = claims.get("userId", Long.class);
            role = claims.get("role", String.class);
            username = claims.getSubject();
            jti = claims.get("jti", String.class);
            schoolId = claims.get("schoolId", Long.class);
            stageId = claims.get("stageId", Long.class);
        } catch (Exception e) {
            writeError(response, 401, "Token无效或已过期");
            return;
        }

        if (userId == null || role == null) {
            writeError(response, 401, "Token无效");
            return;
        }

        CustomUserDetails userDetails = new CustomUserDetails(userId, username, role, null, jti, schoolId, stageId);
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        request.setAttribute("JWT_TOKEN", token);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // context cleanup handled by SecurityContextHolderFilter
        }
    }

    private void writeError(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json;charset=utf-8");
        R<Void> body = new R<>(code, message, null, System.currentTimeMillis());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
