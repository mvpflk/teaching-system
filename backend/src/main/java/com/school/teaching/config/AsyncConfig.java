package com.school.teaching.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步线程池配置。
 *
 * 拆分两个池，避免长任务与短任务互相拖累（队头阻塞）：
 * - 默认池（getAsyncExecutor）：服务高频短任务——行为埋点/审计日志/事件通知等，裸 @Async 走这里。
 * - aiExecutor：服务 AI 长任务（60~300s）——内容生成/评分/组卷/出题，用 @Async("aiExecutor") 标注。
 *
 * 两池均为有界池 + 默认 AbortPolicy：队列满时 submit 会同步抛 RejectedExecutionException，
 * AI 任务的提交方需 catch 该异常并将任务标记为失败（否则任务条目会永久停留在 PENDING）。
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /** 默认池：高频短任务（埋点/审计/通知）。裸 @Async 使用此池。 */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /** AI 长任务专用池：内容生成/评分/组卷/出题。用 @Async("aiExecutor") 显式指定。 */
    @Bean("aiExecutor")
    public Executor aiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-gen-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
            log.error("Async method [{}] threw exception: {}", method.getName(), ex.getMessage(), ex);
    }
}
