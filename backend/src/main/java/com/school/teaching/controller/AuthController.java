package com.school.teaching.controller;

import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.annotation.AuditLog;
import com.school.teaching.common.R;
import com.school.teaching.dto.LoginDTO;
import com.school.teaching.entity.User;
import com.school.teaching.security.LoginRateLimiter;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.TeacherService;
import com.school.teaching.service.UserService;
import com.school.teaching.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "认证管理", description = "用户登录、登出、获取当前用户信息")
public class AuthController {

    @Autowired private UserService userService;
    @Autowired private TeacherService teacherService;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private LoginRateLimiter rateLimiter;

    @AuditLog(eventType = AuditEventType.USER_LOGIN, description = "用户登录")
    @PostMapping("/actions/login")
    @Operation(summary = "用户登录", description = "用户名密码登录，返回JWT Token和用户信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登录成功"),
        @ApiResponse(responseCode = "401", description = "用户名或密码错误"),
        @ApiResponse(responseCode = "403", description = "账号已被禁用"),
        @ApiResponse(responseCode = "429", description = "登录尝试过于频繁")
    })
    public ResponseEntity<R<Map<String, Object>>> login(@Valid @RequestBody LoginDTO loginDTO,
                                                         HttpServletRequest request) {
        String ip = getClientIp(request);

        if (rateLimiter.isBlocked(ip, loginDTO.getUsername())) {
            return ResponseEntity.status(429).body(R.error(429, "登录尝试过于频繁，请稍后再试"));
        }

        User user = userService.login(loginDTO.getUsername(), loginDTO.getPassword());

        if (user == null) {
            rateLimiter.recordAttempt(ip, loginDTO.getUsername(), false);
            return ResponseEntity.status(401).body(R.error(401, "用户名或密码错误"));
        }

        rateLimiter.recordAttempt(ip, loginDTO.getUsername(), true);

        if (user.getStatus() != null && user.getStatus() != 1) {
            return ResponseEntity.status(403).body(R.error(403, "账号已被禁用"));
        }

        String roleName = user.getRoleName() != null ? user.getRoleName() : "STUDENT";
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), roleName);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("role", roleName);
        data.put("mustChangePassword", user.getMustChangePassword() != null && user.getMustChangePassword() == 1);

        if (isTeacherRole(roleName)) {
            data.put("teacherSummary", teacherService.getTeacherSummary(user.getId()));
        }

        return ResponseEntity.ok(R.ok(data, "登录成功"));
    }

    @GetMapping("/actions/info")
    @Operation(summary = "获取当前用户信息", description = "根据Token获取当前登录用户的基本信息和角色")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "账号已被禁用")
    })
    public R<Map<String, Object>> info() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");

        User user = userService.getUserById(userId);
        if (user == null) return R.notFound("用户不存在");
        if (user.getStatus() != null && user.getStatus() != 1) {
            return R.error(403, "账号已被禁用");
        }

        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("realName", user.getRealName());
        info.put("role", user.getRoleName() != null ? user.getRoleName() : getRoleName(user.getRoleId(), user.getId()));
        info.put("avatar", user.getAvatarUrl());
        info.put("email", user.getEmail());

        String roleName = user.getRoleName() != null ? user.getRoleName() : getRoleName(user.getRoleId(), user.getId());
        if (isTeacherRole(roleName)) {
            info.put("teacherSummary", teacherService.getTeacherSummary(user.getId()));
        }

        if (SecurityUtils.isStudent()) {
            info.putAll(userService.getStudentClassInfo(userId));
        }

        return R.ok(info);
    }

    @PostMapping("/actions/logout")
    @AuditLog(eventType = AuditEventType.USER_LOGOUT, description = "用户登出")
    @Operation(summary = "用户登出", description = "将当前Token加入黑名单")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "退出成功")
    })
    public R<String> logout(HttpServletRequest request) {
        String token = (String) request.getAttribute("JWT_TOKEN");
        if (token != null) {
            jwtUtils.blacklist(token);
        }
        return R.ok("退出成功");
    }

    /**
     * 位掩码角色: SUPER_ADMIN=8(bit3), ADMIN=1(bit0), INSPECTOR=16(bit4), TEACHER=2(bit1), STUDENT=4(bit2)
     * HEAD_TEACHER 非独立位，是 TEACHER(bit1) + classes.headTeacherId 匹配
     */
    private static final java.util.Set<String> TEACHER_ROLES = java.util.Set.of("TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN");
    private boolean isTeacherRole(String role) { return TEACHER_ROLES.contains(role); }

    private String getRoleName(Long roleId, Long userId) {
        if (roleId == null) return "STUDENT";
        long v = roleId.longValue();
        if ((v & 8) != 0) return "SUPER_ADMIN";
        if ((v & 64) != 0) return "REGION_ADMIN";
        if ((v & 1) != 0) return "ADMIN";
        if ((v & 16) != 0) return "INSPECTOR";
        if ((v & 2) != 0) {
            return teacherService.isHeadTeacher(userId) ? "HEAD_TEACHER" : "TEACHER";
        }
        if ((v & 32) != 0) return "PARENT";
        return "STUDENT";
    }

    /** 外部系统统一认证登录（预留，当前返回501） */
    @PostMapping("/actions/external-login")
    @AuditLog(eventType = AuditEventType.USER_LOGIN, description = "外部用户登录")
    @Operation(summary = "外部系统登录", description = "预留接口，当前未开放")
    @ApiResponses({
        @ApiResponse(responseCode = "501", description = "外部登录暂未开放")
    })
    public R<Map<String, Object>> externalLogin(@RequestBody Map<String, Object> body) {
        return R.error(501, "外部登录暂未开放");
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) return realIp;
        return request.getRemoteAddr();
    }
}
