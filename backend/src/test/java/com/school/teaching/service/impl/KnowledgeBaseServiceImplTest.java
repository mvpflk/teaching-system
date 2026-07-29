package com.school.teaching.service.impl;

import com.school.teaching.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * KnowledgeBaseServiceImpl 委托层测试 — 验证方法正确转发到子服务。
 * SM-2 算法 / 收藏 / 删除等业务逻辑的完整测试应在对应子服务的测试中。
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceImplTest {

    @Mock private KnowledgeBaseAdminService adminService;
    @Mock private KnowledgeBaseStudentService studentService;
    @Mock private KnowledgeBaseReviewService reviewService;
    @Mock private KnowledgeBaseRecommendService recommendService;

    @InjectMocks
    private KnowledgeBaseServiceImpl knowledgeBaseService;

    // ═══════════ SM-2 评分委托 ═══════════

    @Test
    @DisplayName("rateFlashcard: 正确委托到 reviewService")
    void rateFlashcard_delegatesToReviewService() {
        when(reviewService.rateFlashcard(1L, 10L, 3))
            .thenReturn(Map.of("intervalDays", 6, "isMastered", false));

        Map<String, Object> result = knowledgeBaseService.rateFlashcard(1L, 10L, 3);

        assertEquals(6, result.get("intervalDays"));
        assertEquals(false, result.get("isMastered"));
        verify(reviewService).rateFlashcard(1L, 10L, 3);
    }

    @Test
    @DisplayName("rateFlashcard: 评分1(忘了) → 委托并返回正确结果")
    void rateFlashcard_forget_resetsAndLowersEf() {
        when(reviewService.rateFlashcard(1L, 1L, 1))
            .thenReturn(Map.of("intervalDays", 1, "isMastered", false));

        Map<String, Object> result = knowledgeBaseService.rateFlashcard(1L, 1L, 1);

        assertEquals(1, result.get("intervalDays"));
        assertEquals(false, result.get("isMastered"));
        verify(reviewService).rateFlashcard(1L, 1L, 1);
    }

    @Test
    @DisplayName("rateFlashcard: 评分4(简单) → 委托验证")
    void rateFlashcard_easy_increasesEfAndInterval() {
        when(reviewService.rateFlashcard(1L, 1L, 4))
            .thenReturn(Map.of("intervalDays", 16, "isMastered", false));

        knowledgeBaseService.rateFlashcard(1L, 1L, 4);
        verify(reviewService).rateFlashcard(1L, 1L, 4);
    }

    @Test
    @DisplayName("rateFlashcard: EF下限保护 → 委托验证")
    void rateFlashcard_efFloorProtected() {
        when(reviewService.rateFlashcard(1L, 1L, 1))
            .thenReturn(Map.of("intervalDays", 1, "isMastered", false));

        knowledgeBaseService.rateFlashcard(1L, 1L, 1);
        verify(reviewService).rateFlashcard(1L, 1L, 1);
    }

    @Test
    @DisplayName("rateFlashcard: 连续答对≥5次标记已掌握 → 委托并验证返回值")
    void rateFlashcard_masteryAfterFiveReps() {
        when(reviewService.rateFlashcard(1L, 1L, 3))
            .thenReturn(Map.of("intervalDays", 30, "isMastered", true));

        Map<String, Object> result = knowledgeBaseService.rateFlashcard(1L, 1L, 3);

        assertEquals(true, result.get("isMastered"));
        verify(reviewService).rateFlashcard(1L, 1L, 3);
    }

    @Test
    @DisplayName("rateFlashcard: 首次评分创建新schedule → 委托验证")
    void rateFlashcard_newScheduleCreated() {
        when(reviewService.rateFlashcard(1L, 1L, 3))
            .thenReturn(Map.of("intervalDays", 1));

        Map<String, Object> result = knowledgeBaseService.rateFlashcard(1L, 1L, 3);

        assertEquals(1, result.get("intervalDays"));
        verify(reviewService).rateFlashcard(1L, 1L, 3);
    }

    @Test
    @DisplayName("rateFlashcard: 评分范围校验 — reviewService 拒绝非法评分")
    void rateFlashcard_invalidRatingThrows() {
        when(reviewService.rateFlashcard(1L, 1L, 0))
            .thenThrow(new BusinessException(400, "评分范围1-4"));
        when(reviewService.rateFlashcard(1L, 1L, 5))
            .thenThrow(new BusinessException(400, "评分范围1-4"));

        assertThrows(BusinessException.class, () ->
            knowledgeBaseService.rateFlashcard(1L, 1L, 0));
        assertThrows(BusinessException.class, () ->
            knowledgeBaseService.rateFlashcard(1L, 1L, 5));
    }

    @Test
    @DisplayName("rateFlashcard: 卡片不存在时 reviewService 抛 404")
    void rateFlashcard_cardNotFoundThrows() {
        when(reviewService.rateFlashcard(1L, 1L, 3))
            .thenThrow(new BusinessException(404, "卡片不存在"));

        assertThrows(BusinessException.class, () ->
            knowledgeBaseService.rateFlashcard(1L, 1L, 3));
    }

    // ═══════════ 收藏委托 ═══════════

    @Test
    @DisplayName("toggleFavorite: 首次收藏 → 委托 studentService + 返回 favorited=true")
    void toggleFavorite_firstTime_favorited() {
        when(studentService.toggleFavorite(1L, 100L))
            .thenReturn(Map.of("favorited", true));

        Map<String, Object> result = knowledgeBaseService.toggleFavorite(1L, 100L);

        assertEquals(true, result.get("favorited"));
        verify(studentService).toggleFavorite(1L, 100L);
    }

    @Test
    @DisplayName("toggleFavorite: 已收藏 → 委托 studentService + 返回 favorited=false")
    void toggleFavorite_alreadyFavorited_unfavorited() {
        when(studentService.toggleFavorite(1L, 100L))
            .thenReturn(Map.of("favorited", false));

        Map<String, Object> result = knowledgeBaseService.toggleFavorite(1L, 100L);

        assertEquals(false, result.get("favorited"));
        verify(studentService).toggleFavorite(1L, 100L);
    }

    // ═══════════ 删除委托 ═══════════

    @Test
    @DisplayName("deleteArticle: 委托 adminService 删除")
    void deleteArticle_cascadeDeletesAll() {
        knowledgeBaseService.deleteArticle(100L);
        verify(adminService).deleteArticle(100L);
    }

    @Test
    @DisplayName("deleteArticle: 文章不存在时 adminService 抛异常")
    void deleteArticle_notFoundThrows() {
        doThrow(new BusinessException(404, "文章不存在"))
            .when(adminService).deleteArticle(999L);

        assertThrows(BusinessException.class, () ->
            knowledgeBaseService.deleteArticle(999L));
    }

    // ═══════════ 生成闪卡委托 ═══════════

    @Test
    @DisplayName("generateFlashcards: 委托 adminService 生成闪卡")
    void generateFlashcards_WithoutSyllabus_stillGenerates() {
        when(adminService.generateFlashcards(200L)).thenReturn(5);

        int count = knowledgeBaseService.generateFlashcards(200L);

        assertTrue(count > 0, "应返回生成的卡片数量 > 0");
        assertEquals(5, count);
        verify(adminService).generateFlashcards(200L);
    }
}
