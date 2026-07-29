package com.school.teaching.service;

import java.util.List;
import java.util.Map;

public interface AiServiceGateway {

    List<Map<String, Object>> generateQuestions(Map<String, Object> params);

    Map<String, Object> scoreTextAnswer(Map<String, Object> params);

    String generateContent(Map<String, Object> params);

    String getProvider();

    // ── Agent 模块专用方法（默认抛出 UnsupportedOperationException） ──

    default Map<String, Object> callWithTools(List<Map<String, Object>> messages, List<Map<String, Object>> tools,
                                               double temperature, int maxTokens,
                                               String customApiKey, String customBaseUrl, String customModel,
                                               Long userId) {
        throw new UnsupportedOperationException("callWithTools 未被当前 Provider 实现");
    }

    default String generateContentQuiet(Map<String, Object> params,
                                         String customApiKey, String customBaseUrl, String customModel) {
        throw new UnsupportedOperationException("generateContentQuiet 未被当前 Provider 实现");
    }

    default String generateContentQuiet(Map<String, Object> params) {
        return generateContentQuiet(params, null, null, null);
    }

    default Map<String, Object> callVision(List<String> imageBase64List, String prompt, Map<String, Object> params) {
        throw new UnsupportedOperationException("callVision 未被当前 Provider 实现");
    }
}
