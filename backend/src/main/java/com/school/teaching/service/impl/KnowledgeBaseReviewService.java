package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseReviewService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseReviewService.class);

    @Autowired private KnowledgeArticleMapper articleMapper;
    @Autowired private KnowledgeFlashcardMapper flashcardMapper;
    @Autowired private KnowledgeReviewScheduleMapper scheduleMapper;
    @Autowired(required = false) private CreditTransactionMapper creditTransactionMapper;

    public List<Map<String, Object>> getTodayReviewCards(Long studentId) {
        List<KnowledgeReviewSchedule> schedules = scheduleMapper.selectList(
            new LambdaQueryWrapper<KnowledgeReviewSchedule>()
                .eq(KnowledgeReviewSchedule::getStudentId, studentId)
                .le(KnowledgeReviewSchedule::getNextReviewAt, LocalDateTime.now())
                .orderByAsc(KnowledgeReviewSchedule::getNextReviewAt));

        List<Long> cardIds = schedules.stream().map(KnowledgeReviewSchedule::getFlashcardId).collect(Collectors.toList());
        List<Long> articleIds = schedules.stream().map(KnowledgeReviewSchedule::getArticleId).distinct().collect(Collectors.toList());

        Map<Long, KnowledgeFlashcard> cardMap = cardIds.isEmpty() ? Map.of() :
            flashcardMapper.selectBatchIds(cardIds).stream().collect(Collectors.toMap(KnowledgeFlashcard::getId, c -> c));
        Map<Long, KnowledgeArticle> articleMap = articleIds.isEmpty() ? Map.of() :
            articleMapper.selectBatchIds(articleIds).stream().collect(Collectors.toMap(KnowledgeArticle::getId, a -> a));

        return schedules.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("schedule", s);
            m.put("flashcard", cardMap.get(s.getFlashcardId()));
            KnowledgeArticle a = articleMap.get(s.getArticleId());
            m.put("articleTitle", a != null ? a.getTitle() : "");
            return m;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> rateFlashcard(Long studentId, Long flashcardId, int rating) {
        if (rating < 1 || rating > 4) throw new BusinessException(400, "评分需为 1-4");

        KnowledgeFlashcard card = flashcardMapper.selectById(flashcardId);
        if (card == null) throw new BusinessException(404, "卡片不存在");

        KnowledgeReviewSchedule schedule = scheduleMapper.selectOne(
            new LambdaQueryWrapper<KnowledgeReviewSchedule>()
                .eq(KnowledgeReviewSchedule::getStudentId, studentId)
                .eq(KnowledgeReviewSchedule::getFlashcardId, flashcardId)
                .last("LIMIT 1"));

        if (schedule == null) {
            schedule = new KnowledgeReviewSchedule();
            schedule.setStudentId(studentId);
            schedule.setFlashcardId(flashcardId);
            schedule.setArticleId(card.getArticleId());
            schedule.setEaseFactor(new BigDecimal("2.50"));
            schedule.setIntervalDays(0);
            schedule.setRepetitions(0);
        }

        applySm2Algorithm(schedule, rating);

        if (schedule.getId() != null) {
            scheduleMapper.updateById(schedule);
        } else {
            try {
                scheduleMapper.insert(schedule);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                schedule = scheduleMapper.selectOne(
                    new LambdaQueryWrapper<KnowledgeReviewSchedule>()
                        .eq(KnowledgeReviewSchedule::getStudentId, studentId)
                        .eq(KnowledgeReviewSchedule::getFlashcardId, flashcardId)
                        .last("LIMIT 1"));
                applySm2Algorithm(schedule, rating);
                scheduleMapper.updateById(schedule);
            }
        }

        if (creditTransactionMapper != null) {
            try {
                LocalDate today = LocalDate.now();
                Long todayCount = scheduleMapper.selectCount(
                    new QueryWrapper<KnowledgeReviewSchedule>()
                        .eq("student_id", studentId)
                        .ge("last_review_at", today.atStartOfDay())
                        .lt("last_review_at", today.plusDays(1).atStartOfDay()));
                int dailyGoal = 5;
                if (todayCount != null && todayCount.intValue() >= dailyGoal) {
                    String bizKey = "CARD_DAILY:" + studentId + ":" + today;
                    Long existCount = creditTransactionMapper.selectCount(
                        new LambdaQueryWrapper<CreditTransaction>()
                            .eq(CreditTransaction::getBizKey, bizKey));
                    if (existCount == 0) {
                        CreditTransaction txn = new CreditTransaction();
                        txn.setStudentId(studentId);
                        txn.setTransactionType("earn");
                        txn.setCreditAmount(1);
                        txn.setSourceType("CARD_REVIEW");
                        txn.setDescription("每日卡片复习达标(" + todayCount + "张)");
                        txn.setBizKey(bizKey);
                        txn.setBalanceAfter(0);
                        txn.setCreateTime(LocalDateTime.now());
                        creditTransactionMapper.insert(txn);
                        log.debug("卡片积分: studentId={} 每日达标 {}张", studentId, todayCount);
                    }
                }
            } catch (Exception e) {
                log.warn("卡片积分发放失败 studentId={}", studentId, e);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nextReviewAt", schedule.getNextReviewAt());
        result.put("intervalDays", schedule.getIntervalDays());
        result.put("isMastered", schedule.getIsMastered() == 1);
        result.put("message", rating == 1 ? "别灰心，明天再试！" :
                               rating == 2 ? "加把劲，很快就能记住！" :
                               rating == 3 ? "不错，继续加油！" :
                                             "太棒了，记得很牢！");
        return result;
    }

    @Transactional
    public void rateNodeKnowledge(Long studentId, Long nodeId, int rating) {
        if (rating < 1 || rating > 4) throw new BusinessException(400, "评分需为 1-4");

        KnowledgeReviewSchedule schedule = scheduleMapper.selectOne(
            new LambdaQueryWrapper<KnowledgeReviewSchedule>()
                .eq(KnowledgeReviewSchedule::getStudentId, studentId)
                .eq(KnowledgeReviewSchedule::getNodeId, nodeId)
                .eq(KnowledgeReviewSchedule::getSourceType, "CHECKPOINT")
                .last("LIMIT 1"));

        if (schedule == null) {
            schedule = new KnowledgeReviewSchedule();
            schedule.setStudentId(studentId);
            schedule.setNodeId(nodeId);
            schedule.setSourceType("CHECKPOINT");
            schedule.setEaseFactor(new BigDecimal("2.50"));
            schedule.setIntervalDays(0);
            schedule.setRepetitions(0);
        }

        applySm2Algorithm(schedule, rating);

        if (schedule.getId() != null) {
            scheduleMapper.updateById(schedule);
        } else {
            try {
                scheduleMapper.insert(schedule);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                schedule = scheduleMapper.selectOne(
                    new LambdaQueryWrapper<KnowledgeReviewSchedule>()
                        .eq(KnowledgeReviewSchedule::getStudentId, studentId)
                        .eq(KnowledgeReviewSchedule::getNodeId, nodeId)
                        .eq(KnowledgeReviewSchedule::getSourceType, "CHECKPOINT")
                        .last("LIMIT 1"));
                applySm2Algorithm(schedule, rating);
                scheduleMapper.updateById(schedule);
            }
        }
    }

    private void applySm2Algorithm(KnowledgeReviewSchedule schedule, int rating) {
        BigDecimal ef = schedule.getEaseFactor();
        int interval = schedule.getIntervalDays();
        int reps = schedule.getRepetitions();

        if (rating >= 3) {
            reps++;
            if (reps == 1) interval = 1;
            else if (reps == 2) interval = 6;
            else interval = (int) Math.round(interval * ef.doubleValue());
            if (rating == 4) ef = ef.add(new BigDecimal("0.15"));
        } else {
            reps = 0;
            interval = 1;
            if (rating == 1) ef = ef.subtract(new BigDecimal("0.20"));
            else ef = ef.subtract(new BigDecimal("0.15"));
        }

        if (ef.doubleValue() < 1.3) ef = new BigDecimal("1.30");

        schedule.setEaseFactor(ef);
        schedule.setIntervalDays(interval);
        schedule.setRepetitions(reps);
        schedule.setLastReviewAt(LocalDateTime.now());
        schedule.setNextReviewAt(LocalDateTime.now().plusDays(interval));
        schedule.setLastRating(rating);
        schedule.setIsMastered(reps >= 5 ? 1 : 0);
    }

    @Transactional
    public void startLearning(Long studentId, Long articleId) {
        List<KnowledgeFlashcard> cards = flashcardMapper.selectList(
            new LambdaQueryWrapper<KnowledgeFlashcard>()
                .eq(KnowledgeFlashcard::getArticleId, articleId));

        Set<Long> existIds = new HashSet<>(scheduleMapper.selectList(
            new LambdaQueryWrapper<KnowledgeReviewSchedule>()
                .eq(KnowledgeReviewSchedule::getStudentId, studentId)
                .in(!cards.isEmpty(), KnowledgeReviewSchedule::getFlashcardId,
                    cards.stream().map(KnowledgeFlashcard::getId).collect(Collectors.toList()))
        ).stream().map(KnowledgeReviewSchedule::getFlashcardId).collect(Collectors.toList()));

        List<KnowledgeReviewSchedule> toInsert = new ArrayList<>();
        for (KnowledgeFlashcard card : cards) {
            if (!existIds.contains(card.getId())) {
                KnowledgeReviewSchedule s = new KnowledgeReviewSchedule();
                s.setStudentId(studentId);
                s.setFlashcardId(card.getId());
                s.setArticleId(articleId);
                s.setEaseFactor(new BigDecimal("2.50"));
                s.setIntervalDays(0);
                s.setRepetitions(0);
                s.setNextReviewAt(LocalDateTime.now());
                toInsert.add(s);
            }
        }
        if (!toInsert.isEmpty()) {
            for (KnowledgeReviewSchedule s : toInsert) {
                try {
                    scheduleMapper.insert(s);
                } catch (org.springframework.dao.DuplicateKeyException e) {
                    log.debug("跳过重复复习计划: studentId={}, flashcardId={}", studentId, s.getFlashcardId());
                }
            }
        }
    }
}
