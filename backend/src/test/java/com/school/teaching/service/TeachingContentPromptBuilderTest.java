package com.school.teaching.service;

import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TeachingContentPromptBuilder 路由回归测试。
 * 复现审计发现 P0:build() 缺 CONSOLIDATION_MATERIAL 路由 → 每次巩固材料生成命中
 * default 分支抛 "不支持的内容类型",导致巩固材料功能全链路失效。
 */
class TeachingContentPromptBuilderTest {

    @Test
    @DisplayName("P0: CONSOLIDATION_MATERIAL 路由使用_instructionPrompt,不再抛'不支持的内容类型'")
    void consolidationRouting() {
        Map<String, Object> params = new HashMap<>();
        params.put("subject", "数学[职高]");
        params.put("_instructionPrompt", "巩固材料PROMPT内容XYZ");
        params.put("_maxTokens", 4000);

        TeachingContentPromptBuilder.PromptResult pr = assertDoesNotThrow(
            () -> TeachingContentPromptBuilder.build("CONSOLIDATION_MATERIAL", params),
            "巩固材料不应再抛'不支持的内容类型'");

        assertEquals("巩固材料PROMPT内容XYZ", pr.prompt(), "应使用_instructionPrompt作为prompt");
        assertEquals(4000, pr.maxTokens(), "应使用_maxTokens");
        assertFalse(pr.isQuestionType(), "巩固材料是内容类型,非题目类型");
    }

    @Test
    @DisplayName("未知内容类型仍应抛异常(不误伤default保护)")
    void unknownTypeStillThrows() {
        Map<String, Object> params = new HashMap<>();
        params.put("subject", "数学[职高]");
        assertThrows(IllegalArgumentException.class,
            () -> TeachingContentPromptBuilder.build("SOME_UNKNOWN_TYPE", params));
    }
}
