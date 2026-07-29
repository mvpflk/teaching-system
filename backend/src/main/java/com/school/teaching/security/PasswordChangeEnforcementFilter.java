package com.school.teaching.security;

import com.school.teaching.entity.User;
import com.school.teaching.mapper.UserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

public class PasswordChangeEnforcementFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PasswordChangeEnforcementFilter.class);
    private static final Set<String> ALLOWED_PREFIXES = Set.of(
        "/api/auth/actions/logout",
        "/api/auth/actions/info",
        "/api/profile/actions/password",
        "/api/doc.html",
        "/api/swagger",
        "/api/v3",
        "/api/webjars",
        "/api/access",
        "/api/uploads"
    );
    private static final Set<String> ALLOWED_GET_PREFIXES = Set.of(
        "/api/settings/grades",
        "/api/settings/subjects",
        "/api/settings/features",
        "/api/dictionary",
        "/api/knowledge-node",
        "/api/credit/actions/ranking",
        "/api/credit/actions/moral-ranking",
        "/api/credit/shop",
        "/api/credit/titles",
        "/api/bbs/categories",
        "/api/showcase",
        "/api/class/",
        "/api/class/list"
    );

    private final UserMapper userMapper;
    public PasswordChangeEnforcementFilter(UserMapper userMapper) { this.userMapper = userMapper; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            filterChain.doFilter(request, response);
            return;
        }

        Long userId = ((CustomUserDetails) auth.getPrincipal()).getUserId();
        if (userId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        for (String prefix : ALLOWED_PREFIXES) {
            if (path.startsWith(prefix)) { filterChain.doFilter(request, response); return; }
        }
        if ("GET".equalsIgnoreCase(method)) {
            for (String prefix : ALLOWED_GET_PREFIXES) {
                if (path.startsWith(prefix)) { filterChain.doFilter(request, response); return; }
            }
        }

        try {
            User user = userMapper.selectById(userId);
            if (user != null && user.getMustChangePassword() != null && user.getMustChangePassword() == 1) {
                response.setStatus(403);
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write(
                    "{\"code\":403,\"message\":\"请先修改默认密码后再使用系统\",\"data\":{\"mustChangePassword\":true},\"timestamp\":" +
                    System.currentTimeMillis() + "}");
                return;
            }
        } catch (Exception e) {
            log.warn("Password enforcement check failed for userId={}", userId, e);
        }

        filterChain.doFilter(request, response);
    }
}
