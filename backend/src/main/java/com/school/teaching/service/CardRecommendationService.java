package com.school.teaching.service;

import com.school.teaching.dto.ExamPrepPackDTO;
import com.school.teaching.dto.QuickReviewDTO;
import com.school.teaching.dto.FlashcardDTO;
import com.school.teaching.dto.DailyCardDTO;
import com.school.teaching.dto.RelatedCardsDTO;

import java.util.List;

public interface CardRecommendationService {

    DailyCardDTO getDailyCard(Long studentId);

    RelatedCardsDTO getRelatedCards(Long submissionId, Long studentId, int limit);

    List<FlashcardDTO> getCardsByNodeId(Long nodeId, int limit);

    QuickReviewDTO startQuickReview(Long classId, Long subjectId, Long nodeId, int limit);

    void recordQuickReview(String sessionId, Long studentId, int cardIndex, boolean correct);

    ExamPrepPackDTO getExamPrepPack(Long studentId, Long taskId);

    // ── v167: 卡片审核 + 考纲权重 + AI评估 ──
    java.util.Map<String, Object> getReviewQueue(Long subjectId, String status, int page, int size);
    int batchReviewCards(List<Long> cardIds, String action, boolean adoptAiVersion, Long reviewerId);
    void setExamWeight(Long nodeId, String weight);
    void triggerBatchEvaluate(List<Long> cardIds);
    int triggerBatchEvaluateAll();
    java.util.Map<String, Object> getEvaluationProgress();
}
