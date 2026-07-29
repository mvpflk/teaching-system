package com.school.teaching.precision.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.common.ExamTaskHandler;
import com.school.teaching.entity.ExamSyllabus;
import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.entity.PrecisionProgress;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.entity.Student;
import com.school.teaching.mapper.*;
import com.school.teaching.precision.PrecisionEnglishService;
import com.school.teaching.precision.PrecisionMathService;
import com.school.teaching.service.ExamSyllabusService;
import com.school.teaching.service.SystemService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrecisionDiagnosisServiceTest {

    @Mock private PrecisionProgressMapper progressMapper;
    @Mock private PrecisionVocabularyMapper vocabMapper;
    @Mock private KnowledgeNodeMapper nodeMapper;
    @Mock private QuestionBankMapper questionMapper;
    @Mock private StudentMapper studentMapper;
    @Mock private DictSubjectMapper subjectMapper;
    @Mock private WrongQuestionMapper wrongMapper;
    @Mock private PrecisionEnglishService englishService;
    @Mock private PrecisionMathService mathService;
    @Mock private ExamSyllabusService examSyllabusService;
    @Mock private SystemService systemService;
    @Mock private AutoGroupService autoGroupService;
    @Mock private PrecisionHelper helper;

    @InjectMocks
    private PrecisionDiagnosisService service;

    private static final Long STUDENT_ID = 42L;
    private static final String SUBJECT = "数学[职高]";

    // ═══════════════════════════════════════════
    //  Test data helpers
    // ═══════════════════════════════════════════

    private QuestionBank makeChoice(Long id, String answer, Long catId, String subject) {
        QuestionBank q = new QuestionBank();
        q.setId(id);
        q.setQuestionType("SINGLE_CHOICE");
        q.setCorrectAnswer(answer);
        q.setSubject(subject != null ? subject : SUBJECT);
        q.setCategoryId(catId);
        q.setQuestionText("题" + id + "正文");
        q.setExplanation("解析" + id);
        q.setOptions("[\"A. 选项1\",\"B. 选项2\",\"C. 选项3\",\"D. 选项4\"]");
        q.setStatus(1);
        return q;
    }

    private QuestionBank makeFillIn(Long id, String answer, Long catId, String subject) {
        QuestionBank q = makeChoice(id, answer, catId, subject);
        q.setQuestionType("FILL_IN");
        q.setOptions(null);
        return q;
    }

    private QuestionBank makeTrueFalse(Long id, String answer, Long catId) {
        QuestionBank q = makeChoice(id, answer, catId, null);
        q.setQuestionType("TRUE_FALSE");
        return q;
    }

    private QuestionBank makeEssay(Long id, Long catId) {
        QuestionBank q = makeChoice(id, "", catId, null);
        q.setQuestionType("ESSAY");
        q.setCorrectAnswer(null);
        return q;
    }

    @SafeVarargs
    private final Map<String, Object> answer(Long qid, String answer, String type, Map.Entry<String, Object>... extra) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("questionId", qid);
        a.put("answer", answer);
        a.put("questionType", type != null ? type : "SINGLE_CHOICE");
        a.put("questionText", "题" + qid + "正文");
        a.put("moduleName", "模块1");
        for (Map.Entry<String, Object> e : extra) a.put(e.getKey(), e.getValue());
        return a;
    }

    private static Map.Entry<String, Object> entry(String k, Object v) {
        return new AbstractMap.SimpleEntry<>(k, v);
    }

    private void stubHelperProfileLockAndSave(Object lockObj) {
        when(helper.getProfileLock(anyLong())).thenReturn(lockObj != null ? lockObj : new Object());
        Map<String, Object> emptyProfile = new LinkedHashMap<>();
        when(helper.loadProfileForWrite(anyLong())).thenReturn(emptyProfile);
        doNothing().when(helper).saveProfile(anyLong(), anyMap());
    }

    private void stubProgressAndWrongAnswers() {
        doNothing().when(helper).updateProgressForAnswer(anyLong(), any(), anyString(), anyBoolean());
        doNothing().when(helper).saveWrongAnswer(anyLong(), any(), anyString(), anyBoolean(), anyString());
    }

    private void stubDefaultBoundaryConfig() {
        when(systemService.getIntConfig("remedial.boundary_min", 45)).thenReturn(45);
        when(systemService.getIntConfig("remedial.boundary_max", 55)).thenReturn(55);
    }

    // ═══════════════════════════════════════════
    //  submitDiagnosis tests
    // ═══════════════════════════════════════════

    @Test
    @DisplayName("混合正确/错误答案 → 正确计算得分和分类统计")
    void submitDiagnosis_mixedAnswers_returnsCorrectBreakdown() {
        QuestionBank q1 = makeChoice(1L, "A", 10L, null);
        QuestionBank q2 = makeChoice(2L, "B", 20L, null);
        QuestionBank q3 = makeFillIn(3L, "x=1", 30L, null);
        when(questionMapper.selectBatchIds(Set.of(1L, 2L, 3L)))
            .thenReturn(List.of(q1, q2, q3));

        KnowledgeNode kn20 = new KnowledgeNode(); kn20.setId(20L); kn20.setName("不等式");
        KnowledgeNode kn30 = new KnowledgeNode(); kn30.setId(30L); kn30.setName("方程");
        when(nodeMapper.selectBatchIds(anySet())).thenReturn(List.of(kn20, kn30));

        stubHelperProfileLockAndSave(null);
        stubProgressAndWrongAnswers();

        Student st = new Student(); st.setId(STUDENT_ID); st.setPrecisionProfile(null);
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(st);

        // q1(A→A) correct, q2(B→A) wrong, q3(x=1→"wrong") wrong → 1/3 = 33
        List<Map<String, Object>> answers = List.of(
            answer(1L, "A", "SINGLE_CHOICE"),
            answer(2L, "A", "SINGLE_CHOICE"),
            answer(3L, "wrong answer", "FILL_IN"));

        Map<String, Object> result = service.submitDiagnosis(STUDENT_ID, SUBJECT, answers);
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.get("diagnosisReport");

        assertEquals(1, report.get("correctCount"));
        assertEquals(3, report.get("totalQuestions"));
        assertEquals(33, report.get("score"));

        @SuppressWarnings("unchecked")
        Map<String, Object> tb = (Map<String, Object>) report.get("typeBreakdown");
        assertEquals(Map.of("correct", 1, "total", 2), tb.get("choice"));
        assertEquals(Map.of("correct", 0, "total", 1), tb.get("fillIn"));
        assertEquals(Map.of("pending", 0, "total", 0), tb.get("essay"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> weakItems = (List<Map<String, Object>>) report.get("weakItems");
        assertEquals(2, weakItems.size());
        assertEquals(2L, weakItems.get(0).get("questionId"));
        assertEquals("不等式", weakItems.get(0).get("knowledgeNodeName"));
    }

    @Test
    @DisplayName("全部答对 → score=100, no weakItems, skip auto-group")
    void submitDiagnosis_allCorrect_returns100() {
        QuestionBank q1 = makeChoice(1L, "A", null, null);
        QuestionBank q2 = makeChoice(2L, "B", null, null);
        when(questionMapper.selectBatchIds(Set.of(1L, 2L))).thenReturn(List.of(q1, q2));

        stubHelperProfileLockAndSave(null);
        stubProgressAndWrongAnswers();

        Student st = new Student(); st.setId(STUDENT_ID); st.setPrecisionProfile(null);
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(st);

        List<Map<String, Object>> answers = List.of(
            answer(1L, "A", "SINGLE_CHOICE"),
            answer(2L, "B", "SINGLE_CHOICE"));

        Map<String, Object> result = service.submitDiagnosis(STUDENT_ID, SUBJECT, answers);
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.get("diagnosisReport");

        assertEquals(100, report.get("score"));
        assertEquals(2, report.get("correctCount"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> weak = (List<Map<String, Object>>) report.get("weakItems");
        assertTrue(weak.isEmpty());
        assertFalse(report.containsKey("autoGroupNote"));
    }

    @Test
    @DisplayName("全部答错 → score=0, 入组")
    void submitDiagnosis_allWrong_returns0() {
        QuestionBank q1 = makeChoice(1L, "A", null, null);
        when(questionMapper.selectBatchIds(Set.of(1L))).thenReturn(List.of(q1));

        stubHelperProfileLockAndSave(null);
        stubProgressAndWrongAnswers();
        stubDefaultBoundaryConfig();

        Student st = new Student(); st.setId(STUDENT_ID); st.setPrecisionProfile(null);
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(st);
        when(systemService.getIntConfig("remedial.auto_group_threshold", 50)).thenReturn(50);

        List<Map<String, Object>> answers = List.of(
            answer(1L, "B", "SINGLE_CHOICE"));

        service.submitDiagnosis(STUDENT_ID, SUBJECT, answers);
        verify(autoGroupService).addSingleStudent(STUDENT_ID, SUBJECT, 0);
    }

    @Test
    @DisplayName("空答案列表 → score=0, 不崩溃")
    void submitDiagnosis_emptyAnswers_returns0() {
        when(helper.getProfileLock(anyLong())).thenReturn(new Object());
        Map<String, Object> emptyProfile = new LinkedHashMap<>();
        when(helper.loadProfileForWrite(anyLong())).thenReturn(emptyProfile);
        doNothing().when(helper).saveProfile(anyLong(), anyMap());

        Student st = new Student(); st.setId(STUDENT_ID); st.setPrecisionProfile(null);
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(st);

        Map<String, Object> result = service.submitDiagnosis(STUDENT_ID, SUBJECT, List.of());
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.get("diagnosisReport");

        assertEquals(0, report.get("score"));
        assertEquals(0, report.get("correctCount"));
        assertEquals(0, report.get("totalQuestions"));
    }

    @Test
    @DisplayName("问答题≥3 pending → skip auto-group")
    void submitDiagnosis_essayPendingAtLeast3_skipsAutoGroup() {
        QuestionBank e1 = makeEssay(1L, null);
        QuestionBank e2 = makeEssay(2L, null);
        QuestionBank e3 = makeEssay(3L, null);
        when(questionMapper.selectBatchIds(anySet())).thenReturn(List.of(e1, e2, e3));

        stubHelperProfileLockAndSave(null);
        stubProgressAndWrongAnswers();

        Student st = new Student(); st.setId(STUDENT_ID); st.setPrecisionProfile(null);
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(st);

        // 每个 essay 答案 ≥ 5 chars → pending_review
        List<Map<String, Object>> answers = List.of(
            answer(1L, "这是一段完整的回答内容", "ESSAY"),
            answer(2L, "这是第二段回答内容", "ESSAY"),
            answer(3L, "这是第三段回答内容", "ESSAY"));

        Map<String, Object> result = service.submitDiagnosis(STUDENT_ID, SUBJECT, answers);
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.get("diagnosisReport");

        assertEquals("问答题待评阅≥3道，跳过自动入组，等待教师人工评阅", report.get("autoGroupNote"));
        verify(autoGroupService, never()).addSingleStudent(anyLong(), anyString(), anyInt());
    }

    @Test
    @DisplayName("边界分数 45-55 且全部作答 → skip auto-group")
    void submitDiagnosis_boundaryScore4555_skipsAutoGroup() {
        // 5 题对 2 题 = 40% → below 45, should NOT skip. 调整: 5 题对 3 题 = 60% → above 55.
        // To get score 50: 4 题对 2 题 → 50%
        QuestionBank q1 = makeChoice(1L, "A", null, null);
        QuestionBank q2 = makeChoice(2L, "B", null, null);
        QuestionBank q3 = makeChoice(3L, "C", null, null);
        QuestionBank q4 = makeChoice(4L, "D", null, null);
        when(questionMapper.selectBatchIds(anySet())).thenReturn(List.of(q1, q2, q3, q4));

        stubHelperProfileLockAndSave(null);
        stubProgressAndWrongAnswers();
        stubDefaultBoundaryConfig();

        Student st = new Student(); st.setId(STUDENT_ID); st.setPrecisionProfile(null);
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(st);

        List<Map<String, Object>> answers = List.of(
            answer(1L, "A", "SINGLE_CHOICE"),
            answer(2L, "B", "SINGLE_CHOICE"),
            answer(3L, "X", "SINGLE_CHOICE"),
            answer(4L, "Y", "SINGLE_CHOICE"));

        Map<String, Object> result = service.submitDiagnosis(STUDENT_ID, SUBJECT, answers);
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.get("diagnosisReport");

        assertEquals("诊断分数50处于边界(45-55)且完成率100%，标记待教师人工复核", report.get("autoGroupNote"));
        verify(autoGroupService, never()).addSingleStudent(anyLong(), anyString(), anyInt());
    }

    @Test
    @DisplayName("低分(<threshold) → 自动入组")
    void submitDiagnosis_lowScore_callsAutoGroup() {
        QuestionBank q1 = makeChoice(1L, "A", null, null);
        when(questionMapper.selectBatchIds(Set.of(1L))).thenReturn(List.of(q1));

        stubHelperProfileLockAndSave(null);
        stubProgressAndWrongAnswers();
        stubDefaultBoundaryConfig();

        Student st = new Student(); st.setId(STUDENT_ID); st.setPrecisionProfile(null);
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(st);
        when(systemService.getIntConfig("remedial.auto_group_threshold", 50)).thenReturn(50);

        // 答错 → score=0 < 50
        List<Map<String, Object>> answers = List.of(
            answer(1L, "B", "SINGLE_CHOICE"));

        service.submitDiagnosis(STUDENT_ID, SUBJECT, answers);
        verify(autoGroupService).addSingleStudent(STUDENT_ID, SUBJECT, 0);
    }

    @Test
    @DisplayName("填空题模糊匹配(去标点) → 判对")
    void submitDiagnosis_fillInFuzzy_marksCorrect() {
        // matchFillInAnswer("hello world", "hello, world!") → saExact="helloworld" contains ep="hello" → true
        QuestionBank q1 = makeFillIn(1L, "hello, world!", null, null);
        when(questionMapper.selectBatchIds(Set.of(1L))).thenReturn(List.of(q1));

        stubHelperProfileLockAndSave(null);
        stubProgressAndWrongAnswers();

        Student st = new Student(); st.setId(STUDENT_ID); st.setPrecisionProfile(null);
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(st);

        List<Map<String, Object>> answers = List.of(
            answer(1L, "hello world", "FILL_IN"));

        Map<String, Object> result = service.submitDiagnosis(STUDENT_ID, SUBJECT, answers);
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.get("diagnosisReport");

        assertEquals(100, report.get("score"));
        assertEquals(1, report.get("correctCount"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) report.get("itemResults");
        assertEquals("fuzzy", items.get(0).get("matchMode"));
    }

    @Test
    @DisplayName("TRUE_FALSE 读题 → answerMatch 判断正确")
    void submitDiagnosis_trueFalse_correct() {
        QuestionBank q1 = makeTrueFalse(1L, "A", null);
        when(questionMapper.selectBatchIds(Set.of(1L))).thenReturn(List.of(q1));

        stubHelperProfileLockAndSave(null);
        stubProgressAndWrongAnswers();

        Student st = new Student(); st.setId(STUDENT_ID); st.setPrecisionProfile(null);
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(st);

        // TRUE_FALSE with correctAnswer="A" (true), studentAnswer="A" → correct
        List<Map<String, Object>> answers = List.of(
            answer(1L, "A", "TRUE_FALSE"));

        Map<String, Object> result = service.submitDiagnosis(STUDENT_ID, SUBJECT, answers);
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.get("diagnosisReport");

        assertEquals(100, report.get("score"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) report.get("itemResults");
        assertTrue((Boolean) items.get(0).get("isCorrect"));
        assertEquals("exact", items.get(0).get("matchMode"));
    }

    @Test
    @DisplayName("已有历史诊断 → isFirstDiagnosis=false, 显示分数变化")
    void submitDiagnosis_existingProfile_showsPreviousScore() {
        QuestionBank q1 = makeChoice(1L, "A", null, null);
        when(questionMapper.selectBatchIds(Set.of(1L))).thenReturn(List.of(q1));

        stubHelperProfileLockAndSave(null);
        stubProgressAndWrongAnswers();

        // 已有 precisionProfile JSON 包含该学科上次诊断
        String existingProfile = "{\"数学[职高]\":{\"diagnoseScore\":30,\"lastDiagnoseAt\":\"2026-01-15\"}}";
        Student st = new Student(); st.setId(STUDENT_ID); st.setPrecisionProfile(existingProfile);
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(st);

        // 答对 → score=100
        List<Map<String, Object>> answers = List.of(
            answer(1L, "A", "SINGLE_CHOICE"));

        Map<String, Object> result = service.submitDiagnosis(STUDENT_ID, SUBJECT, answers);
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.get("diagnosisReport");

        assertFalse((Boolean) report.get("isFirstDiagnosis"));
        assertEquals(30, report.get("previousScore"));
        assertEquals(70, report.get("scoreChange")); // 100 - 30
    }

    @Test
    @DisplayName("首次诊断 → isFirstDiagnosis=true")
    void submitDiagnosis_firstDiagnosis_showsTrue() {
        QuestionBank q1 = makeChoice(1L, "A", null, null);
        when(questionMapper.selectBatchIds(Set.of(1L))).thenReturn(List.of(q1));

        stubHelperProfileLockAndSave(null);
        stubProgressAndWrongAnswers();

        Student st = new Student(); st.setId(STUDENT_ID); st.setPrecisionProfile(null);
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(st);

        List<Map<String, Object>> answers = List.of(
            answer(1L, "A", "SINGLE_CHOICE"));

        Map<String, Object> result = service.submitDiagnosis(STUDENT_ID, SUBJECT, answers);
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.get("diagnosisReport");

        assertTrue((Boolean) report.get("isFirstDiagnosis"));
        assertNull(report.get("previousScore"));
        assertNull(report.get("scoreChange"));
    }

    @Test
    @DisplayName("弱项知识点名称正确回填")
    void submitDiagnosis_weakItems_withNodeNames() {
        QuestionBank q1 = makeChoice(1L, "A", 10L, null);
        QuestionBank q2 = makeChoice(2L, "B", 20L, null);
        when(questionMapper.selectBatchIds(Set.of(1L, 2L))).thenReturn(List.of(q1, q2));

        KnowledgeNode kn20 = new KnowledgeNode(); kn20.setId(20L); kn20.setName("数列");
        when(nodeMapper.selectBatchIds(Set.of(20L))).thenReturn(List.of(kn20));

        stubHelperProfileLockAndSave(null);
        stubProgressAndWrongAnswers();

        Student st = new Student(); st.setId(STUDENT_ID); st.setPrecisionProfile(null);
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(st);

        // q1 correct(A→A), q2 wrong(B→A) → weak item q2 with node 20
        List<Map<String, Object>> answers = List.of(
            answer(1L, "A", "SINGLE_CHOICE"),
            answer(2L, "A", "SINGLE_CHOICE"));

        Map<String, Object> result = service.submitDiagnosis(STUDENT_ID, SUBJECT, answers);
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.get("diagnosisReport");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> weak = (List<Map<String, Object>>) report.get("weakItems");

        assertEquals(1, weak.size());
        assertEquals(2L, weak.get(0).get("questionId"));
        assertEquals(20L, weak.get(0).get("knowledgeNodeId"));
        assertEquals("数列", weak.get(0).get("knowledgeNodeName"));
    }

    @Test
    @DisplayName("模块得分按准确率升序排列")
    void submitDiagnosis_moduleScores_sortedByAccuracy() {
        QuestionBank q1 = makeChoice(1L, "A", null, null);
        QuestionBank q2 = makeChoice(2L, "B", null, null);
        when(questionMapper.selectBatchIds(Set.of(1L, 2L))).thenReturn(List.of(q1, q2));

        stubHelperProfileLockAndSave(null);
        stubProgressAndWrongAnswers();

        Student st = new Student(); st.setId(STUDENT_ID); st.setPrecisionProfile(null);
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(st);

        // q1(A→A) correct, q2(B→A) wrong → same module "模块1"
        List<Map<String, Object>> answers = List.of(
            answer(1L, "A", "SINGLE_CHOICE"),
            answer(2L, "A", "SINGLE_CHOICE"));

        Map<String, Object> result = service.submitDiagnosis(STUDENT_ID, SUBJECT, answers);
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.get("diagnosisReport");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modules = (List<Map<String, Object>>) report.get("moduleScores");

        assertEquals(1, modules.size());
        assertEquals("模块1", modules.get(0).get("moduleName"));
        assertEquals(50L, modules.get(0).get("accuracy")); // 1/2 = 50%
    }

    @Test
    @DisplayName("诊断分析包含 level/advice/estimatedScore")
    void submitDiagnosis_analysis_includesLevelAndAdvice() {
        QuestionBank q1 = makeChoice(1L, "A", null, null);
        when(questionMapper.selectBatchIds(Set.of(1L))).thenReturn(List.of(q1));

        stubHelperProfileLockAndSave(null);
        stubProgressAndWrongAnswers();

        Student st = new Student(); st.setId(STUDENT_ID); st.setPrecisionProfile(null);
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(st);

        List<Map<String, Object>> answers = List.of(
            answer(1L, "A", "SINGLE_CHOICE"));

        Map<String, Object> result = service.submitDiagnosis(STUDENT_ID, SUBJECT, answers);
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.get("diagnosisReport");

        assertEquals("优秀", report.get("level"));
        assertNotNull(report.get("advice"));
        assertNotNull(report.get("estimatedScore"));
        assertTrue(report.containsKey("coverage"));
    }

    @Test
    @DisplayName("questionText fallback: answer 空时从 QuestionBank 回填")
    void submitDiagnosis_questionTextFallback() {
        QuestionBank q1 = makeChoice(1L, "A", null, null);
        when(questionMapper.selectBatchIds(Set.of(1L))).thenReturn(List.of(q1));

        stubHelperProfileLockAndSave(null);
        stubProgressAndWrongAnswers();

        Student st = new Student(); st.setId(STUDENT_ID); st.setPrecisionProfile(null);
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(st);

        Map<String, Object> ans = new LinkedHashMap<>();
        ans.put("questionId", 1L);
        ans.put("answer", "A");
        ans.put("questionType", "SINGLE_CHOICE");
        // 不传 questionText → fallback 从 qMap 读取
        ans.put("questionText", "null");

        Map<String, Object> result = service.submitDiagnosis(STUDENT_ID, SUBJECT, List.of(ans));
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.get("diagnosisReport");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) report.get("itemResults");

        assertEquals("题1正文", items.get(0).get("questionText"));
    }

    @Test
    @DisplayName("英语学科触发 determineStage")
    void submitDiagnosis_englishSubject_callsDetermineStage() {
        QuestionBank q1 = makeChoice(1L, "A", null, "英语[职高]");
        when(questionMapper.selectBatchIds(Set.of(1L))).thenReturn(List.of(q1));

        stubHelperProfileLockAndSave(null);
        stubProgressAndWrongAnswers();

        Student st = new Student(); st.setId(STUDENT_ID); st.setPrecisionProfile(null);
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(st);

        List<Map<String, Object>> answers = List.of(
            answer(1L, "A", "SINGLE_CHOICE"));

        service.submitDiagnosis(STUDENT_ID, "英语[职高]", answers);
        verify(englishService).determineStage(STUDENT_ID, answers);
    }

    // ═══════════════════════════════════════════
    //  gradeOneAnswer tests
    // ═══════════════════════════════════════════

    @Test
    @DisplayName("gradeOneAnswer: 题目不存在 → not_found")
    void gradeOneAnswer_questionNotFound() {
        when(questionMapper.selectById(999L)).thenReturn(null);

        Map<String, Object> r = service.gradeOneAnswer(1L, 999L, "A", SUBJECT, "SINGLE_CHOICE");
        assertFalse((Boolean) r.get("correct"));
        assertEquals("not_found", r.get("matchMode"));
        assertEquals("题目不存在", r.get("explanation"));
    }

    @Test
    @DisplayName("gradeOneAnswer: ESSAY ≥5字符 → pending_review")
    void gradeOneAnswer_essayPendingReview() {
        when(questionMapper.selectById(1L)).thenReturn(makeEssay(1L, null));

        Map<String, Object> r = service.gradeOneAnswer(1L, 1L, "这是一段完整回答", SUBJECT, "ESSAY");
        assertNull(r.get("correct"));
        assertEquals("pending_review", r.get("matchMode"));
    }

    @Test
    @DisplayName("gradeOneAnswer: ESSAY <5字符 → unanswered")
    void gradeOneAnswer_essayTooShort() {
        when(questionMapper.selectById(1L)).thenReturn(makeEssay(1L, null));

        Map<String, Object> r = service.gradeOneAnswer(1L, 1L, "hi", SUBJECT, "ESSAY");
        assertNull(r.get("correct"));
        assertEquals("pending_review", r.get("matchMode"));
        assertEquals("请认真作答（至少5个字）。", r.get("explanation"));
    }

    @Test
    @DisplayName("gradeOneAnswer: FILL_IN 正确匹配")
    void gradeOneAnswer_fillInCorrect() {
        QuestionBank q = makeFillIn(1L, "hello", null, null);
        when(questionMapper.selectById(1L)).thenReturn(q);

        Map<String, Object> r = service.gradeOneAnswer(1L, 1L, "Hello", SUBJECT, "FILL_IN");
        assertTrue((Boolean) r.get("correct"));
    }

    @Test
    @DisplayName("gradeOneAnswer: FILL_IN 错误匹配")
    void gradeOneAnswer_fillInWrong() {
        QuestionBank q = makeFillIn(1L, "hello", null, null);
        when(questionMapper.selectById(1L)).thenReturn(q);

        Map<String, Object> r = service.gradeOneAnswer(1L, 1L, "world", SUBJECT, "FILL_IN");
        assertFalse((Boolean) r.get("correct"));
        assertEquals("incorrect", r.get("matchMode"));
    }

    @Test
    @DisplayName("gradeOneAnswer: SINGLE_CHOICE 正确")
    void gradeOneAnswer_choiceCorrect() {
        QuestionBank q = makeChoice(1L, "A", null, null);
        when(questionMapper.selectById(1L)).thenReturn(q);

        Map<String, Object> r = service.gradeOneAnswer(1L, 1L, "A", SUBJECT, "SINGLE_CHOICE");
        assertTrue((Boolean) r.get("correct"));
        assertEquals("exact", r.get("matchMode"));
    }

    @Test
    @DisplayName("gradeOneAnswer: SINGLE_CHOICE 错误")
    void gradeOneAnswer_choiceWrong() {
        QuestionBank q = makeChoice(1L, "A", null, null);
        when(questionMapper.selectById(1L)).thenReturn(q);

        Map<String, Object> r = service.gradeOneAnswer(1L, 1L, "B", SUBJECT, "SINGLE_CHOICE");
        assertFalse((Boolean) r.get("correct"));
        assertEquals("incorrect", r.get("matchMode"));
    }

    // ═══════════════════════════════════════════
    //  getDiagnosis tests
    // ═══════════════════════════════════════════

    @Test
    @DisplayName("getDiagnosis: 英语学科委托英语服务并透传结果")
    void getDiagnosis_englishSubject_delegatesToEnglishService() {
        Map<String, Object> mockResult = Map.of("score", 75, "level", "发展中");
        when(englishService.diagnose(STUDENT_ID)).thenReturn(mockResult);

        Map<String, Object> result = service.getDiagnosis(STUDENT_ID, "英语[职高]");
        assertSame(mockResult, result);
        verify(englishService).diagnose(STUDENT_ID);
    }

    @Test
    @DisplayName("getDiagnosis: 数学学科调用数学服务并注入考纲标题")
    void getDiagnosis_mathSubject_injectsSyllabus() {
        Map<String, Object> mathResult = new LinkedHashMap<>(Map.of("score", 80));
        when(mathService.diagnose(STUDENT_ID)).thenReturn(mathResult);
        when(helper.getSubjectId("数学[职高]")).thenReturn(22L);

        ExamSyllabus s = new ExamSyllabus();
        s.setTitle("函数与导数");
        when(examSyllabusService.getSyllabiByNodeId(22L)).thenReturn(List.of(s));

        Map<String, Object> result = service.getDiagnosis(STUDENT_ID, "数学[职高]");
        assertEquals(80, result.get("score"));
        assertEquals("函数与导数", result.get("syllabusTitle"));
        verify(mathService).diagnose(STUDENT_ID);
    }

    @Test
    @DisplayName("getDiagnosis: 数学学科考纲注入失败不阻塞")
    void getDiagnosis_mathSubject_syllabusFailsGracefully() {
        Map<String, Object> mathResult = new LinkedHashMap<>(Map.of("score", 80));
        when(mathService.diagnose(STUDENT_ID)).thenReturn(mathResult);
        when(helper.getSubjectId("数学[职高]")).thenThrow(new RuntimeException("DB fail"));

        Map<String, Object> result = service.getDiagnosis(STUDENT_ID, "数学[职高]");
        assertEquals(80, result.get("score"));
        assertNull(result.get("syllabusTitle"));
    }

    @Test
    @DisplayName("getDiagnosis: 题库充足返回15题")
    void getDiagnosis_sufficientPool_returns15Questions() {
        List<QuestionBank> pool = new ArrayList<>();
        for (long i = 1; i <= 20; i++) {
            pool.add(makeChoice(i, "A", i, "信息技术应用基础[职高]"));
        }
        when(questionMapper.selectList(any())).thenReturn(pool);

        Map<String, Object> result = service.getDiagnosis(STUDENT_ID, "信息技术应用基础[职高]");
        assertEquals(15, result.get("totalQuestions"));
        assertEquals(15, ((List<?>) result.get("questions")).size());
    }

    @Test
    @DisplayName("getDiagnosis: 题库不足扩大搜索范围（上限15题）")
    void getDiagnosis_poolUnder15_broadensSearch() {
        List<QuestionBank> initial = new ArrayList<>();
        for (long i = 1; i <= 8; i++) {
            initial.add(makeChoice(i, "A", i, "信息技术应用基础[职高]"));
        }
        List<QuestionBank> extra = new ArrayList<>();
        for (long i = 9; i <= 16; i++) {
            extra.add(makeChoice(i, "A", i, "信息技术应用基础[职高]"));
        }
        when(questionMapper.selectList(any())).thenReturn(initial, extra);

        Map<String, Object> result = service.getDiagnosis(STUDENT_ID, "信息技术应用基础[职高]");
        assertEquals(15, result.get("totalQuestions"), "扩大后16题，上限15");
        assertNull(result.get("warning"));
        verify(questionMapper, times(2)).selectList(any());
    }

    @Test
    @DisplayName("getDiagnosis: 扩大后仍不足5题返回警告")
    void getDiagnosis_poolStillUnder5_returnsWarning() {
        List<QuestionBank> initial = new ArrayList<>();
        for (long i = 1; i <= 2; i++) {
            initial.add(makeChoice(i, "A", i, "稀有学科[职高]"));
        }
        List<QuestionBank> extra = new ArrayList<>();
        for (long i = 3; i <= 4; i++) {
            extra.add(makeChoice(i, "A", i, "稀有学科"));
        }
        when(questionMapper.selectList(any())).thenReturn(initial, extra);

        Map<String, Object> result = service.getDiagnosis(STUDENT_ID, "稀有学科[职高]");
        assertEquals(0, result.get("totalQuestions"));
        assertTrue(((String) result.get("warning")).contains("题库题目不足"));
    }

    @Test
    @DisplayName("getDiagnosis: 阶段标签过滤仅保留精确匹配")
    void getDiagnosis_stageTagFilter_usesExactMatch() {
        List<QuestionBank> pool = new ArrayList<>();
        for (long i = 1; i <= 12; i++) {
            pool.add(makeChoice(i, "A", i, "计算机基础"));
        }
        for (long i = 13; i <= 20; i++) {
            pool.add(makeChoice(i, "A", i, "计算机基础[职高]"));
        }
        when(questionMapper.selectList(any())).thenReturn(pool);

        Map<String, Object> result = service.getDiagnosis(STUDENT_ID, "计算机基础[职高]");
        assertEquals(8, ((List<?>) result.get("questions")).size());
    }

    @Test
    @DisplayName("getDiagnosis: TRUE_FALSE 缺省选项自动注入")
    void getDiagnosis_trueFalseWithoutOptions_injectsDefaultOptions() {
        List<QuestionBank> pool = new ArrayList<>();
        for (long i = 1; i <= 14; i++) {
            pool.add(makeChoice(i, "A", i, "道德法治[职高]"));
        }
        QuestionBank tf = makeChoice(15L, "A", 99L, "道德法治[职高]");
        tf.setQuestionType("TRUE_FALSE");
        tf.setOptions(null);
        pool.add(tf);
        when(questionMapper.selectList(any())).thenReturn(pool);

        Map<String, Object> result = service.getDiagnosis(STUDENT_ID, "道德法治[职高]");
        List<?> questions = (List<?>) result.get("questions");
        Map<String, Object> tfQuestion = (Map<String, Object>) questions.stream()
            .map(q -> (Map<String, Object>) q)
            .filter(q -> "TRUE_FALSE".equals(q.get("questionType")))
            .findFirst().orElseThrow(() -> new AssertionError("未找到 TRUE_FALSE 题目"));
        assertEquals(List.of("A. √", "B. ×"), tfQuestion.get("options"));
    }

    @Test
    @DisplayName("getDiagnosis: 通用路径成功注入考纲标题")
    void getDiagnosis_generalPath_injectsSyllabusTitle() {
        List<QuestionBank> pool = new ArrayList<>();
        for (long i = 1; i <= 15; i++) {
            pool.add(makeChoice(i, "A", i, "语文[职高]"));
        }
        when(questionMapper.selectList(any())).thenReturn(pool);
        when(helper.getSubjectId("语文[职高]")).thenReturn(30L);
        ExamSyllabus s = new ExamSyllabus();
        s.setTitle("文言文阅读");
        when(examSyllabusService.getSyllabiByNodeId(30L)).thenReturn(List.of(s));

        Map<String, Object> result = service.getDiagnosis(STUDENT_ID, "语文[职高]");
        assertEquals("文言文阅读", result.get("syllabusTitle"));
    }

    @Test
    @DisplayName("getDiagnosis: 无考纲时不写入 syllabusTitle")
    void getDiagnosis_generalPath_noSyllabus_omitsTitle() {
        List<QuestionBank> pool = new ArrayList<>();
        for (long i = 1; i <= 15; i++) {
            pool.add(makeChoice(i, "A", i, "语文[职高]"));
        }
        when(questionMapper.selectList(any())).thenReturn(pool);
        when(helper.getSubjectId("语文[职高]")).thenReturn(30L);
        when(examSyllabusService.getSyllabiByNodeId(30L)).thenReturn(List.of());

        Map<String, Object> result = service.getDiagnosis(STUDENT_ID, "语文[职高]");
        assertNull(result.get("syllabusTitle"));
    }

    // ═══════════════════════════════════════════
    //  getDashboard tests
    // ═══════════════════════════════════════════

    @Test
    @DisplayName("getDashboard: 无画像返回空数据")
    void getDashboard_noProfile() {
        Student st = new Student(); st.setId(STUDENT_ID); st.setPrecisionProfile(null);
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(st);
        when(vocabMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        Map<String, Object> dash = service.getDashboard(STUDENT_ID);
        assertNotNull(dash.get("english"));
        assertNotNull(dash.get("math"));
        assertNotNull(dash.get("profile"));
    }

    @Test
    @DisplayName("getDashboard: 有画像返回画像数据")
    void getDashboard_withProfile() {
        String profileJson = "{\"英语[职高]\":{\"streakWeeks\":5}}";
        Student st = new Student(); st.setId(STUDENT_ID); st.setPrecisionProfile(profileJson);
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(st);
        when(vocabMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L);

        List<PrecisionProgress> emptyProgress = List.of();
        when(progressMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(emptyProgress);

        Map<String, Object> dash = service.getDashboard(STUDENT_ID);
        @SuppressWarnings("unchecked")
        Map<String, Object> eng = (Map<String, Object>) dash.get("english");
        assertEquals(10L, eng.get("vocabTotal"));

        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) dash.get("profile");
        assertNotNull(profile.get("英语[职高]"));
    }
}
