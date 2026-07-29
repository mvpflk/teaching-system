package com.school.teaching.service.impl;

import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.AiQuestionGeneratorService;
import com.school.teaching.service.SystemService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WrongQuestionServiceImplTest {

    @Mock private WrongQuestionMapper wrongMapper;
    @Mock private QuestionBankMapper questionMapper;
    @Mock private StudentAnswerMapper studentAnswerMapper;
    @Mock private TaskSubmissionMapper taskSubmissionMapper;
    @Mock private TaskMapper taskMapper;
    @Mock private PracticeSessionMapper sessionMapper;
    @Mock private PracticeSessionItemMapper itemMapper;
    @Mock private KnowledgeNodeMapper nodeMapper;
    @Mock private StudentMapper studentMapper;
    @Mock private UserMapper userMapper;
    @Mock private ClassesMapper classesMapper;
    @Mock private SystemService systemService;
    @Mock private AiQuestionGeneratorService aiService;

    @InjectMocks private WrongQuestionServiceImpl wrongService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(wrongService, "self", wrongService);
    }

    @Test
    void markMasteredWithSource_shouldSetSource() {
        WrongQuestion wq = new WrongQuestion();
        wq.setId(1L); wq.setStudentId(1L); wq.setIsMastered(0);
        when(wrongMapper.selectById(1L)).thenReturn(wq);
        wrongService.markMasteredWithSource(1L, 1L, "derived");
        assertEquals(1, wq.getIsMastered());
        assertEquals("derived", wq.getMasteredSource());
        assertNotNull(wq.getMasteredAt());
        verify(wrongMapper).updateById(wq);
    }

    @Test
    void markMasteredWithSource_shouldThrowOnWrongStudent() {
        WrongQuestion wq = new WrongQuestion(); wq.setId(1L); wq.setStudentId(2L);
        when(wrongMapper.selectById(1L)).thenReturn(wq);
        assertThrows(BusinessException.class, () -> wrongService.markMasteredWithSource(1L, 1L, "manual"));
    }

    @Test
    void recordPractice_shouldIncrementCount() {
        WrongQuestion wq = new WrongQuestion(); wq.setId(1L); wq.setStudentId(1L); wq.setPracticeCount(3);
        when(wrongMapper.selectById(1L)).thenReturn(wq);
        wrongService.recordPractice(1L, 1L);
        assertEquals(4, wq.getPracticeCount());
        assertNotNull(wq.getLastPracticeTime());
        verify(wrongMapper).updateById(wq);
    }

    @Test
    void recordPractice_shouldIgnoreMissing() {
        when(wrongMapper.selectById(999L)).thenReturn(null);
        assertDoesNotThrow(() -> wrongService.recordPractice(999L, 1L));
    }

    @Test
    void submitPractice_correct_shouldAdvanceSpacing() {
        WrongQuestion wq = new WrongQuestion();
        wq.setId(1L); wq.setStudentId(1L); wq.setQuestionId(10L); wq.setWrongCount(2);
        QuestionBank qb = new QuestionBank();
        qb.setId(10L); qb.setCorrectAnswer("A"); qb.setQuestionType("SINGLE_CHOICE");
        when(wrongMapper.selectById(1L)).thenReturn(wq);
        when(questionMapper.selectById(10L)).thenReturn(qb);

        Map<String, Object> result = wrongService.submitPractice(1L, "A", 1L);

        assertTrue((Boolean) result.get("isCorrect"));
        // 新遗忘曲线：第1次答对进入第1轮复习，不立即标记掌握（需5轮）
        assertNull(wq.getIsMastered(), "第1次答对不应标记掌握");
        assertEquals(1, wq.getMasteredStreak().intValue(), "masteredStreak=1");
        assertEquals(1, wq.getConsecutiveCorrect().intValue());
        assertNotNull(wq.getNextReviewAt(), "应有复习计划");
        // practiceCount 在 submitPractice 和 applySpacedRepetition 中各递增一次
        assertEquals(2, wq.getPracticeCount().intValue());
    }

    @Test
    void submitPractice_wrong_shouldIncrementCount() {
        WrongQuestion wq = new WrongQuestion();
        wq.setId(1L); wq.setStudentId(1L); wq.setQuestionId(10L); wq.setWrongCount(0);
        QuestionBank qb = new QuestionBank();
        qb.setId(10L); qb.setCorrectAnswer("B"); qb.setQuestionType("SINGLE_CHOICE");
        when(wrongMapper.selectById(1L)).thenReturn(wq);
        when(questionMapper.selectById(10L)).thenReturn(qb);

        Map<String, Object> result = wrongService.submitPractice(1L, "A", 1L);

        assertFalse((Boolean) result.get("isCorrect"));
        // applySpacedRepetition 内部已递增 wrongCount（0→1）
        assertEquals(1, wq.getWrongCount().intValue());
        assertNull(wq.getIsMastered());
        // practiceCount 在 submitPractice 和 applySpacedRepetition 中各递增一次（null→1→2）
        assertEquals(2, wq.getPracticeCount().intValue());
    }

    @Test
    void deleteWrongQuestion_shouldDelete() {
        WrongQuestion wq = new WrongQuestion(); wq.setId(1L); wq.setStudentId(1L);
        when(wrongMapper.selectById(1L)).thenReturn(wq);
        wrongService.deleteWrongQuestion(1L, 1L);
        verify(wrongMapper).deleteById(1L);
    }

    @Test
    void deleteWrongQuestion_shouldThrow() {
        WrongQuestion wq = new WrongQuestion(); wq.setId(1L); wq.setStudentId(2L);
        when(wrongMapper.selectById(1L)).thenReturn(wq);
        assertThrows(BusinessException.class, () -> wrongService.deleteWrongQuestion(1L, 1L));
    }

    @Test
    void getPracticeList_shouldWork() {
        WrongQuestion wq = new WrongQuestion();
        wq.setId(1L); wq.setStudentId(1L); wq.setQuestionId(10L); wq.setWrongCount(1);
        when(wrongMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(wq)));
        QuestionBank qb = new QuestionBank();
        qb.setId(10L); qb.setQuestionText("题"); qb.setQuestionType("SINGLE_CHOICE");
        when(questionMapper.selectBatchIds(any())).thenReturn(new ArrayList<>(List.of(qb)));

        List<Map<String, Object>> result = wrongService.getPracticeList(1L);
        assertEquals(1, result.size());
    }

    @Test
    void getStudentStats_shouldNotThrow() {
        assertDoesNotThrow(() -> wrongService.getStudentStats(1L));
    }

    @Test
    void listWrongQuestions_shouldFilterByMastered() {
        when(wrongMapper.selectCount(any())).thenReturn(2L, 1L);
        when(wrongMapper.selectList(any())).thenReturn(new ArrayList<>());

        Map<String, Object> result = wrongService.listWrongQuestions(1L, 1, 1, 20, "");
        assertEquals(2L, result.get("total"));
        assertEquals(1L, result.get("masteredCount"));
    }

    @Test
    void markUnmastered_shouldClearMastered() {
        WrongQuestion wq = new WrongQuestion(); wq.setId(1L); wq.setStudentId(1L); wq.setIsMastered(1);
        when(wrongMapper.selectById(1L)).thenReturn(wq);
        wrongService.markUnmastered(1L, 1L);
        assertEquals(0, wq.getIsMastered().intValue());
        verify(wrongMapper).updateById(wq);
    }

    // ═══════════ applySpacedRepetition 单元测试 ═══════════

    private void applySpacedRepetition(WrongQuestion wq, boolean correct, String source) {
        ReflectionTestUtils.invokeMethod(
            wrongService, "applySpacedRepetition", wq, correct, source);
    }

    @Test
    @DisplayName("第1次答对 → nextReviewAt = now + 1天, masteredStreak=1")
    void spacedRep_firstCorrect_reviewIn1Day() {
        WrongQuestion wq = new WrongQuestion();
        wq.setId(1L);
        LocalDateTime before = LocalDateTime.now();

        applySpacedRepetition(wq, true, "single_practice");

        assertNotNull(wq.getNextReviewAt());
        long diffHours = java.time.Duration.between(before, wq.getNextReviewAt()).toHours();
        assertTrue(diffHours >= 20 && diffHours <= 28,
            "预期+1天, 实际差" + diffHours + "小时");
        assertEquals(1, wq.getMasteredStreak().intValue());
        assertEquals(1, wq.getConsecutiveCorrect().intValue());
        assertNull(wq.getIsMastered(), "第1次不应标记掌握");
    }

    @Test
    @DisplayName("第2次答对 → nextReviewAt = now + 3天")
    void spacedRep_secondCorrect_reviewIn3Days() {
        WrongQuestion wq = new WrongQuestion();
        wq.setId(1L);
        wq.setMasteredStreak(1);
        wq.setConsecutiveCorrect(1);
        LocalDateTime before = LocalDateTime.now();

        applySpacedRepetition(wq, true, "single_practice");

        long diffHours = java.time.Duration.between(before, wq.getNextReviewAt()).toHours();
        assertTrue(diffHours >= 60 && diffHours <= 80, "预期+3天");
        assertEquals(2, wq.getMasteredStreak().intValue());
        assertNull(wq.getIsMastered());
    }

    @Test
    @DisplayName("第3次答对 → nextReviewAt = now + 7天")
    void spacedRep_thirdCorrect_reviewIn7Days() {
        WrongQuestion wq = new WrongQuestion();
        wq.setId(1L);
        wq.setMasteredStreak(2);
        wq.setConsecutiveCorrect(2);
        LocalDateTime before = LocalDateTime.now();

        applySpacedRepetition(wq, true, "single_practice");

        long diffHours = java.time.Duration.between(before, wq.getNextReviewAt()).toHours();
        assertTrue(diffHours >= 155 && diffHours <= 180, "预期+7天");
        assertEquals(3, wq.getMasteredStreak().intValue());
        assertNull(wq.getIsMastered());
    }

    @Test
    @DisplayName("第4次答对 → nextReviewAt = now + 14天")
    void spacedRep_fourthCorrect_reviewIn14Days() {
        WrongQuestion wq = new WrongQuestion();
        wq.setId(1L);
        wq.setMasteredStreak(3);
        wq.setConsecutiveCorrect(3);
        LocalDateTime before = LocalDateTime.now();

        applySpacedRepetition(wq, true, "single_practice");

        long diffHours = java.time.Duration.between(before, wq.getNextReviewAt()).toHours();
        assertTrue(diffHours >= 320 && diffHours <= 350, "预期+14天");
        assertEquals(4, wq.getMasteredStreak().intValue());
        assertNull(wq.getIsMastered());
    }

    @Test
    @DisplayName("第5次答对 → 永久掌握: isMastered=1, nextReviewAt=null")
    void spacedRep_fifthCorrect_shouldMaster() {
        WrongQuestion wq = new WrongQuestion();
        wq.setId(1L);
        wq.setMasteredStreak(4);
        wq.setConsecutiveCorrect(4);
        wq.setIsMastered(0);

        applySpacedRepetition(wq, true, "single_practice");

        assertEquals(1, wq.getIsMastered().intValue());
        assertNull(wq.getNextReviewAt());
        assertEquals(5, wq.getMasteredStreak().intValue());
        assertEquals("single_practice", wq.getMasteredSource());
        assertNotNull(wq.getMasteredAt());
    }

    @Test
    @DisplayName("答错 → 重置进度: masteredStreak=0, nextReviewAt=null")
    void spacedRep_wrong_shouldReset() {
        WrongQuestion wq = new WrongQuestion();
        wq.setId(1L);
        wq.setMasteredStreak(3);
        wq.setConsecutiveCorrect(3);
        wq.setNextReviewAt(LocalDateTime.now().plusDays(3));
        wq.setWrongCount(0);
        wq.setIsMastered(1);
        wq.setMasteredAt(LocalDateTime.now().minusDays(1));
        wq.setMasteredSource("single_practice");

        applySpacedRepetition(wq, false, null);

        assertEquals(0, wq.getMasteredStreak().intValue());
        assertEquals(0, wq.getConsecutiveCorrect().intValue());
        assertNull(wq.getNextReviewAt());
        assertEquals(1, wq.getWrongCount().intValue());
        assertEquals(0, wq.getIsMastered().intValue());
        assertNull(wq.getMasteredAt());
        assertNull(wq.getMasteredSource());
    }

    @Test
    @DisplayName("null wq → 抛IllegalArgumentException")
    void spacedRep_nullWq_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
            () -> applySpacedRepetition(null, true, "single_practice"));
    }

    @Test
    @DisplayName("masteredStreak/consecutiveCorrect为null → 正常处理")
    void spacedRep_nullFields_shouldHandleGracefully() {
        WrongQuestion wq = new WrongQuestion();
        wq.setId(1L);

        applySpacedRepetition(wq, true, "single_practice");

        assertNotNull(wq.getNextReviewAt());
        assertEquals(1, wq.getMasteredStreak().intValue());
        assertEquals(1, wq.getConsecutiveCorrect().intValue());
    }
}
