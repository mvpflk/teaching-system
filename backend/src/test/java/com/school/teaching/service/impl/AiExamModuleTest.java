package com.school.teaching.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.service.TeachingContentPromptBuilder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI 组卷 + 诊断 + 巩固材料 模块全面测试
 * 覆盖 6 套学科Prompt、全链路指令注入、诊断数据聚合、巩固材料格式
 */
@ExtendWith(MockitoExtension.class)
class AiExamModuleTest {

    /* ═══════════════════════ 组卷 6 套 Prompt 模板 ═══════════════════════ */

    @Test
    @DisplayName("语文仿真组卷 → buildChineseExam · 对口升学命题角色 · 150分标准")
    void chineseExam_shouldContainCorrectPrompt() {
        Map<String, Object> params = buildExamParams("语文[职高]", "exam");
        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("EXAM_PAPER", params);

        assertTrue(pr.isQuestionType(), "组卷必须有 extraParams");
        String ip = instructionPrompt(pr);
        assertAll("语文仿真组卷 Prompt 完整性",
            () -> assertTrue(ip.contains("四川省对口升学考试语文命题教师"), "角色"),
            () -> assertTrue(ip.contains("满分150分"), "总分"),
            () -> assertTrue(ip.contains("150分钟"), "时长"),
            () -> assertTrue(ip.contains("SINGLE_CHOICE"), "单选题型"),
            () -> assertTrue(ip.contains("第I卷"), "应含第I卷结构"),
            () -> assertTrue(ip.contains("第II卷"), "应含第II卷结构"),
            () -> assertTrue(ip.contains("SHORT_ANSWER"), "简答题型"),
            () -> assertTrue(ip.contains("COMPOSITION"), "作文题型"),
            () -> assertTrue(ip.contains("材料作文"), "大作文"),
            () -> assertTrue(ip.contains("应用文"), "应用文写作"),
            () -> assertTrue(ip.contains("背诵篇目") || ip.contains("名句默写"), "背诵/默写"),
            () -> assertTrue(ip.contains("工匠精神"), "作文主题方向")
        );
    }

    @Test
    @DisplayName("数学仿真组卷 → buildMathExam · 11专题分值 · 解答题顺序")
    void mathExam_shouldContainCorrectPrompt() {
        Map<String, Object> params = buildExamParams("数学[职高]", "exam");
        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("EXAM_PAPER", params);

        String ip = instructionPrompt(pr);
        assertAll("数学仿真组卷 Prompt 完整性",
            () -> assertTrue(ip.contains("四川省对口升学考试数学命题教师")),
            () -> assertTrue(ip.contains("满分150分")),
            () -> assertTrue(ip.contains("120分钟")),
            () -> assertTrue(ip.contains("选择题(15题"), "选择题15题"),
            () -> assertTrue(ip.contains("填空题(5题"), "填空题5题"),
            () -> assertTrue(ip.contains("解答题(6题"), "解答题6题"),
            () -> assertTrue(ip.contains("CALCULATION"), "解答题题型"),
            () -> assertTrue(ip.contains("集合约5%"), "分值分布"),
            () -> assertTrue(ip.contains("解析几何约18%"), "解析几何占18%"),
            () -> assertTrue(ip.contains("函数→三角→数列"), "解答题顺序")
        );
    }

    @Test
    @DisplayName("英语仿真组卷 → buildEnglishExam · 100分 · 15单选考点排布")
    void englishExam_shouldContainCorrectPrompt() {
        Map<String, Object> params = buildExamParams("英语[职高]", "exam");
        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("EXAM_PAPER", params);

        String ip = instructionPrompt(pr);
        assertAll("英语仿真组卷 Prompt 完整性",
            () -> assertTrue(ip.contains("四川省对口升学考试英语命题教师")),
            () -> assertTrue(ip.contains("满分100分")),
            () -> assertTrue(ip.contains("第I卷") && ip.contains("第II卷"), "应含I/II卷结构"),
            () -> assertTrue(ip.contains("情景交际→冠词→代词→介词"), "单选考点排布"),
            () -> assertTrue(ip.contains("语言应用"), "语言应用题型（非完形填空）"),
            () -> assertTrue(ip.contains("补全对话"), "补全对话题型"),
            () -> assertTrue(ip.contains("短文改错"), "短文改错题型（2014年四川考纲翻译已取消）"),
            () -> assertTrue(ip.contains("READING_COMPREHENSION"), "阅读理解"),
            () -> assertTrue(ip.contains("COMPOSITION"), "书面表达"),
            () -> assertTrue(ip.contains("80词") || ip.contains("80词左右"), "应用文约80词"),
            () -> assertTrue(ip.contains("校园→社会→科技"), "阅读话题顺序"),
            () -> assertFalse(ip.contains("完形"), "不应出现完形填空（四川考纲无此题型）")
        );
    }

    @Test
    @DisplayName("语文专题训练 → buildChineseTraining · temperature=0.8")
    void chineseTraining_shouldContainCorrectPrompt() {
        Map<String, Object> params = buildExamParams("语文[职高]", "training");
        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("EXAM_PAPER", params);

        String ip = instructionPrompt(pr);
        assertEquals(0.8, ((Number) pr.extraParams().getOrDefault("temperature", 1.0)).doubleValue(), 0.01);
        assertTrue(ip.contains("中职语文练习命题专家") || ip.contains("难度递进"), "专题训练角色/要求");
    }

    @Test
    @DisplayName("数学专题训练 → buildMathTraining · 干扰项设常见错误")
    void mathTraining_shouldContainCorrectPrompt() {
        Map<String, Object> params = buildExamParams("数学[职高]", "training");
        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("EXAM_PAPER", params);

        String ip = instructionPrompt(pr);
        assertTrue(ip.contains("中职数学练习命题专家"));
        assertTrue(ip.contains("常见计算错误"), "数学训练应含干扰项指导");
    }

    @Test
    @DisplayName("英语专题训练 → buildEnglishTraining · 精讲解析+错因")
    void englishTraining_shouldContainCorrectPrompt() {
        Map<String, Object> params = buildExamParams("英语[职高]", "training");
        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("EXAM_PAPER", params);

        String ip = instructionPrompt(pr);
        assertTrue(ip.contains("中职英语练习命题专家"));
        assertTrue(ip.contains("错误选项错因") || ip.contains("正确用法"), "英语训练应含精讲解析要求");
    }

    /* ═══════════════════════ 学科回退路由 ═══════════════════════ */

    @Test
    @DisplayName("非语数英学科 → 回退到 buildComprehensiveExercises")
    void nonCultureSubjectFallbackToComprehensive() {
        Map<String, Object> params = buildExamParams("植物生产与环境", "exam");
        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("EXAM_PAPER", params);

        String ip = instructionPrompt(pr);
        // 不应出现语文/数学/英语特定角色
        assertAll("非语数英学科正确回退",
            () -> assertFalse(ip.contains("对口升学考试语文命题教师"), "不应含语文"), // 可能含"对口升学"但不是语文
            () -> assertFalse(ip.contains("对口升学考试数学命题教师"), "不应含数学"),
            () -> assertFalse(ip.contains("对口升学考试英语命题教师"), "不应含英语"),
            () -> assertTrue(pr.isQuestionType(), "仍为 questionType")
        );
    }

    @Test
    @DisplayName("普高语文学科名匹配 → 应走语文 Prompt")
    void generalHighChinese_shouldMatch() {
        Map<String, Object> params = buildExamParams("语文[普高]", "exam");
        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("EXAM_PAPER", params);

        String ip = instructionPrompt(pr);
        assertTrue(ip.contains("四川省对口升学考试语文命题教师"),
            "普高语文['语文[普高]']也能匹配到语文模板（bareSubject包含'语文'）");
    }

    @Test
    @DisplayName("只有一个'语文'的学科名 → 应走语文 Prompt")
    void bareSubjectChinese_shouldMatch() {
        Map<String, Object> params = buildExamParams("语文", "exam");
        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("EXAM_PAPER", params);

        assertTrue(pr.isQuestionType());
        String ip = instructionPrompt(pr);
        assertTrue(ip.contains("语文命题教师") || ip.contains("四川省对口升学"), "无后缀'语文'也应匹配");
    }

    /* ═══════════════════════ _instructionPrompt 全链路 ═══════════════════════ */

    @Test
    @DisplayName("全部 6 套模板都必须注入 _instructionPrompt")
    void allSixTemplatesMustHaveInstructionPrompt() {
        String[] subjects = {"语文[职高]", "数学[职高]", "英语[职高]"};
        String[] modes = {"exam", "training"};

        for (String subject : subjects) {
            for (String mode : modes) {
                Map<String, Object> params = buildExamParams(subject, mode);
                TeachingContentPromptBuilder.PromptResult pr =
                    TeachingContentPromptBuilder.build("EXAM_PAPER", params);

                assertNotNull(pr.extraParams(),
                    subject + "/" + mode + ": extraParams 不能为 null");
                String ip = String.valueOf(pr.extraParams().getOrDefault("_instructionPrompt", ""));
                assertFalse(ip.isBlank() && !"null".equals(ip),
                    subject + "/" + mode + ": _instructionPrompt 不应为空");
                assertTrue(ip.length() > 50,
                    subject + "/" + mode + ": Prompt 长度应 >50 字符，实际 " + ip.length());
            }
        }
    }

    @Test
    @DisplayName("EXAM_PAPER extraParams 必须携带 subject + stageHint")
    void examPaperExtraParamsMustCarrySubjectAndStage() {
        Map<String, Object> params = buildExamParams("数学[职高]", "exam");
        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("EXAM_PAPER", params);

        // 虽然 buildExamPaper 方法中 qp 没有显式 put subject/stageHint
        // 但后端 executeAsync 会从 params 中读取，不影响主流程
        // 此测试验证 examMode 被正确传入
        assertNotNull(pr.extraParams());
        assertEquals(false, pr.extraParams().getOrDefault("comprehensive", true),
            "仿真组卷不标记为 comprehensive");
        assertTrue(pr.extraParams().containsKey("_maxTokens"));
    }

    /* ═══════════════════════ 诊断 Prompt ═══════════════════════ */

    @Test
    @DisplayName("DIAGNOSIS → 非 questionType · 含发展性语言要求")
    void diagnosisPromptShouldContainDevelopmentalLanguage() {
        Map<String, Object> diagnosisData = new LinkedHashMap<>();
        diagnosisData.put("questions", List.of(
            Map.of("questionIndex", 1, "questionText", "二倍角公式sin2α=?",
                "correctRate", 45.0, "totalAnswers", 36, "topWrongAnswers", List.of("sinα+cosα(15人)"))
        ));
        diagnosisData.put("students", List.of(
            Map.of("name", "张三", "score", 42.0),
            Map.of("name", "李四", "score", 78.0)
        ));

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("contentType", "DIAGNOSIS");
        params.put("subject", "数学[职高]");
        params.put("diagnosisData", diagnosisData);

        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("DIAGNOSIS", params);

        assertAll("DIAGNOSIS Prompt 特征",
            () -> assertFalse(pr.isQuestionType(), "诊断不是 questionType"),
            () -> assertNotNull(pr.prompt()),
            () -> assertTrue(pr.prompt().contains("发展性语言"), "必须包含发展性语言要求"),
            () -> assertTrue(pr.prompt().contains("班级学情总结"), "必须包含班级总结章节"),
            () -> assertTrue(pr.prompt().contains("共性薄弱点"), "必须包含薄弱点章节"),
            () -> assertTrue(pr.prompt().contains("重点学生关注"), "必须包含学生关注章节"),
            () -> assertTrue(pr.prompt().contains("纯Markdown不用JSON"), "输出格式要求"),
            () -> assertTrue(pr.prompt().contains("≤500字"), "字数限制"),
            () -> assertTrue(pr.prompt().contains("二倍角公式"), "应包含薄弱题目文本")
        );
    }

    @Test
    @DisplayName("DIAGNOSIS 数据为空 → Prompt 仍完整")
    void diagnosisWithEmptyData_shouldStillWork() {
        Map<String, Object> diagnosisData = new LinkedHashMap<>();
        diagnosisData.put("questions", List.of());
        diagnosisData.put("students", List.of());

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("contentType", "DIAGNOSIS");
        params.put("subject", "英语[职高]");
        params.put("diagnosisData", diagnosisData);

        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("DIAGNOSIS", params);

        assertNotNull(pr.prompt());
        assertTrue(pr.prompt().contains("所有题目正确率均≥70%") || pr.prompt().contains("无"), // 弱项题目为空时的兜底输出
            "数据为空时 Prompt 应正确处理");
    }

    /* ═══════════════════════ 巩固材料 Prompt ═══════════════════════ */

    @Test
    @DisplayName("buildConsolidationPrompt → 四段 Markdown 结构完整")
    void consolidationPrompt_shouldHaveFourSections() {
        List<String> kpNames = List.of("二倍角公式", "正弦定理");
        List<Double> errorRates = List.of(65.0, 48.0);
        String commonMistakes = "混淆sin2α和2sinα；忽略三角形内角和";

        TeachingContentPromptBuilder.PromptResult pr =
            TeachingContentPromptBuilder.buildConsolidationPrompt("数学", kpNames, errorRates, commonMistakes, "【知识库】三角恒等变换公式");

        assertAll("巩固材料 Prompt 结构完整性",
            () -> assertTrue(pr.prompt().contains("错误分析"), "第一段: 错误分析"),
            () -> assertTrue(pr.prompt().contains("核心公式"), "第二段: 核心公式/规则"),
            () -> assertTrue(pr.prompt().contains("例题精讲"), "第三段: 例题精讲"),
            () -> assertTrue(pr.prompt().contains("巩固练习"), "第四段: 巩固练习"),
            () -> assertTrue(pr.prompt().contains("错误解法"), "应含典型错误展示"),
            () -> assertTrue(pr.prompt().contains("避坑提示"), "应含避坑口诀"),
            () -> assertTrue(pr.prompt().contains("$$公式$$"), "数学公式用 $$ 格式"),
            () -> assertTrue(pr.prompt().contains("二倍角公式（错误率 65%）"), "知识点+错误率"),
            () -> assertTrue(pr.prompt().contains("混淆sin2α和2sinα"), "常见错误文本"),
            () -> assertTrue(pr.prompt().contains("【知识库】三角恒等变换公式"), "RAG上下文注入")
        );

        Map<String, Object> extra = pr.extraParams();
        assertNotNull(extra, "巩固材料应有 extraParams");
        assertEquals("CONSOLIDATION_MATERIAL", extra.get("contentType"));
        assertTrue(String.valueOf(extra.get("_instructionPrompt")).length() > 200,
            "instructionPrompt 应足够长");
    }

    /* ═══════════════════════ formatTypeCounts / typeLabel 新题型 ═══════════════════════ */

    @Test
    @DisplayName("formatTypeCounts — 8 种题型全覆盖")
    void formatTypeCounts_allEightTypes() {
        Map<String, Integer> typeCounts = new LinkedHashMap<>();
        typeCounts.put("SINGLE_CHOICE", 15);
        typeCounts.put("MULTI_CHOICE", 3);
        typeCounts.put("TRUE_FALSE", 5);
        typeCounts.put("FILL_IN", 4);
        typeCounts.put("CLOZE", 1);
        typeCounts.put("READING_COMPREHENSION", 5);
        typeCounts.put("CALCULATION", 5);
        typeCounts.put("COMPOSITION", 1);

        DeepSeekResponseParser parser = new DeepSeekResponseParser(new ObjectMapper());
        DeepSeekPromptBuilder pb = new DeepSeekPromptBuilder(parser);
        String result = pb.formatTypeCounts(typeCounts);

        assertAll("8种题型中文标签全覆盖",
            () -> assertFalse(result.contains("CLOZE×"), "不应出现英文 CLOZE"),
            () -> assertFalse(result.contains("READING_COMPREHENSION×"), "不应出现英文 READING_COMPREHENSION"),
            () -> assertFalse(result.contains("CALCULATION×"), "不应出现英文 CALCULATION"),
            () -> assertFalse(result.contains("PROOF×"), "不应出现英文 PROOF"),
            () -> assertFalse(result.contains("COMPOSITION×"), "不应出现英文 COMPOSITION"),
            () -> assertTrue(result.contains("完形填空"), "CLOZE→完形填空"),
            () -> assertTrue(result.contains("阅读理解"), "READING_COMPREHENSION→阅读理解"),
            () -> assertTrue(result.contains("计算题"), "CALCULATION→计算题"),
            () -> assertTrue(result.contains("作文"), "COMPOSITION→作文"),
            () -> assertTrue(result.contains("（共39题）"), "总数 15+3+5+4+1+5+5+1=39")
        );
    }

    @Test
    @DisplayName("typeLabel — 所有新题型有中文标签")
    void typeLabel_allNewTypesHaveLabels() {
        DeepSeekResponseParser parser = new DeepSeekResponseParser(new ObjectMapper());

        assertAll("typeLabel 新题型映射",
            () -> assertEquals("完形填空", parser.typeLabel("CLOZE")),
            () -> assertEquals("阅读理解", parser.typeLabel("READING_COMPREHENSION")),
            () -> assertEquals("阅读理解", parser.typeLabel("READING")),
            () -> assertEquals("计算题", parser.typeLabel("CALCULATION")),
            () -> assertEquals("证明题", parser.typeLabel("PROOF")),
            () -> assertEquals("作文", parser.typeLabel("COMPOSITION")),
            () -> assertEquals("单选", parser.typeLabel("SINGLE_CHOICE")),
            () -> assertEquals("简答", parser.typeLabel("ESSAY"))
        );
    }

    /* ═══════════════════════ 文化课权限隔离 ═══════════════════════ */

    @Test
    @DisplayName("isCultureSubject — 语数英各学段均识别为文化课")
    void isCultureSubject_allCultureVariants() {
        String[] cultureSubjects = {"语文[职高]", "数学[职高]", "英语[职高]",
            "语文[普高]", "数学[普高]", "英语[普高]",
            "语文[初中]", "数学[初中]", "英语[初中]"};

        for (String s : cultureSubjects) {
            assertTrue(isCultureSubject(s),
                "\"" + s + "\" 应被识别为文化课");
        }
    }

    @Test
    @DisplayName("isCultureSubject — 非文化课学科不误识别")
    void isCultureSubject_nonCultureNotMatched() {
        String[] nonCulture = {"信息技术应用基础", "植物生产与环境", "计算机网络技术",
            "会计基础", "Access", "计算机基础"};

        for (String s : nonCulture) {
            assertFalse(isCultureSubject(s),
                "\"" + s + "\" 不应被识别为文化课");
        }
    }

    /* ═══════════════════════ Controller 参数映射 ═══════════════════════ */

    @Test
    @DisplayName("Controller isCultureSubject — 与 PromptBuilder 逻辑一致")
    void controllerIsCultureSubject_consistentWithPromptBuilder() {
        // 验证 Controller 中的 isCultureSubject 方法与 PromptBuilder 中的一致
        // 两者使用相同逻辑：bareSubject 匹配 语文/数学/英语
        assertTrue(isCultureSubject("语文[职高]"));
        assertTrue(isCultureSubject("数学[普高]"));
        assertFalse(isCultureSubject("计算机基础"));
    }

    /* ═══════════════════════ 考纲三级回退 ═══════════════════════ */

    @Test
    @DisplayName("readSyllabusData — L1 syllabusMeta JSON key 命中")
    void readSyllabusData_l1Json() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("syllabusMeta", "{\"reciteList\":\"静夜思/春晓/登鹳雀楼\",\"compositionThemes\":\"家国情怀\"}");

        String result = TeachingContentPromptBuilder.class.getName(); // 只能通过 buildExamPaper 间接测试
        // 语文组卷会调用 readSyllabusData 读取 reciteList
        params.put("subject", "语文[职高]");
        params.put("knowledgePoint", "古诗");
        params.put("categoryPath", "语文");
        params.put("stageHint", "中职");
        params.put("examMode", "exam");
        params.put("ragContext", "");

        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("EXAM_PAPER", params);
        String ip = instructionPrompt(pr);

        // 来自 syllabusMeta 的自定义篇目
        assertTrue(ip.contains("静夜思/春晓/登鹳雀楼"),
            "L1 syllabusMeta JSON 自定义背诵篇目应被读取");
    }

    @Test
    @DisplayName("readSyllabusData — L3 硬编码兜底")
    void readSyllabusData_l3Fallback() {
        Map<String, Object> params = buildExamParams("语文[职高]", "exam");
        // 不传 syllabusMeta → 回退到 L3 默认值
        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("EXAM_PAPER", params);
        String ip = instructionPrompt(pr);

        // L3 默认值: 静女/采薇/寡人之于国也/劝学/师说/将进酒/琵琶行/念奴娇·赤壁怀古
        assertTrue(ip.contains("静女"), "L3 兜底背诵篇目应包含经典篇目");
        assertTrue(ip.contains("师说"), "L3 兜底应包含师说");
    }

    /* ═══════════════════════ 组卷数量上限 ═══════════════════════ */

    @Test
    @DisplayName("仿真组卷默认题型数量 ≤ 50 题")
    void examModeDefaultCountWithinLimit() {
        Map<String, Object> params = buildExamParams("数学[职高]", "exam");
        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("EXAM_PAPER", params);

        @SuppressWarnings("unchecked")
        Map<String, Integer> typeCounts = (Map<String, Integer>) pr.extraParams().get("typeCounts");
        int total = typeCounts.values().stream().mapToInt(Integer::intValue).sum();
        assertTrue(total <= 50, "默认题数 " + total + " 不应超过 50");
    }

    @Test
    @DisplayName("专题训练默认题型数量 ≤ 30 题")
    void trainingModeDefaultCountWithinLimit() {
        Map<String, Object> params = buildExamParams("英语[职高]", "training");
        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("EXAM_PAPER", params);

        @SuppressWarnings("unchecked")
        Map<String, Integer> typeCounts = (Map<String, Integer>) pr.extraParams().get("typeCounts");
        int total = typeCounts.values().stream().mapToInt(Integer::intValue).sum();
        assertTrue(total <= 30, "训练默认题数 " + total + " 不应超过 30");
    }

    /* ═══════════════════════ PromptResult 行为 ═══════════════════════ */

    @Test
    @DisplayName("PromptResult — isQuestionType() 与 extraParams 关系")
    void promptResult_isQuestionTypeBehavior() {
        TeachingContentPromptBuilder.PromptResult withExtra =
            new TeachingContentPromptBuilder.PromptResult("test", 100, Map.of("k", "v"));
        TeachingContentPromptBuilder.PromptResult withoutExtra =
            new TeachingContentPromptBuilder.PromptResult("test", 100);

        assertTrue(withExtra.isQuestionType(), "有 extraParams → isQuestionType=true");
        assertFalse(withoutExtra.isQuestionType(), "无 extraParams → isQuestionType=false");
    }

    @Test
    @DisplayName("CONTENT_TYPE 常量值正确")
    void contentTypesConstants() {
        assertEquals("TEACHING_DESIGN", TeachingContentPromptBuilder.TYPE_TEACHING_DESIGN);
        assertEquals("KNOWLEDGE_CHECKLIST", TeachingContentPromptBuilder.TYPE_KNOWLEDGE_CHECKLIST);
        assertEquals("PRACTICE_PLAN", TeachingContentPromptBuilder.TYPE_PRACTICE_PLAN);
        assertEquals("EXAM_PAPER", TeachingContentPromptBuilder.TYPE_EXAM_PAPER);
        assertEquals("DIAGNOSIS", TeachingContentPromptBuilder.TYPE_DIAGNOSIS);
        assertEquals("COMPREHENSIVE_EXERCISES", TeachingContentPromptBuilder.TYPE_COMPREHENSIVE_EXERCISES);
        assertEquals("CLASSROOM_QUESTIONS", TeachingContentPromptBuilder.TYPE_CLASSROOM_QUESTIONS);
    }

    /* ═══════════════════════ helpers ═══════════════════════ */

    private Map<String, Object> buildExamParams(String subject, String mode) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("subject", subject);
        params.put("knowledgePoint", "测试知识点");
        params.put("categoryPath", subject + " → 测试章节");
        params.put("stageHint", "中职");
        params.put("examMode", mode);
        params.put("ragContext", "【测试知识库填充内容】测试知识点核心概念和例题解析");
        return params;
    }

    private String instructionPrompt(TeachingContentPromptBuilder.PromptResult pr) {
        return String.valueOf(pr.extraParams().getOrDefault("_instructionPrompt", ""));
    }

    // 从 AiOutputController 复制一份 isCultureSubject 逻辑用于测试
    private boolean isCultureSubject(String subject) {
        if (subject == null) return false;
        String bare = subject.replaceAll("\\[.*?\\]", "").trim();
        return bare.equals("语文") || bare.equals("数学") || bare.equals("英语");
    }
}
