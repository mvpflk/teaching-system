package com.school.teaching.aspect;

import com.school.teaching.common.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class LogAspect {

    private static final int MAX_ARG_LENGTH = 200;

    @Around("@annotation(com.school.teaching.aspect.Log)")
    public Object aroundLogAnnotation(ProceedingJoinPoint joinPoint) throws Throwable {
        return logAround(joinPoint, null);
    }

    @Around("execution(public * com.school.teaching.controller..*.*(..))")
    public Object aroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        return logAround(joinPoint, null);
    }

    private Object logAround(ProceedingJoinPoint joinPoint, String customDesc) throws Throwable {
        long start = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String method = signature.getDeclaringType().getSimpleName() + "." + signature.getName();

        String args = formatArgs(signature.getParameterNames(), joinPoint.getArgs());
        String uri = getRequestUri();

        log.info("→ {} | {} {} | args: {}", method, getMethod(), uri, args);

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;

            String status = "OK";
            if (result instanceof R<?> r) {
                status = r.getCode() != null ? r.getCode().toString() : "OK";
            }

            if (elapsed > 1000) {
                log.warn("← {} | {}ms | status={} [SLOW]", method, elapsed, status);
            } else {
                log.info("← {} | {}ms | status={}", method, elapsed, status);
            }
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("← {} | {}ms | ERROR: {}", method, elapsed, e.getMessage());
            throw e;
        }
    }

    private String formatArgs(String[] names, Object[] args) {
        if (args == null || args.length == 0) return "[]";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            if (names != null && i < names.length) {
                sb.append(names[i]).append("=");
            }
            if (args[i] instanceof HttpServletRequest) {
                sb.append("[request]");
            } else {
                String s = String.valueOf(args[i]);
                if (s.length() > MAX_ARG_LENGTH) {
                    s = s.substring(0, MAX_ARG_LENGTH) + "...";
                }
                sb.append(s);
            }
        }
        return sb.toString();
    }

    private String getRequestUri() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                return req.getMethod() + " " + req.getRequestURI();
            }
        } catch (Exception ignored) { log.warn("Failed to get request URI", ignored); }
        return "-";
    }

    private String getMethod() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest().getMethod();
            }
        } catch (Exception ignored) { log.warn("Failed to get request method", ignored); }
        return "-";
    }
}
