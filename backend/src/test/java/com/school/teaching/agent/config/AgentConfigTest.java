package com.school.teaching.agent.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Semaphore;

import static org.junit.jupiter.api.Assertions.*;

class AgentConfigTest {

    @Test
    @DisplayName("agentExecutor: 创建线程池并执行任务")
    void agentExecutorRunsTask() throws Exception {
        AgentConfig config = new AgentConfig();
        config.setCorePoolSize(2);
        config.setMaxPoolSize(4);
        config.setQueueCapacity(10);

        var executor = config.agentExecutor();
        assertNotNull(executor);
        assertFalse(executor.isShutdown());

        var future = executor.submit(() -> 42);
        assertEquals(42, future.get());

        executor.shutdown();
    }

    @Test
    @DisplayName("agentConcurrencyLimit: 创建指定容量的 Semaphore")
    void concurrencyLimit() {
        AgentConfig config = new AgentConfig();
        config.setMaxConcurrentLoops(5);

        Semaphore sem = config.agentConcurrencyLimit();
        assertEquals(5, sem.availablePermits());
    }

    @Test
    @DisplayName("agentConcurrencyLimit: 可配置为不同值")
    void concurrencyLimitConfigurable() {
        AgentConfig config = new AgentConfig();
        config.setMaxConcurrentLoops(3);

        Semaphore sem = config.agentConcurrencyLimit();
        assertEquals(3, sem.availablePermits());
    }

    @Test
    @DisplayName("defaults: 默认值合理")
    void defaultsAreReasonable() {
        AgentConfig config = new AgentConfig();
        assertEquals(5, config.getMaxConcurrentLoops());
        assertEquals(10, config.getMaxSteps());
        assertEquals(180_000, config.getTotalTimeoutMs());
        assertEquals(30_000, config.getApiTimeoutMs());
        assertEquals(28_000, config.getTokenWarning());
        assertEquals(32_000, config.getTokenMax());
        assertEquals(7, config.getSessionTtlDays());
        assertEquals(500, config.getSqlLimit());
        assertEquals("/tmp/teaching-ppt/", config.getPptDir());
    }
}