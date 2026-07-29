package com.school.teaching.service.impl;

import com.school.teaching.entity.QuestionBank;
import com.school.teaching.mapper.QuestionBankMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * QuestionSaveService 落库回归测试。
 *
 * 复现 2026-07-14 生产故障：客观题生成后 question_bank.options / correct_answer 为 NULL
 * （R116 拆分遗漏：非 TRUE_FALSE 分支从未调用 qb.setOptions()/setCorrectAnswer()）。
 */
@ExtendWith(MockitoExtension.class)
class QuestionSaveServiceTest {

    @Mock
    private QuestionBankMapper questionBankMapper;

    @InjectMocks
    private QuestionSaveService service;

    private QuestionBank capture(Map<String, Object> q) {
        List<QuestionBank> captured = new ArrayList<>();
        when(questionBankMapper.insert(any(QuestionBank.class))).thenAnswer(inv -> {
            captured.add(inv.getArgument(0));
            return 1;
        });
        Map<String, Object> params = new HashMap<>();
        params.put("subject", "数学[职高]");
        service.saveQuestions(1L, params, List.of(q));
        assertEquals(1, captured.size());
        return captured.get(0);
    }

    @Test
    @DisplayName("Q1: 单选题 options + correctAnswer 必须落库(不再NULL)")
    void singleChoice_persistsOptionsAndAnswer() {
        Map<String, Object> q = new HashMap<>();
        q.put("questionType", "SINGLE_CHOICE");
        q.put("questionText", "二次函数 y=x²-2x+1 的最小值?");
        q.put("options", new ArrayList<>(List.of("0", "1", "-1", "2")));
        q.put("correctAnswer", "A");
        QuestionBank qb = capture(q);
        assertNotNull(qb.getOptions(), "单选题 options 不应为 NULL");
        assertFalse(qb.getOptions().isBlank());
        assertTrue(qb.getOptions().contains("0"), "options 应含选项内容");
        assertEquals("A", qb.getCorrectAnswer(), "单选题 correctAnswer 不应为 NULL");
    }

    @Test
    @DisplayName("Q2: 多选题 options + correctAnswer 落库")
    void multiChoice_persistsOptionsAndAnswer() {
        Map<String, Object> q = new HashMap<>();
        q.put("questionType", "MULTI_CHOICE");
        q.put("questionText", "下列哪些是二次函数?");
        q.put("options", new ArrayList<>(List.of("y=x²", "y=2x", "y=x²+1", "y=3")));
        q.put("correctAnswer", "AC");
        QuestionBank qb = capture(q);
        assertNotNull(qb.getOptions(), "多选题 options 不应为 NULL");
        assertEquals("AC", qb.getCorrectAnswer());
    }

    @Test
    @DisplayName("Q3: 填空题 correctAnswer 落库")
    void fillIn_persistsAnswer() {
        Map<String, Object> q = new HashMap<>();
        q.put("questionType", "FILL_IN");
        q.put("questionText", "函数 y=2x+1 的斜率是 ____。");
        q.put("correctAnswer", "2");
        QuestionBank qb = capture(q);
        assertEquals("2", qb.getCorrectAnswer(), "填空题 correctAnswer 不应为 NULL");
    }

    @Test
    @DisplayName("Q4: 判断题 options/answer 仍由 normalizeTrueFalse 正确设置")
    void trueFalse_stillNormalized() {
        Map<String, Object> q = new HashMap<>();
        q.put("questionType", "TRUE_FALSE");
        q.put("questionText", "三角形内角和为180度。");
        q.put("correctAnswer", "T");
        QuestionBank qb = capture(q);
        assertNotNull(qb.getOptions(), "判断题 options 应为 √/× ");
        assertEquals("A", qb.getCorrectAnswer(), "T 应归一为 A");
    }

    @Test
    @DisplayName("Q5: 带A.前缀的选项保存时应被剥离(防前端A.A.双前缀)")
    void singleChoice_stripsOptionPrefix() {
        Map<String, Object> q = new HashMap<>();
        q.put("questionType", "SINGLE_CHOICE");
        q.put("questionText", "以下关于桌面的描述正确的是?");
        q.put("options", new ArrayList<>(List.of(
            "A. 桌面是屏幕区域", "B. 桌面是背景图", "C. 桌面是C盘", "D. 桌面是文件夹")));
        q.put("correctAnswer", "A");
        QuestionBank qb = capture(q);
        assertNotNull(qb.getOptions());
        assertFalse(qb.getOptions().contains("A. 桌面"), "选项不应保留 'A.' 前缀, 实际=" + qb.getOptions());
        assertTrue(qb.getOptions().contains("桌面是屏幕区域"), "选项内容应保留");
    }

    @Test
    @DisplayName("Q6: 返回map带intent/category/aiCategory(智慧大屏四类提问分组依赖)")
    void returnMap_carriesIntentAndCategory() {
        List<QuestionBank> captured = new ArrayList<>();
        when(questionBankMapper.insert(any(QuestionBank.class))).thenAnswer(inv -> {
            captured.add(inv.getArgument(0));
            return 1;
        });
        Map<String, Object> q = new HashMap<>();
        q.put("questionType", "SINGLE_CHOICE");
        q.put("questionText", "以下哪个是操作系统?");
        q.put("options", new ArrayList<>(List.of("Windows", "Word", "Excel", "Chrome")));
        q.put("correctAnswer", "A");
        q.put("intent", "考查操作系统概念");
        q.put("category", "RECALL");
        List<Map<String, Object>> results = service.saveQuestions(1L, new HashMap<>(), List.of(q));
        assertEquals(1, results.size());
        assertEquals("考查操作系统概念", results.get(0).get("intent"), "返回map应带intent");
        assertEquals("RECALL", results.get(0).get("category"), "返回map应带category");
        assertEquals("RECALL", results.get(0).get("aiCategory"), "返回map应带aiCategory(前端键名)");
    }
}
