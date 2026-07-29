package com.school.teaching.service;

import com.school.teaching.entity.KnowledgeArticle;
import java.util.List;
import java.util.Map;

public interface KnowledgeBaseService {

    // ─── 学生端 ───
    Map<String, Object> listArticles(Long subjectId, String chapter, String task, String tags,
                                     Integer difficulty, String keyword, int page, int size);
    Map<String, Object> getArticleDetail(Long articleId, Long studentId);
    List<Map<String, Object>> getChapterTree(Long subjectId);
    List<Map<String, Object>> getTags(Long subjectId);
    List<Map<String, Object>> getTodayReviewCards(Long studentId);
    Map<String, Object> rateFlashcard(Long studentId, Long flashcardId, int rating);
    void startLearning(Long studentId, Long articleId);
    Map<String, Object> getProgress(Long studentId, Long subjectId);
    Map<String, Object> toggleFavorite(Long studentId, Long articleId);
    List<Map<String, Object>> getFavorites(Long studentId);
    List<Map<String, Object>> search(String keyword, Long subjectId, int limit);

    // ─── 管理端 ───
    KnowledgeArticle createArticle(KnowledgeArticle article);
    KnowledgeArticle updateArticle(Long id, KnowledgeArticle article);
    void deleteArticle(Long id);
    Map<String, Object> getAdminArticleList(Long subjectId, String status, String chapter, String task, int page, int size);
    Map<String, Object> getAdminStats(Long subjectId);
    Map<String, Object> importFromMarkdown(String basePath, Long subjectId);

    /** 为文章重新生成记忆卡片（删除旧卡片→规则提取新卡片） */
    int generateFlashcards(Long articleId);

    /** v167: AI 辅助生成多样化卡片（5种类型+考纲上下文，删除旧卡→AI生成→自动质量评估） */
    int generateFlashcardsWithAI(Long articleId);

    /** 批量为无卡片的文章生成记忆卡片，返回处理的文章数 */
    int generateFlashcardsBatch(int limit);

    /** 保存自测结果 */
    void saveQuizResult(Long studentId, Long articleId, int total, int correct, String wrongIds);

    /** 查询文章自测历史 */
    List<Map<String, Object>> getQuizHistory(Long studentId, Long articleId);

    /** 薄弱标签分析：聚合 quiz_results + 文章 tags，返回学生薄弱标签 */
    Map<String, Object> getWeakAnalysis(Long studentId, Long subjectId);

    /** 推荐文章：基于薄弱标签匹配未掌握文章，按匹配度+难度排序 */
    List<Map<String, Object>> getRecommendations(Long studentId, Long subjectId);

    /** 今日学习统计：学习量 + 平均分 + 连续学习天数 */
    Map<String, Object> getDailyStats(Long studentId, Long subjectId);

    /** 教师端：全班知识学习统计 */
    List<Map<String, Object>> getClassStats(Long teacherUserId, Long subjectId);

    /** 按专业分组返回学科列表（学生端） */
    Map<String, Object> getSubjectsGrouped(Long studentId);

    /** 按学科ID集合返回分组列表（教师端：仅所授学科） */
    Map<String, Object> getSubjectsForTeacher(java.util.Set<Long> subjectIds);

    /** v169: 清空知识卡片（可按学科筛选，不传=全清），返回删除数量 */
    int clearAllFlashcards(Long subjectId);

    /**
     * v169: 异步清空+分段重生全部卡片。不阻塞请求线程。
     * 每批 10 篇文章，批间间隔 2s，防止打爆 AI API。
     */
    void regenerateAllFlashcardsAsync(Long subjectId);

    /** v169: 查询异步重生进度 */
    Map<String, Object> getRegenerationProgress(Long subjectId);

    /** 统计已发布文章数（可按学科筛选） */
    long countPublishedArticles(Long subjectId);
}
