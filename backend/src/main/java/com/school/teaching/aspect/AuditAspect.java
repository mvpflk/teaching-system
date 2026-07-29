package com.school.teaching.aspect;

import com.school.teaching.annotation.Audit;
import com.school.teaching.entity.AuditLog;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.AuditLogService;
import com.school.teaching.utils.JsonUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * Service 层 @Audit 注解切面。
 *
 * 环绕处理：方法成功 → 记录 SUCCESS；方法抛异常 → 记录 FAILURE + 异常信息。
 * 复用现有 AuditLog 实体和 AuditLogService.asyncSave() 异步写入。
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    @Autowired
    private AuditLogService auditLogService;
    @Autowired(required = false)
    private HttpServletRequest request;

    /** 环绕通知：统一处理成功和失败 */
    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        AuditLog auditLog = buildBaseLog(joinPoint, audit);

        try {
            Object result = joinPoint.proceed();

            // 成功
            auditLog.setStatus("SUCCESS");
            auditLogService.asyncSave(auditLog);
            return result;

        } catch (Throwable e) {
            // 失败 — 记录异常信息
            auditLog.setStatus("FAILURE");
            String errMsg = e.getClass().getSimpleName() + ": " + (e.getMessage() != null ? e.getMessage() : "");
            auditLog.setErrorMessage(errMsg.length() > 500 ? errMsg.substring(0, 500) : errMsg);
            try {
                auditLogService.asyncSave(auditLog);
            } catch (Exception ignored) {
                log.error("审计失败日志写入异常", ignored);
            }
            throw e; // 原样抛出，不影响业务
        }
    }

    /** 构建审计日志基础字段 */
    private AuditLog buildBaseLog(ProceedingJoinPoint joinPoint, Audit audit) {
        AuditLog log = new AuditLog();
        log.setEventType(audit.value());
        log.setDescription(audit.value());
        log.setOperation(getOperation(joinPoint));
        log.setCreatedTime(LocalDateTime.now());

        // 操作人信息
        var userDetails = SecurityUtils.getCurrentUser();
        if (userDetails != null) {
            log.setUserId(userDetails.getUserId());
            log.setUsername(userDetails.getUsername());
            log.setRole(userDetails.getRole());
        } else {
            log.setUsername("ANONYMOUS");
        }

        // 请求信息
        if (request != null) {
            log.setIpAddress(request.getRemoteAddr());
            log.setRequestUrl(request.getRequestURI());
            log.setMethod(request.getMethod());
        }

        // 方法参数
        log.setParams(extractParams(joinPoint));

        return log;
    }

    private String getOperation(ProceedingJoinPoint joinPoint) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        return sig.getDeclaringType().getSimpleName() + "." + sig.getName();
    }

    private String extractParams(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) return null;
            StringBuilder sb = new StringBuilder("[");
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
