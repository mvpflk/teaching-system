package com.school.teaching.utils;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 混合限流：用户级 + IP 级双层封禁。
 *
 * 场景：学校微机室 150 名学生共用同一出口 IP。
 *   1）用户级：单用户 60 秒内失败 N 次 → 该用户名封禁 M 分钟
 *   2）IP 级：同一 IP 下所有用户失败总和超上限 → 该 IP 封禁 N 分钟
 *
 * Redis 可用时使用 Lua 脚本保证原子性；不可用时回退本地 ConcurrentHashMap。
 */
@Component
public class RedisRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    // ── Redis Key 前缀 ──
    private static final String FAIL_USER_PREFIX  = "ratelimit:fail:user:";
    private static final String FAIL_IP_PREFIX    = "ratelimit:fail:ip:";
    private static final String BLOCK_USER_PREFIX = "ratelimit:block:user:";
    private static final String BLOCK_IP_PREFIX   = "ratelimit:block:ip:";

    // ── Lua 脚本：原子记录登录失败 ──
    // KEYS[1] = userFailKey   "ratelimit:fail:user:{userId}"
    // KEYS[2] = ipFailKey     "ratelimit:fail:ip:{ip}"
    // KEYS[3] = userBlockKey  "ratelimit:block:user:{userId}"
    // KEYS[4] = ipBlockKey    "ratelimit:block:ip:{ip}"
    // ARGV[1] = maxFailuresPerUser
    // ARGV[2] = maxFailuresPerIp
    // ARGV[3] = windowSeconds
    // ARGV[4] = blockMinutesPerUser
    // ARGV[5] = blockMinutesPerIp
    //
    // 逻辑：
    //   ① IP 已封禁 → 不再计数直接返回
    //   ② 用户未封禁 → INCR 用户失败计数器，达阈值则 SETEX 用户封禁键
    //   ③ 始终 INCR IP 失败计数器（即使该用户已封禁，其他用户仍可能从同 IP 尝试）
    //   ④ IP 计数器达阈值 → SETEX IP 封禁键
    private static final DefaultRedisScript<Long> RECORD_SCRIPT = new DefaultRedisScript<>();
    static {
        RECORD_SCRIPT.setScriptText(
            "if redis.call('EXISTS', KEYS[4]) > 0 then return 0 end\n" +
            "if redis.call('EXISTS', KEYS[3]) == 0 then\n" +
            "  local uf = redis.call('INCR', KEYS[1])\n" +
            "  if uf == 1 then redis.call('EXPIRE', KEYS[1], ARGV[3]) end\n" +
            "  if tonumber(uf) >= tonumber(ARGV[1]) then\n" +
            "    redis.call('SETEX', KEYS[3], tonumber(ARGV[4]) * 60, '1')\n" +
            "  end\n" +
            "end\n" +
            "local ipf = redis.call('INCR', KEYS[2])\n" +
            "if ipf == 1 then redis.call('EXPIRE', KEYS[2], ARGV[3]) end\n" +
            "if tonumber(ipf) >= tonumber(ARGV[2]) then\n" +
            "  redis.call('SETEX', KEYS[4], tonumber(ARGV[5]) * 60, '1')\n" +
            "end\n" +
            "return 0"
        );
        RECORD_SCRIPT.setResultType(Long.class);
    }

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    // ── 可配置参数（application.yml login.rate-limit.*） ──
    @Value("${login.rate-limit.max-failures-per-user:5}")
    private int maxFailuresPerUser;

    @Value("${login.rate-limit.block-minutes-per-user:5}")
    private int blockMinutesPerUser;

    @Value("${login.rate-limit.max-failures-per-ip:50}")
    private int maxFailuresPerIp;

    @Value("${login.rate-limit.block-minutes-per-ip:5}")
    private int blockMinutesPerIp;

    @Value("${login.rate-limit.window-seconds:60}")
    private int windowSeconds;

    // ── 本地回退（Redis 不可用时的兜底） ──
    private final ConcurrentHashMap<String, LocalEntry> localUserFails = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalEntry> localIpFails   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long>       localUserBlock = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long>       localIpBlock   = new ConcurrentHashMap<>();

    @PostConstruct
    void logConfig() {
        log.info("登录限流配置: 用户={}次/{}秒→封{}分, IP={}次/{}秒→封{}分",
            maxFailuresPerUser, windowSeconds, blockMinutesPerUser,
            maxFailuresPerIp, windowSeconds, blockMinutesPerIp);
    }

    // ── 公开 API ──

    /** 检查 IP 或用户是否被封禁 */
    public boolean isBlocked(String ip, String userId) {
        if (redisTemplate != null) {
            try {
                return Boolean.TRUE.equals(redisTemplate.hasKey(BLOCK_USER_PREFIX + userId))
                    || Boolean.TRUE.equals(redisTemplate.hasKey(BLOCK_IP_PREFIX + ip));
            } catch (Exception e) {
                log.warn("Redis不可用（isBlocked），回退本地", e);
            }
        }
        return isBlockedLocal(ip, userId);
    }

    /** 记录一次登录尝试结果 */
    public void recordAttempt(String ip, String userId, boolean success) {
        if (success) {
            clear(ip, userId);
            return;
        }
        if (redisTemplate != null) {
            try {
                redisTemplate.execute(RECORD_SCRIPT, keys(ip, userId),
                    String.valueOf(maxFailuresPerUser),
                    String.valueOf(maxFailuresPerIp),
                    String.valueOf(windowSeconds),
                    String.valueOf(blockMinutesPerUser),
                    String.valueOf(blockMinutesPerIp));
                return;
            } catch (Exception e) {
                log.warn("Redis不可用（recordAttempt），回退本地", e);
            }
        }
        recordFailureLocal(ip, userId);
    }

    /** Redis 是否可用 */
    public boolean isRedisAvailable() {
        return redisTemplate != null;
    }

    /** 清理本地过期条目（由外部 @Scheduled 定时调用） */
    public void cleanupLocal() {
        long now = System.currentTimeMillis();
        localUserBlock.entrySet().removeIf(e -> now >= e.getValue());
        localIpBlock.entrySet().removeIf(e -> now >= e.getValue());
        localUserFails.entrySet().removeIf(e -> now - e.getValue().windowStart > 600_000);
        localIpFails.entrySet().removeIf(e -> now - e.getValue().windowStart > 600_000);
    }

    // ── 私有实现 ──

    private List<String> keys(String ip, String userId) {
        return Arrays.asList(
            FAIL_USER_PREFIX  + userId,
            FAIL_IP_PREFIX    + ip,
            BLOCK_USER_PREFIX + userId,
            BLOCK_IP_PREFIX   + ip
        );
    }

    // ── 本地回退 ──

    private boolean isBlockedLocal(String ip, String userId) {
        long now = System.currentTimeMillis();

        Long uBlock = localUserBlock.get(userId);
        if (uBlock != null) {
            if (now < uBlock) return true;
            localUserBlock.remove(userId);
            localUserFails.remove(userId);
        }

        Long iBlock = localIpBlock.get(ip);
        if (iBlock != null) {
            if (now < iBlock) return true;
            localIpBlock.remove(ip);
            localIpFails.remove(ip);
        }

        return false;
    }

    private void recordFailureLocal(String ip, String userId) {
        long now = System.currentTimeMillis();

        // IP 已封禁 → 不再计数
        Long iBlock = localIpBlock.get(ip);
        if (iBlock != null && now < iBlock) return;

        // ── 用户级计数 ──
        Long uBlock = localUserBlock.get(userId);
        if (uBlock == null || now >= uBlock) {
            LocalEntry ue = localUserFails.computeIfAbsent(userId, k -> new LocalEntry(now));
            synchronized (ue) {
                if (now - ue.windowStart > windowSeconds * 1000L) {
                    ue.windowStart = now;
                    ue.count = 0;
                }
                ue.count++;
                if (ue.count >= maxFailuresPerUser) {
                    localUserBlock.put(userId, now + blockMinutesPerUser * 60_000L);
                    localUserFails.remove(userId);
                    log.warn("用户 [{}] 本地封禁 {} 分钟 — {} 次失败", userId, blockMinutesPerUser, ue.count);
                }
            }
        }

        // ── IP 级计数（即使该用户已封禁，仍计入 IP 总量） ──
        LocalEntry ie = localIpFails.computeIfAbsent(ip, k -> new LocalEntry(now));
        synchronized (ie) {
            if (now - ie.windowStart > windowSeconds * 1000L) {
                ie.windowStart = now;
                ie.count = 0;
            }
            ie.count++;
            if (ie.count >= maxFailuresPerIp) {
                localIpBlock.put(ip, now + blockMinutesPerIp * 60_000L);
                localIpFails.remove(ip);
                log.warn("IP [{}] 本地封禁 {} 分钟 — {} 次失败", ip, blockMinutesPerIp, ie.count);
            }
        }
    }

    /** 登录成功时清除用户级限制（IP 级不清除，因其他用户仍可能尝试） */
    private void clear(String ip, String userId) {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(FAIL_USER_PREFIX + userId);
                redisTemplate.delete(BLOCK_USER_PREFIX + userId);
            } catch (Exception ignored) { /* best-effort */ }
        }
        localUserFails.remove(userId);
        localUserBlock.remove(userId);
    }

    private static class LocalEntry {
        long windowStart;
        int count = 0;
        LocalEntry(long now) { this.windowStart = now; }
    }
}
