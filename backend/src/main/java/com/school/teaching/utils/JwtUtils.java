package com.school.teaching.utils;

import com.school.teaching.entity.JwtBlacklist;
import com.school.teaching.mapper.JwtBlacklistMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey secretKey;

    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private JwtBlacklistMapper jwtBlacklistMapper;

    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    /** Redis 可用性标记 — 连续失败 3 次后短期跳过 Redis 避免超时等待 */
    private final AtomicBoolean redisAvailable = new AtomicBoolean(true);

    // [已移除] 旧代码：private final ConcurrentHashMap<String, Long> memoryBlacklist = new ConcurrentHashMap<>();
    // 内存回退已改为数据库持久化存储（jwt_blacklist 表），重启不清除

    @PostConstruct
    public void init() {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException(
                "JWT secret is not configured. Please set jwt.secret in application.yml or JWT_SECRET env variable.");
        }
        int byteLen = secret.getBytes(StandardCharsets.UTF_8).length;
        if (byteLen < 32) {
            throw new IllegalStateException(
                "JWT secret is too short: " + byteLen + " bytes, minimum 32 bytes (256 bits) required. "
                + "Generate one with: openssl rand -base64 32");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, null, null);
    }

    public String generateToken(Long userId, String username, String role, Long schoolId, Long stageId) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("jti", jti);
        if (schoolId != null) claims.put("schoolId", schoolId);
        if (stageId != null) claims.put("stageId", stageId);
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .id(jti)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    public Long getSchoolId(String token) {
        return parseToken(token).get("schoolId", Long.class);
    }

    public Long getStageId(String token) {
        return parseToken(token).get("stageId", Long.class);
    }

    public String getJti(String token) {
        return parseToken(token).get("jti", String.class);
    }

    /** 查询 token 是否已被拉黑（Redis优先，不可用时数据库回退） */
    public boolean isBlacklisted(String token) {
        try {
            String jti = getJti(token);
            if (jti == null) return false;
            // 尝试 Redis（带降级标记避免持续超时等待）
            if (redisTemplate != null && redisAvailable.get()) {
                try {
                    boolean found = Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
                    redisAvailable.set(true);
                    return found;
                } catch (Exception redisEx) {
                    redisAvailable.set(false);
                    log.warn("Redis不可用，黑名单回退到数据库", redisEx);
                }
            }
            // 数据库回退
            if (jwtBlacklistMapper != null) {
                try {
                    return jwtBlacklistMapper.existsByJti(jti) > 0;
                } catch (Exception dbEx) {
                    log.error("数据库黑名单查询失败 jti={}", jti, dbEx);
                }
            }
        } catch (Exception e) { log.warn("黑名单检查异常", e); }
        return false;
    }

    /** 将 token 拉入黑名单（Redis优先，不可用时数据库回退） */
    public void blacklist(String token) {
        try {
            Claims claims = parseToken(token);
            String jti = claims.get("jti", String.class);
            if (jti == null || jti.isEmpty()) return;
            long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (ttl <= 0) return;
            // 尝试 Redis（带降级标记）
            boolean redisOk = false;
            if (redisTemplate != null && redisAvailable.get()) {
                try {
                    redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "1", ttl, TimeUnit.MILLISECONDS);
                    redisOk = true;
                    redisAvailable.set(true);
                } catch (Exception redisEx) {
                    redisAvailable.set(false);
                    log.warn("Redis不可用，黑名单写入回退到数据库", redisEx);
                }
            }
            // 数据库回退
            if (!redisOk && jwtBlacklistMapper != null) {
                try {
                    JwtBlacklist entry = new JwtBlacklist();
                    entry.setJti(jti);
                    entry.setExpiresAt(LocalDateTime.ofInstant(
                        new Date(System.currentTimeMillis() + ttl).toInstant(), ZoneId.systemDefault()));
                    jwtBlacklistMapper.insert(entry);
                } catch (Exception dbEx) { log.error("数据库黑名单写入失败 jti={}", jti, dbEx); }
            }
            // [已移除] 旧内存回退代码：
            // if (!redisOk) { memoryBlacklist.put(jti, System.currentTimeMillis() + ttl); }
            // if (memoryBlacklist.size() > 1000) { memoryBlacklist.entrySet().removeIf(...) }
        } catch (Exception ignored) { log.warn("JWT黑名单操作失败(可能是token已过期或无效)", ignored); }
    }

    /** 定期清理过期黑名单记录（每天凌晨2点执行） */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredBlacklist() {
        if (jwtBlacklistMapper == null) return;
        try {
            int deleted = jwtBlacklistMapper.deleteExpired();
            if (deleted > 0) {
                log.info("清理过期JWT黑名单 {} 条", deleted);
            }
        } catch (Exception e) {
            log.error("清理过期JWT黑名单失败", e);
        }
    }

    /** Redis 是否可用 */
    public boolean isRedisAvailable() {
        return redisTemplate != null;
    }
}
