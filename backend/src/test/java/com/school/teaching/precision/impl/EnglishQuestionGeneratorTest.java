package com.school.teaching.precision.impl;

import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.mapper.KnowledgeNodeMapper;
import com.school.teaching.mapper.PrecisionEnglishReadingPassageMapper;
import com.school.teaching.mapper.QuestionBankMapper;
import com.school.teaching.service.impl.DeepSeekGateway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * EnglishQuestionGenerator 硬化回归测试。
 *
 * 复现审计发现:英语语法题生成绕过统一解析器,自研裸解析 → getOrDefault null 存字面量 "null"、
 * 字段名刚性、选项 null → "null"(下游 Map.of NPE)、无有效性过滤。
 */
@ExtendWith(MockitoExtension.class)
class EnglishQuestionGeneratorTest {

    @Mock private DeepSeekGateway deepSeekGateway;
    @Mock private QuestionBankMapper questionMapper;
    @Mock private KnowledgeNodeMapper nodeMapper;
    @Mock private PrecisionEnglishReadingPassageMapper passageMapper;

    @InjectMocks private EnglishQuestionGenerator generator;

    @Test
    @DisplayName("硬化: null字段跳过 + 字段别名 + 选项null→[] + 剥A.前缀,不存字面量null")
    void hardening() {
        KnowledgeNode node = new KnowledgeNode();
        node.setName("一般现在时");
        when(nodeMapper.selectById(1L)).thenReturn(node);

        // 三道:①正常(选项带A.前缀) ②题干/答案为null(应跳过) ③字段别名question/correctAnswer+无选项
        String aiJson = "["
            + "{\"questionText\":\"He ___ to school every day.\",\"options\":[\"A. go\",\"B. goes\",\"C. going\",\"D. gone\"],\"answer\":\"B\",\"explanation\":\"第三人称单数\"},"
            + "{\"questionText\":null,\"answer\":null},"
            + "{\"question\":\"She ___ a book now.\",\"correctAnswer\":\"A\"}"
            + "]";
        when(deepSeekGateway.generateContent(any())).thenReturn(aiJson);

        List<QuestionBank> saved = new ArrayList<>();
        when(questionMapper.insert(any(QuestionBank.class))).thenAnswer(inv -> {
            saved.add(inv.getArgument(0));
            return 1;
        });

        generator.generateGrammarQuestions(1L, 3);

        assertEquals(2, saved.size(), "题干/答案为null的题应被跳过,只存2道");
        for (QuestionBank qb : saved) {
            assertNotEquals("null", qb.getQuestionText(), "题干不应是字面量null");
            assertNotEquals("null", qb.getCorrectAnswer(), "答案不应是字面量null");
            assertNotEquals("null", qb.getOptions(), "选项不应是字面量null");
        }
        // 第1道:选项剥离A.前缀
        assertTrue(saved.get(0).getOptions().contains("goes"), "选项内容保留");
        assertFalse(saved.get(0).getOptions().contains("A. go"), "选项应剥离A.前缀");
        // 第2道:字段别名 question→题干, correctAnswer→答案, 无选项→[]
        assertEquals("She ___ a book now.", saved.get(1).getQuestionText(), "question字段别名应识别");
        assertEquals("A", saved.get(1).getCorrectAnswer(), "correctAnswer字段别名应识别");
        assertEquals("[]", saved.get(1).getOptions(), "无选项应存空数组而非null");
    }
}
