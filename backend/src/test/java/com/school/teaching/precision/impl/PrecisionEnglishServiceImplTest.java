package com.school.teaching.precision.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrecisionEnglishServiceImplTest {

    @Mock private PrecisionVocabularyMapper vocabMapper;
    @Mock private PrecisionVocabularySeedMapper seedMapper;
    @Mock private KnowledgeNodeMapper nodeMapper;
    @Mock private QuestionBankMapper questionMapper;
    @Mock private StudentMapper studentMapper;
    @Mock private PrecisionEnglishReadingPassageMapper passageMapper;
    @Mock private PrecisionProgressMapper progressMapper;
    @Mock private WrongQuestionMapper wrongMapper;
    @Mock private PrecisionEnglishQuickTestMapper quickTestMapper;
    @Mock private PrecisionHelper helper;
    @Mock private PlatformTransactionManager transactionManager;

    @InjectMocks
    private PrecisionEnglishServiceImpl englishService;

    // ═══════════ getDashboard ═══════════

    @Test
    @DisplayName("未诊断学生: getDashboard 返回默认 stage=1 且 dailyTask 不为 null")
    void getDashboard_noProfile_shouldReturnDefaults() {
        when(studentMapper.selectById(1L)).thenReturn(studentWithProfile((String) null));
        when(vocabMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(seedMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        Map<String, Object> dash = englishService.getDashboard(1L);

        assertEquals(1, dash.get("stage"));
        assertEquals("🌱 生存词汇", dash.get("stageName"));
        assertEquals(0, dash.get("vocabKnown"));
        assertEquals(0, dash.get("streak"));
        assertNotNull(dash.get("dailyTask"), "dailyTask 不应为 null");
        assertEquals(List.of(), dash.get("achievements"));
    }

    @Test
    @DisplayName("已诊断学生: 画像数据正确回填")
    void getDashboard_withProfile_shouldReturnProfileData() {
        Map<String, Object> eng = makeEngProfile(3, 120, 5, 1);
        when(studentMapper.selectById(1L)).thenReturn(studentWithProfile(eng));
        lenient().when(vocabMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        lenient().when(seedMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        KnowledgeNode n = makeGrammarNode();
        lenient().when(nodeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(n));
        lenient().when(nodeMapper.selectBatchIds(anyList())).thenReturn(List.of(n));

        Map<String, Object> dash = englishService.getDashboard(1L);

        assertEquals(3, dash.get("stage"));
        assertEquals("📦 词汇扩展", dash.get("stageName"));
        assertEquals(120, dash.get("vocabKnown"));
        assertEquals(5, dash.get("streak"));
        assertEquals(1, dash.get("freezeCards"));
    }

    // ═══════════ buildDailyTask ═══════════

    @Test
    @DisplayName("阶段1: 6词汇 0语法")
    void buildDailyTask_stage1_shouldReturnVocabOnly() {
        when(vocabMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(seedMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        Map<String, Object> task = englishService.buildDailyTask(1L, 1, new LinkedHashMap<>());

        assertEquals(6, task.get("totalQuestions"));
        assertEquals(6, task.get("vocabCount"));
        assertEquals(0, task.get("grammarCount"));
    }

    @Test
    @DisplayName("阶段2+: 4词汇 4语法")
    void buildDailyTask_stage2Plus_shouldReturnVocabAndGrammar() {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("grammarNodesUnlocked", List.of(101L, 102L));
        profile.put("grammarNodesMastered", List.of());
        profile.put("grammarNodesSkipped", List.of());
        when(vocabMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(seedMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(nodeMapper.selectById(101L)).thenReturn(makeGrammarNode());
        when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        Map<String, Object> task = englishService.buildDailyTask(1L, 2, profile);

        assertEquals(8, task.get("totalQuestions"));
        assertEquals(4, task.get("vocabCount"));
        assertEquals(4, task.get("grammarCount"));
    }

    // ═══════════ determineStage ═══════════

    @Test
    @DisplayName("词汇<20% → 阶段1（估算<100词）")
    void determineStage_vocabUnder30_shouldReturnStage1() {
        when(helper.getProfileLock(anyLong())).thenReturn(new Object());
        when(studentMapper.selectById(1L)).thenReturn(studentWithProfile((String) null));

        // 1/20=5%×500=25词 → stage=1
        Map<String, Object> result = englishService.determineStage(1L,
            buildDiagnosisAnswers(1, 20, 0, 10));

        assertEquals(1, result.get("stage"));
        assertTrue(((Number) result.get("vocabKnown")).intValue() <= 100);
    }

    @Test
    @DisplayName("词汇80% → 阶段5（估算400词）")
    void determineStage_vocab80to150_shouldReturnStage3() {
        when(helper.getProfileLock(anyLong())).thenReturn(new Object());
        when(studentMapper.selectById(1L)).thenReturn(studentWithProfile((String) null));

        // 12/15=80%×500=400词 → stage=5
        Map<String, Object> result = englishService.determineStage(1L,
            buildDiagnosisAnswers(12, 15, 1, 3));

        assertEquals(5, result.get("stage"));
    }

    // ═══════════ submitDrillAnswer ═══════════

    @Test
    @DisplayName("首次答对 → correct=true hintLevel=0")
    void submitDrillAnswer_firstCorrect_shouldReturnCorrect() {
        QuestionBank q = new QuestionBank();
        q.setId(1L); q.setQuestionType("SINGLE_CHOICE");
        q.setCorrectAnswer("B"); q.setExplanation("解析");
        when(questionMapper.selectById(1L)).thenReturn(q);

        Map<String, Object> result = englishService.submitDrillAnswer(
            1L, 1L, "B", 0, "SINGLE_CHOICE", "英语[职高]", null, null);

        assertEquals(true, result.get("correct"));
        assertEquals(0, result.get("hintLevel"));
        assertNotNull(result.get("message"));
    }

    @Test
    @DisplayName("首次答错→提示1")
    void submitDrillAnswer_firstWrong_shouldReturnHint() {
        QuestionBank q = new QuestionBank();
        q.setId(1L); q.setQuestionType("SINGLE_CHOICE");
        q.setCorrectAnswer("B"); q.setGrammarNodeId(101L);
        KnowledgeNode node = makeGrammarNode();
        when(questionMapper.selectById(1L)).thenReturn(q);
        when(nodeMapper.selectById(101L)).thenReturn(node);

        Map<String, Object> result = englishService.submitDrillAnswer(
            1L, 1L, "A", 0, "SINGLE_CHOICE", "英语[职高]", null, null);

        assertEquals(false, result.get("correct"));
        assertEquals(1, result.get("hintLevel"));
        assertNotNull(result.get("hint"));
    }

    @Test
    @DisplayName("第三次答错→揭示答案(hintLevel=3)")
    void submitDrillAnswer_thirdWrong_shouldRevealAnswer() {
        QuestionBank q = new QuestionBank();
        q.setId(1L); q.setQuestionType("SINGLE_CHOICE");
        q.setCorrectAnswer("B"); q.setExplanation("解析");
        when(questionMapper.selectById(1L)).thenReturn(q);

        Map<String, Object> result = englishService.submitDrillAnswer(
            1L, 1L, "D", 2, "SINGLE_CHOICE", "英语[职高]", null, null);

        assertEquals(false, result.get("correct"));
        assertEquals(3, result.get("hintLevel"));
        assertEquals("B", result.get("correctAnswer"));
    }

    @Test
    @DisplayName("词汇英译中: 答对 → correct=true + seed解释")
    void submitDrillAnswer_vocabEn2cn_correct_shouldWork() {
        PrecisionVocabularySeed seed = new PrecisionVocabularySeed();
        seed.setWord("finish"); seed.setMeaning("完成;结束"); seed.setExample("Please finish your homework.");
        when(seedMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(seed));

        Map<String, Object> result = englishService.submitDrillAnswer(
            1L, -1L, "完成", 0, "FILL_IN", "英语[职高]", "finish", "en2cn");

        assertEquals(true, result.get("correct"));
        assertNotNull(result.get("message"));
    }

    @Test
    @DisplayName("词汇中译英: 答错 → 答案揭示含正确英文 + 音标")
    void submitDrillAnswer_vocabCn2en_wrongFinal_shouldRevealWord() {
        PrecisionVocabularySeed seed = new PrecisionVocabularySeed();
        seed.setWord("finish"); seed.setMeaning("完成"); seed.setPhonetic("/ˈfɪnɪʃ/");
        when(seedMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(seed));

        Map<String, Object> result = englishService.submitDrillAnswer(
            1L, -1L, "wrong", 2, "FILL_IN", "英语[职高]", "finish", "cn2en");

        assertEquals(false, result.get("correct"));
        assertEquals(3, result.get("hintLevel"));
        assertTrue(String.valueOf(result.get("correctAnswer")).contains("finish"));
    }

    // ═══════════ completeDrill ═══════════

    @Test
    @DisplayName("第一组完成→获每日积分")
    void completeDrill_groupSeq1_shouldEarnCredits() {
        when(helper.getProfileLock(anyLong())).thenReturn(new Object());
        when(studentMapper.selectById(1L)).thenReturn(
            studentWithProfile(makeEngProfile(2, 50, 3, 0)));
        lenient().when(vocabMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<Map<String, Object>> answers = List.of(
            Map.of("correct", true, "hintLevel", 0),
            Map.of("correct", true, "hintLevel", 0),
            Map.of("correct", true, "hintLevel", 0),
            Map.of("correct", true, "hintLevel", 0),
            Map.of("correct", true, "hintLevel", 0),
            Map.of("correct", true, "hintLevel", 0)
        );

        Map<String, Object> result = englishService.completeDrill(1L, answers, 1, 300);

        assertEquals(6, result.get("correctCount"));
        assertEquals(6, result.get("totalQuestions"));
        assertNotNull(result.get("streak"));
        assertNotNull(result.get("owlMessage"));
        verify(studentMapper, atLeastOnce()).updateById(any(Student.class));
    }

    // ═══════════ useFreezeCard ═══════════

    @Test
    @DisplayName("无冰冻卡→抛BusinessException")
    void useFreezeCard_noCards_shouldThrow() {
        when(helper.getProfileLock(anyLong())).thenReturn(new Object());
        when(studentMapper.selectById(1L)).thenReturn(
            studentWithProfile(makeEngProfile(1, 10, 3, 0)));

        assertThrows(com.school.teaching.exception.BusinessException.class,
            () -> englishService.useFreezeCard(1L));
    }

    @Test
    @DisplayName("有冰冻卡→消耗1张")
    void useFreezeCard_hasCards_shouldUseOne() {
        when(helper.getProfileLock(anyLong())).thenReturn(new Object());
        when(studentMapper.selectById(1L)).thenReturn(
            studentWithProfile(makeEngProfile(2, 50, 5, 2)));

        Map<String, Object> result = englishService.useFreezeCard(1L);

        assertEquals(1, result.get("freezeCards"));
        assertEquals(5, result.get("streak"));
        verify(studentMapper).updateById(any(Student.class));
    }

    // ═══════════ getGrammarTree ═══════════

    @Test
    @DisplayName("无语法节点 → 返回空列表")
    void getGrammarTree_noNodes_shouldReturnEmpty() {
        when(studentMapper.selectById(1L)).thenReturn(
            studentWithProfile(makeEngProfile(1, 10, 0, 0)));
        when(nodeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<Map<String, Object>> tree = englishService.getGrammarTree(1L);

        assertTrue(tree.isEmpty());
    }

    // ═══════════ freezeCardStatus ═══════════

    @Test
    @DisplayName("冻结卡状态正常返回")
    void freezeCardStatus_shouldReturnCountAndStreak() {
        when(studentMapper.selectById(1L)).thenReturn(
            studentWithProfile(makeEngProfile(2, 60, 7, 1)));

        Map<String, Object> result = englishService.freezeCardStatus(1L);

        assertEquals(1, result.get("freezeCards"));
        assertEquals(7, result.get("streak"));
    }

    // ═══════════ 去重逻辑测试 ═══════════

    @Test
    @DisplayName("buildDailyTask 阶段1: 词汇题目无重复")
    void buildDailyTask_stage1_vocabNoDuplicates() {
        // vocabMapper 返回 PrecisionVocabulary（待复习词汇）
        List<PrecisionVocabulary> dueWords = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            PrecisionVocabulary v = new PrecisionVocabulary();
            v.setId((long) i);
            v.setStudentId(1L);
            v.setWord("review" + i);
            v.setMasterLevel(2);
            dueWords.add(v);
        }
        when(vocabMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(dueWords);
        
        // seedMapper 返回 PrecisionVocabularySeed（新词种子库）
        // 前5个与 dueWords 重叠，测试去重逻辑
        List<PrecisionVocabularySeed> seeds = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            PrecisionVocabularySeed seed = new PrecisionVocabularySeed();
            seed.setId((long) i);
            seed.setWord("review" + i);
            seed.setMeaning("词义" + i);
            seed.setFrequencyRank(i);
            seeds.add(seed);
        }
        when(seedMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(seeds);

        Map<String, Object> task = englishService.buildDailyTask(1L, 1, new LinkedHashMap<>());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> vocabQuestions = (List<Map<String, Object>>) task.get("vocabQuestions");
        Set<String> words = new HashSet<>();
        for (Map<String, Object> q : vocabQuestions) {
            String word = (String) q.get("word");
            assertTrue(words.add(word), "发现重复词汇: " + word);
        }
        assertTrue(vocabQuestions.size() <= 6, "返回数量超过请求数量");
    }

    @Test
    @DisplayName("buildDailyTask 阶段2+: 词汇和语法题目无重复")
    void buildDailyTask_stage2Plus_noDuplicates() {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("grammarNodesUnlocked", List.of(101L, 102L));
        profile.put("grammarNodesMastered", List.of());
        profile.put("grammarNodesSkipped", List.of());
        
        // vocabMapper 返回 PrecisionVocabulary（待复习词汇）
        List<PrecisionVocabulary> dueWords = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            PrecisionVocabulary v = new PrecisionVocabulary();
            v.setId((long) i);
            v.setStudentId(1L);
            v.setWord("review" + i);
            v.setMasterLevel(2);
            dueWords.add(v);
        }
        when(vocabMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(dueWords);
        
        // seedMapper 返回 PrecisionVocabularySeed（新词种子库，前2个与 dueWords 重叠）
        List<PrecisionVocabularySeed> seeds = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            PrecisionVocabularySeed seed = new PrecisionVocabularySeed();
            seed.setId((long) i);
            seed.setWord("review" + i);
            seed.setMeaning("词义" + i);
            seed.setFrequencyRank(i);
            seeds.add(seed);
        }
        when(seedMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(seeds);
        
        when(nodeMapper.selectById(101L)).thenReturn(makeGrammarNode());
        
        // 语法题：8道题，grammarNodeId 匹配 101L
        List<QuestionBank> grammarQs = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            QuestionBank q = new QuestionBank();
            q.setId((long) i);
            q.setQuestionType("SINGLE_CHOICE");
            q.setQuestionText("语法题" + i);
            q.setOptions("{\"A\":\"选项A\",\"B\":\"选项B\",\"C\":\"选项C\",\"D\":\"选项D\"}");
            q.setCorrectAnswer("A");
            q.setGrammarNodeId(101L);
            q.setSubject("英语[职高]");
            q.setStatus(1);
            grammarQs.add(q);
        }
        when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(grammarQs);

        Map<String, Object> task = englishService.buildDailyTask(1L, 2, profile);

        // 验证词汇题目无重复
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> vocabQuestions = (List<Map<String, Object>>) task.get("vocabQuestions");
        Set<String> vocabWords = new HashSet<>();
        for (Map<String, Object> q : vocabQuestions) {
            String word = (String) q.get("word");
            assertTrue(vocabWords.add(word), "发现重复词汇: " + word);
        }
        assertTrue(vocabQuestions.size() <= 4, "词汇数量超过请求数量");

        // 验证语法题目无重复
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> grammarQuestions = (List<Map<String, Object>>) task.get("grammarQuestions");
        Set<Long> grammarIds = new HashSet<>();
        for (Map<String, Object> q : grammarQuestions) {
            Long id = (Long) q.get("questionId");
            assertTrue(grammarIds.add(id), "发现重复语法题ID: " + id);
        }
        assertTrue(grammarQuestions.size() <= 4, "语法数量超过请求数量");
    }

    @Test
    @DisplayName("getStageTest: 题目无重复")
    void getStageTest_noDuplicates() {
        // 准备测试数据
        Long studentId = 1L;
        int stage = 2;
        String testType = "vocab";

        when(helper.getProfileLock(anyLong())).thenReturn(new Object());
        // 模拟学生 profile
        Map<String, Object> eng = makeEngProfile(2, 50, 3, 0);
        when(studentMapper.selectById(1L)).thenReturn(studentWithProfile(eng));

        // 模拟 quickTestMapper 返回测试记录
        List<com.school.teaching.entity.PrecisionEnglishQuickTest> tests = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            com.school.teaching.entity.PrecisionEnglishQuickTest test = new com.school.teaching.entity.PrecisionEnglishQuickTest();
            test.setQuestionId((long) i);
            test.setStage(stage);
            test.setType("VOCAB");
            test.setSortOrder(i);
            tests.add(test);
        }
        when(quickTestMapper.selectList(any())).thenReturn(tests);

        // 模拟题库查询
        List<QuestionBank> questions = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            QuestionBank q = new QuestionBank();
            q.setId((long) i);
            q.setQuestionType("FILL_IN");
            q.setQuestionText("测试题目" + i);
            q.setCorrectAnswer("答案" + i);
            questions.add(q);
        }
        when(questionMapper.selectBatchIds(anyList())).thenReturn(questions);

        // 调用方法
        List<Map<String, Object>> result = englishService.getStageTest(studentId, stage, testType);

        // 验证无重复
        Set<Long> questionIds = new HashSet<>();
        for (Map<String, Object> item : result) {
            Long id = (Long) item.get("questionId");
            assertTrue(questionIds.add(id), "发现重复题目ID: " + id);
        }
    }

    @Test
    @DisplayName("diagnose: 词汇题目无重复")
    void diagnose_vocabNoDuplicates() {
        when(helper.getProfileLock(anyLong())).thenReturn(new Object());
        // 模拟学生 profile
        when(studentMapper.selectById(1L)).thenReturn(studentWithProfile((String) null));

        // 模拟种子词库
        List<PrecisionVocabularySeed> seeds = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            PrecisionVocabularySeed seed = new PrecisionVocabularySeed();
            seed.setWord("word" + i);
            seed.setMeaning("释义" + i);
            seed.setPhonetic("/wɜːrd" + i + "/");
            seed.setFrequencyRank(i);
            seeds.add(seed);
        }
        when(seedMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(seeds);

        // 模拟语法题库
        List<QuestionBank> grammarQuestions = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            QuestionBank q = new QuestionBank();
            q.setId((long) (200 + i));
            q.setQuestionType("SINGLE_CHOICE");
            q.setQuestionText("语法题" + i);
            q.setCorrectAnswer("A");
            q.setGrammarNodeId(101L);
            grammarQuestions.add(q);
        }
        when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(grammarQuestions);

        // 调用方法
        Map<String, Object> result = englishService.diagnose(1L);

        // 验证词汇题无重复
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> vocabQuestions = (List<Map<String, Object>>) result.get("questions");
        Set<String> words = new HashSet<>();
        for (Map<String, Object> q : vocabQuestions) {
            String prompt = (String) q.get("prompt");
            if (prompt != null) {
                assertTrue(words.add(prompt), "发现重复词汇: " + prompt);
            }
        }
    }

    // ═══════════ 辅助方法 ═══════════

    private static Map<String, Object> makeEngProfile(int stage, int vocabKnown, int streak, int freezeCards) {
        Map<String, Object> eng = new LinkedHashMap<>();
        eng.put("stage", stage);
        eng.put("vocabKnown", vocabKnown);
        eng.put("streak", streak);
        eng.put("freezeCards", freezeCards);
        eng.put("longestStreak", 10);
        eng.put("totalPractices", 5);
        eng.put("totalMinutes", 25);
        eng.put("achievements", new ArrayList<>());
        eng.put("creditsEarned", 10);
        eng.put("grammarNodesUnlocked", List.of());
        eng.put("grammarNodesMastered", List.of());
        eng.put("grammarNodesSkipped", List.of());
        eng.put("stageProgress", Map.of("vocab", 0.5, "grammar", 0.0));
        return eng;
    }

    private Student studentWithProfile(Map<String, Object> engProfile) {
        try {
            Map<String, Object> full = Map.of("english", engProfile);
            Student st = new Student();
            st.setId(1L);
            st.setClassId(1L);
            st.setPrecisionProfile(new ObjectMapper().writeValueAsString(full));
            return st;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Student studentWithProfile(String profileJson) {
        Student st = new Student();
        st.setId(1L);
        st.setClassId(1L);
        st.setPrecisionProfile(profileJson);
        return st;
    }

    private KnowledgeNode makeGrammarNode() {
        KnowledgeNode n = new KnowledgeNode();
        n.setId(101L); n.setName("一般现在时");
        n.setGrammarCategory("tense"); n.setUnlockStage(2);
        n.setLevel(4); n.setParentId(1L);
        n.setContent("提示:主语是第三人称单数时动词加-s或-es");
        return n;
    }

    private List<Map<String, Object>> buildDiagnosisAnswers(int vocabCorrect, int vocabTotal,
                                                             int grammarCorrect, int grammarTotal) {
        List<Map<String, Object>> answers = new ArrayList<>();
        for (int i = 0; i < vocabTotal; i++)
            answers.add(Map.of("type", "英译中", "questionType", "FILL_IN", "correct", i < vocabCorrect));
        for (int i = 0; i < grammarTotal; i++)
            answers.add(Map.of("type", "语法选择", "questionType", "SINGLE_CHOICE", "correct", i < grammarCorrect));
        return answers;
    }
}
