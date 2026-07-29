package com.school.teaching.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * AI 调用 Prometheus 指标暴露服务
 * <p>
 * 暴露指标：
 * <ul>
 *   <li>{@code ai_calls_total} — Counter，按 capability+status 分标签</li>
 *   <li>{@code ai_tokens_used_total} — Counter，按 capability 分标签</li>
 *   <li>{@code ai_calls_latency_seconds} — Timer，P50/P95/P99 分位数</li>
 * </ul>
 */
@Slf4j
@Service
public class AiMetricsService {

    private final MeterRegistry registry;

    public AiMetricsService(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * 记录一次 AI 调用
     */
    public void recordCall(String capability, String status, int tokensUsed,
                           int promptTokens, int completionTokens, long latencyMs, String model) {
        try {
            // 调用计数 — 按 capability + status 分标签
            Counter.builder("ai.calls.total")
                    .tag("capability", nullToEmpty(capability))
                    .tag("status", nullToEmpty(status))
                    .description("AI 调用总次数")
                    .register(registry)
                    .increment();

            // Token 消耗
            if (tokensUsed > 0) {
                Counter.builder("ai.tokens.used")
                        .tag("capability", nullToEmpty(capability))
                        .description("AI Token 总消耗")
                        .register(registry)
                        .increment(tokensUsed);
            }

            // 延迟分布
            Timer.builder("ai.calls.latency")
                    .tag("capability", nullToEmpty(capability))
                    .description("AI 调用延迟")
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .publishPercentileHistogram(true)
                    .register(registry)
                    .record(Duration.ofMillis(latencyMs));

        } catch (Exception e) {
            // 指标记录失败不应影响业务
            log.debug("AI 指标记录失败: {}", e.getMessage());
        }
    }

    /**
     * 记录失败的 AI 调用（无 token 消耗、延迟近似值）
     */
    public void recordFailure(String capability, long latencyMs) {
        recordCall(capability, "FAILED", 0, 0, 0, latencyMs, null);
    }

    private static String nullToEmpty(String s) {
        return s == null || s.isEmpty() ? "unknown" : s;
    }
}
