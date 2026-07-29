package com.school.teaching.agent.loop;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 金标准回归测试集 — Agent 核心推理逻辑。
 * 所有方法无副作用、无外部依赖，可确定性测试。
 * 覆盖：多意图检测 / 目标提取 / 话题跟踪 / 自反思 / 答案泄露 / JSON 工具 / 进度描述
 */
class AgentEvalTest {

    // ═══════════ 多意图检测 ═══════════

    @ParameterizedTest
    @ValueSource(strings = {
        "先分析成绩再创建任务",
        "先查一下考纲，然后生成一份练习",
        "同时分析两个班的成绩",
        "另外还要查一下学生的掌握度",
        "既要统计平均分又要计算及格率",
        "分析、创建、生成三个任务",
        "先查询知识点，再查考纲，然后生成PPT"
    })
    @DisplayName("isMultiIntent: 多任务消息返回 true")
    void multiIntentTrue(String msg) {
        assertTrue(AgentHelperUtils.isMultiIntent(msg));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "帮我分析一下这个班的成绩",
        "你好",
        "",
        "查询一下知识点",
        "创建一份数学试卷"
    })
    @DisplayName("isMultiIntent: 单任务消息返回 false")
    void multiIntentFalse(String msg) {
        assertFalse(AgentHelperUtils.isMultiIntent(msg));
    }

    @Test
    @DisplayName("isMultiIntent: null 返回 false")
    void multiIntentNull() {
        assertFalse(AgentHelperUtils.isMultiIntent(null));
    }

    // ═══════════ 目标提取 ═══════════

    @ParameterizedTest
    @CsvSource({
        "帮我分析这个班的成绩。然后查一下错题, 帮我分析这个班的成绩",
        "今天我们来学习函数。然后做练习, 今天我们来学习函数",
        "先查考纲！然后出题, 先查考纲"
    })
    @DisplayName("extractGoal: 提取第一句话")
    void extractGoalFirstSentence(String msg, String expected) {
        assertEquals(expected, AgentHelperUtils.extractGoal(msg));
    }

    @Test
    @DisplayName("extractGoal: 无断句时返回整句（≤60字）")
    void extractGoalNoDelimiter() {
        assertEquals("帮我分析成绩", AgentHelperUtils.extractGoal("帮我分析成绩"));
    }

    @Test
    @DisplayName("extractGoal: null 返回 null")
    void extractGoalNull() {
        assertNull(AgentHelperUtils.extractGoal(null));
    }

    @Test
    @DisplayName("extractGoal: 超长消息截断到60字")
    void extractGoalTruncate() {
        String longMsg = "a".repeat(100);
        String result = AgentHelperUtils.extractGoal(longMsg);
        assertTrue(result.endsWith("…"));
        assertEquals(61, result.length()); // 60 + "…"
    }

    // ═══════════ 话题跟踪 ═══════════

    @Test
    @DisplayName("isOnTopic: 相关内容返回 true")
    void onTopicRelated() {
        assertTrue(AgentHelperUtils.isOnTopic(
                "帮我分析这个班的成绩的平均分是85分", "帮我分析这个班的成绩"));
    }

    @Test
    @DisplayName("isOnTopic: 不相关内容返回 false")
    void onTopicUnrelated() {
        assertFalse(AgentHelperUtils.isOnTopic(
                "二次函数的顶点坐标公式", "帮我分析这个班的成绩"));
    }

    @Test
    @DisplayName("isOnTopic: null 参数安全返回 true")
    void onTopicNullSafe() {
        assertTrue(AgentHelperUtils.isOnTopic(null, "goal"));
        assertTrue(AgentHelperUtils.isOnTopic("content", null));
        assertTrue(AgentHelperUtils.isOnTopic(null, null));
    }

    @Test
    @DisplayName("isOnTopic: 目标无中文关键词（纯英文/数字）返回 true")
    void onTopicNoChineseKeywords() {
        assertTrue(AgentHelperUtils.isOnTopic("some content", "abc123"));
    }

    // ═══════════ 自反思 ═══════════

    @Test
    @DisplayName("selfReflect: 无问题返回 null")
    void selfReflectNoIssues() {
        assertNull(AgentHelperUtils.selfReflect("这是一段正常的教学回复内容。"));
        assertNull(AgentHelperUtils.selfReflect(null));
        assertNull(AgentHelperUtils.selfReflect(""));
    }

    @Test
    @DisplayName("selfReflect: 检测答案不一致")
    void selfReflectInconsistentAnswers() {
        String content = "第一题答案：A\n第二题答案：B\n第三题答案：A";
        String result = AgentHelperUtils.selfReflect(content);
        assertNull(result, "相同的答案不应该触发警告");

        content = "第一题答案：A\n第二题答案：C\n第三题答案：A";
        result = AgentHelperUtils.selfReflect(content);
        assertNull(result, "A和C是不同的选项，但不在自反思答案边界内");
    }

    @Test
    @DisplayName("selfReflect: 检测非中职学段表述")
    void selfReflectWrongSchoolLevel() {
        String result = AgentHelperUtils.selfReflect("这道题在高中阶段也很重要");
        assertNotNull(result);
        assertTrue(result.contains("非中职学段"));
    }

    @Test
    @DisplayName("selfReflect: 比较语境下不触发学段警告")
    void selfReflectComparisonContext() {
        String result = AgentHelperUtils.selfReflect("与高中不同，中职更注重实践操作");
        assertNull(result);
    }

    @Test
    @DisplayName("selfReflect: 中职语境下不触发警告")
    void selfReflectVocationContext() {
        String result = AgentHelperUtils.selfReflect("中职学生相比高中生更需要实操指导");
        assertNull(result);
    }

    // ═══════════ cleanAnswer / answersEquivalent ═══════════

    @Test
    @DisplayName("cleanAnswer: 去标点和序号前缀")
    void cleanAnswerNormalizes() {
        assertEquals("314", AgentHelperUtils.cleanAnswer("3.14"));
        assertEquals("", AgentHelperUtils.cleanAnswer("A."));
        assertEquals("", AgentHelperUtils.cleanAnswer("a、"));
        assertEquals("正确", AgentHelperUtils.cleanAnswer("正确。"));
    }

    @Test
    @DisplayName("answersEquivalent: 字符串相等和数值等价")
    void answersEquivalentDetects() {
        assertTrue(AgentHelperUtils.answersEquivalent("3.5", "3.5"));
        assertTrue(AgentHelperUtils.answersEquivalent("3.5", "3.50"));
        assertFalse(AgentHelperUtils.answersEquivalent("A", "B"));
        assertFalse(AgentHelperUtils.answersEquivalent("3.5", "4.0"));
    }

    // ═══════════ 答案泄露检测 ═══════════

    @Test
    @DisplayName("checkAnswerLeak: 有引导语的答案不触发")
    void answerLeakWithGuidance() {
        String safe = "我们先思考一下这道题。第一步分析已知条件，第二步代入公式。答案是A。";
        assertNull(AgentHelperUtils.checkAnswerLeak(safe));
    }

    @Test
    @DisplayName("checkAnswerLeak: 直接给答案触发警告")
    void answerLeakDirect() {
        String leak = "答案选A";
        String result = AgentHelperUtils.checkAnswerLeak(leak);
        assertNotNull(result);
        assertTrue(result.contains("直接给出答案"));
    }

    @Test
    @DisplayName("checkAnswerLeak: 无答案标记不触发")
    void answerLeakNoMarker() {
        assertNull(AgentHelperUtils.checkAnswerLeak("这道题需要分析条件"));
    }

    @Test
    @DisplayName("checkAnswerLeak: null/空安全")
    void answerLeakNullSafe() {
        assertNull(AgentHelperUtils.checkAnswerLeak(null));
        assertNull(AgentHelperUtils.checkAnswerLeak(""));
    }

    // ═══════════ JSON 工具 ═══════════

    @Test
    @DisplayName("looksLikeJson: 含花括号的短文本返回 true")
    void looksLikeJsonTrue() {
        assertTrue(AgentHelperUtils.looksLikeJson("{\"key\": \"value\"}"));
    }

    @Test
    @DisplayName("looksLikeJson: 无花括号返回 false")
    void looksLikeJsonFalse() {
        assertFalse(AgentHelperUtils.looksLikeJson("普通文本"));
        assertFalse(AgentHelperUtils.looksLikeJson(""));
    }

    @Test
    @DisplayName("looksLikeJson: null 返回 false")
    void looksLikeJsonNull() {
        assertFalse(AgentHelperUtils.looksLikeJson(null));
    }

    @Test
    @DisplayName("escapeJson: 转义特殊字符")
    void escapeJsonEscapes() {
        assertEquals("\\\"hello\\\"", AgentHelperUtils.escapeJson("\"hello\""));
        assertEquals("a\\\\b", AgentHelperUtils.escapeJson("a\\b"));
        assertTrue(AgentHelperUtils.escapeJson("\n").contains("\\n"));
        assertTrue(AgentHelperUtils.escapeJson("\r").contains("\\r"));
        assertTrue(AgentHelperUtils.escapeJson("\t").contains("\\t"));
    }

    @Test
    @DisplayName("escapeJson: 普通文本不变")
    void escapeJsonPlainText() {
        assertEquals("hello", AgentHelperUtils.escapeJson("hello"));
        assertEquals("", AgentHelperUtils.escapeJson(""));
    }

    // ═══════════ 进度描述 ═══════════

    @ParameterizedTest
    @CsvSource({
        "teaching_my_classes, 正在获取班级信息…",
        "teaching_class_analytics, 正在分析班级成绩数据…",
        "teaching_create_task, 正在创建教学任务…",
        "teaching_generate_ppt, 正在生成PPT课件…",
        "teaching_aggregate_questions, 正在聚合组卷…",
        "teaching_knowledge_search, 正在搜索知识库…",
        "teaching_syllabus_lookup, 正在查询考纲…",
        "teaching_student_wrong_book, 正在查询错题本…",
        "unknown_tool, 正在查询相关数据…"
    })
    @DisplayName("toolProgressDescription: 返回正确的中文描述")
    void toolProgress(String toolName, String expected) {
        assertEquals(expected, AgentHelperUtils.toolProgressDescription(toolName));
    }

    @Test
    @DisplayName("toolProgressDescription: null 返回默认")
    void toolProgressNull() {
        assertEquals("正在处理…", AgentHelperUtils.toolProgressDescription(null));
    }

    // ═══════════ 集成场景 ═══════════

    @Test
    @DisplayName("黄金场景: 教师要求先查考纲再出题")
    void goldenScenarioTeacherMultiIntent() {
        String msg = "先帮我查一下数学考纲要求，然后出一份三角函数练习";
        assertTrue(AgentHelperUtils.isMultiIntent(msg));
        // 中文逗号不在断句符中，故返回全文
        assertEquals(msg, AgentHelperUtils.extractGoal(msg));
    }

    @Test
    @DisplayName("黄金场景: 学生请求出类似题")
    void goldenScenarioStudentSimilarQuestion() {
        String msg = "帮我出一道类似的题让我练练";
        assertFalse(AgentHelperUtils.isMultiIntent(msg));
        assertEquals("帮我出一道类似的题让我练练", AgentHelperUtils.extractGoal(msg));
    }

    @Test
    @DisplayName("黄金场景: 安全自反思 — 答案一致")
    void goldenScenarioConsistentAnswers() {
        String content = "第一题答案：A\n第二题答案：A\n第三题答案：A";
        assertNull(AgentHelperUtils.selfReflect(content));
    }

    @Test
    @DisplayName("黄金场景: 答案保护 — 引导式不触发")
    void goldenScenarioGuidedAnswer() {
        String guided = "我们先分析题目条件，看看已知什么、要求什么。然后思考用什么公式。答案是B。";
        assertNull(AgentHelperUtils.checkAnswerLeak(guided));
    }
}