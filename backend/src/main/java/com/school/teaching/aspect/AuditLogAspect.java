package com.school.teaching.aspect;

import com.school.teaching.annotation.AuditLog;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.AuditLogService;
import com.school.teaching.utils.JsonUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import java.time.LocalDateTime;

@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    @Autowired
    private AuditLogService auditLogService;
    @Autowired(required = false)
    private HttpServletRequest request;

    @AfterReturning(pointcut = "@annotation(auditLog)", returning = "result")
    public void afterReturning(JoinPoint joinPoint, AuditLog auditLog, Object result) {
        try {
            Long userId = null;
            String username = null;
            String role = null;

            // 登录事件：从 LoginDTO 提取用户名（尚无 SecurityContext）
            boolean isLoginEvent = auditLog.eventType() == com.school.teaching.annotation.AuditEventType.USER_LOGIN;
            if (isLoginEvent) {
                for (Object arg : joinPoint.getArgs()) {
                    if (arg instanceof com.school.teaching.dto.LoginDTO dto) {
                        username = dto.getUsername();
                        break;
                    }
                }
            } else {
                var userDetails = SecurityUtils.getCurrentUser();
                if (userDetails != null) {
                    userId = userDetails.getUserId();
                    role = userDetails.getRole();
                    username = userDetails.getUsername();
                }
            }

            // 判断登录是否真正成功（检查 ResponseEntity 状态码），成功时提取 role/userId
            String status = "SUCCESS";
            if (isLoginEvent && result instanceof ResponseEntity<?> re) {
                boolean success = re.getStatusCode().is2xxSuccessful();
                status = success ? "SUCCESS" : "FAILURE";
                if (success) {
                    Object body = re.getBody();
                    if (body instanceof com.school.teaching.common.R<?> r) {
                        Object data = r.getData();
                        if (data instanceof java.util.Map<?, ?> map) {
                            role = (String) map.get("role");
                            Object uid = map.get("userId");
                            if (uid != null) userId = uid instanceof Number ? ((Number) uid).longValue() : Long.valueOf(uid.toString());
                        }
                    }
                }
            }

            com.school.teaching.entity.AuditLog log = new com.school.teaching.entity.AuditLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setRole(role);
            log.setEventType(auditLog.eventType().name());
            log.setDescription(auditLog.description().isEmpty()
                ? auditLog.eventType().getLabel() : auditLog.description());
            log.setTargetTable(auditLog.targetTable().isEmpty() ? null : auditLog.targetTable());
            log.setOperation(getOperation(joinPoint));
            log.setStatus(status);
            log.setCreatedTime(LocalDateTime.now());

            if (request != null) {
                log.setIpAddress(getClientIp(request));
                log.setRequestUrl(request.getRequestURI());
                log.setMethod(request.getMethod());
            }

            log.setParams(extractParams(joinPoint));

            auditLogService.asyncSave(log);

        } catch (Exception ignored) {
            log.error("审计日志记录失败", ignored);
        }
    }

    /** 方法执行异常时记录失败日志 */
    @AfterThrowing(pointcut = "@annotation(auditLog)", throwing = "e")
    public void afterThrowing(JoinPoint joinPoint, AuditLog auditLog, Throwable e) {
        try {
            Long userId = null;
            String username = null;
            String role = null;

            var userDetails = SecurityUtils.getCurrentUser();
            if (userDetails != null) {
                userId = userDetails.getUserId();
                role = userDetails.getRole();
                username = userDetails.getUsername();
            }

            com.school.teaching.entity.AuditLog log = new com.school.teaching.entity.AuditLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setRole(role);
            log.setEventType(auditLog.eventType().name());
            log.setDescription(auditLog.description().isEmpty()
                ? auditLog.eventType().getLabel() : auditLog.description());
            log.setTargetTable(auditLog.targetTable().isEmpty() ? null : auditLog.targetTable());
            log.setOperation(getOperation(joinPoint));
            log.setStatus("FAILURE");
            String errMsg = e.getClass().getSimpleName() + ": " + (e.getMessage() != null ? e.getMessage() : "");
            log.setErrorMessage(errMsg.length() > 500 ? errMsg.substring(0, 500) : errMsg);
            log.setCreatedTime(LocalDateTime.now());

            if (request != null) {
                log.setIpAddress(getClientIp(request));
                log.setRequestUrl(request.getRequestURI());
                log.setMethod(request.getMethod());
            }

            log.setParams(extractParams(joinPoint));
            auditLogService.asyncSave(log);

        } catch (Exception ignored) {
            log.error("审计失败日志记录异常", ignored);
        }
    }

    /** 从反向代理 header 提取真实客户端 IP */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri.trim();
        return request.getRemoteAddr();
    }

    private String getOperation(JoinPoint joinPoint) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        return sig.getDeclaringType().getSimpleName() + "." + sig.getName();
    }

    private String extractParams(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) return null;
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof HttpServletRequest) continue;
                if (sb.length() > 1) sb.append(", ");
                String s;
                try {
                    s = JsonUtils.toJson(args[i]);
                } catch (Exception e) {
                    s = args[i] != null ? args[i].toString() : "null";
                }
                if (s != null && s.length() > 200) s = s.substring(0, 200) + "...";
                sb.append(s != null ? s : "null");
            }
            sb.append("]");
            return sb.length() > 500 ? sb.substring(0, 500) : sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
