package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import static com.school.teaching.service.impl.KnowledgeBaseHelper.*;
import com.school.teaching.service.ExamSyllabusService;
import com.school.teaching.service.TeacherService;
import com.school.teaching.utils.ScoreUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.school.teaching.agent.prompt.PromptTemplateCache;

@Service
public class KnowledgeBaseAdminService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseAdminService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ConcurrentHashMap<String, Map<String, Object>> regenProgress = new ConcurrentHashMap<>();

    @Lazy @Autowired private KnowledgeBaseAdminService self;

    @Autowired private KnowledgeArticleMapper articleMapper;
    @Autowired private KnowledgeFlashcardMapper flashcardMapper;
    @Autowired private KnowledgeReviewScheduleMapper scheduleMapper;
    @Autowired(required = false) private DeepSeekGateway deepSeekGateway;
    @Autowired(required = false) private CardQualityEvaluator cardQualityEvaluator;
    @Autowired(required = false) private ExamSyllabusService examSyllabusService;
    @Autowired private KnowledgeNodeMapper nodeMapper;
    @Autowired private StudentFavoritesMapper favoritesMapper;
    @Autowired private KnowledgeQuizResultMapper quizResultMapper;
    @Autowired(required = false) private CreditTransactionMapper creditTransactionMapper;
    @Autowired private TeacherService teacherService;
    @Autowired private DictSubjectMapper subjectMapper;
    @Autowired private ClassesMapper classesMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired(required = false) private PromptTemplateCache promptTemplateCache;

    @Transactional
    public KnowledgeArticle createArticle(KnowledgeArticle article) {
        if (article.getStatus() == null) article.setStatus("DRAFT");
        articleMapper.insert(article);
        return article;
    }

    @Transactional
    public KnowledgeArticle updateArticle(Long id, KnowledgeArticle article) {
        KnowledgeArticle exist = articleMapper.selectById(id);
        if (exist == null) throw new BusinessException(404, "文章不存在");
        article.setId(id);
        articleMapper.updateById(article);
        return articleMapper.selectById(id);
    }

    @Transactional
    public void deleteArticle(Long id) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) throw new BusinessException(404, "文章不存在");
        List<KnowledgeFlashcard> cards = flashcardMapper.selectList(
            new LambdaQueryWrapper<KnowledgeFlashcard>()
                .eq(KnowledgeFlashcard::getArticleId, id));
        List<Long> cardIds = cards.stream().map(KnowledgeFlashcard::getId).collect(Collectors.toList());
        if (!cardIds.isEmpty()) {
            scheduleMapper.delete(new LambdaQueryWrapper<KnowledgeReviewSchedule>()
                .in(KnowledgeReviewSchedule::getFlashcardId, cardIds));
            flashcardMapper.deleteBatchIds(cardIds);
        }
        scheduleMapper.delete(new LambdaQueryWrapper<KnowledgeReviewSchedule>()
            .eq(KnowledgeReviewSchedule::getArticleId, id));
        favoritesMapper.delete(new LambdaQueryWrapper<StudentFavorites>()
            .eq(StudentFavorites::getArticleId, id));
        articleMapper.deleteById(id);
    }

    public Map<String, Object> getAdminArticleList(Long subjectId, String status, String chapter, String task, int page, int size) {
        LambdaQueryWrapper<KnowledgeArticle> qw = new LambdaQueryWrapper<>();
        if (subjectId != null) qw.eq(KnowledgeArticle::getSubjectId, subjectId);
        if (status != null) qw.eq(KnowledgeArticle::getStatus, status);
        if (chapter != null && !chapter.isEmpty()) qw.eq(KnowledgeArticle::getChapter, chapter);
        if (task != null && !task.isEmpty()) qw.eq(KnowledgeArticle::getTask, task);
        qw.orderByDesc(KnowledgeArticle::getUpdatedAt);

        Page<KnowledgeArticle> result = articleMapper.selectPage(new Page<>(page, size), qw);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("records", result.getRecords());
        map.put("total", result.getTotal());
        return map;
    }

    public Map<String, Object> getAdminStats(Long subjectId) {
        Map<String, Object> stats = new LinkedHashMap<>();

        LambdaQueryWrapper<KnowledgeArticle> pubQw = new LambdaQueryWrapper<KnowledgeArticle>()
            .eq(KnowledgeArticle::getStatus, "PUBLISHED");
        if (subjectId != null) pubQw.eq(KnowledgeArticle::getSubjectId, subjectId);
        stats.put("totalArticles", articleMapper.selectCount(pubQw));

        LambdaQueryWrapper<KnowledgeArticle> draftQw = new LambdaQueryWrapper<KnowledgeArticle>()
            .eq(KnowledgeArticle::getStatus, "DRAFT");
        if (subjectId != null) draftQw.eq(KnowledgeArticle::getSubjectId, subjectId);
        stats.put("draftArticles", articleMapper.selectCount(draftQw));

        Long totalFlashcards;
        if (subjectId != null) {
            List<Long> subjectArticleIds = articleMapper.selectList(
                new LambdaQueryWrapper<KnowledgeArticle>()
                    .select(KnowledgeArticle::getId)
                    .eq(KnowledgeArticle::getSubjectId, subjectId))
                .stream().map(KnowledgeArticle::getId).collect(Collectors.toList());
            totalFlashcards = subjectArticleIds.isEmpty() ? 0L
                : flashcardMapper.selectCount(
                    new LambdaQueryWrapper<KnowledgeFlashcard>()
                        .in(KnowledgeFlashcard::getArticleId, subjectArticleIds));
        } else {
            totalFlashcards = flashcardMapper.selectCount(null);
        }
        stats.put("totalFlashcards", totalFlashcards);

        List<KnowledgeArticle> top = articleMapper.selectList(
            new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, "PUBLISHED")
                .orderByDesc(KnowledgeArticle::getViewCount)
                .last("LIMIT 10"));
        stats.put("topArticles", top);
        return stats;
    }

    @Transactional
    public Map<String, Object> importFromMarkdown(String basePath, Long subjectId) {
        java.io.File baseDir = new java.io.File(basePath).getAbsoluteFile();
        String allowedRoot = new java.io.File("docs/").getAbsoluteFile().getPath();
        if (!baseDir.getPath().startsWith(allowedRoot))
            throw new BusinessException(400, "导入路径仅限 docs/ 目录内");
        if (!baseDir.exists() || !baseDir.isDirectory())
            throw new BusinessException(400, "导入目录不存在: " + basePath);

        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        java.io.File[] chapters = baseDir.listFiles(java.io.File::isDirectory);
        if (chapters != null) {
            for (java.io.File chapterDir : chapters) {
                String chapterName = chapterDir.getName();
                java.io.File[] tasks = chapterDir.listFiles(java.io.File::isDirectory);
                if (tasks != null) {
                    for (java.io.File taskDir : tasks) {
                        String taskName = taskDir.getName();
                        java.io.File[] mds = taskDir.listFiles(f -> f.getName().endsWith(".md"));
                        if (mds != null) {
                            for (java.io.File md : mds) {
                                try {
                                    String content = java.nio.file.Files.readString(md.toPath());
                                    String title = md.getName().replace(".md", "");
                                    String[] lines = content.split("\n");
                                    for (String line : lines) {
                                        if (line.startsWith("# ")) {
                                            title = line.substring(2).trim();
                                            break;
                                        }
                                    }

                                    Long existCount = articleMapper.selectCount(
                                        new LambdaQueryWrapper<KnowledgeArticle>()
                                            .eq(KnowledgeArticle::getTitle, title)
                                            .eq(KnowledgeArticle::getChapter, chapterName));
                                    if (existCount > 0) { skipped++; continue; }

                                    KnowledgeArticle article = new KnowledgeArticle();
                                    article.setTitle(title);
                                    article.setContentMd(content);
                                    article.setExcerpt(content.length() > 200
                                        ? content.substring(0, 200) + "..." : content);
                                    article.setSubjectId(subjectId);
                                    article.setChapter(chapterName);
                                    article.setTask(taskName);
                                    article.setDifficulty(1);
                                    article.setStatus("PUBLISHED");

                                    extractSections(content, article);

                                    articleMapper.insert(article);

                                    List<KnowledgeFlashcard> cards = extractFlashcards(article);
                                    if (!cards.isEmpty()) {
                                        for (KnowledgeFlashcard card : cards) {
                                            card.setArticleId(article.getId());
                                            flashcardMapper.insert(card);
                                        }
                                    }

                                    imported++;
                                } catch (Exception e) {
                                    errors.add(md.getName() + ": " + e.getMessage());
                                    log.error("导入文件失败: {}", md.getName(), e);
                                }
                            }
                        }
                    }
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", imported);
        result.put("skipped", skipped);
        result.put("errors", errors);
        return result;
    }

    @Transactional
    public int generateFlashcardsWithAI(Long articleId) {
        return doGenerateFlashcardsWithAI(articleId, false);
    }

    @Transactional
    int doGenerateFlashcardsWithAI(Long articleId, boolean quiet) {
        KnowledgeArticle article = articleMapper.selectById(articleId);
        if (article == null) throw new BusinessException(404, "文章不存在");
        if (deepSeekGateway == null) throw new BusinessException(503, "AI 服务未配置");

        NodeContext ctx = buildNodeContext(article.getNodeId());

        var pr = com.school.teaching.service.CardPromptBuilder.build(
            ctx.cardProfileGroup, ctx.nodeContent, ctx.syllabusContext,
            article.getTitle(), article.getContentMd() != null ? article.getContentMd() : "",
            ctx.path, ctx.subjectName);
        String prompt = pr.prompt();
        if (promptTemplateCache != null) {
            String override = promptTemplateCache.getFinal("card_prompt", ctx.subjectName);
            if (override != null) prompt = override;
        }
        int maxTokens = pr.maxTokens();

        String response = quiet
            ? deepSeekGateway.generateContentQuiet(Map.of(
                "prompt", prompt, "maxTokens", maxTokens, "temperature", 0.7))
            : deepSeekGateway.generateContent(Map.of(
                "prompt", prompt, "maxTokens", maxTokens, "temperature", 0.7));
        if (response == null || response.isBlank()) return 0;

        List<Map<String, Object>> cardDataList = parseCardArray(response);
        if (cardDataList.isEmpty()) return 0;

        flashcardMapper.delete(new LambdaQueryWrapper<KnowledgeFlashcard>()
            .eq(KnowledgeFlashcard::getArticleId, articleId));

        int sortOrder = 0;
        List<Long> newCardIds = new ArrayList<>();
        for (Map<String, Object> data : cardDataList) {
            String frontText = String.valueOf(data.getOrDefault("frontText", ""));
            String backText = String.valueOf(data.getOrDefault("backText", ""));
            if (frontText.isBlank() || backText.isBlank()) continue;

            KnowledgeFlashcard card = new KnowledgeFlashcard();
            card.setArticleId(articleId);
            card.setFrontText(frontText.trim());
            card.setBackText(backText.trim());
            card.setSortOrder(sortOrder++);
            String cardType = String.valueOf(data.getOrDefault("cardType", "DEFINITION"));
            card.setCardType(List.of("DEFINITION", "PROCEDURE", "COMPARISON", "APPLICATION", "SCENARIO")
                .contains(cardType) ? cardType : "DEFINITION");
            if (!ctx.path.isEmpty()) card.setContextPath(ctx.path);
            Object qid = data.get("linkedQuestionId");
            if (qid instanceof Number) card.setLinkedQuestionId(((Number) qid).longValue());

            flashcardMapper.insert(card);
            newCardIds.add(card.getId());
        }

        if (cardQualityEvaluator != null && !newCardIds.isEmpty()) {
            List<Long> ids = new ArrayList<>(newCardIds);
            org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        cardQualityEvaluator.batchEvaluate(ids);
                    }
                });
        }

        log.info("AI生成卡片完成: articleId={} count={} path={}", articleId, newCardIds.size(), ctx.path);
        return newCardIds.size();
    }

    private NodeContext buildNodeContext(Long startNodeId) {
        NodeContext ctx = new NodeContext();
        if (startNodeId == null) return ctx;
        KnowledgeNode startNode = nodeMapper.selectById(startNodeId);
        if (startNode == null) return ctx;

        Long subjectId = startNode.getSubjectId();
        if (subjectId == null) { ctx.path = startNode.getName(); return ctx; }
        Map<Long, KnowledgeNode> nodeMap = nodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>().eq(KnowledgeNode::getSubjectId, subjectId))
            .stream().collect(Collectors.toMap(KnowledgeNode::getId, n -> n, (a, b) -> a));

        List<String> path = new ArrayList<>();
        KnowledgeNode current = startNode;
        KnowledgeNode rootNode = null;
        for (int i = 0; i < 8 && current != null; i++) {
            path.add(0, current.getName());
            if (current.getLevel() != null && current.getLevel() == 1) { rootNode = current; break; }
            current = nodeMap.get(current.getParentId());
        }
        ctx.path = String.join(" > ", path);
        ctx.subjectName = rootNode != null ? rootNode.getName() : "";

        ctx.nodeContent = (startNode.getContent() != null) ? startNode.getContent() : "";
        if (subjectId != null) {
            DictSubject subject = subjectMapper.selectById(subjectId);
            ctx.cardProfileGroup = (subject != null && subject.getCardProfileGroup() != null)
                ? subject.getCardProfileGroup() : "";
        }

        if (rootNode != null && examSyllabusService != null) {
            try {
                var syllabi = examSyllabusService.getSyllabiByNodeId(rootNode.getId());
                if (syllabi != null && !syllabi.isEmpty()) {
                    String meta = syllabi.get(0).getSyllabusMeta();
                    ctx.syllabusContext = (meta != null && !meta.isBlank()) ? meta
                        : (syllabi.get(0).getContent() != null
                            ? syllabi.get(0).getContent().substring(0, Math.min(500, syllabi.get(0).getContent().length())) : "");
                }
            } catch (Exception ignored) { log.debug("获取考纲上下文失败", ignored); }
        }
        return ctx;
    }

    private static class NodeContext {
        String path = "", subjectName = "", syllabusContext = "",
               cardProfileGroup = "", nodeContent = "";
    }

    @Transactional
    public int generateFlashcards(Long articleId) {
        KnowledgeArticle article = articleMapper.selectById(articleId);
        if (article == null) throw new BusinessException(404, "文章不存在");

        flashcardMapper.delete(new LambdaQueryWrapper<KnowledgeFlashcard>()
            .eq(KnowledgeFlashcard::getArticleId, articleId));

        List<KnowledgeFlashcard> cards = extractFlashcards(article);
        if (cards.isEmpty()) return 0;

        for (KnowledgeFlashcard card : cards) {
            flashcardMapper.insert(card);
        }
        return cards.size();
    }

    @Transactional
    public int generateFlashcardsBatch(int limit) {
        int maxLimit = Math.min(limit > 0 ? limit : 50, 100);
        List<Long> articleIds = new ArrayList<>();
        List<Map<String, Object>> rows = articleMapper.selectMaps(
            new QueryWrapper<KnowledgeArticle>()
                .select("id")
                .eq("status", "PUBLISHED")
                .apply("NOT EXISTS (SELECT 1 FROM knowledge_flashcards f WHERE f.article_id = knowledge_articles.id)")
                .last("LIMIT " + maxLimit));
        for (Map<String, Object> row : rows) {
            Object id = row.get("id");
            if (id instanceof Number) articleIds.add(((Number) id).longValue());
        }

        if (articleIds.isEmpty()) return 0;
        List<KnowledgeArticle> articles = articleMapper.selectBatchIds(articleIds);

        int processed = 0;
        for (KnowledgeArticle article : articles) {
            List<KnowledgeFlashcard> cards = extractFlashcards(article);
            if (!cards.isEmpty()) {
                for (KnowledgeFlashcard card : cards) {
                    card.setArticleId(article.getId());
                    flashcardMapper.insert(card);
                }
                processed++;
                log.info("为文章 [{}] {} 生成了 {} 张卡片", article.getId(), article.getTitle(), cards.size());
            }
        }
        return processed;
    }

    public List<Map<String, Object>> getClassStats(Long teacherUserId, Long subjectId) {
        List<Long> classIds = teacherService.getAccessibleClassIds(teacherUserId);
        if (classIds == null || classIds.isEmpty()) return Collections.emptyList();
        List<Map<String, Object>> result = new ArrayList<>();
        List<KnowledgeArticle> articles = articleMapper.selectList(
            new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getSubjectId, subjectId)
                .eq(KnowledgeArticle::getStatus, "PUBLISHED"));
        for (Long classId : classIds) {
            Classes cls = classesMapper.selectById(classId);
            if (cls == null) continue;
            List<Student> students = studentMapper.selectList(
                new LambdaQueryWrapper<Student>().eq(Student::getClassId, classId));
            List<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toList());
            if (studentIds.isEmpty()) continue;
            List<KnowledgeQuizResult> quizResults = quizResultMapper.selectList(
                new LambdaQueryWrapper<KnowledgeQuizResult>()
                    .in(KnowledgeQuizResult::getStudentId, studentIds)
                    .eq(KnowledgeQuizResult::getSubjectId, subjectId));
            Map<Long, List<KnowledgeQuizResult>> byArticle = quizResults.stream()
                .collect(Collectors.groupingBy(KnowledgeQuizResult::getArticleId));
            Set<Long> activeStudentIds = quizResults.stream()
                .map(KnowledgeQuizResult::getStudentId).collect(Collectors.toSet());
            List<Long> inactiveStudentIds = studentIds.stream()
                .filter(sid -> !activeStudentIds.contains(sid)).collect(Collectors.toList());
            List<Map<String, Object>> articleStats = new ArrayList<>();
            Map<String, Long> tagCount = new LinkedHashMap<>();
            for (KnowledgeArticle a : articles) {
                List<KnowledgeQuizResult> results = byArticle.getOrDefault(a.getId(), Collections.emptyList());
                List<BigDecimal> articleScores = results.stream().filter(r -> r.getScore() != null)
                    .map(KnowledgeQuizResult::getScore).collect(Collectors.toList());
                double avgScore = ScoreUtils.avg(articleScores);
                if (results.isEmpty() && avgScore == 0) continue;
                Map<String, Object> stat = new LinkedHashMap<>();
                stat.put("articleId", a.getId());
                stat.put("title", a.getTitle());
                stat.put("chapter", a.getChapter());
                stat.put("attemptedCount", results.size());
                stat.put("totalStudents", studentIds.size());
                stat.put("avgScore", Math.round(avgScore * 10) / 10.0);
                articleStats.add(stat);
                if (avgScore < 60 && a.getTags() != null && !a.getTags().isEmpty()) {
                    try {
                        List<String> tags = OBJECT_MAPPER.readValue(a.getTags(), List.class);
                        for (String t : tags) if (t != null) tagCount.merge(t.trim(), 1L, Long::sum);
                    } catch (Exception e) { /* skip */ }
                }
            }
            articleStats.sort((a, b) -> Double.compare((Double) a.get("avgScore"), (Double) b.get("avgScore")));
            List<Map<String, Object>> weakTags = tagCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(10)
                .map(e -> { Map<String, Object> m = new LinkedHashMap<>(); m.put("tag", e.getKey()); m.put("count", e.getValue()); return m; })
                .collect(Collectors.toList());
            List<Map<String, Object>> inactiveStudents = students.stream()
                .filter(s -> !activeStudentIds.contains(s.getId()))
                .map(s -> { Map<String, Object> m = new LinkedHashMap<>(); m.put("studentId", s.getId()); m.put("studentNumber", s.getStudentNumber() != null ? s.getStudentNumber() : ("UID:" + s.getUserId())); return m; })
                .collect(Collectors.toList());
            Map<String, Object> classData = new LinkedHashMap<>();
            classData.put("classId", classId);
            classData.put("className", cls.getClassName());
            classData.put("studentCount", studentIds.size());
            classData.put("activeStudentCount", activeStudentIds.size());
            classData.put("articleStats", articleStats);
            classData.put("classWeakTags", weakTags);
            classData.put("inactiveStudents", inactiveStudents);
            result.add(classData);
        }
        return result;
    }

    @Transactional
    public int clearAllFlashcards(Long subjectId) {
        List<Long> cardIds;
        if (subjectId != null) {
            List<KnowledgeArticle> articles = articleMapper.selectList(
                new LambdaQueryWrapper<KnowledgeArticle>()
                    .eq(KnowledgeArticle::getSubjectId, subjectId));
            List<Long> articleIds = articles.stream()
                .map(KnowledgeArticle::getId).collect(Collectors.toList());
            if (articleIds.isEmpty()) return 0;
            cardIds = flashcardMapper.selectList(
                new LambdaQueryWrapper<KnowledgeFlashcard>()
                    .in(KnowledgeFlashcard::getArticleId, articleIds))
                .stream().map(KnowledgeFlashcard::getId).collect(Collectors.toList());
        } else {
            cardIds = flashcardMapper.selectList(null)
                .stream().map(KnowledgeFlashcard::getId).collect(Collectors.toList());
        }
        if (cardIds.isEmpty()) return 0;

        scheduleMapper.delete(
            new LambdaQueryWrapper<KnowledgeReviewSchedule>()
                .in(KnowledgeReviewSchedule::getFlashcardId, cardIds));

        return flashcardMapper.deleteBatchIds(cardIds);
    }

    @Async
    public void regenerateAllFlashcardsAsync(Long subjectId) {
        String progressKey = subjectId != null ? String.valueOf(subjectId) : "_all_";
        Map<String, Object> prog = new LinkedHashMap<>();
        prog.put("status", "running");
        prog.put("total", 0);
        prog.put("generated", 0);
        prog.put("failed", 0);
        prog.put("currentBatch", 0);
        prog.put("totalBatches", 0);
        regenProgress.put(progressKey, prog);

        log.info("异步卡片重生开始 subjectId={}", subjectId);

        try {
            int cleared = self.clearAllFlashcards(subjectId);
            log.info("已清空 {} 张旧卡片", cleared);

            var qw = new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, "PUBLISHED");
            if (subjectId != null) qw.eq(KnowledgeArticle::getSubjectId, subjectId);
            List<KnowledgeArticle> articles = articleMapper.selectList(qw);

            int total = articles.size();
            prog.put("total", total);

            if (articles.isEmpty()) {
                log.info("无已发布文章，跳过卡片生成");
                prog.put("status", "done");
                scheduleRegenCleanup(progressKey);
                return;
            }

            int generated = 0, failed = 0;
            int chunkSize = 10;
            int totalBatches = (total + chunkSize - 1) / chunkSize;
            prog.put("totalBatches", totalBatches);

            for (int i = 0; i < total; i += chunkSize) {
                int end = Math.min(i + chunkSize, total);
                int batchNo = (i / chunkSize) + 1;
                prog.put("currentBatch", batchNo);
                log.info("异步卡片重生: 批次 {}/{} (文章 {}-{})", batchNo, totalBatches, i + 1, end);

                for (int j = i; j < end; j++) {
                    var article = articles.get(j);
                    try {
                        int count = self.doGenerateFlashcardsWithAI(article.getId(), true);
                        if (count > 0) generated++;
                        else failed++;
                        log.debug("AI生成文章[{}] {} → {}张卡片", article.getId(), article.getTitle(), count);
                    } catch (Exception e) {
                        failed++;
                        log.warn("AI生成失败 articleId={} title={}", article.getId(), article.getTitle(), e);
                    }
                }

                prog.put("generated", generated);
                prog.put("failed", failed);

                if (end < total) {
                    try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                }
            }

            prog.put("status", "done");
            log.info("异步卡片重生完成: total={} generated={} failed={} cleared={}",
                total, generated, failed, cleared);
        } catch (Exception e) {
            prog.put("status", "error");
            prog.put("error", e.getMessage());
            log.error("异步卡片重生异常 subjectId={}", subjectId, e);
        }
        scheduleRegenCleanup(progressKey);
    }

    private void scheduleRegenCleanup(String key) {
        CompletableFuture.runAsync(() -> {
            try { Thread.sleep(300_000); } catch (InterruptedException ignored) {}
            regenProgress.remove(key);
        });
    }

    public Map<String, Object> getRegenerationProgress(Long subjectId) {
        String key = subjectId != null ? String.valueOf(subjectId) : "_all_";
        return regenProgress.getOrDefault(key, Map.of("status", "idle"));
    }

    public long countPublishedArticles(Long subjectId) {
        var qw = new LambdaQueryWrapper<KnowledgeArticle>()
            .eq(KnowledgeArticle::getStatus, "PUBLISHED");
        if (subjectId != null) qw.eq(KnowledgeArticle::getSubjectId, subjectId);
        return articleMapper.selectCount(qw);
    }
}
