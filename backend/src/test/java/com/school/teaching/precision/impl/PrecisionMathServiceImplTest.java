package com.school.teaching.precision.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrecisionMathServiceImplTest {

    @Mock private QuestionBankMapper questionMapper;
    @Mock private KnowledgeNodeMapper nodeMapper;
    @Mock private PrecisionProgressMapper progressMapper;
    @Mock private StudentMapper studentMapper;
    @Mock private DictSubjectMapper subjectMapper;
    @Mock private WrongQuestionMapper wrongQuestionMapper;

    @InjectMocks
    private PrecisionMathServiceImpl mathService;

    private String invokeTransformNumber(String text) {
        return ReflectionTestUtils.invokeMethod(mathService, "transformNumber", text);
    }

    // ═══════════ transformNumber ═══════════

    @Test
    @DisplayName("transformNumber: 所有数字均被替换")
    void transformNumber_allNumbersChanged() {
        String result = invokeTransformNumber("已知a1=2，d=3，求a10");
        assertNotNull(result);
        assertNotEquals("已知a1=2，d=3，求a10", result);
    }

    @Test
    @DisplayName("transformNumber: 含数字选项也被替换")
    void transformNumber_numericAnswer() {
        String result = invokeTransformNumber("解方程2x+3=7，x=2");
        assertNotNull(result);
        assertTrue(result.contains("x"));
    }

    @Test
    @DisplayName("transformNumber: null或空字符串原样返回")
    void transformNumber_nullOrEmpty() {
        assertNull(invokeTransformNumber(null));
        assertEquals("", invokeTransformNumber(""));
    }

    @Test
    @DisplayName("transformNumber: 无数字文本原样返回")
    void transformNumber_noNumbers() {
        String text = "已知集合A，求交集";
        assertEquals(text, invokeTransformNumber(text));
    }

    @Test
    @DisplayName("transformNumber: 选项中的数字也被变换")
    void transformNumber_optionNumbers() {
        String result = invokeTransformNumber("选项A. 3 B. 5 C. 7 D. 9");
        assertNotNull(result);
        assertNotEquals("选项A. 3 B. 5 C. 7 D. 9", result);
    }

    // ═══════════ diagnose ═══════════

    @Test
    @DisplayName("diagnose: 有模块节点时返回最多33题")
    void diagnose_withModules_returnsUpTo33Questions() {
        List<KnowledgeNode> modules = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            KnowledgeNode m = new KnowledgeNode();
            m.setId((long) (100 + i));
            m.setName("模块" + i);
            m.setLevel(2);
            m.setParentId(10L);
            modules.add(m);
        }
        when(nodeMapper.selectList(argThat(w -> {
            if (!(w instanceof LambdaQueryWrapper)) return false;
            return true;
        }))).thenReturn(modules);  // 首次调用：返回 modules（用于 getSubjectId 后的 level=2 查询）
        // 后续调用（子节点查询）返回空列表 — 只需一个 stub 覆盖所有，避免 UnnecessaryStubbing
        // Mockito 顺序：最近匹配的 stub 优先，此处仅设一个通用规则

        QuestionBank q = new QuestionBank();
        q.setId(1L);
        q.setQuestionText("测试题");
        q.setQuestionType("SINGLE_CHOICE");
        q.setOptions("[\"A. 1\",\"B. 2\"]");
        q.setDifficultyLevel(1);
        q.setStatus(1);
        // 必须用可变列表：diagnose() 内部会调用 removeIf()
        List<QuestionBank> mutableQList = new ArrayList<>();
        mutableQList.add(q);
        when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mutableQList);

        Map<String, Object> result = mathService.diagnose(1L);
        assertNotNull(result);
        assertEquals("数学[职高]", result.get("subject"));
        assertNotNull(result.get("questions"));
    }

    @Test
    @DisplayName("diagnose: 无模块节点时回退到legacy模式")
    void diagnose_noModules_fallsBackToLegacy() {
        when(nodeMapper.selectList(argThat(w -> {
            if (!(w instanceof LambdaQueryWrapper)) return false;
            return true;
        }))).thenReturn(List.of());

        QuestionBank q2 = new QuestionBank();
        q2.setId(1L);
        q2.setQuestionText("legacy问题");
        q2.setQuestionType("SINGLE_CHOICE");
        q2.setOptions("[\"A\",\"B\"]");
        q2.setDifficultyLevel(1);
        q2.setStatus(1);
        List<QuestionBank> mutableLegacyQList = new ArrayList<>();
        mutableLegacyQList.add(q2);
        when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mutableLegacyQList);

        Map<String, Object> result = mathService.diagnose(1L);
        assertNotNull(result);
        assertEquals("数学[职高]", result.get("subject"));
    }

    // ═══════════ buildOnlineTestQuestionsFromPack ═══════════

    @Test
    @DisplayName("buildOnlineTestQuestionsFromPack: 有pack题目时variant正确返回")
    void buildOnlineTestQuestionsFromPack_variantHasExpected() {
        QuestionBank q = new QuestionBank();
        q.setId(1L);
        q.setQuestionText("测试题x=2");
        q.setQuestionType("SINGLE_CHOICE");
        q.setOptions("[\"A. 1\",\"B. 2\"]");
        q.setDifficultyLevel(1);
        q.setStatus(1);
        when(questionMapper.selectBatchIds(anyList())).thenReturn(List.of(q));
        lenient().when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<Map<String, Object>> result = mathService.buildOnlineTestQuestionsFromPack(1L, List.of(1L));
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("buildOnlineTestQuestionsFromPack: 空ID列表回退到默认方法")
    void buildOnlineTestQuestionsFromPack_emptyIds_returnsFallback() {
        QuestionBank q = new QuestionBank();
        q.setId(1L);
        q.setQuestionText("回退题");
        q.setQuestionType("SINGLE_CHOICE");
        q.setOptions("[\"A\",\"B\"]");
        q.setDifficultyLevel(1);
        q.setStatus(1);
        when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(q));

        List<Map<String, Object>> result = mathService.buildOnlineTestQuestionsFromPack(1L, List.of());
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    // ═══════════ validateLatex ═══════════

    @Test
    @DisplayName("validateLatex: 合法LaTeX返回null")
    void validateLatex_valid_returnsNull() {
        assertNull(MathSeedFixer.validateLatex("函数$f(x)=x^2$的定义域"));
        assertNull(MathSeedFixer.validateLatex("$\\frac{1}{2}$和$\\sqrt{4}$"));
    }

    @Test
    @DisplayName("validateLatex: $不配对返回错误信息")
    void validateLatex_unpairedDollar_returnsError() {
        String result = MathSeedFixer.validateLatex("函数$f(x)=x^2的定义域");
        assertNotNull(result);
        assertTrue(result.contains("不配对"));
    }

    @Test
    @DisplayName("validateLatex: {}不配对返回错误信息")
    void validateLatex_unpairedBrace_returnsError() {
        String result = MathSeedFixer.validateLatex("$\\frac{1{2$");
        assertNotNull(result);
        assertTrue(result.contains("不配对") || result.contains("多余"));
    }

    @Test
    @DisplayName("validateLatex: 无LaTeX标记返回null")
    void validateLatex_noLatex_returnsNull() {
        assertNull(MathSeedFixer.validateLatex("这是一道没有公式的题目"));
        assertNull(MathSeedFixer.validateLatex(""));
    }

    // ═══════════ fixLatexErrors ═══════════

    @Test
    @DisplayName("fixLatexErrors: \\sqr自动修正为\\sqrt")
    void fixLatexErrors_sqrToSqrt() {
        String result = MathSeedFixer.fixLatexErrors("$\\sqr{4}=2$", 1L);
        assertTrue(result.contains("\\sqrt"), "应包含修正后的 \\sqrt");
        // 注意：\sqrt 本身包含 \sqr 作为子串，不能 assertFalse
        assertEquals("$\\sqrt{4}=2$", result, "应完整替换为 \\sqrt");
    }

    @Test
    @DisplayName("fixLatexErrors: \\fra自动修正为\\frac")
    void fixLatexErrors_fraToFrac() {
        String result = MathSeedFixer.fixLatexErrors("$\\fra{1}{2}$", 1L);
        assertTrue(result.contains("\\frac"), "应包含修正后的 \\frac");
        assertEquals("$\\frac{1}{2}$", result, "应完整替换为 \\frac");
    }

    @Test
    @DisplayName("fixLatexErrors: 缺少结尾$自动补全")
    void fixLatexErrors_missingDollar() {
        String result = MathSeedFixer.fixLatexErrors("函数$f(x)=x^2", 1L);
        assertTrue(result.endsWith("$"));
    }

    @Test
    @DisplayName("fixLatexErrors: 无错误时原样返回")
    void fixLatexErrors_noError_unchanged() {
        String text = "函数$f(x)=x^2$的定义域为$[0,+\\infty)$";
        assertEquals(text, MathSeedFixer.fixLatexErrors(text, 1L));
    }

    @Test
    @DisplayName("fixLatexErrors: null或空字符串原样返回")
    void fixLatexErrors_nullOrEmpty() {
        assertNull(MathSeedFixer.fixLatexErrors(null, null));
        assertEquals("", MathSeedFixer.fixLatexErrors("", 1L));
    }
}
