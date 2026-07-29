package com.school.teaching.agent.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentRateLimiterTest {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;

    private AgentRateLimiter createLimiter() {
        AgentRateLimiter limiter = new AgentRateLimiter(redis);
        ReflectionTestUtils.setField(limiter, "teacherLimit", 20);
        ReflectionTestUtils.setField(limiter, "studentLimit", 10);
        return limiter;
    }

    @Test
    @DisplayName("checkLimit: 首次调用返回 null（有配额）")
    void firstCallAllowed() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);

        AgentRateLimiter limiter = createLimiter();
        assertNull(limiter.checkLimit(1L, "TEACHER"));
        verify(redis).expire(anyString(), any());
    }

    @Test
    @DisplayName("checkLimit: 超过限额返回提示消息")
    void overLimitReturnsMessage() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(11L);

        AgentRateLimiter limiter = createLimiter();
        String msg = limiter.checkLimit(1L, "STUDENT");
        assertNotNull(msg);
        assertTrue(msg.contains("已用完"));
    }

    @Test
    @DisplayName("checkLimit: 学生和教师不同限额")
    void studentAndTeacherDifferentLimits() {
        when(redis.opsForValue()).thenReturn(valueOps);

        AgentRateLimiter limiter = createLimiter();

        when(valueOps.increment(anyString())).thenReturn(21L);
        assertNotNull(limiter.checkLimit(1L, "TEACHER"));

        when(valueOps.increment(anyString())).thenReturn(11L);
        assertNotNull(limiter.checkLimit(2L, "STUDENT"));
    }

    @Test
    @DisplayName("checkLimit: 在限额内返回 null")
    void withinLimitReturnsNull() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(5L);

        AgentRateLimiter limiter = createLimiter();
        assertNull(limiter.checkLimit(1L, "TEACHER"));
    }

    @Test
    @DisplayName("hasFreeQuota: 有配额返回 true")
    void hasFreeQuotaReturnsTrue() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn("3");

        AgentRateLimiter limiter = createLimiter();
        assertTrue(limiter.hasFreeQuota(1L, "TEACHER"));
    }

    @Test
    @DisplayName("hasFreeQuota: 无配额返回 false")
    void hasFreeQuotaReturnsFalse() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn("20");

        AgentRateLimiter limiter = createLimiter();
        assertFalse(limiter.hasFreeQuota(1L, "TEACHER"));
    }

    @Test
    @DisplayName("hasFreeQuota: 无记录返回 true（首次使用）")
    void hasFreeQuotaNoRecord() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);

        AgentRateLimiter limiter = createLimiter();
        assertTrue(limiter.hasFreeQuota(1L, "STUDENT"));
    }
}