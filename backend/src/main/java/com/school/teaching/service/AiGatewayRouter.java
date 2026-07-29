package com.school.teaching.service;

import com.school.teaching.service.impl.GenericAiGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 网关路由器 — 支持内置 provider + 自定义 provider。
 *
 * 内置: deepseek / agnes
 * 自定义: 用户在系统设置中配置 ai.custom.{name}.* → ai.provider = custom.{name}
 */
@Slf4j
@Primary
@Component("aiGatewayRouter")
public class AiGatewayRouter implements AiServiceGateway {

    private final AiServiceGateway deepSeekGateway;
    private final AiServiceGateway agnesGateway;
    private final AiConfigHolder config;
    private final ConcurrentHashMap<String, AiServiceGateway> customGateways = new ConcurrentHashMap<>();

    public AiGatewayRouter(@Qualifier("deepSeekGateway") AiServiceGateway deepSeekGateway,
                           @Qualifier("agnesAiGateway") AiServiceGateway agnesGateway,
                           AiConfigHolder config) {
        this.deepSeekGateway = deepSeekGateway;
        this.agnesGateway = agnesGateway;
        this.config = config;
    }

    private AiServiceGateway resolve() {
        String provider = config.getProvider();

        if ("agnes".equalsIgnoreCase(provider)) {
            return agnesGateway;
        }
        if ("deepseek".equalsIgnoreCase(provider)) {
            return deepSeekGateway;
        }
        // 自定义 provider: custom.{name}
        if (provider != null && provider.startsWith("custom.")) {
            String name = provider.substring("custom.".length());
            return customGateways.computeIfAbsent(name,
                k -> new GenericAiGateway(config, k));
        }

        // 默认
        return deepSeekGateway;
    }

    @Override
    public List<Map<String, Object>> generateQuestions(Map<String, Object> params) {
        return resolve().generateQuestions(params);
    }

    @Override
    public Map<String, Object> scoreTextAnswer(Map<String, Object> params) {
        return resolve().scoreTextAnswer(params);
    }

    @Override
    public String generateContent(Map<String, Object> params) {
        return resolve().generateContent(params);
    }

    @Override
    public String getProvider() {
        return resolve().getProvider();
    }

    @Override
    public Map<String, Object> callWithTools(List<Map<String, Object>> messages, List<Map<String, Object>> tools,
                                              double temperature, int maxTokens,
                                              String customApiKey, String customBaseUrl, String customModel,
                                              Long userId) {
        return resolve().callWithTools(messages, tools, temperature, maxTokens,
                customApiKey, customBaseUrl, customModel, userId);
    }

    @Override
    public String generateContentQuiet(Map<String, Object> params,
                                        String customApiKey, String customBaseUrl, String customModel) {
        return resolve().generateContentQuiet(params, customApiKey, customBaseUrl, customModel);
    }

    @Override
    public Map<String, Object> callVision(List<String> imageBase64List, String prompt, Map<String, Object> params) {
        return resolve().callVision(imageBase64List, prompt, params);
    }
}
