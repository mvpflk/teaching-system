package com.school.teaching.service.impl;

import com.school.teaching.service.AiConfigHolder;
import com.school.teaching.service.AiServiceGateway;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 自定义 Provider 网关 — 用户自助接入的第三方大模型。
 *
 * 配置键约定（在 system_settings 中）：
 *   ai.custom.{name}.api-key     = sk-xxx         （必填）
 *   ai.custom.{name}.base-url    = https://...     （必填，OpenAI兼容端点）
 *   ai.custom.{name}.model       = model-name      （必填）
 *   ai.custom.{name}.timeout-seconds = 60          （可选，默认120）
 *   ai.provider                  = custom.{name}   （激活此provider）
 *
 * 用法示例（接入 Qwen）：
 *   ai.custom.qwen.api-key  = sk-xxx
 *   ai.custom.qwen.base-url = https://dashscope.aliyuncs.com/compatible-mode/v1
 *   ai.custom.qwen.model    = qwen-plus
 *   ai.provider             = custom.qwen
 */
@Slf4j
public class GenericAiGateway extends DeepSeekGateway implements AiServiceGateway {

    private final String prefix;

    public GenericAiGateway(AiConfigHolder config, String providerKey) {
        super(config);
        if (providerKey.startsWith("custom.")) {
            this.prefix = "ai." + providerKey;
        } else {
            this.prefix = "ai.custom." + providerKey;
        }
    }

    @Override
    protected String configPrefix() { return prefix; }

    @Override
    protected String providerName() { return prefix; }

    /** 自定义 key 格式各异，不加前缀约束 */
    @Override
    protected String apiKeyPrefix() { return ""; }

    @Override
    public List<Map<String, Object>> generateQuestions(Map<String, Object> params) {
        log.info("GenericAiGateway.generateQuestions via {}", prefix);
        return super.generateQuestions(params);
    }

    @Override
    public String generateContent(Map<String, Object> params) {
        log.info("GenericAiGateway.generateContent via {}", prefix);
        return super.generateContent(params);
    }
}
