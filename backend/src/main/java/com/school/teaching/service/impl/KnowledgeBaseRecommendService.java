package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.utils.ScoreUtils;
import static com.school.teaching.service.impl.KnowledgeBaseHelper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseRecommendService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseRecommendService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired private KnowledgeArticleMapper articleMapper;
    @Autowired private KnowledgeQuizResultMapper quizResultMapper;

    public Map<String, Object> getWeakAnalysis(Long studentId, Long subjectId) {
        List<KnowledgeQuizResult> results = quizResultMapper.selectList(
            new LambdaQueryWrapper<KnowledgeQuizResult>()
                .eq(KnowledgeQuizResult::getStudentId, studentId)
                .eq(subjectId != null, KnowledgeQuizResult::getSubjectId, subjectId)
                .orderByDesc(KnowledgeQuizResult::getCreatedAt));
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("totalQuizzes", results.size());
        if (results.isEmpty()) { res.put("weakTags", Collections.emptyList()); return res; }
        Map<Long, Integer> articleWrongWeight = new LinkedHashMap<>();
        Set<Long> articleIds = new LinkedHashSet<>();
        for (KnowledgeQuizResult r : results) {
            if (r.getWrongQuestionIds() == null || r.getWrongQuestionIds().isEmpty() || r.getWrongQuestionIds().equals("[]")) continue;
            articleIds.add(r.getArticleId());
            try {
                List<Integer> ids = OBJECT_MAPPER.readValue(r.getWrongQuestionIds(), List.class);
                articleWrongWeight.merge(r.getArticleId(), ids.size(), Integer::sum);
            } catch (Exception e) { /* skip */ }
        }
        res.put("totalWrongArticles", articleIds.size());
        if (articleIds.isEmpty()) { res.put("weakTags", Collections.emptyList()); return res; }
        List<KnowledgeArticle> articles = articleMapper.selectList(
            new LambdaQueryWrapper<KnowledgeArticle>().in(KnowledgeArticle::getId, articleIds));
        Map<String, Long> tagCount = new LinkedHashMap<>();
        for (KnowledgeArticle a : articles) {
            if (a.getTags() == null || a.getTags().isEmpty()) continue;
            int weight = articleWrongWeight.getOrDefault(a.getId(), 1);
            try {
                List<String> list = OBJECT_MAPPER.readValue(a.getTags(), List.class);
                for (String t : list) if (t != null && !t.trim().isEmpty()) tagCount.merge(t.trim(), (long) weight, Long::sum);
            } catch (Exception e) { /* skip */ }
        }
        List<Map<String, Object>> weakTags = tagCount.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(10)
            .map(e -> { Map<String, Object> m = new LinkedHashMap<>(); m.put("tag", e.getKey()); m.put("count", e.getValue()); return m; })
            .collect(Collectors.toList());
        res.put("weakTags", weakTags);
        return res;
    }

    public List<Map<String, Object>> getRecommendations(Long studentId, Long subjectId) {
        Map<String, Object> weak = getWeakAnalysis(studentId, subjectId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> weakTags = (List<Map<String, Object>>) weak.get("weakTags");
        List<KnowledgeArticle> all = articleMapper.selectList(
            new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, "PUBLISHED")
                .eq(KnowledgeArticle::getSubjectId, subjectId));
        if (weakTags == null || weakTags.isEmpty()) {
            return all.stream().filter(a -> a.getDifficulty() != null && a.getDifficulty() <= 2)
                .limit(6).map(a -> toSimpleMap(a)).collect(Collectors.toList());
        }
        List<String> weakNames = weakTags.stream().map(m -> (String) m.get("tag")).collect(Collectors.toList());
        List<KnowledgeQuizResult> allResults = quizResultMapper.selectList(
            new LambdaQueryWrapper<KnowledgeQuizResult>()
                .eq(KnowledgeQuizResult::getStudentId, studentId)
                .orderByDesc(KnowledgeQuizResult::getCreatedAt));
        Map<Long, BigDecimal> latestScore = new LinkedHashMap<>();
        for (KnowledgeQuizResult r : allResults) {
            if (r.getScore() != null) latestScore.putIfAbsent(r.getArticleId(), r.getScore());
        }
        Set<Long> masteredIds = latestScore.entrySet().stream()
            .filter(e -> e.getValue().compareTo(new BigDecimal("80")) >= 0)
            .map(Map.Entry::getKey).collect(Collectors.toSet());
        List<Map<String, Object>> scored = new ArrayList<>();
        for (KnowledgeArticle a : all) {
            if (masteredIds.contains(a.getId()) || a.getTags() == null || a.getTags().isEmpty()) continue;
            try {
                List<String> tags = OBJECT_MAPPER.readValue(a.getTags(), List.class);
                long match = tags.stream().filter(t -> weakNames.contains(t)).count();
                if (match > 0) {
                    Map<String, Object> m = toSimpleMap(a);
                    m.put("matchCount", (int) match);
                    BigDecimal ls = latestScore.get(a.getId());
                    if (ls != null && ls.compareTo(new BigDecimal("60")) < 0) m.put("needsReview", true);
                    scored.add(m);
                }
            } catch (Exception e) { /* skip */ }
        }
        scored.sort((a, b) -> {
            boolean ra = a.containsKey("needsReview") && (Boolean) a.get("needsReview");
            boolean rb = b.containsKey("needsReview") && (Boolean) b.get("needsReview");
            if (ra != rb) return ra ? -1 : 1;
            int c = Integer.compare((Integer) b.get("matchCount"), (Integer) a.get("matchCount"));
            if (c != 0) return c;
            return Integer.compare((Integer) a.get("difficulty"), (Integer) b.get("difficulty"));
        });
        return scored.stream().limit(6).collect(Collectors.toList());
    }

    public Map<String, Object> getDailyStats(Long studentId, Long subjectId) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        Long todayQuizzes = quizResultMapper.selectCount(
            new LambdaQueryWrapper<KnowledgeQuizResult>()
                .eq(KnowledgeQuizResult::getStudentId, studentId)
                .eq(subjectId != null, KnowledgeQuizResult::getSubjectId, subjectId)
                .ge(KnowledgeQuizResult::getCreatedAt, todayStart)
                .lt(KnowledgeQuizResult::getCreatedAt, todayEnd));
        List<KnowledgeQuizResult> recent = quizResultMapper.selectList(
            new LambdaQueryWrapper<KnowledgeQuizResult>()
                .eq(KnowledgeQuizResult::getStudentId, studentId)
                .eq(subjectId != null, KnowledgeQuizResult::getSubjectId, subjectId)
                .orderByDesc(KnowledgeQuizResult::getCreatedAt).last("LIMIT 5"));
        List<BigDecimal> scores = recent.stream().filter(r -> r.getScore() != null)
            .map(KnowledgeQuizResult::getScore).collect(Collectors.toList());
        double avgScore = ScoreUtils.avg(scores);
        List<String> dates = new ArrayList<>();
        LocalDate day = today;
        for (int i = 0; i < 30; i++) {
            dates.add(day.toString());
            day = day.minusDays(1);
        }
        QueryWrapper<KnowledgeQuizResult> qw = new QueryWrapper<KnowledgeQuizResult>()
            .select("DATE(created_at) as d, COUNT(*) as c")
            .eq("student_id", studentId)
            .in("DATE(created_at)", dates);
        if (subjectId != null) qw.eq("subject_id", subjectId);
        List<Map<String, Object>> dailyCounts = quizResultMapper.selectMaps(
            qw.groupBy("DATE(created_at)").orderByDesc("DATE(created_at)"));

        Set<LocalDate> activeDays = dailyCounts.stream()
            .map(m -> LocalDate.parse((String) m.get("d")))
            .collect(Collectors.toSet());
        int streak = 0;
        LocalDate d = today;
        while (streak < 30) {
            if (!activeDays.contains(d)) break;
            streak++;
            d = d.minusDays(1);
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("todayQuizzes", todayQuizzes != null ? todayQuizzes : 0L);
        res.put("avgScore", Math.round(avgScore * 10) / 10.0);
        res.put("streak", streak);
        return res;
    }
}
