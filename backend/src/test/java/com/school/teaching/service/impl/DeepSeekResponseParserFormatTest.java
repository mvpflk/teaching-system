package com.school.teaching.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DeepSeekResponseParser 格式兼容回归测试。
 *
 * 复现 2026-07-14 生产故障：AI 返回 {exam:{questions:[{question,type:"单选题",options:["A. …"]}]}}
 * 结构（题干字段 question、中文题型、对象包裹、选项带 A. 前缀），旧解析器整批过滤 → 100% 失败。
 * 这些用例用生产真实畸形样本，验证兼容层能正确解析。
 */
class DeepSeekResponseParserFormatTest {

    private DeepSeekResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new DeepSeekResponseParser(new ObjectMapper());
    }

    @Test
    @DisplayName("F1: 生产真实结构 {exam:{questions:[{question,单选题,\"A.\"前缀}]}} 应正常解析")
    void parseProductionExamWrapper() {
        String raw = """
            ```json
            {
              "exam": {
                "title": "二次函数综合练习题（四川省对口升学考试模拟）",
                "description": "依据考纲命制",
                "totalScore": 75,
                "questions": [
                  {
                    "id": 1,
                    "type": "单选题",
                    "score": 2,
                    "knowledgeNodeId": 3101,
                    "question": "二次函数 y = -x² + 4x - 3 的开口方向和对称轴分别是？",
                    "options": ["A. 开口向上，对称轴 x = 2", "B. 开口向下，对称轴 x = -2", "C. 开口向上，对称轴 x = -2", "D. 开口向下，对称轴 x = 2"],
                    "correctAnswer": "D",
                    "explanation": "a=-1<0 开口向下，对称轴 x=-b/2a=2"
                  },
                  {
                    "id": 2,
                    "type": "单选题",
                    "question": "二次函数 y = x² - 2x + 1 的最小值是？",
                    "options": ["A. 0", "B. 1", "C. -1", "D. 2"],
                    "correctAnswer": "A",
                    "explanation": "顶点 (1,0)"
                  }
                ]
              }
            }
            ```
            """;
        List<Map<String, Object>> result = parser.parseQuestions(raw, new HashMap<>());

        assertEquals(2, result.size(), "应解析出 2 道题（旧解析器会得 0 道）");
        Map<String, Object> q1 = result.get(0);
        assertFalse(String.valueOf(q1.get("questionText")).isBlank(), "题干不应为空（question 字段兼容）");
        assertFalse(String.valueOf(q1.get("questionText")).equals("null"), "题干不应是字符串 null");
        assertEquals("SINGLE_CHOICE", q1.get("questionType"), "中文题型'单选题'应映射为 SINGLE_CHOICE");
        assertEquals("D", q1.get("correctAnswer"));
        @SuppressWarnings("unchecked")
        List<String> opts = (List<String>) q1.get("options");
        assertNotNull(opts);
        assertFalse(opts.get(0).startsWith("A."), "选项应剥离 'A.' 前缀，实际=" + opts.get(0));
    }

    @Test
    @DisplayName("F2: 填空题中文题型 + question 字段 应映射 FILL_IN 并接受")
    void parseFillInChineseType() {
        String raw = """
            {"questions":[
              {"type":"填空题","question":"函数 y=2x+1 的斜率是 ____。","answer":"2","explanation":"一次项系数"}
            ]}
            """;
        List<Map<String, Object>> result = parser.parseQuestions(raw, new HashMap<>());
        assertEquals(1, result.size());
        assertEquals("FILL_IN", result.get(0).get("questionType"));
        assertEquals("2", result.get(0).get("correctAnswer"), "answer 字段应被识别为答案");
    }

    @Test
    @DisplayName("F3: 纯 {questions:[]} 包裹（无 exam 层）应解析")
    void parsePlainQuestionsWrapper() {
        String raw = """
            {"questions":[
              {"questionType":"SINGLE_CHOICE","question":"1+1=?","options":["1","2","3","4"],"correctAnswer":"B"}
            ]}
            """;
        List<Map<String, Object>> result = parser.parseQuestions(raw, new HashMap<>());
        assertEquals(1, result.size());
        assertEquals("SINGLE_CHOICE", result.get(0).get("questionType"));
    }

    @Test
    @DisplayName("F4: 回归 — 标准扁平数组 + questionText 仍正常解析")
    void parseStandardFlatArray() {
        String raw = """
            [
              {"questionText":"标准格式题干？","questionType":"SINGLE_CHOICE","options":["甲","乙","丙","丁"],"correctAnswer":"A","explanation":"x"}
            ]
            """;
        List<Map<String, Object>> result = parser.parseQuestions(raw, new HashMap<>());
        assertEquals(1, result.size());
        assertEquals("标准格式题干？", result.get(0).get("questionText"));
        assertEquals("SINGLE_CHOICE", result.get(0).get("questionType"));
        assertEquals("A", result.get(0).get("correctAnswer"));
    }

    @Test
    @DisplayName("F5: 判断题中文题型 应映射 TRUE_FALSE")
    void parseTrueFalseChineseType() {
        String raw = """
            {"data":{"questions":[
              {"type":"判断题","question":"三角形内角和为180度。","options":["正确","错误"],"correctAnswer":"A"}
            ]}}
            """;
        List<Map<String, Object>> result = parser.parseQuestions(raw, new HashMap<>());
        assertEquals(1, result.size());
        assertEquals("TRUE_FALSE", result.get(0).get("questionType"), "嵌套 data.questions + 判断题 应解析");
    }

    @Test
    @DisplayName("F6: normalizeType 单元覆盖")
    void normalizeTypeMapping() {
        assertEquals("SINGLE_CHOICE", DeepSeekResponseParser.normalizeType("单选题"));
        assertEquals("MULTI_CHOICE", DeepSeekResponseParser.normalizeType("多选题"));
        assertEquals("TRUE_FALSE", DeepSeekResponseParser.normalizeType("判断题"));
        assertEquals("FILL_IN", DeepSeekResponseParser.normalizeType("填空题"));
        assertEquals("SINGLE_CHOICE", DeepSeekResponseParser.normalizeType("SINGLE_CHOICE"));
        assertEquals("SINGLE_CHOICE", DeepSeekResponseParser.normalizeType(null));
    }

    @Test
    @DisplayName("F7: 填空题(无选项+中文答案)即使题型标签漂移也应识别为FILL_IN不被过滤")
    void parseFillInStructuralInference() {
        String raw = """
            {"questions":[
              {"type":"fill_blank","question":"启动Windows后看到的整个屏幕区域称为____。","correctAnswer":"桌面"},
              {"type":"单选题","question":"任务栏中显示已打开程序的区域是____。","answer":"活动任务区"}
            ]}
            """;
        List<Map<String, Object>> result = parser.parseQuestions(raw, new HashMap<>());
        assertEquals(2, result.size(), "两道填空题都应保留（旧逻辑会因缺选项/答案非A-D被全过滤）");
        assertEquals("FILL_IN", result.get(0).get("questionType"), "fill_blank 应映射 FILL_IN");
        assertEquals("FILL_IN", result.get(1).get("questionType"), "无选项+非A-D答案的'单选题'应结构推断为 FILL_IN");
    }

    @Test
    @DisplayName("F8: normalizeType 英文别名(标签漂移)")
    void normalizeTypeEnglishAliases() {
        assertEquals("FILL_IN", DeepSeekResponseParser.normalizeType("fill_blank"));
        assertEquals("FILL_IN", DeepSeekResponseParser.normalizeType("completion"));
        assertEquals("MULTI_CHOICE", DeepSeekResponseParser.normalizeType("multiple_choice"));
        assertEquals("TRUE_FALSE", DeepSeekResponseParser.normalizeType("judgement"));
        assertEquals("SINGLE_CHOICE", DeepSeekResponseParser.normalizeType("single_choice"));
    }
}
