package com.school.teaching.agent.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "agent")
public class AgentConfig {

    /** 最大并发 Agent 循环数 */
    private int maxConcurrentLoops = 5;

    /** 单次 Agent 对话最大工具调用步数 */
    private int maxSteps = 10;

    /** 单次 Agent 对话总超时（毫秒） */
    private long totalTimeoutMs = 180_000;

    /** 单次 API 调用超时（毫秒） */
    private long apiTimeoutMs = 30_000;

    /** Token 警告阈值，超过后触发压缩 */
    private int tokenWarning = 28_000;

    /** Token 上限（模型 context window） */
    private int tokenMax = 32_000;

    /** 会话过期天数（Redis TTL） */
    private int sessionTtlDays = 7;

    /** 工具查询 SQL LIMIT 上限 */
    private int sqlLimit = 500;

    /** PPT 生成文件存储目录 */
    private String pptDir = "/tmp/teaching-ppt/";

    /** 线程池核心线程数 */
    private int corePoolSize = 3;

    /** 线程池最大线程数 */
    private int maxPoolSize = 12;

    /** 线程池工作队列容量 */
    private int queueCapacity = 20;

    /** SSE 端点允许的 Origin 域名列表（空=渐进式安全：仅日志警告不拦截） */
    private List<String> allowedOrigins = Collections.emptyList();

    @Bean(name = "agentExecutor", destroyMethod = "shutdown")
    public ExecutorService agentExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize, maxPoolSize,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                r -> {
                    Thread t = new Thread(r, "agent-loop");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy());
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    @Bean(name = "agentConcurrencyLimit")
    public Semaphore agentConcurrencyLimit() {
        return new Semaphore(maxConcurrentLoops);
    }
}
