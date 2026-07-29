package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.service.TeacherService;
import static com.school.teaching.service.impl.KnowledgeBaseHelper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseStudentService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseStudentService.class);

    @Autowired private KnowledgeArticleMapper articleMapper;
    @Autowired private KnowledgeFlashcardMapper flashcardMapper;
    @Autowired private KnowledgeReviewScheduleMapper scheduleMapper;
    @Autowired private StudentFavoritesMapper favoritesMapper;
    @Autowired private KnowledgeQuizResultMapper quizResultMapper;
    @Autowired private KnowledgeNodeMapper nodeMapper;
    @Autowired private DictMajorMapper majorMapper;
    @Autowired private DictMajorSubjectMapper majorSubjectMapper;
    @Autowired private DictSubjectMapper subjectMapper;
    @Autowired private ClassesMapper classesMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private TeacherService teacherService;
    @Autowired private AiOutputMapper aiOutputMapper;

    public Map<String, Object> listArticles(Long subjectId, String chapter, String task, String tags,
                                            Integer difficulty, String keyword, int page, int size) {
        LambdaQueryWrapper<KnowledgeArticle> qw = new LambdaQueryWrapper<>();
        qw.eq(KnowledgeArticle::getStatus, "PUBLISHED");
        if (subjectId != null) qw.eq(KnowledgeArticle::getSubjectId, subjectId);
        if (chapter != null && !chapter.isEmpty()) qw.eq(KnowledgeArticle::getChapter, chapter);
        if (task != null && !task.isEmpty()) qw.eq(KnowledgeArticle::getTask, task);
        if (difficulty != null) qw.eq(KnowledgeArticle::getDifficulty, difficulty);
        if (keyword != null && !keyword.isEmpty())
            qw.and(w -> w.like(KnowledgeArticle::getTitle, keyword)
                        .or().like(KnowledgeArticle::getContentMd, keyword));
        if (tags != null && !tags.isEmpty()) {
            String[] tagArr = tags.split(",");
            for (String tag : tagArr) {
                String safe = tag.trim()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("'", "\\'");
                qw.apply("JSON_CONTAINS(tags, CAST({0} AS JSON))", "[\"" + safe + "\"]");
            }
        }
        qw.orderByDesc(KnowledgeArticle::getViewCount);

        Page<KnowledgeArticle> result = articleMapper.selectPage(new Page<>(page, size), qw);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("records", result.getRecords());
        map.put("total", result.getTotal());
        map.put("page", page);
        map.put("size", size);
        return map;
    }

    public Map<String, Object> getArticleDetail(Long articleId, Long studentId) {
        KnowledgeArticle article = articleMapper.selectById(articleId);
        if (article == null) throw new BusinessException(404, "文章不存在");

        List<KnowledgeFlashcard> cards = flashcardMapper.selectList(
            new LambdaQueryWrapper<KnowledgeFlashcard>()
                .eq(KnowledgeFlashcard::getArticleId, articleId)
                .orderByAsc(KnowledgeFlashcard::getSortOrder));
        article.setFlashcards(cards);

        articleMapper.update(null,
            new LambdaUpdateWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getId, articleId)
                .setSql("view_count = view_count + 1"));
        article.setViewCount(article.getViewCount() + 1);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("article", article);

        if (studentId != null) {
            List<KnowledgeReviewSchedule> schedules = scheduleMapper.selectList(
                new LambdaQueryWrapper<KnowledgeReviewSchedule>()
                    .eq(KnowledgeReviewSchedule::getStudentId, studentId)
                    .eq(KnowledgeReviewSchedule::getArticleId, articleId));
            long mastered = schedules.stream().filter(s -> s.getIsMastered() == 1).count();
            result.put("progress", Map.of(
                "totalCards", cards.size(),
                "reviewedCards", schedules.size(),
                "masteredCards", mastered
            ));

            Long favCount = favoritesMapper.selectCount(
                new LambdaQueryWrapper<StudentFavorites>()
                    .eq(StudentFavorites::getStudentId, studentId)
                    .eq(StudentFavorites::getArticleId, articleId));
            result.put("isFavorited", favCount > 0);
        }
        return result;
    }

    public List<Map<String, Object>> getChapterTree(Long subjectId) {
        List<Map<String, Object>> rows = articleMapper.selectMaps(
            new QueryWrapper<KnowledgeArticle>()
                .select("chapter, task, COUNT(*) as cnt")
                .eq("status", "PUBLISHED")
                .eq("subject_id", subjectId)
                .groupBy("chapter, task")
                .orderByAsc("chapter, task"));

        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String chapter = (String) row.getOrDefault("chapter", "未分类");
            grouped.computeIfAbsent(chapter, k -> new ArrayList<>()).add(row);
        }

        List<KnowledgeNode> renderNodes = nodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getSubjectId, subjectId)
                .eq(KnowledgeNode::getLevel, 3)
                .isNotNull(KnowledgeNode::getRenderType)
                .ne(KnowledgeNode::getRenderType, ""));
        if (!renderNodes.isEmpty()) {
            List<KnowledgeNode> validNodes = renderNodes.stream()
                .filter(n -> n.getParentId() != null)
                .collect(Collectors.toList());
            if (!validNodes.isEmpty()) {
                Map<Long, List<KnowledgeNode>> byParent = validNodes.stream()
                    .collect(Collectors.groupingBy(KnowledgeNode::getParentId, LinkedHashMap::new, Collectors.toList()));
                if (!byParent.isEmpty()) {
                    List<KnowledgeNode> l2Nodes = nodeMapper.selectList(
                        new LambdaQueryWrapper<KnowledgeNode>()
                            .eq(KnowledgeNode::getSubjectId, subjectId)
                            .eq(KnowledgeNode::getLevel, 2)
                            .in(KnowledgeNode::getId, byParent.keySet()));
                    Map<Long, String> l2NameMap = l2Nodes.stream()
                        .collect(Collectors.toMap(KnowledgeNode::getId, KnowledgeNode::getName));
                    for (Map.Entry<Long, List<KnowledgeNode>> entry : byParent.entrySet()) {
                        String chapterName = l2NameMap.get(entry.getKey());
                        if (chapterName == null) continue;
                        List<Map<String, Object>> existing = grouped.computeIfAbsent(chapterName, k -> new ArrayList<>());
                        for (KnowledgeNode rn : entry.getValue()) {
                            Map<String, Object> vt = new LinkedHashMap<>();
                            vt.put("task", rn.getName());
                            vt.put("cnt", 0);
                            vt.put("renderType", rn.getRenderType());
                            existing.add(vt);
                        }
                    }
                }
            }
        }

        List<Map<String, Object>> tree = new ArrayList<>();
        grouped.forEach((chapter, tasks) -> {
            Map<String, Object> ch = new LinkedHashMap<>();
            ch.put("name", chapter);
            List<Map<String, Object>> taskList = tasks.stream().map(t -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", t.getOrDefault("task", "未分类"));
                m.put("count", t.getOrDefault("cnt", 0));
                Object rt = t.get("renderType");
                if (rt != null) m.put("renderType", rt);
                return m;
            }).collect(Collectors.toList());
            ch.put("tasks", taskList);
            ch.put("count", taskList.stream().mapToLong(t -> ((Number) t.get("count")).longValue()).sum());
            tree.add(ch);
        });
        return tree;
    }

    public List<Map<String, Object>> getTags(Long subjectId) {
        List<KnowledgeArticle> articles = articleMapper.selectList(
            new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, "PUBLISHED")
                .eq(KnowledgeArticle::getSubjectId, subjectId)
                .isNotNull(KnowledgeArticle::getTags));

        Map<String, Long> tagCount = new LinkedHashMap<>();
        for (KnowledgeArticle a : articles) {
            try {
                String tags = a.getTags();
                if (tags != null && !tags.isEmpty()) {
                    String[] arr = tags.replaceAll("[\\[\\]\"]", "").split(",");
                    for (String t : arr) {
                        String trimmed = t.trim();
                        if (!trimmed.isEmpty()) tagCount.merge(trimmed, 1L, Long::sum);
                    }
                }
            } catch (Exception e) { log.debug("解析标签失败: {}", e.getMessage()); }
        }
        return tagCount.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .map(e -> Map.<String, Object>of("name", e.getKey(), "count", e.getValue()))
            .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> toggleFavorite(Long studentId, Long articleId) {
        Long existCount = favoritesMapper.selectCount(
            new LambdaQueryWrapper<StudentFavorites>()
                .eq(StudentFavorites::getStudentId, studentId)
                .eq(StudentFavorites::getArticleId, articleId));
        boolean favorited;
        if (existCount > 0) {
            favoritesMapper.delete(
                new LambdaQueryWrapper<StudentFavorites>()
                    .eq(StudentFavorites::getStudentId, studentId)
                    .eq(StudentFavorites::getArticleId, articleId));
            favorited = false;
        } else {
            StudentFavorites f = new StudentFavorites();
            f.setStudentId(studentId);
            f.setArticleId(articleId);
            favoritesMapper.insert(f);
            favorited = true;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("favorited", favorited);
        return map;
    }

    public List<Map<String, Object>> getFavorites(Long studentId) {
        List<StudentFavorites> favs = favoritesMapper.selectList(
            new LambdaQueryWrapper<StudentFavorites>()
                .eq(StudentFavorites::getStudentId, studentId)
                .orderByDesc(StudentFavorites::getCreatedAt));

        if (favs.isEmpty()) return List.of();

        List<Long> articleIds = favs.stream().map(StudentFavorites::getArticleId).collect(Collectors.toList());
        Map<Long, KnowledgeArticle> articleMap = articleMapper.selectBatchIds(articleIds).stream()
            .collect(Collectors.toMap(KnowledgeArticle::getId, a -> a));

        return favs.stream().map(f -> {
            Map<String, Object> m = new LinkedHashMap<>();
            KnowledgeArticle a = articleMap.get(f.getArticleId());
            if (a != null) {
                m.put("id", a.getId());
                m.put("title", a.getTitle());
                m.put("excerpt", a.getExcerpt());
                m.put("chapter", a.getChapter());
                m.put("difficulty", a.getDifficulty());
                m.put("tags", a.getTags());
                m.put("viewCount", a.getViewCount());
            }
            m.put("favoritedAt", f.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> getProgress(Long studentId, Long subjectId) {
        Map<String, Object> stats = scheduleMapper.selectMaps(
            new QueryWrapper<KnowledgeReviewSchedule>()
                .select("COUNT(DISTINCT article_id) as studiedArticles",
                        "COUNT(*) as totalCards",
                        "SUM(is_mastered) as masteredCards",
                        "SUM(CASE WHEN next_review_at <= NOW() THEN 1 ELSE 0 END) as todayReview")
                .eq("student_id", studentId)).stream().findFirst().orElse(Map.of());

        LambdaQueryWrapper<KnowledgeArticle> aqw = new LambdaQueryWrapper<KnowledgeArticle>()
            .eq(KnowledgeArticle::getStatus, "PUBLISHED");
        if (subjectId != null) aqw.eq(KnowledgeArticle::getSubjectId, subjectId);
        long totalArticles = articleMapper.selectCount(aqw);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("studiedArticles", stats.getOrDefault("studiedArticles", 0L));
        map.put("totalArticles", totalArticles);
        map.put("totalCards", stats.getOrDefault("totalCards", 0L));
        map.put("masteredCards", stats.getOrDefault("masteredCards", 0L));
        map.put("todayReview", stats.getOrDefault("todayReview", 0L));
        return map;
    }

    public List<Map<String, Object>> search(String keyword, Long subjectId, int limit) {
        LambdaQueryWrapper<KnowledgeArticle> qw = new LambdaQueryWrapper<KnowledgeArticle>()
            .eq(KnowledgeArticle::getStatus, "PUBLISHED");
        if (subjectId != null) qw.eq(KnowledgeArticle::getSubjectId, subjectId);
        if (keyword != null && !keyword.isBlank())
            qw.and(w -> w.like(KnowledgeArticle::getTitle, keyword)
                        .or().like(KnowledgeArticle::getContentMd, keyword));

        int safeLimit = Math.min(Math.max(limit, 1), 50);
        List<KnowledgeArticle> articles = articleMapper.selectPage(
            new Page<>(1, safeLimit), qw).getRecords();

        return articles.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("excerpt", a.getExcerpt());
            m.put("chapter", a.getChapter());
            m.put("difficulty", a.getDifficulty());
            return m;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void saveQuizResult(Long studentId, Long articleId, int total, int correct, String wrongIds) {
        KnowledgeQuizResult result = new KnowledgeQuizResult();
        result.setStudentId(studentId);
        result.setArticleId(articleId);
        KnowledgeArticle article = articleMapper.selectById(articleId);
        result.setSubjectId(article != null ? article.getSubjectId() : null);
        result.setTotalQuestions(total);
        result.setCorrectCount(correct);
        result.setScore(total > 0 ? new BigDecimal(correct * 100.0 / total).setScale(1, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
        result.setWrongQuestionIds(wrongIds);
        quizResultMapper.insert(result);
    }

    public List<Map<String, Object>> getQuizHistory(Long studentId, Long articleId) {
        LambdaQueryWrapper<KnowledgeQuizResult> qw = new LambdaQueryWrapper<KnowledgeQuizResult>()
            .eq(KnowledgeQuizResult::getStudentId, studentId)
            .eq(KnowledgeQuizResult::getArticleId, articleId)
            .orderByDesc(KnowledgeQuizResult::getCreatedAt)
            .last("LIMIT 10");
        return quizResultMapper.selectList(qw).stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("score", r.getScore());
            m.put("totalQuestions", r.getTotalQuestions());
            m.put("correctCount", r.getCorrectCount());
            m.put("createdAt", r.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> getSubjectsGrouped(Long studentId) {
        Map<String, Object> result = new HashMap<>();

        List<DictSubject> allSubjects = subjectMapper.selectList(
            new LambdaQueryWrapper<DictSubject>().eq(DictSubject::getStatus, 1));
        Map<Long, DictSubject> subjectMap = allSubjects.stream()
            .collect(Collectors.toMap(DictSubject::getId, s -> s));

        List<KnowledgeArticle> articles = articleMapper.selectList(null);
        Map<Long, Long> articleCount = articles.stream()
            .collect(Collectors.groupingBy(KnowledgeArticle::getSubjectId, Collectors.counting()));

        List<DictMajorSubject> allMappings = majorSubjectMapper.selectList(null);
        Map<Long, Long> subjectMajorCount = allMappings.stream()
            .collect(Collectors.groupingBy(DictMajorSubject::getSubjectId, Collectors.counting()));

        List<DictMajor> allMajors = majorMapper.selectList(
            new LambdaQueryWrapper<DictMajor>().eq(DictMajor::getStatus, 1));
        Map<Long, String> majorNameMap = allMajors.stream()
            .collect(Collectors.toMap(DictMajor::getId, DictMajor::getMajorName));

        if (studentId == null) {
            result.put("majorName", null);

            Map<Long, String> subjectMajorMap = new HashMap<>();
            for (DictMajorSubject m : allMappings) {
                Long sid = m.getSubjectId();
                String mName = majorNameMap.get(m.getMajorId());
                if (mName == null) continue;
                if (subjectMajorMap.containsKey(sid)) {
                    subjectMajorMap.put(sid, null);
                } else {
                    subjectMajorMap.put(sid, mName);
                }
            }

            List<Map<String, Object>> publicSubjects = new ArrayList<>();
            Map<String, List<Map<String, Object>>> majorGroupsMap = new LinkedHashMap<>();

            for (DictSubject subject : allSubjects) {
                Long sid = subject.getId();
                Long count = subjectMajorCount.getOrDefault(sid, 0L);
                Map<String, Object> item = new HashMap<>();
                item.put("id", sid);
                item.put("name", subject.getSubjectName());
                item.put("articleCount", articleCount.getOrDefault(sid, 0L));

                if (count >= 2) {
                    publicSubjects.add(item);
                } else if (count == 1) {
                    String mName = subjectMajorMap.getOrDefault(sid, "其他");
                    majorGroupsMap.computeIfAbsent(mName, k -> new ArrayList<>()).add(item);
                } else {
                    majorGroupsMap.computeIfAbsent("其他", k -> new ArrayList<>()).add(item);
                }
            }

            List<Map<String, Object>> majorGroups = new ArrayList<>();
            for (Map.Entry<String, List<Map<String, Object>>> entry : majorGroupsMap.entrySet()) {
                Map<String, Object> group = new HashMap<>();
                group.put("majorName", entry.getKey());
                group.put("subjects", entry.getValue());
                majorGroups.add(group);
            }

            result.put("publicSubjects", publicSubjects);
            result.put("majorGroups", majorGroups);
            List<Map<String, Object>> allMajorSubjects = new ArrayList<>();
            for (Map<String, Object> group : majorGroups) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> subjects = (List<Map<String, Object>>) group.get("subjects");
                allMajorSubjects.addAll(subjects);
            }
            result.put("majorSubjects", allMajorSubjects);
            return result;
        }

        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            result.put("publicSubjects", List.of());
            result.put("majorSubjects", List.of());
            result.put("majorName", null);
            return result;
        }
        Classes clazz = classesMapper.selectById(student.getClassId());
        if (clazz == null || clazz.getMajor() == null) {
            result.put("publicSubjects", List.of());
            result.put("majorSubjects", List.of());
            result.put("majorName", null);
            return result;
        }

        String majorName = clazz.getMajor();
        DictMajor major = findMajorByName(majorName);
        if (major == null) {
            result.put("majorName", majorName);
            result.put("publicSubjects", List.of());
            result.put("majorSubjects", List.of());
            return result;
        }
        result.put("majorName", major.getMajorName());

        Set<Long> mySubjectIds = allMappings.stream()
            .filter(m -> m.getMajorId().equals(major.getId()))
            .map(DictMajorSubject::getSubjectId)
            .collect(Collectors.toSet());

        List<Map<String, Object>> publicSubjects = new ArrayList<>();
        List<Map<String, Object>> majorSubjects = new ArrayList<>();

        for (Long subjectId : mySubjectIds) {
            DictSubject subject = subjectMap.get(subjectId);
            if (subject == null) continue;

            Long count = subjectMajorCount.getOrDefault(subjectId, 0L);
            Map<String, Object> item = new HashMap<>();
            item.put("id", subjectId);
            item.put("name", subject.getSubjectName());
            item.put("articleCount", articleCount.getOrDefault(subjectId, 0L));

            if (count >= 2) {
                publicSubjects.add(item);
            } else {
                majorSubjects.add(item);
            }
        }

        result.put("publicSubjects", publicSubjects);
        result.put("majorSubjects", majorSubjects);
        return result;
    }

    public Map<String, Object> getSubjectsForTeacher(Set<Long> subjectIds) {
        Map<String, Object> result = new HashMap<>();
        if (subjectIds == null || subjectIds.isEmpty()) {
            result.put("publicSubjects", List.of());
            result.put("majorSubjects", List.of());
            return result;
        }

        List<DictSubject> allSubjects = subjectMapper.selectList(
            new LambdaQueryWrapper<DictSubject>().eq(DictSubject::getStatus, 1));
        Map<Long, String> subjectNameMap = allSubjects.stream()
            .collect(Collectors.toMap(DictSubject::getId, DictSubject::getSubjectName));

        List<KnowledgeArticle> articles = articleMapper.selectList(null);
        Map<Long, Long> articleCount = articles.stream()
            .collect(Collectors.groupingBy(KnowledgeArticle::getSubjectId, Collectors.counting()));

        List<DictMajorSubject> allMappings = majorSubjectMapper.selectList(null);
        Map<Long, Long> subjectMajorCount = allMappings.stream()
            .collect(Collectors.groupingBy(DictMajorSubject::getSubjectId, Collectors.counting()));

        List<Map<String, Object>> publicSubjects = new ArrayList<>();
        List<Map<String, Object>> majorSubjects = new ArrayList<>();

        for (Long subjectId : subjectIds) {
            String name = subjectNameMap.get(subjectId);
            if (name == null) continue;

            Long count = subjectMajorCount.getOrDefault(subjectId, 0L);
            Map<String, Object> item = new HashMap<>();
            item.put("id", subjectId);
            item.put("name", name);
            item.put("articleCount", articleCount.getOrDefault(subjectId, 0L));

            if (count >= 2) {
                publicSubjects.add(item);
            } else {
                majorSubjects.add(item);
            }
        }

        result.put("publicSubjects", publicSubjects);
        result.put("majorSubjects", majorSubjects);
        return result;
    }

    private DictMajor findMajorByName(String majorName) {
        DictMajor major = majorMapper.selectOne(
            new LambdaQueryWrapper<DictMajor>()
                .eq(DictMajor::getMajorName, majorName)
                .eq(DictMajor::getStatus, 1)
                .last("LIMIT 1"));
        if (major != null) return major;

        List<DictMajor> candidates = majorMapper.selectList(
            new LambdaQueryWrapper<DictMajor>()
                .like(DictMajor::getMajorName, majorName)
                .eq(DictMajor::getStatus, 1)
                .last("LIMIT 2"));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    // ======================== 知识清单（AI 生成，学生浏览） ========================

    /**
     * 分页获取学生可访问的知识清单（教师发布后学生可见，按学科过滤）
     */
    public Map<String, Object> listChecklists(Long studentId, String keyword, int page, int size) {
        // 1. 获取学生可访问的学科名称列表
        Map<String, Object> grouped = getSubjectsGrouped(studentId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> publicSubjects = (List<Map<String, Object>>) grouped.get("publicSubjects");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> majorSubjects = (List<Map<String, Object>>) grouped.get("majorSubjects");

        Set<String> accessibleSubjects = new LinkedHashSet<>();
        if (publicSubjects != null) {
            for (Map<String, Object> s : publicSubjects) {
                Object name = s.get("name");
                if (name != null) accessibleSubjects.add(name.toString());
            }
        }
        if (majorSubjects != null) {
            for (Map<String, Object> s : majorSubjects) {
                Object name = s.get("name");
                if (name != null) accessibleSubjects.add(name.toString());
            }
        }

        // 2. 无可访问学科 → 返回空
        if (accessibleSubjects.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("records", List.of());
            empty.put("total", 0L);
            empty.put("page", page);
            empty.put("size", size);
            return empty;
        }

        // 3. 查询已发布的知识清单
        LambdaQueryWrapper<AiOutput> qw = new LambdaQueryWrapper<>();
        qw.eq(AiOutput::getOutputType, "KNOWLEDGE_CHECKLIST");
        qw.eq(AiOutput::getStatus, 1);
        qw.eq(AiOutput::getIsLatest, 1);
        qw.in(AiOutput::getSubject, accessibleSubjects);
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(AiOutput::getTitle, keyword).or().like(AiOutput::getContent, keyword));
        }
        qw.orderByDesc(AiOutput::getCreatedAt);

        Page<AiOutput> result = aiOutputMapper.selectPage(new Page<>(page, size), qw);

        // 4. 转换为前端需要的格式
        List<Map<String, Object>> records = result.getRecords().stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("title", o.getTitle());
            m.put("subject", o.getSubject());
            m.put("createdAt", o.getCreatedAt());
            // 摘要：取 content 前 120 字符，剔除 Markdown 标记
            String content = o.getContent();
            String excerpt = "";
            if (content != null) {
                String plain = content.replaceAll("[#*_`>\\[\\]()!|~-]", " ").replaceAll("\\s+", " ").trim();
                excerpt = plain.length() > 120 ? plain.substring(0, 120) + "..." : plain;
            }
            m.put("excerpt", excerpt);
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> pageResult = new LinkedHashMap<>();
        pageResult.put("records", records);
        pageResult.put("total", result.getTotal());
        pageResult.put("page", page);
        pageResult.put("size", size);
        return pageResult;
    }

    /**
     * 获取单个知识清单详情（含完整 Markdown 内容），校验学科权限
     */
    public Map<String, Object> getChecklistDetail(Long id, Long studentId) {
        AiOutput o = aiOutputMapper.selectById(id);
        if (o == null) throw new BusinessException(404, "知识清单不存在");
        if (!"KNOWLEDGE_CHECKLIST".equals(o.getOutputType())) throw new BusinessException(404, "知识清单不存在");
        if (o.getStatus() == null || o.getStatus() != 1) throw new BusinessException(404, "知识清单未发布");

        // 校验学科权限
        Map<String, Object> grouped = getSubjectsGrouped(studentId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> publicSubjects = (List<Map<String, Object>>) grouped.get("publicSubjects");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> majorSubjects = (List<Map<String, Object>>) grouped.get("majorSubjects");
        Set<String> accessible = new LinkedHashSet<>();
        if (publicSubjects != null) publicSubjects.forEach(s -> { Object n = s.get("name"); if (n != null) accessible.add(n.toString()); });
        if (majorSubjects != null) majorSubjects.forEach(s -> { Object n = s.get("name"); if (n != null) accessible.add(n.toString()); });
        if (o.getSubject() != null && !accessible.contains(o.getSubject())) {
            throw new BusinessException(403, "无权访问此学科的知识清单");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", o.getId());
        result.put("title", o.getTitle());
        result.put("subject", o.getSubject());
        result.put("content", o.getContent());
        result.put("createdAt", o.getCreatedAt());
        return result;
    }
}
