package com.school.teaching.common.health;

import com.school.teaching.service.AiConfigHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * DeepSeek AI 网关健康检查。
 * 验证 API Key 是否已配置（非空），不发起真实 API 调用以避免消耗配额。
 */
@Component
public class AiGatewayHealthIndicator implements HealthIndicator {

    @Autowired
    private AiConfigHolder aiConfig;

    @Override
    public Health health() {
        String apiKey = aiConfig.getDecrypted("ai.deepseek.api-key");
        String baseUrl = aiConfig.get("ai.deepseek.base-url");
        String model = aiConfig.get("ai.deepseek.model");

        if (apiKey == null || apiKey.isBlank()) {
            return Health.down()
                    .withDetail("error", "DeepSeek API Key 未配置 — 请在系统设置→AI配置中填入")
                    .withDetail("baseUrl", baseUrl)
                    .build();
        }

        if (!apiKey.startsWith("sk-")) {
            return Health.down()
                    .withDetail("error", "DeepSeek API Key 解密失败或格式异常(不以sk-开头) — 请重新填入Key")
                    .withDetail("baseUrl", baseUrl)
                    .withDetail("apiKeyConfigured", false)
                    .withDetail("note", "可能是jwt.secret变更导致旧Key无法解密，请在AI配置中重新填入保存")
                    .build();
        }

        // 仅本地校验：Key 已配置且格式正确即可，不发起外部 API 调用
        // 避免健康检查消耗配额、触发限流、或在 Key 失效时导致 Docker 重启循环
        return Health.up()
                .withDetail("baseUrl", baseUrl)
                .withDetail("model", model)
                .withDetail("apiKeyConfigured", true)
                .withDetail("note", "API Key 已配置（仅本地校验，未做外部连通性测试）")
                .build();
    }
}
