package com.school.teaching.common.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Redis 健康检查。
 * 尝试 ping Redis 服务器；若连接工厂不存在（如 local profile 禁用了 Redis），返回 UP + 备注。
 */
@Component
public class RedisHealthIndicator implements HealthIndicator {

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    @Override
    public Health health() {
        if (redisConnectionFactory == null) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("note", "Redis 未启用（当前环境使用 simple 缓存）");
            return Health.up().withDetails(details).build();
        }
        try {
            var conn = redisConnectionFactory.getConnection();
            String pong = conn.ping();
            conn.close();
            return Health.up()
                    .withDetail("ping", pong != null ? pong : "OK")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getClass().getSimpleName() + ": " + e.getMessage())
                    .build();
        }
    }
}
