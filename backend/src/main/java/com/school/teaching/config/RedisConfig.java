package com.school.teaching.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * 自定义缓存配置 —— 按缓存名称设置不同 TTL。
     *
     * 缓存名称对照：
     *   pending_count     → 30s   学生待办任务计数
     *   submission_status → 30s   学生任务提交状态
     *   task_list         → 60s   任务列表（分页+角色）
     *   class_students    → 300s  班级学生列表
     *
     * @ConditionalOnBean: local profile 排除了 RedisAutoConfiguration，
     * 无 RedisConnectionFactory 时跳过此 Bean，Spring Cache 自动回退 ConcurrentHashMap。
     */
    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 默认配置（60 秒 TTL）
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(60))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

        // 各缓存独立 TTL
        Map<String, RedisCacheConfiguration> configMap = Map.of(
            "pending_count",     defaults.entryTtl(Duration.ofSeconds(30)),
            "submission_status", defaults.entryTtl(Duration.ofSeconds(30)),
            "task_list",         defaults.entryTtl(Duration.ofSeconds(60)),
            "class_students",    defaults.entryTtl(Duration.ofSeconds(300)),
            "knowledge_tree",    defaults.entryTtl(Duration.ofHours(1))
        );

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaults)
            .withInitialCacheConfigurations(configMap)
            .build();
    }
}
