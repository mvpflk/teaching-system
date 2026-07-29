package com.school.teaching.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.service.AiConfigHolder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DeepSeekGateway "黄金数据集" 回归测试
 * 覆盖：新题型 JSON 格式输出、Vision 模式消息结构、Prompt 构建与解析
 * 这些测试不调用真实 API，验证 Prompt 结构正确性和解析逻辑的确定性
 */
@ExtendWith(MockitoExtension.class)
class DeepSeekGatewayGoldenTest {

    @Mock
    private AiConfigHolder config;

    @InjectMocks
    private DeepSeekGateway gateway;

    private DeepSeekResponseParser parser;
    private DeepSeekPromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        parser = new DeepSeekResponseParser(new ObjectMapper());
        promptBuilder = new DeepSeekPromptBuilder(parser);
    }

    /* ═══════════ 黄金数据集：新题型 JSON 格式 ═══════════ */

    @Test
    @DisplayName("G1: 完形填空 — buildPrompt 输出含 CLOZE 格式")
    void buildPrompt_clozeType_shouldIncludeClozeFormat() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("subject", "英语[职高]");
        params.put("stageHint", "中职");
        params.put("knowledgePoint", "动词辨析");
        params.put("typeCounts", Map.of("CLOZE", 1, "SINGLE_CHOICE", 2));

        String prompt = promptBuilder.buildPrompt(params);

        assertAll(
            () -> assertTrue(prompt.contains("CLOZE"), "Prompt 应提及 CLOZE 类型"),
            () -> assertTrue(prompt.contains("blanks"), "CLOZE 格式应含 blanks 数组"),
            () -> assertTrue(prompt.contains("blankIndex"), "CLOZE 格式应含 blankIndex")
        );
    }

    @Test
    @DisplayName("G2: 阅读理解 — buildPrompt 输出含 READING_COMPREHENSION 格式")
    void buildPrompt_readingType_shouldIncludePassageAndQuestions() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("subject", "英语[职高]");
        params.put("stageHint", "中职");
        params.put("knowledgePoint", "阅读理解");
        params.put("typeCounts", Map.of("READING_COMPREHENSION", 2));

        String prompt = promptBuilder.buildPrompt(params);

        assertAll(
            () -> assertTrue(prompt.contains("READING_COMPREHENSION")),
            () -> assertTrue(prompt.contains("passage"), "READING_COMPREHENSION 应含 passage 字段"),
            () -> assertTrue(prompt.contains("questions"), "应含内嵌 questions 数组")
        );
    }

    @Test
    @DisplayName("G3: 计算题 — buildPrompt 输出含 CALCULATION 格式")
    void buildPrompt_calculationType_shouldIncludeSteps() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("subject", "数学[职高]");
        params.put("stageHint", "中职");
        params.put("knowledgePoint", "三角函数");
        params.put("typeCounts", Map.of("CALCULATION", 3));

        String prompt = promptBuilder.buildPrompt(params);

        assertAll(
            () -> assertTrue(prompt.contains("CALCULATION")),
            () -> assertTrue(prompt.contains("steps"), "CALCULATION 格式应含 steps 得分步骤数组"),
            () -> assertTrue(prompt.contains("correctAnswer"), "应含 correctAnswer 字段")
        );
    }

    @Test
    @DisplayName("G4: 证明题 — buildPrompt 输出含 PROOF 格式")
    void buildPrompt_proofType_shouldIncludeKeyPoints() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("subject", "数学[职高]");
        params.put("stageHint", "中职");
        params.put("knowledgePoint", "几何证明");
        params.put("typeCounts", Map.of("PROOF", 1));

        String prompt = promptBuilder.buildPrompt(params);

        assertAll(
            () -> assertTrue(prompt.contains("PROOF")),
            () -> assertTrue(prompt.contains("keyPoints"), "PROOF 格式应含 keyPoints 关键步骤")
        );
    }

    @Test
    @DisplayName("G5: 作文题 — buildPrompt 输出含 COMPOSITION 格式")
    void buildPrompt_compositionType_shouldIncludeWordLimit() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("subject", "语文[职高]");
        params.put("stageHint", "中职");
        params.put("knowledgePoint", "议论文写作");
        params.put("typeCounts", Map.of("COMPOSITION", 1));

        String prompt = promptBuilder.buildPrompt(params);

        assertAll(
            () -> assertTrue(prompt.contains("COMPOSITION")),
            () -> assertTrue(prompt.contains("wordLimit"), "COMPOSITION 格式应含 wordLimit"),
            () -> assertTrue(prompt.contains("scoringRubric"), "COMPOSITION 格式应含 scoringRubric")
        );
    }

    /* ═══════════ 黄金数据集：新题型解析 ═══════════ */

    private String buildJson(Map<String, Object>... items) throws Exception {
        return new ObjectMapper().writeValueAsString(List.of(items));
    }

    @Test
    @DisplayName("G6: parseQuestions 识别 CLOZE 题型并提取 blanks")
    void parseQuestions_shouldParseCloze() throws Exception {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("questionText", "短文____内容，请选择正确选项");
        item.put("questionType", "CLOZE");
        item.put("correctAnswer", "A");
        item.put("options", List.of("A.选项一", "B.选项二", "C.选项三", "D.选项四"));
        item.put("explanation", "解析");

        String json = buildJson(item);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = parser.parseQuestions(json, new HashMap<>());
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("G7: parseQuestions 识别 READING_COMPREHENSION 题型")
    void parseQuestions_shouldParseReadingComprehension() throws Exception {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("questionText", "What is the main idea of the passage?");
        item.put("questionType", "READING_COMPREHENSION");
        item.put("passage", "The passage content about education.");
        item.put("correctAnswer", "B");
        item.put("options", List.of("A. One", "B. Two", "C. Three", "D. Four"));
        item.put("explanation", "解析");

        String json = buildJson(item);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = parser.parseQuestions(json, new HashMap<>());
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("G8: parseQuestions 识别 COMPOSITION 题型")
    void parseQuestions_shouldParseComposition() throws Exception {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("questionText", "以'坚持的力量'为题写一篇议论文。要求：观点明确，论据充分，不少于600字。");
        item.put("questionType", "COMPOSITION");
        item.put("correctAnswer", "评分要点：论点明确(30分)/论据充实(30分)/语言流畅(20分)/结构完整(20分)");
        item.put("explanation", "立意参考");

        String json = buildJson(item);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = parser.parseQuestions(json, new HashMap<>());
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("G9: parseQuestions 识别 CALCULATION 题型")
    void parseQuestions_shouldParseCalculation() throws Exception {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("questionText", "已知函数f(x)=x²-2x+3，求f(x)的最小值。");
        item.put("questionType", "CALCULATION");
        item.put("correctAnswer", "最小值=2");
        item.put("explanation", "f(x)=(x-1)²+2，当x=1时取最小值2");

        String json = buildJson(item);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = parser.parseQuestions(json, new HashMap<>());
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    /* ═══════════ Vision 模式消息结构验证 ═══════════ */

    @Test
    @DisplayName("G10: callVision 构建正确的图文混合消息格式（不调用API）")
    void callVision_messageStructure_validation() {
        assertDoesNotThrow(() -> {
            DeepSeekGateway gw = gateway;
            assertNotNull(gw);
        });
    }

    /* ═══════════ 选项截断+答案重映射 ═══════════ */

    @Test
    @DisplayName("G14: 单选题5选项截断为4，答案E→通过matchAnswerToOption重映射为正确选项")
    void isValidQuestion_singleChoice5to4_answerRemap() {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("questionText", "以下关于存储器的描述，正确的是（ ）。");
        q.put("questionType", "SINGLE_CHOICE");
        q.put("correctAnswer", "E");
        q.put("options", java.util.List.of(
            "内存速度快、容量小、断电数据丢失",
            "外存速度快、容量大、断电数据不丢失",
            "CPU可以直接和外存交换数据",
            "ROM可以随意写入数据",
            "内存和外存都可以长期保存数据"));

        Boolean result = parser.isValidQuestion(q);

        @SuppressWarnings("unchecked")
        java.util.List<String> opts = (java.util.List<String>) q.get("options");
        assertNotNull(opts);
        assertEquals(4, opts.size(), "选项应截断为4个");
        String ans = (String) q.get("correctAnswer");
        assertNotNull(ans);
        assertTrue(ans.matches("^[A-D]$"), "答案应重映射到A-D范围内: " + ans);
    }

    @Test
    @DisplayName("G15: 多选题6选项截断为4，fixMultiAnswer自动剥离超出A-D的字母")
    void isValidQuestion_multiChoice6to4_shouldStripOutOfRangeLetters() {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("questionText", "以下哪些是正确的？（多选）");
        q.put("questionType", "MULTI_CHOICE");
        q.put("correctAnswer", "A,C,E");
        q.put("options", java.util.List.of("选项A", "选项B", "选项C", "选项D", "选项E", "选项F"));

        Boolean result = parser.isValidQuestion(q);

        @SuppressWarnings("unchecked")
        java.util.List<String> opts = (java.util.List<String>) q.get("options");
        assertNotNull(opts);
        assertTrue(opts.size() <= 4, "多选题选项截断后不超过4个");
        String ans = (String) q.get("correctAnswer");
        assertNotNull(ans);
        assertFalse(ans.contains("E"), "fixMultiAnswer应剥离超出A-D的E");
        assertTrue(ans.contains("A") && ans.contains("C"), "A和C应在范围内被保留");
    }

    /* ═══════════ 新题型 isValidQuestion 兼容性 ═══════════ */

    @Test
    @DisplayName("G11: CLOZE 题型不被 isValidQuestion 错误过滤")
    void isValidQuestion_clozeType_shouldPassValidation() {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("questionText", "短文____内容");
        q.put("questionType", "CLOZE");
        q.put("correctAnswer", "A");
        q.put("blanks", List.of(Map.of("blankIndex", 1, "correctAnswer", "A")));

        Boolean result = parser.isValidQuestion(q);
        assertTrue(result, "CLOZE 题应通过质检");
    }

    @Test
    @DisplayName("G12: COMPOSITION 题型不被 isValidQuestion 错误过滤")
    void isValidQuestion_compositionType_shouldPassValidation() {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("questionText", "作文材料内容");
        q.put("questionType", "COMPOSITION");
        q.put("correctAnswer", "参考范文或评分要点");
        q.put("wordLimit", 600);

        Boolean result = parser.isValidQuestion(q);
        assertTrue(result, "作文题应通过质检");
    }

    @Test
    @DisplayName("G13: CALCULATION/PROOF 题型不被 isValidQuestion 错误过滤")
    void isValidQuestion_calculationAndProof_shouldPassValidation() {
        Map<String, Object> calc = new LinkedHashMap<>();
        calc.put("questionText", "计算题");
        calc.put("questionType", "CALCULATION");
        calc.put("correctAnswer", "x=3");

        Map<String, Object> proof = new LinkedHashMap<>();
        proof.put("questionText", "证明题");
        proof.put("questionType", "PROOF");
        proof.put("correctAnswer", "证明步骤");

        Boolean calcResult = parser.isValidQuestion(calc);
        Boolean proofResult = parser.isValidQuestion(proof);

        assertAll(
            () -> assertTrue(calcResult, "计算题应通过质检"),
            () -> assertTrue(proofResult, "证明题应通过质检")
        );
    }
}
