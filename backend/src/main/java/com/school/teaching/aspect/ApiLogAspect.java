package com.school.teaching.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Aspect
@Component
@ConditionalOnProperty(name = "api-log.enabled", havingValue = "true", matchIfMissing = true)
public class ApiLogAspect {

    private static final Logger log = LoggerFactory.getLogger(ApiLogAspect.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> SENSITIVE_KEYS = Set.of("password", "oldPassword", "newPassword", "secret", "token", "authorization");

    @Autowired(required = false)
    private HttpServletRequest request;

    @Autowired(required = false)
    private HttpServletResponse response;

    @Value("#{'${api-log.exclude-paths:/api/health,/api/doc.html,/api/v3/api-docs,/actuator/health,/swagger-resources}'.split(',')}")
    private List<String> excludePaths;

    @Value("${api-log.response-max-length:500}")
    private int responseMaxLength;

    @Around("execution(* com.school.teaching.controller..*.*(..))")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        // 非 Web 请求（如 @Scheduled 定时任务）直接放行
        if (org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() == null) {
            return joinPoint.proceed();
        }
        // 跳过排除路径
        if (request != null && isExcluded(request.getRequestURI())) {
            return joinPoint.proceed();
        }

        long start = System.nanoTime();
        String url = buildUrl();
        String httpMethod = request != null ? request.getMethod() : "UNKNOWN";
        String ip = RequestUtils.getClientIp(request);
        Long userId = SecurityUtils.getCurrentUserId();

        try {
            Object result = joinPoint.proceed();
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            doLog(url, httpMethod, ip, userId, joinPoint, result, null, durationMs);
            return result;
        } catch (Throwable t) {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            doLog(url, httpMethod, ip, userId, joinPoint, null, t, durationMs);
            throw t;
        }
    }

    private void doLog(String url, String method, String ip, Long userId,
                       ProceedingJoinPoint joinPoint, Object result, Throwable error, long durationMs) {
        try {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("url", url);
            msg.put("method", method);
            msg.put("ip", ip);
            msg.put("userId", userId != null ? userId : "ANONYMOUS");

            // 请求参数
            Map<String, Object> params = extractParams(joinPoint);
            if (!params.isEmpty()) {
                maskSensitive(params);
                msg.put("params", params);
            }

            // 响应状态码和响应体
            if (error != null) {
                msg.put("status", extractErrorStatus(error));
                msg.put("response", truncate(error.getClass().getSimpleName() + ": " + error.getMessage()));
            } else {
                int status = extractStatus(result);
                msg.put("status", status);
                if (status < 400 && result != null) {
                    // 成功响应截断输出
                    String respStr = toSafeString(result);
                    if (respStr != null) {
                        msg.put("response", truncate(respStr));
                    }
                } else if (status >= 400 && result != null) {
                    msg.put("response", truncate(toSafeString(result)));
                }
            }

            msg.put("duration", durationMs);

            // 单行 JSON 输出
            log.info(MAPPER.writeValueAsString(msg));

        } catch (Exception e) {
            log.warn("API日志记录异常", e);
        }
    }

    // ── 工具方法 ──

    private String buildUrl() {
        if (request == null) return "unknown";
        String uri = request.getRequestURI();
        String qs = request.getQueryString();
        return qs != null ? uri + "?" + qs : uri;
    }

    private boolean isExcluded(String uri) {
        for (String pattern : excludePaths) {
            String p = pattern.trim();
            if (p.isEmpty()) continue;
            if (uri.startsWith(p) || uri.matches(p.replace("*", ".*"))) {
                return true;
            }
        }
        return false;
    }

    /** 提取方法参数（跳过大对象和 servlet 类型） */
    private Map<String, Object> extractParams(ProceedingJoinPoint jp) {
        Map<String, Object> result = new LinkedHashMap<>();
        Object[] args = jp.getArgs();
        if (args == null || args.length == 0) return result;
        org.aspectj.lang.reflect.MethodSignature sig =
            (org.aspectj.lang.reflect.MethodSignature) jp.getSignature();
        String[] names = sig.getParameterNames();
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) continue;
            if (args[i] instanceof HttpServletRequest) continue;
            if (args[i] instanceof HttpServletResponse) continue;
            String name = names != null && i < names.length ? names[i] : "arg" + i;
            String val = toSafeString(args[i]);
            if (val != null && val.length() > 200) val = val.substring(0, 200) + "...";
            result.put(name, val);
        }
        return result;
    }

    /** 递归掩码敏感字段 */
    @SuppressWarnings("unchecked")
    private void maskSensitive(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (SENSITIVE_KEYS.contains(entry.getKey())) {
                map.put(entry.getKey(), "******");
            } else if (entry.getValue() instanceof Map) {
                maskSensitive((Map<String, Object>) entry.getValue());
            }
        }
    }

    /** 提取 HTTP 状态码 */
    private int extractStatus(Object result) {
        if (result instanceof ResponseEntity<?> re) {
            return re.getStatusCode().value();
        }
        if (result instanceof R<?> r) {
            return r.getCode() != null ? r.getCode() : 200;
        }
        if (response != null) {
            int s = response.getStatus();
            return s > 0 ? s : 200;
        }
        return 200;
    }

    /** 提取异常对应的状态码 */
    private int extractErrorStatus(Throwable t) {
        if (t instanceof ResponseStatusException rse) {
            return rse.getStatusCode().value();
        }
        String cn = t.getClass().getSimpleName();
        if (cn.contains("NotFound") || cn.contains("Unknown")) return 404;
        if (cn.contains("BadRequest") || cn.contains("IllegalArgument") || cn.contains("Validation")) return 400;
        if (cn.contains("Forbidden") || cn.contains("AccessDenied")) return 403;
        if (cn.contains("Conflict")) return 409;
        return 500;
    }

    private String toSafeString(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof String s) return s;
            if (obj instanceof ResponseEntity<?> re) {
                Object body = re.getBody();
                return body != null ? MAPPER.writeValueAsString(body) : null;
            }
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            String s = obj.toString();
            return s.length() > 500 ? s.substring(0, 500) : s;
        }
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > responseMaxLength ? s.substring(0, responseMaxLength) + "..." : s;
    }
}
