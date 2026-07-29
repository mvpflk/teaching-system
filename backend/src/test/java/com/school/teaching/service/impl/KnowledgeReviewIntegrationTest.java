package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.service.KnowledgeBaseService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 场景4: 知识库复习→SM-2调度 核心链路集成测试
 *
 * 测试链路: 创建知识点→创建文章→创建卡片→SM-2评分→验证复习计划
 *
 * SM-2算法验证:
 * - 评分1(完全忘记): reps=0, interval=1天, ef-=0.20
 * - 评分2(模糊): reps=0, interval=1天, ef-=0.15
 * - 评分3(记得): reps+1, interval按阶段增长, ef不变
 * - 评分4(轻松): reps+1, interval按阶段增长, ef+=0.15
 * - 掌握判定: reps>=5 → isMastered=1
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
@Disabled("需要运行中的 MySQL — 请先启动数据库再运行此测试")
@DisplayName("知识库复习集成测试: SM-2调度算法验证")
class KnowledgeReviewIntegrationTest {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private KnowledgeFlashcardMapper flashcardMapper;

    @Autowired
    private KnowledgeArticleMapper articleMapper;

    @Autowired
    private KnowledgeReviewScheduleMapper scheduleMapper;

    private static final Long STUDENT_ID = 1L;

    private Long testFlashcardId;
    private Long testArticleId;

    /**
     * 每个测试前创建测试卡片和文章
     */
    @BeforeEach
    void setUp() {
        // 创建测试文章
        KnowledgeArticle article = new KnowledgeArticle();
        article.setTitle("集成测试-SM2测试文章");
        article.setContentMd("# SM-2 Test\n\n这是用于集成测试SM-2算法的文章。");
        article.setStatus("PUBLISHED");
        article.setDifficulty(1);
        article.setSubjectId(1L); // 数学[职高]或任意学科
        articleMapper.insert(article);
        testArticleId = article.getId();

        // 创建测试卡片
        KnowledgeFlashcard card = new KnowledgeFlashcard();
        card.setArticleId(testArticleId);
        card.setFrontText("SM-2算法的全称是什么？");
        card.setBackText("SuperMemo 2 间隔重复算法");
        card.setSortOrder(1);
        flashcardMapper.insert(card);
        testFlashcardId = card.getId();
    }

    /**
     * 核心链路: 首次评分(3分)→验证SM-2调度参数
     */
    @Test
    @DisplayName("SM-2首次评分3分: 间隔1天, reps=1, ef=2.50")
    void rateFlashcard_firstRating3_shouldSetInterval1() {
        Map<String, Object> result = knowledgeBaseService.rateFlashcard(
            STUDENT_ID, testFlashcardId, 3);

        assertNotNull(result);
        assertEquals(1, result.get("intervalDays"), "首次评分3分: 间隔应为1天");
        assertEquals(false, result.get("isMastered"), "首次评分不应标记为已掌握");

        // 验证数据库中的复习计划
        KnowledgeReviewSchedule schedule = scheduleMapper.selectOne(
            new LambdaQueryWrapper<KnowledgeReviewSchedule>()
                .eq(KnowledgeReviewSchedule::getStudentId, STUDENT_ID)
                .eq(KnowledgeReviewSchedule::getFlashcardId, testFlashcardId));
        assertNotNull(schedule, "复习计划应已创建");
        assertEquals(1, schedule.getIntervalDays(), "间隔天数应为1");
        assertEquals(1, schedule.getRepetitions(), "重复次数应为1");
        assertEquals(new BigDecimal("2.50"), schedule.getEaseFactor(), "easiness factor初始应为2.50");
        assertEquals(Integer.valueOf(3), schedule.getLastRating(), "上次评分应为3");
        assertNotNull(schedule.getNextReviewAt(), "应有下次复习时间");
        assertNotNull(schedule.getLastReviewAt(), "应有上次复习时间");
    }

    /**
     * 评分1(完全忘记): 间隔重置为1天, ef降低0.20
     */
    @Test
    @DisplayName("SM-2评分1(完全忘记): reps=0, interval=1, ef-=0.20")
    void rateFlashcard_rating1_shouldResetAndLowerEf() {
        // 先评3分建立基础
        knowledgeBaseService.rateFlashcard(STUDENT_ID, testFlashcardId, 3);

        // 评1分(完全忘记)
        Map<String, Object> result = knowledgeBaseService.rateFlashcard(
            STUDENT_ID, testFlashcardId, 1);

        KnowledgeReviewSchedule schedule = scheduleMapper.selectOne(
            new LambdaQueryWrapper<KnowledgeReviewSchedule>()
                .eq(KnowledgeReviewSchedule::getStudentId, STUDENT_ID)
                .eq(KnowledgeReviewSchedule::getFlashcardId, testFlashcardId));
        assertNotNull(schedule);
        assertEquals(0, schedule.getRepetitions(), "评分1: 重复次数应重置为0");
        assertEquals(1, schedule.getIntervalDays(), "评分1: 间隔应重置为1天");
        assertEquals(new BigDecimal("2.30"), schedule.getEaseFactor(), "评分1: ef应降低0.20(=2.50-0.20=2.30)");
        assertEquals(Integer.valueOf(1), schedule.getLastRating(), "上次评分应为1");
    }

    /**
     * 评分4(轻松): ef增加0.15
     */
    @Test
    @DisplayName("SM-2评分4(轻松): interval=6, ef+=0.15")
    void rateFlashcard_rating4_shouldIncreaseEf() {
        // 先评3分(第一次)
        knowledgeBaseService.rateFlashcard(STUDENT_ID, testFlashcardId, 3);

        // 再评4分(第二次: reps=2 → interval=6)
        Map<String, Object> result = knowledgeBaseService.rateFlashcard(
            STUDENT_ID, testFlashcardId, 4);

        KnowledgeReviewSchedule schedule = scheduleMapper.selectOne(
            new LambdaQueryWrapper<KnowledgeReviewSchedule>()
                .eq(KnowledgeReviewSchedule::getStudentId, STUDENT_ID)
                .eq(KnowledgeReviewSchedule::getFlashcardId, testFlashcardId));
        assertNotNull(schedule);
        assertEquals(2, schedule.getRepetitions(), "评分4: 重复次数应为2");
        assertEquals(6, schedule.getIntervalDays(), "评分4: 第2次间隔应为6天");
        assertEquals(new BigDecimal("2.65"), schedule.getEaseFactor(),
            "评分4: ef应增加0.15(=2.50+0.15=2.65)");
    }

    /**
     * 连续正确评分5次→掌握判定
     */
    @Test
    @DisplayName("SM-2连续5次正确: isMastered=1")
    void rateFlashcard_masteryAfterFiveReps() {
        // 连续5次评分3+ 实现掌握(3,3,3,3,3)
        for (int i = 0; i < 5; i++) {
            knowledgeBaseService.rateFlashcard(STUDENT_ID, testFlashcardId, 3);
        }

        KnowledgeReviewSchedule schedule = scheduleMapper.selectOne(
            new LambdaQueryWrapper<KnowledgeReviewSchedule>()
                .eq(KnowledgeReviewSchedule::getStudentId, STUDENT_ID)
                .eq(KnowledgeReviewSchedule::getFlashcardId, testFlashcardId));
        assertNotNull(schedule);
        assertEquals(5, schedule.getRepetitions(), "5次正确后重复次数应为5");
        assertEquals(1, schedule.getIsMastered(), "5次正确后应标记为已掌握(isMastered=1)");
    }

    /**
     * ef不低于1.30(下限保护)
     */
    @Test
    @DisplayName("SM-2 ef下限保护: 连续差评后ef不低于1.30")
    void rateFlashcard_efFloorProtected() {
        // 先评3分建基础
        knowledgeBaseService.rateFlashcard(STUDENT_ID, testFlashcardId, 3);
        // 连续评1分10次测试下限
        for (int i = 0; i < 10; i++) {
            knowledgeBaseService.rateFlashcard(STUDENT_ID, testFlashcardId, 1);
        }

        KnowledgeReviewSchedule schedule = scheduleMapper.selectOne(
            new LambdaQueryWrapper<KnowledgeReviewSchedule>()
                .eq(KnowledgeReviewSchedule::getStudentId, STUDENT_ID)
                .eq(KnowledgeReviewSchedule::getFlashcardId, testFlashcardId));
        assertNotNull(schedule);
        assertTrue(schedule.getEaseFactor().doubleValue() >= 1.30,
            "ef不应低于1.30(下限保护): 实际=" + schedule.getEaseFactor());
    }

    /**
     * 评分无效值(0或5)应被拒绝
     */
    @Test
    @DisplayName("SM-2评分校验: 评分<1或>4应抛异常")
    void rateFlashcard_invalidRating_shouldThrowException() {
        assertThrows(com.school.teaching.exception.BusinessException.class,
            () -> knowledgeBaseService.rateFlashcard(STUDENT_ID, testFlashcardId, 0),
            "评分0应被拒绝");
        assertThrows(com.school.teaching.exception.BusinessException.class,
            () -> knowledgeBaseService.rateFlashcard(STUDENT_ID, testFlashcardId, 5),
            "评分5应被拒绝");
    }

    /**
     * 卡片不存在时评分应抛异常
     */
    @Test
    @DisplayName("SM-2评分校验: 不存在的卡片应抛404")
    void rateFlashcard_nonExistentCard_shouldThrowNotFound() {
        com.school.teaching.exception.BusinessException ex =
            assertThrows(com.school.teaching.exception.BusinessException.class,
                () -> knowledgeBaseService.rateFlashcard(STUDENT_ID, 99999L, 3));
        assertEquals(404, ex.getCode(), "不存在的卡片应返回404");
    }

    /**
     * 验证startLearning创建复习计划
     */
    @Test
    @DisplayName("startLearning: 创建学习计划后schedule应存在")
    void startLearning_shouldCreateSchedules() {
        knowledgeBaseService.startLearning(STUDENT_ID, testArticleId);

        List<KnowledgeReviewSchedule> schedules = scheduleMapper.selectList(
            new LambdaQueryWrapper<KnowledgeReviewSchedule>()
                .eq(KnowledgeReviewSchedule::getStudentId, STUDENT_ID)
                .eq(KnowledgeReviewSchedule::getFlashcardId, testFlashcardId));
        assertFalse(schedules.isEmpty(), "startLearning应创建复习计划");
        KnowledgeReviewSchedule schedule = schedules.get(0);
        assertEquals(testFlashcardId, schedule.getFlashcardId());
        assertEquals(testArticleId, schedule.getArticleId());
        assertEquals(new BigDecimal("2.50"), schedule.getEaseFactor());
        assertEquals(0, schedule.getIntervalDays());
        assertEquals(0, schedule.getRepetitions());
    }

    /**
     * 验证学习进度统计
     */
    @Test
    @DisplayName("学习进度: getProgress返回统计信息")
    void getProgress_shouldReturnStats() {
        // 先创建学习计划
        knowledgeBaseService.startLearning(STUDENT_ID, testArticleId);

        Map<String, Object> progress = knowledgeBaseService.getProgress(STUDENT_ID, null);
        assertNotNull(progress, "进度统计不应为null");
        assertNotNull(progress.get("studiedArticles"), "应有studiedArticles");
        assertNotNull(progress.get("totalCards"), "应有totalCards");
        assertNotNull(progress.get("masteredCards"), "应有masteredCards");
        assertNotNull(progress.get("todayReview"), "应有todayReview");
    }
}
