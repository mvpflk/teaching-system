package com.school.teaching.service.impl;

import com.school.teaching.entity.KnowledgeArticle;
import com.school.teaching.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    @Autowired private KnowledgeBaseStudentService studentService;
    @Autowired private KnowledgeBaseReviewService reviewService;
    @Autowired private KnowledgeBaseRecommendService recommendService;
    @Autowired private KnowledgeBaseAdminService adminService;

    @Override
    public Map<String, Object> listArticles(Long subjectId, String chapter, String task, String tags,
                                            Integer difficulty, String keyword, int page, int size) {
        return studentService.listArticles(subjectId, chapter, task, tags, difficulty, keyword, page, size);
    }

    @Override
    public Map<String, Object> getArticleDetail(Long articleId, Long studentId) {
        return studentService.getArticleDetail(articleId, studentId);
    }

    @Override
    public List<Map<String, Object>> getChapterTree(Long subjectId) {
        return studentService.getChapterTree(subjectId);
    }

    @Override
    public List<Map<String, Object>> getTags(Long subjectId) {
        return studentService.getTags(subjectId);
    }

    @Override
    public List<Map<String, Object>> getTodayReviewCards(Long studentId) {
        return reviewService.getTodayReviewCards(studentId);
    }

    @Override
    public Map<String, Object> rateFlashcard(Long studentId, Long flashcardId, int rating) {
        return reviewService.rateFlashcard(studentId, flashcardId, rating);
    }

    @Override
    public void startLearning(Long studentId, Long articleId) {
        reviewService.startLearning(studentId, articleId);
    }

    @Override
    public Map<String, Object> getProgress(Long studentId, Long subjectId) {
        return studentService.getProgress(studentId, subjectId);
    }

    @Override
    public Map<String, Object> toggleFavorite(Long studentId, Long articleId) {
        return studentService.toggleFavorite(studentId, articleId);
    }

    @Override
    public List<Map<String, Object>> getFavorites(Long studentId) {
        return studentService.getFavorites(studentId);
    }

    @Override
    public List<Map<String, Object>> search(String keyword, Long subjectId, int limit) {
        return studentService.search(keyword, subjectId, limit);
    }

    @Override
    public KnowledgeArticle createArticle(KnowledgeArticle article) {
        return adminService.createArticle(article);
    }

    @Override
    public KnowledgeArticle updateArticle(Long id, KnowledgeArticle article) {
        return adminService.updateArticle(id, article);
    }

    @Override
    public void deleteArticle(Long id) {
        adminService.deleteArticle(id);
    }

    @Override
    public Map<String, Object> getAdminArticleList(Long subjectId, String status, String chapter, String task, int page, int size) {
        return adminService.getAdminArticleList(subjectId, status, chapter, task, page, size);
    }

    @Override
    public Map<String, Object> getAdminStats(Long subjectId) {
        return adminService.getAdminStats(subjectId);
    }

    @Override
    public Map<String, Object> importFromMarkdown(String basePath, Long subjectId) {
        return adminService.importFromMarkdown(basePath, subjectId);
    }

    @Override
    public int generateFlashcards(Long articleId) {
        return adminService.generateFlashcards(articleId);
    }

    @Override
    public int generateFlashcardsWithAI(Long articleId) {
        return adminService.generateFlashcardsWithAI(articleId);
    }

    @Override
    public int generateFlashcardsBatch(int limit) {
        return adminService.generateFlashcardsBatch(limit);
    }

    @Override
    public void saveQuizResult(Long studentId, Long articleId, int total, int correct, String wrongIds) {
        studentService.saveQuizResult(studentId, articleId, total, correct, wrongIds);
    }

    @Override
    public List<Map<String, Object>> getQuizHistory(Long studentId, Long articleId) {
        return studentService.getQuizHistory(studentId, articleId);
    }

    @Override
    public Map<String, Object> getWeakAnalysis(Long studentId, Long subjectId) {
        return recommendService.getWeakAnalysis(studentId, subjectId);
    }

    @Override
    public List<Map<String, Object>> getRecommendations(Long studentId, Long subjectId) {
        return recommendService.getRecommendations(studentId, subjectId);
    }

    @Override
    public Map<String, Object> getDailyStats(Long studentId, Long subjectId) {
        return recommendService.getDailyStats(studentId, subjectId);
    }

    @Override
    public List<Map<String, Object>> getClassStats(Long teacherUserId, Long subjectId) {
        return adminService.getClassStats(teacherUserId, subjectId);
    }

    @Override
    public Map<String, Object> getSubjectsGrouped(Long studentId) {
        return studentService.getSubjectsGrouped(studentId);
    }

    @Override
    public Map<String, Object> getSubjectsForTeacher(Set<Long> subjectIds) {
        return studentService.getSubjectsForTeacher(subjectIds);
    }

    @Override
    public int clearAllFlashcards(Long subjectId) {
        return adminService.clearAllFlashcards(subjectId);
    }

    @Override
    public void regenerateAllFlashcardsAsync(Long subjectId) {
        adminService.regenerateAllFlashcardsAsync(subjectId);
    }

    @Override
    public Map<String, Object> getRegenerationProgress(Long subjectId) {
        return adminService.getRegenerationProgress(subjectId);
    }

    @Override
    public long countPublishedArticles(Long subjectId) {
        return adminService.countPublishedArticles(subjectId);
    }
}
