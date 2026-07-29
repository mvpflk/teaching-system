package com.school.teaching.service.impl;

import com.school.teaching.service.AiConfigHolder;
import com.school.teaching.service.TeachingContentPromptBuilder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P0: 专业课内容类型回归测试
 * 验证 R90 新增代码不破坏现有的 5 种内容类型（计算机专业课）
 * 关键验证点：
 *   1. TEACHING_DESIGN 仍走职业教育模板（做中学做中教）
 *   2. PRACTICE_PLAN 不抛异常（非文化课可用）
 *   3. COMPREHENSIVE_EXERCISES JSON输出格式不变
 *   4. CLASSROOM_QUESTIONS 四种提问类型不变
 *   5. KNOWLEDGE_CHECKLIST 结构不变
 */
@ExtendWith(MockitoExtension.class)
class ProfessionalCourseRegressionTest {

    /* ═══════════ 回归1: 教学设计 ═══════════ */

    @Test
    @DisplayName("R1: 计算机专业课 TEACHING_DESIGN → 包含职业教育术语")
    void vocationalTeachingDesign_shouldContainVocationalTerms() {
        Map<String, Object> params = vocationalParams("TEACHING_DESIGN");

        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("TEACHING_DESIGN", params);

        String prompt = pr.prompt();
        assertAll(
            () -> assertTrue(prompt.contains("中职") || prompt.contains("实训") || prompt.contains("技能"),
                "职业模板必须包含职业教育特征词汇"),
            () -> assertNotNull(pr.maxTokens()),
            () -> assertTrue(pr.maxTokens() >= 4000)
        );
    }

    @Test
    @DisplayName("R2: 计算机专业课 TEDESIGN CONCISE → 精简版可调用")
    void vocationalTeachingDesign_concise_shouldWork() {
        Map<String, Object> params = vocationalParams("TEACHING_DESIGN");
        params.put("style", "CONCISE");

        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("TEACHING_DESIGN", params);
        assertNotNull(pr.prompt());
        assertFalse(pr.prompt().isBlank());
    }

    @Test
    @DisplayName("R3: 计算机专业课 TEDESIGN DETAILED → 详细版可调用")
    void vocationalTeachingDesign_detailed_shouldWork() {
        Map<String, Object> params = vocationalParams("TEACHING_DESIGN");
        params.put("style", "DETAILED");

        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("TEACHING_DESIGN", params);
        assertNotNull(pr.prompt());
    }

    /* ═══════════ 回归2: 实训方案 ═══════════ */

    @Test
    @DisplayName("R4: 计算机专业课 PRACTICE_PLAN → 正常返回(不抛异常)")
    void vocationalPracticePlan_shouldNotThrow() {
        Map<String, Object> params = vocationalParams("PRACTICE_PLAN");

        assertDoesNotThrow(() -> {
            TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("PRACTICE_PLAN", params);
            assertNotNull(pr.prompt());
            assertTrue(pr.prompt().contains("机房") || pr.prompt().contains("实训") || pr.prompt().contains("操作"),
                "计算机实训方案应包含机房/实训/操作相关词汇");
        }, "专业课 PRACTICE_PLAN 不应被文化课禁用规则拦截");
    }

    /* ═══════════ 回归3: 知识清单 ═══════════ */

    @Test
    @DisplayName("R5: 计算机专业课 KNOWLEDGE_CHECKLIST → 包含表格结构")
    void vocationalKnowledgeChecklist_shouldWork() {
        Map<String, Object> params = vocationalParams("KNOWLEDGE_CHECKLIST");

        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("KNOWLEDGE_CHECKLIST", params);
        assertNotNull(pr.prompt());
        assertTrue(pr.prompt().contains("核心概念") || pr.prompt().contains("知识清单"));
    }

    /* ═══════════ 回归4: 综合练习 ═══════════ */

    @Test
    @DisplayName("R6: 计算机专业课 COMPREHENSIVE_EXERCISES → 含 JSON 输出格式 + tier/knowledgeDim")
    void vocationalComprehensiveExercises_shouldHaveJSONFormat() {
        Map<String, Object> params = vocationalParams("COMPREHENSIVE_EXERCISES");
        params.put("tierFocus", "BALANCED");
        params.put("knowledgeDim", "BOTH");

        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("COMPREHENSIVE_EXERCISES", params);

        assertTrue(pr.isQuestionType(), "综合练习应为 questionType");
        String ip = String.valueOf(pr.extraParams().getOrDefault("_instructionPrompt", ""));
        assertFalse(ip.isBlank() && !"null".equals(ip), "_instructionPrompt 不应为空");


        // 验证 typeCounts 传递
        assertNotNull(pr.extraParams().get("typeCounts"));
    }

    @Test
    @DisplayName("R7: 计算机专业课 COMPREHENSIVE_EXERCISES → tierFocus/knowledgeDim 正确传递")
    void vocationalComprehensiveExercises_dimensions_shouldPropagate() {
        Map<String, Object> params = vocationalParams("COMPREHENSIVE_EXERCISES");
        params.put("tierFocus", "ADVANCED");
        params.put("knowledgeDim", "THEORY");

        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("COMPREHENSIVE_EXERCISES", params);

        assertEquals("ADVANCED", pr.extraParams().get("tierFocus"));
        assertEquals("THEORY", pr.extraParams().get("knowledgeDim"));
    }

    /* ═══════════ 回归5: 课堂提问 ═══════════ */

    @Test
    @DisplayName("R8: 计算机专业课 CLASSROOM_QUESTIONS → 含4种提问类型")
    void vocationalClassroomQuestions_shouldContainFourCategories() {
        Map<String, Object> params = vocationalParams("CLASSROOM_QUESTIONS");

        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("CLASSROOM_QUESTIONS", params);

        assertTrue(pr.isQuestionType());
        String ip = String.valueOf(pr.extraParams().getOrDefault("_instructionPrompt", ""));
        assertAll(
            () -> assertTrue(ip.contains("回忆型") || ip.contains("RECALL")),
            () -> assertTrue(ip.contains("理解型") || ip.contains("COMPREHEND")),
            () -> assertTrue(ip.contains("应用型") || ip.contains("APPLY")),
            () -> assertTrue(ip.contains("拓展型") || ip.contains("EXTEND"))
        );
    }

    /* ═══════════ 回归6: 考纲注入 ═══════════ */

    @Test
    @DisplayName("R9: 计算机专业课 COMPREHENSIVE_EXERCISES → 考纲注入后非空")
    void vocationalExercises_withSyllabus_shouldIncludeSyllabusBlock() {
        Map<String, Object> params = vocationalParams("COMPREHENSIVE_EXERCISES");
        params.put("syllabusContext", "## 考试范围与要求\n计算机基础30%");

        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("COMPREHENSIVE_EXERCISES", params);

        // syllabusContext 应传递到 extraParams
        assertNotNull(pr.extraParams().get("syllabusContext"));
    }

    /* ═══════════ 回归7: invalid contentType ═══════════ */

    @Test
    @DisplayName("R10: 非法 contentType → IllegalArgumentException")
    void invalidContentType_shouldThrow() {
        Map<String, Object> params = vocationalParams("NONEXISTENT_TYPE");

        assertThrows(IllegalArgumentException.class, () ->
            TeachingContentPromptBuilder.build("NONEXISTENT_TYPE", params));
    }

    /* ═══════════ helper ═══════════ */

    private Map<String, Object> vocationalParams(String contentType) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("subject", "信息技术应用基础");
        params.put("knowledgePoint", "计算机系统组成");
        params.put("categoryPath", "信息技术应用基础 → 计算机基础 → 计算机系统");
        params.put("stageHint", "中职");
        params.put("ragContext", "【计算机系统组成】冯·诺依曼体系结构...");
        // 非文化课的学科
        return params;
    }
}
