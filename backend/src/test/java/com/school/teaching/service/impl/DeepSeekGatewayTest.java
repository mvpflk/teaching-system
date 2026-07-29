package com.school.teaching.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.service.AiConfigHolder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DeepSeekGatewayTest {

    private DeepSeekResponseParser parser;
    private DeepSeekPromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        parser = new DeepSeekResponseParser(new ObjectMapper());
        promptBuilder = new DeepSeekPromptBuilder(parser);
    }

    @SuppressWarnings("unchecked")
    private String buildSystemPrompt(Map<String, Object> params) {
        return promptBuilder.buildSystemPrompt(params);
    }

    /* ───────── 考纲约束 ───────── */

    @Test
    @DisplayName("包含syllabusTitle+syllabusScope → prompt含考纲信息")
    void buildSystemPrompt_withSyllabus_shouldIncludeSyllabusInfo() {
        Map<String, Object> params = new HashMap<>();
        params.put("subject", "数学[职高]");
        params.put("stageHint", "中职");
        params.put("syllabusTitle", "四川省对口升学数学考纲");
        params.put("syllabusScope", "集合、不等式、函数");

        String prompt = buildSystemPrompt(params);
        assertAll(
            () -> assertTrue(prompt.contains("四川省对口升学数学考纲")),
            () -> assertTrue(prompt.contains("集合、不等式、函数")),
            () -> assertTrue(prompt.contains("四川省对口升学考试考点"))
        );
    }

    @Test
    @DisplayName("包含syllabusContext → prompt含上下文内容")
    void buildSystemPrompt_withSyllabusContext_shouldIncludeContext() {
        Map<String, Object> params = new HashMap<>();
        params.put("subject", "英语[职高]");
        params.put("stageHint", "中职");
        params.put("syllabusContext", "【词汇】1500核心词\n【语法】8大时态");

        String prompt = buildSystemPrompt(params);
        assertTrue(prompt.contains("【词汇】1500核心词"));
        assertTrue(prompt.contains("8大时态"));
    }

    /* ───────── 出题约束 ───────── */

    @Test
    @DisplayName("questionType参数 → prompt含难度约束0.40~0.70")
    void buildSystemPrompt_withQuestionType_shouldIncludeDifficulty() {
        Map<String, Object> params = new HashMap<>();
        params.put("subject", "数学[职高]");
        params.put("stageHint", "中职");
        params.put("questionType", "SINGLE_CHOICE");

        String prompt = buildSystemPrompt(params);
        assertAll(
            () -> assertTrue(prompt.contains("0.40~0.70")),
            () -> assertTrue(prompt.contains("四川省对口升学考试考点")),
            () -> assertTrue(prompt.contains("选择题和填空题"))
        );
    }

    @Test
    @DisplayName("prompt含'出题' → 自动触发出题模式")
    void buildSystemPrompt_promptContainsChuTi_shouldTriggerQuestionMode() {
        Map<String, Object> params = new HashMap<>();
        params.put("subject", "语文[职高]");
        params.put("stageHint", "中职");
        params.put("prompt", "请根据以下知识点出题");

        String prompt = buildSystemPrompt(params);
        assertTrue(prompt.contains("0.40~0.70"));
    }

    /* ───────── 学段信息 ───────── */

    @Test
    @DisplayName("stageHint=中职 → prompt含'中职数学'和'中职学生'")
    void buildSystemPrompt_withStageHint_shouldIncludeStage() {
        Map<String, Object> params = new HashMap<>();
        params.put("subject", "数学[职高]");
        params.put("stageHint", "中职");

        String prompt = buildSystemPrompt(params);
        assertTrue(prompt.contains("中职数学"));
        assertTrue(prompt.contains("中职学生"));
    }

    /* ───────── 边界/无参数 ───────── */

    @Test
    @DisplayName("无考纲信息 → prompt不含考纲标题占位符'「」'")
    void buildSystemPrompt_noSyllabus_shouldNotIncludeSyllabusPlaceholder() {
        Map<String, Object> params = new HashMap<>();
        params.put("subject", "通用");
        params.put("stageHint", "中职");

        String prompt = buildSystemPrompt(params);
        assertFalse(prompt.contains("「」"), "无syllabusTitle时不应出现空占位");
        assertTrue(prompt.contains("四川省对口升学考试考点"), "对口升学约束始终存在");
    }

    @Test
    @DisplayName("syllabusTitle为空字符串 → 不产生考纲标题注入")
    void buildSystemPrompt_emptySyllabusTitle_shouldSkip() {
        Map<String, Object> params = new HashMap<>();
        params.put("subject", "数学");
        params.put("stageHint", "中职");
        params.put("syllabusTitle", "");
        params.put("syllabusScope", "");

        String prompt = buildSystemPrompt(params);
        assertFalse(prompt.contains("所有题目和知识点必须严格限定"),
            "空syllabusTitle不应触发考纲标题块");
    }

    @Test
    @DisplayName("syllabusTitle为'null'字符串 → 跳过标题注入, 但不阻止无条件对口升学行")
    void buildSystemPrompt_nullStringSyllabusTitle_shouldSkip() {
        Map<String, Object> params = new HashMap<>();
        params.put("subject", "数学");
        params.put("stageHint", "中职");
        params.put("syllabusTitle", "null");
        params.put("syllabusScope", "null");

        String prompt = buildSystemPrompt(params);
        assertFalse(prompt.contains("⻄川省"), "不触发错误的考纲标题");
        assertTrue(prompt.contains("四川省对口升学"), "无条件行始终有");
    }

    @Test
    @DisplayName("空params → 默认'中职通用', 不抛异常")
    void buildSystemPrompt_emptyParams_shouldUseDefaults() {
        Map<String, Object> params = new HashMap<>();
        String prompt = assertDoesNotThrow(() -> buildSystemPrompt(params));
        assertTrue(prompt.contains("中职通用"));
    }

    @Test
    @DisplayName("无stageHint → 默认'中职'")
    void buildSystemPrompt_noStageHint_shouldDefaultToZhongzhi() {
        Map<String, Object> params = new HashMap<>();
        params.put("subject", "数学");

        String prompt = buildSystemPrompt(params);
        assertTrue(prompt.contains("中职数学"));
    }

    @Test
    @DisplayName("非出题模式 → 不含难度约束")
    void buildSystemPrompt_notQuestionMode_shouldNotHaveDifficulty() {
        Map<String, Object> params = new HashMap<>();
        params.put("subject", "数学[职高]");
        params.put("stageHint", "中职");

        String prompt = buildSystemPrompt(params);
        assertFalse(prompt.contains("0.40~0.70"));
    }
}
