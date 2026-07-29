package com.school.teaching.service.impl;

import com.school.teaching.service.AiConfigHolder;
import org.springframework.stereotype.Component;

/**
 * Agnes.ai 网关 — OpenAI 兼容接口。
 * 继承 DeepSeekGateway 的所有逻辑，仅覆盖配置前缀和提供商标识。
 */
@Component("agnesAiGateway")
public class AgnesAiGateway extends DeepSeekGateway {

    public AgnesAiGateway(AiConfigHolder config) {
        super(config);
    }

    @Override
    protected String configPrefix() { return "ai.agnes"; }

    @Override
    protected String providerName() { return "Agnes.ai"; }

    @Override
    protected String apiKeyPrefix() { return ""; }
}
