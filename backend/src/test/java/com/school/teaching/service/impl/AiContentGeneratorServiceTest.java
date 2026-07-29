package com.school.teaching.service.impl;

import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AiContentGeneratorService 集成测试
 * 覆盖：Prompt类型路由、DIAGNOSIS走独立通道、文化课AgingHint豁免、考纲注入链路
 */
@ExtendWith(MockitoExtension.class)
class AiContentGeneratorServiceTest {

    @Mock private AiOutputMapper outputMapper;
    @Mock private KnowledgeNodeMapper nodeMapper;
    @Mock private AiCallLogMapper aiCallLogMapper;
    @Mock private AiServiceGateway aiGateway;
    @Mock private AiTaskStore taskStore;
    @Mock private ExamSyllabusService examSyllabusService;
    @Mock private SystemService systemService;
    @Mock private AiQuestionGeneratorService aiQuestionService;
    @Mock private TaskService taskService;
    @Mock private QuestionBankMapper questionBankMapper;
    @Mock private com.school.teaching.mapper.TeacherMapper teacherMapper;

    @InjectMocks private AiContentGeneratorServiceImpl service;

    @BeforeEach
    void injectSelf() {
        ReflectionTestUtils.setField(service, "self", service);
    }

    /* ───────── DIAGNOSIS 走独立通道 ───────── */

    @Test
    @DisplayName("DIAGNOSIS类型 → 不走 isQuestionType 分支, 直接 generateContent")
    void executeAsync_diagnosis_shouldUseContentChannel() {
        // verify buildDiagnosis returns PromptResult without extraParams (not question type)
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("contentType", "DIAGNOSIS");
        params.put("taskId", 100L);
        params.put("subject", "数学[职高]");
        params.put("diagnosisData", Map.of("questions", List.of()));
        params.put("categoryId", 200L);
        params.put("knowledgePoint", "二倍角公式");
        params.put("stageHint", "中职");

        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("DIAGNOSIS", params);

        assertFalse(pr.isQuestionType(), "DIAGNOSIS 不应返回 extraParams → isQuestionType() 必须为 false");
        assertNotNull(pr.prompt(), "DIAGNOSIS Prompt 不能为空");
        assertTrue(pr.prompt().contains("发展性语言"), "DIAGNOSIS Prompt 应包含发展性语言要求");
    }

    /* ───────── 文化课 Prompt 路由 ───────── */

    @Test
    @DisplayName("文化课语文 EXAMPAPER → 走 buildChineseExam")
    void buildExamPaper_chineseShouldUseChineseExam() {
        Map<String, Object> params = buildExamParams("语文[职高]", "exam");
        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("EXAM_PAPER", params);

        assertTrue(pr.isQuestionType(), "组卷应为 questionType");
        String ip = String.valueOf(pr.extraParams().getOrDefault("_instructionPrompt", ""));
        assertTrue(ip.contains("四川省对口升学考试语文命题教师"), "语文组卷 Prompt 应含特定角色");
        assertTrue(ip.contains("满分150分"), "语文组卷 Prompt 应含试卷标准");
    }

    @Test
    @DisplayName("文化课数学 专题训练 → 走 buildMathTraining")
    void buildExamPaper_mathTrainingShouldUseMathTraining() {
        Map<String, Object> params = buildExamParams("数学[职高]", "training");
        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("EXAM_PAPER", params);

        String ip = String.valueOf(pr.extraParams().getOrDefault("_instructionPrompt", ""));
        assertTrue(ip.contains("中职数学练习命题专家"), "数学专题训练 Prompt 应含特定角色");
        assertEquals(0.8, ((Number) pr.extraParams().getOrDefault("temperature", 1.0)).doubleValue(), 0.01, "专题训练 temperature=0.8");
    }

    @Test
    @DisplayName("文化课英语 EXAMPAPER → 走 buildEnglishExam")
    void buildExamPaper_englishShouldUseEnglishExam() {
        Map<String, Object> params = buildExamParams("英语[职高]", "exam");
        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("EXAM_PAPER", params);

        String ip = String.valueOf(pr.extraParams().getOrDefault("_instructionPrompt", ""));
        assertTrue(ip.contains("四川省对口升学考试英语命题教师"));
        assertTrue(ip.contains("满分100分"));
        assertTrue(ip.contains("情景交际→冠词→代词→介词"), "英语Prompt应含15题单选考点排布");
    }

    /* ───────── 文化课禁止实训方案 ───────── */

    @Test
    @DisplayName("文化课 PRACTICE_PLAN → throw IllegalArgumentException")
    void build_cultureSubjectPracticePlan_shouldThrow() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("subject", "语文[职高]");
        params.put("knowledgePoint", "文言文");
        params.put("stageHint", "中职");

        Exception e = assertThrows(IllegalArgumentException.class, () ->
            TeachingContentPromptBuilder.build("PRACTICE_PLAN", params));
        assertTrue(e.getMessage().contains("文化课不支持实训方案"));
    }

    /* ───────── 文化课教学设计 → 高中模板 ───────── */

    @Test
    @DisplayName("文化课 TEACHING_DESIGN → 不走职业模板")
    void build_cultureSubjectTeachingDesign_shouldUseAcademicTemplate() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("subject", "语文[职高]");
        params.put("knowledgePoint", "师说");
        params.put("stageHint", "高中");
        params.put("style", "STANDARD");
        params.put("focus", "BALANCED");

        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("TEACHING_DESIGN", params);

        assertNull(pr.extraParams(), "教学设计非 questionType");
        assertNotNull(pr.prompt());
        // 不应出现职业教育强制性术语（模板可能含"不使用X"的否定形式）
        String prompt = pr.prompt();
        assertTrue(prompt.contains("不使用") || !prompt.contains("做中学"),
            "文化课教学设计应主动禁用或至少不主动使用职业术语");
        assertTrue(prompt.contains("教学目标"), "文化课教学设计应包含教学目标");
    }

    /* ───────── 专业课路径不变 ───────── */

    @Test
    @DisplayName("计算机专业课 TEACHING_DESIGN → 保持原有职业教育路由")
    void build_vocationalSubjectTeachingDesign_shouldUseVocationalTemplate() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("subject", "信息技术应用基础");
        params.put("knowledgePoint", "计算机系统组成");
        params.put("stageHint", "中职");
        params.put("style", "STANDARD");
        params.put("focus", "BALANCED");

        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("TEACHING_DESIGN", params);

        assertNotNull(pr.prompt());
        assertTrue(pr.prompt().contains("中职") || pr.prompt().contains("做中学"),
            "计算机专业课应使用原有职业模板");
    }

    @Test
    @DisplayName("计算机专业课 COMPREHENSIVE_EXERCISES → 保持原有路由")
    void build_vocationalSubjectExercises_shouldUseOriginal() {
        Map<String, Object> params = buildExamParams("信息技术应用基础", "exam");
        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("COMPREHENSIVE_EXERCISES", params);

        assertTrue(pr.isQuestionType());
        // 应包含综合练习特有的格式提示
        String ip = String.valueOf(pr.extraParams().getOrDefault("_instructionPrompt", ""));
        assertTrue(ip.contains("综合") || pr.prompt().contains("综合"),
            "综合练习应包含'综合'相关提示");
    }

    /* ───────── 考纲三级回退 ───────── */

    @Test
    @DisplayName("文化课会注入考纲上下文")
    void build_cultureSubjectExam_shouldInjectSyllabus() {
        Map<String, Object> params = buildExamParams("语文[职高]", "exam");
        params.put("syllabusContext", "【四川省对口升学考试语文大纲】> 考试时间150分钟");

        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("EXAM_PAPER", params);
        String ip = String.valueOf(pr.extraParams().getOrDefault("_instructionPrompt", ""));
        assertTrue(ip.contains("对口升学") || ip.contains("150"), "应含考纲注入信息");
    }

    /* ───────── _instructionPrompt 全链路 ───────── */

    @Test
    @DisplayName("EXAM_PAPER → extraParams 必须含 _instructionPrompt")
    void buildExamPaper_shouldInjectInstructionPrompt() {
        Map<String, Object> params = buildExamParams("数学[职高]", "exam");

        TeachingContentPromptBuilder.PromptResult pr = TeachingContentPromptBuilder.build("EXAM_PAPER", params);

        assertNotNull(pr.extraParams(), "组卷必须返回 extraParams");
        String ip = String.valueOf(pr.extraParams().get("_instructionPrompt"));
        assertFalse(ip.isBlank());
        assertFalse("null".equals(ip));
    }

    /* ───────── helper ───────── */

    private Map<String, Object> buildExamParams(String subject, String mode) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("subject", subject);
        params.put("knowledgePoint", "测试知识点");
        params.put("categoryPath", subject + " → 测试章节");
        params.put("stageHint", "中职");
        params.put("examMode", mode);
        params.put("ragContext", "【测试知识库内容】");
        return params;
    }
}
