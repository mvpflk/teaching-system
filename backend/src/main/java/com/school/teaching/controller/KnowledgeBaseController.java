package com.school.teaching.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.KnowledgeArticle;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.CardRecommendationService;
import com.school.teaching.service.SystemService;
import com.school.teaching.service.impl.KnowledgeBaseAdminService;
import com.school.teaching.service.impl.KnowledgeBaseRecommendService;
import com.school.teaching.service.impl.KnowledgeBaseReviewService;
import com.school.teaching.service.impl.KnowledgeBaseStudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/knowledge-base")
@Tag(name = "知识库", description = "知识文章、章节树、闪卡复习、学习进度")
public class KnowledgeBaseController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseController.class);

    @Autowired private KnowledgeBaseStudentService knowledgeBaseStudentService;
    @Autowired private KnowledgeBaseReviewService knowledgeBaseReviewService;
    @Autowired private KnowledgeBaseRecommendService knowledgeBaseRecommendService;
    @Autowired private KnowledgeBaseAdminService knowledgeBaseAdminService;
    @Autowired private CardRecommendationService cardRecommendationService;
    @Autowired private SystemService systemService;
    @Autowired private com.school.teaching.security.StudentResolver studentResolver;
    @Autowired(required = false) private com.school.teaching.service.BehaviorTrackService behaviorTrackService;

    private void track(String type, Map<String, Object> data) {
        if (behaviorTrackService != null) behaviorTrackService.track(type, data);
    }

    private void checkEnabled() {
        if (!systemService.getBooleanConfig("feature.knowledge_base", false))
            throw new BusinessException(403, "知识库模块未开启");
    }

    @GetMapping("/articles")
    @Operation(summary = "分页获取知识文章", description = "获取知识库文章列表，支持多种筛选条件")
    @Parameter(name = "subjectId", description = "学科ID")
    @Parameter(name = "chapter", description = "章节筛选")
    @Parameter(name = "task", description = "任务类型筛选")
    @Parameter(name = "tags", description = "标签筛选")
    @Parameter(name = "difficulty", description = "难度筛选")
    @Parameter(name = "keyword", description = "搜索关键词")
    @Parameter(name = "page", description = "页码", example = "1")
    @Parameter(name = "size", description = "每页数量", example = "20")
    public R<Map<String, Object>> listArticles(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String chapter,
            @RequestParam(required = false) String task,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        checkEnabled();
        return R.ok(knowledgeBaseStudentService.listArticles(subjectId, chapter, task, tags, difficulty, keyword, page, size));
    }

    @GetMapping("/articles/{id}")
    @Operation(summary = "获取文章详情", description = "获取知识文章的详细内容")
    @Parameter(name = "id", description = "文章ID", required = true, example = "123")
    public R<Map<String, Object>> getArticle(@PathVariable Long id) {
        checkEnabled();
        Long studentId = SecurityUtils.isStudent() ? SecurityUtils.getCurrentUserId() : null;
        track("KNOWLEDGE_ARTICLE_VIEW", Map.of("articleId", id));
        return R.ok(knowledgeBaseStudentService.getArticleDetail(id, studentId));
    }

    @GetMapping("/chapters")
    @Operation(summary = "获取章节树", description = "获取指定学科的章节树结构")
    @Parameter(name = "subjectId", description = "学科ID", required = true, example = "1")
    public R<?> getChapterTree(@RequestParam Long subjectId) {
        checkEnabled();
        return R.ok(knowledgeBaseStudentService.getChapterTree(subjectId));
    }

    @GetMapping("/tags")
    @Operation(summary = "获取知识标签", description = "获取指定学科的知识标签列表")
    @Parameter(name = "subjectId", description = "学科ID", required = true, example = "1")
    public R<?> getTags(@RequestParam Long subjectId) {
        checkEnabled();
        return R.ok(knowledgeBaseStudentService.getTags(subjectId));
    }

    @GetMapping("/review/today")
    @Operation(summary = "获取今日复习任务", description = "学生获取今日需要复习的闪卡列表")
    public R<?> getTodayReview() {
        checkEnabled();
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        track("FLASHCARD_REVIEW", Map.of());
        return R.ok(knowledgeBaseReviewService.getTodayReviewCards(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/review/rate")
    @Operation(summary = "评价闪卡", description = "学生对复习的闪卡进行评价（SM-2算法）")
    public R<?> rateFlashcard(@RequestBody Map<String, Integer> body) {
        checkEnabled();
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        track("FLASHCARD_RATE", Map.of("cardId", body.getOrDefault("cardId", 0)));
        Integer flashcardId = body.get("flashcardId");
        Integer rating = body.get("rating");
        if (flashcardId == null || rating == null) return R.error(400, "缺少 flashcardId 或 rating");
        return R.ok(knowledgeBaseReviewService.rateFlashcard(studentResolver.resolveCurrentStudentId(), flashcardId.longValue(), rating));
    }

    @PostMapping("/articles/{id}/start-learning")
    @Operation(summary = "开始学习文章", description = "学生将文章加入学习队列")
    @Parameter(name = "id", description = "文章ID", required = true, example = "123")
    public R<?> startLearning(@PathVariable Long id) {
        checkEnabled();
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        knowledgeBaseReviewService.startLearning(SecurityUtils.getCurrentUserId(), id);
        track("KNOWLEDGE_START_LEARNING", Map.of("articleId", id));
        return R.ok(null, "已加入学习队列");
    }

    @GetMapping("/progress")
    @Operation(summary = "获取学习进度", description = "学生获取知识库学习进度")
    @Parameter(name = "subjectId", description = "学科ID")
    public R<?> getProgress(@RequestParam(required = false) Long subjectId) {
        checkEnabled();
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        return R.ok(knowledgeBaseStudentService.getProgress(SecurityUtils.getCurrentUserId(), subjectId));
    }

    @PostMapping("/articles/{id}/favorite")
    @Operation(summary = "收藏/取消收藏文章", description = "切换文章的收藏状态")
    @Parameter(name = "id", description = "文章ID", required = true, example = "123")
    public R<?> toggleFavorite(@PathVariable Long id) {
        checkEnabled();
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        track("KNOWLEDGE_ARTICLE_FAVORITE", Map.of("articleId", id));
        return R.ok(knowledgeBaseStudentService.toggleFavorite(SecurityUtils.getCurrentUserId(), id));
    }

    @GetMapping("/favorites")
    @Operation(summary = "获取收藏列表", description = "学生获取收藏的文章列表")
    public R<?> getFavorites() {
        checkEnabled();
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        return R.ok(knowledgeBaseStudentService.getFavorites(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索知识库", description = "全文搜索知识库内容")
    @Parameter(name = "keyword", description = "搜索关键词", required = true)
    @Parameter(name = "subjectId", description = "学科ID")
    @Parameter(name = "limit", description = "返回数量", example = "20")
    public R<?> search(@RequestParam String keyword,
                       @RequestParam(required = false) Long subjectId,
                       @RequestParam(defaultValue = "20") int limit) {
        checkEnabled();
        track("KNOWLEDGE_SEARCH", Map.of("keyword", keyword));
        return R.ok(knowledgeBaseStudentService.search(keyword, subjectId, limit));
    }

    @PostMapping("/articles/{id}/quiz-result")
    @Operation(summary = "保存自测结果", description = "学生保存文章自测结果")
    @Parameter(name = "id", description = "文章ID", required = true, example = "123")
    public R<?> saveQuizResult(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        checkEnabled();
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        int total = (int) body.getOrDefault("totalQuestions", 0);
        int correct = (int) body.getOrDefault("correctCount", 0);
        String wrongIds = body.get("wrongQuestionIds") != null ? body.get("wrongQuestionIds").toString() : null;
        track("KNOWLEDGE_ARTICLE_QUIZ", Map.of("articleId", id, "correct", correct, "total", total));
        knowledgeBaseStudentService.saveQuizResult(SecurityUtils.getCurrentUserId(), id, total, correct, wrongIds);
        return R.ok(null, "自测结果已保存");
    }

    @GetMapping("/articles/{id}/quiz-history")
    @Operation(summary = "获取自测历史", description = "学生查看文章的自测历史记录")
    @Parameter(name = "id", description = "文章ID", required = true, example = "123")
    public R<?> getQuizHistory(@PathVariable Long id) {
        checkEnabled();
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        return R.ok(knowledgeBaseStudentService.getQuizHistory(SecurityUtils.getCurrentUserId(), id));
    }

    @GetMapping("/weak-analysis")
    @Operation(summary = "获取薄弱分析", description = "学生获取薄弱知识点分析")
    @Parameter(name = "subjectId", description = "学科ID", required = true, example = "1")
    public R<?> getWeakAnalysis(@RequestParam Long subjectId) {
        checkEnabled();
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        return R.ok(knowledgeBaseRecommendService.getWeakAnalysis(SecurityUtils.getCurrentUserId(), subjectId));
    }

    @GetMapping("/recommendations")
    @Operation(summary = "获取推荐内容", description = "学生获取个性化推荐的知识内容")
    @Parameter(name = "subjectId", description = "学科ID", required = true, example = "1")
    public R<?> getRecommendations(@RequestParam Long subjectId) {
        checkEnabled();
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        return R.ok(knowledgeBaseRecommendService.getRecommendations(SecurityUtils.getCurrentUserId(), subjectId));
    }

    @GetMapping("/daily-stats")
    @Operation(summary = "获取每日学习统计", description = "学生获取每日学习统计数据")
    @Parameter(name = "subjectId", description = "学科ID", example = "24")
    public R<?> getDailyStats(@RequestParam(defaultValue = "24") Long subjectId) {
        checkEnabled();
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        return R.ok(knowledgeBaseRecommendService.getDailyStats(SecurityUtils.getCurrentUserId(), subjectId));
    }

    @GetMapping("/admin/class-stats")
    @Operation(summary = "获取班级学习统计", description = "教师查看班级知识库学习统计数据")
    @Parameter(name = "subjectId", description = "学科ID", example = "24")
    public R<?> getClassStats(@RequestParam(defaultValue = "24") Long subjectId) {
        checkEnabled();
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可访问");
        return R.ok(knowledgeBaseAdminService.getClassStats(SecurityUtils.getCurrentUserId(), subjectId));
    }

    /**
     * 按专业分组返回学科列表
     * - 学生：按专业返回公共基础课 + 专业课
     * - 教师：仅返回所授学科
     * - 管理员：返回全部学科
     */
    @GetMapping("/subjects-grouped")
    @Operation(summary = "获取分组学科列表", description = "按专业分组返回学科列表（学生/教师/管理员视图不同）")
    public R<Map<String, Object>> getSubjectsGrouped() {
        checkEnabled();
        // 学生 → 按专业分组
        if (SecurityUtils.isStudent()) {
            Long studentId = studentResolver.resolveCurrentStudentId();
            return R.ok(knowledgeBaseStudentService.getSubjectsGrouped(studentId));
        }
        // 教师（非管理员）→ 仅所授学科
        if (SecurityUtils.isTeacherOrAdmin() && !SecurityUtils.isAdmin() && !SecurityUtils.isSuperAdmin()) {
            var teacherSvc = SecurityUtils.teacherService();
            if (teacherSvc != null) {
                java.util.Set<Long> subjectIds = new java.util.LinkedHashSet<>();
                for (var assign : teacherSvc.getTeachingAssignments(SecurityUtils.getCurrentUserId())) {
                    Object sid = assign.get("subjectId");
                    if (sid instanceof Number num && num.longValue() > 0) {
                        subjectIds.add(num.longValue());
                    }
                }
                if (!subjectIds.isEmpty()) {
                    return R.ok(knowledgeBaseStudentService.getSubjectsForTeacher(subjectIds));
                }
            }
            // 教师未分配学科 → 返回空
            return R.ok(Map.of("publicSubjects", java.util.List.of(), "majorSubjects", java.util.List.of()));
        }
        // 管理员 → 全部学科
        return R.ok(knowledgeBaseStudentService.getSubjectsGrouped(null));
    }

    @GetMapping("/daily-card")
    @Operation(summary = "获取每日推荐卡片", description = "学生获取每日知识推荐卡片")
    public R<?> getDailyCard() {
        try {
            checkEnabled();
            if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
            Long studentId = studentResolver.resolveCurrentStudentId();
            if (studentId == null) return R.ok(Map.of("cardId", 0, "reason", "no_student_record"));
            track("DAILY_CARD_VIEW", Map.of());
            var dto = cardRecommendationService.getDailyCard(studentId);
            if (dto == null) return R.ok(Map.of("cardId", 0, "reason", "none"));
            return R.ok(dto);
        } catch (Exception e) {
            log.error("getDailyCard 异常 studentId={}", studentResolver.resolveCurrentStudentId(), e);
            return R.error(500, "每日卡片加载失败: " + e.getMessage());
        }
    }

    @GetMapping("/submissions/{submissionId}/related-cards")
    @Operation(summary = "获取关联卡片", description = "根据提交记录获取相关知识卡片")
    @Parameter(name = "submissionId", description = "提交ID", required = true, example = "456")
    @Parameter(name = "limit", description = "返回数量", example = "5")
    public R<?> getRelatedCards(@PathVariable Long submissionId,
                                @RequestParam(defaultValue = "5") int limit) {
        checkEnabled();
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        Long studentId = studentResolver.resolveCurrentStudentId();
        if (studentId == null) return R.ok(Map.of("totalCards", 0, "reason", "no_student_record"));
        track("RELATED_CARDS_VIEW", Map.of("submissionId", submissionId));
        var dto = cardRecommendationService.getRelatedCards(submissionId, studentId, limit);
        if (dto == null) return R.ok(Map.of("totalCards", 0, "reason", "no_wrong"));
        return R.ok(dto);
    }

    @GetMapping("/nodes/{nodeId}/cards")
    @Operation(summary = "按节点获取卡片", description = "获取指定知识节点的关联卡片")
    @Parameter(name = "nodeId", description = "节点ID", required = true, example = "3096")
    @Parameter(name = "limit", description = "返回数量", example = "5")
    public R<?> getCardsByNodeId(@PathVariable Long nodeId,
                                 @RequestParam(defaultValue = "5") int limit) {
        checkEnabled();
        track("NODE_CARDS_VIEW", Map.of("nodeId", nodeId));
        var cards = cardRecommendationService.getCardsByNodeId(nodeId, limit);
        return R.ok(Map.of("cards", cards, "totalCards", cards.size()));
    }

    @GetMapping("/exam-prep-pack")
    @Operation(summary = "获取备考包", description = "学生获取指定任务的备考知识包")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "123")
    public R<?> getExamPrepPack(@RequestParam Long taskId) {
        checkEnabled();
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        Long studentId = studentResolver.resolveCurrentStudentId();
        if (studentId == null) return R.ok(Map.of("reason", "no_student_record"));
        track("EXAM_PREP_PACK", Map.of("taskId", taskId));
        var dto = cardRecommendationService.getExamPrepPack(studentId, taskId);
        if (dto == null) return R.ok(Map.of("reason", "no_questions"));
        return R.ok(dto);
    }

    // ══════════════════════════════════════════
    //  v167: 卡片审核 + 考纲权重
    // ══════════════════════════════════════════

    /**
     * 教师获取待审核卡片队列，按优先级排序
     */
    @GetMapping("/flashcards/review-queue")
    @Operation(summary = "获取卡片审核队列", description = "教师获取待审核的闪卡队列")
    @Parameter(name = "subjectId", description = "学科ID")
    @Parameter(name = "status", description = "状态: PENDING/APPROVED/REJECTED", example = "PENDING")
    @Parameter(name = "page", description = "页码", example = "1")
    @Parameter(name = "size", description = "每页数量", example = "20")
    public R<?> getReviewQueue(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        checkEnabled();
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师/管理员可访问");
        return R.ok(cardRecommendationService.getReviewQueue(subjectId, status, page, size));
    }

    /**
     * 教师批量审核卡片
     * Body: { cardIds: [1,2], action: "APPROVED"|"REJECTED", adoptAiVersion: false }
     */
    @PostMapping("/flashcards/batch-review")
    @Operation(summary = "批量审核卡片", description = "教师批量审核闪卡（通过/驳回）")
    public R<?> batchReview(@RequestBody Map<String, Object> body) {
        checkEnabled();
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师/管理员可访问");
        @SuppressWarnings("unchecked")
        List<Integer> cardIdsRaw = (List<Integer>) body.get("cardIds");
        if (cardIdsRaw == null || cardIdsRaw.isEmpty()) return R.error(400, "请选择至少一张卡片");
        List<Long> cardIds = cardIdsRaw.stream().map(Integer::longValue).collect(java.util.stream.Collectors.toList());
        String action = String.valueOf(body.getOrDefault("action", "APPROVED"));
        boolean adoptAiVersion = Boolean.TRUE.equals(body.get("adoptAiVersion"));
        Long reviewerId = SecurityUtils.getCurrentUserId();
        int count = cardRecommendationService.batchReviewCards(cardIds, action, adoptAiVersion, reviewerId);
        return R.ok(Map.of("reviewedCount", count), "审核完成");
    }

    /**
     * 教师标记知识点考纲权重
     * Body: { nodeId: 3096, examWeight: "HIGH" }
     */
    @PostMapping("/nodes/set-exam-weight")
    @Operation(summary = "设置考纲权重", description = "教师标记知识点的考纲权重")
    public R<?> setExamWeight(@RequestBody Map<String, Object> body) {
        checkEnabled();
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师/管理员可访问");
        Long nodeId = body.get("nodeId") instanceof Number n ? n.longValue() : null;
        String weight = body.get("examWeight") instanceof String s ? s : null;
        if (nodeId == null || weight == null) return R.error(400, "缺少 nodeId 或 examWeight");
        if (!List.of("HIGH", "MEDIUM", "LOW").contains(weight)) return R.error(400, "examWeight 须为 HIGH/MEDIUM/LOW");
        cardRecommendationService.setExamWeight(nodeId, weight);
        return R.ok(null, "考纲权重已更新");
    }

    /**
     * v167: AI 辅助生成卡片（替代正则提取，生成5种类型）
     */
    @PostMapping("/articles/{id}/ai-generate-flashcards")
    @Operation(summary = "AI生成卡片", description = "使用AI为文章生成知识闪卡")
    @Parameter(name = "id", description = "文章ID", required = true, example = "123")
    public R<?> aiGenerateFlashcards(@PathVariable Long id) {
        checkEnabled();
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师/管理员可访问");
        int count = knowledgeBaseAdminService.generateFlashcardsWithAI(id);
        if (count == 0) return R.error(500, "AI 生成失败，请检查 AI 服务状态或文章内容");
        return R.ok(Map.of("generatedCount", count), "AI 已生成 " + count + " 张卡片");
    }

    /**
     * v167: 批量触发 AI 质量评估
     * Body: { cardIds: [1,2,3] } — 对指定卡片评估
     */
    @PostMapping("/flashcards/batch-evaluate")
    @Operation(summary = "批量AI评估卡片", description = "触发AI对指定卡片进行质量评估")
    public R<?> batchEvaluate(@RequestBody Map<String, Object> body) {
        checkEnabled();
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师/管理员可访问");
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("cardIds");
        if (ids == null || ids.isEmpty()) return R.error(400, "请提供 cardIds");
        List<Long> cardIds = ids.stream().map(Integer::longValue).collect(java.util.stream.Collectors.toList());
        cardRecommendationService.triggerBatchEvaluate(cardIds);
        return R.ok(Map.of("submittedCount", cardIds.size()), "评估任务已提交");
    }

    /**
     * v167: 查询 AI 评估进度
     */
    @GetMapping("/flashcards/evaluation-progress")
    @Operation(summary = "查询AI评估进度", description = "获取AI卡片评估任务的进度")
    public R<?> evaluationProgress() {
        checkEnabled();
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师/管理员可访问");
        return R.ok(cardRecommendationService.getEvaluationProgress());
    }

    /**
     * v167: 一键评估所有未评分的存量卡片
     */
    @PostMapping("/flashcards/batch-evaluate-all")
    @Operation(summary = "评估所有未评分卡片", description = "一键触发所有未评分卡片的AI评估")
    public R<?> batchEvaluateAll() {
        checkEnabled();
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师/管理员可访问");
        int count = cardRecommendationService.triggerBatchEvaluateAll();
        if (count == 0) return R.ok(Map.of("count", 0), "没有需要评估的卡片");
        return R.ok(Map.of("submittedCount", count), "已提交 " + count + " 张卡片的评估任务，请稍后刷新");
    }

    // ═══════════════════════════════════════════════════════
    //  v169: 卡片清空 + 批量重生
    // ═══════════════════════════════════════════════════════

    /**
     * v169: 清空知识卡片（可按学科筛选，不传=全清）。
     * 仅管理员可操作。
     */
    @DeleteMapping("/flashcards/clear-all")
    @Operation(summary = "清空所有卡片", description = "管理员清空知识卡片（可按学科筛选）")
    @Parameter(name = "subjectId", description = "学科ID（不传=全部清空）")
    public R<?> clearAllFlashcards(@RequestParam(required = false) Long subjectId) {
        checkEnabled();
        if (!SecurityUtils.isAdmin()) return R.error(403, "仅管理员可执行此操作");
        int count = knowledgeBaseAdminService.clearAllFlashcards(subjectId);
        return R.ok(Map.of("deletedCount", count), "已清空 " + count + " 张卡片");
    }

    /**
     * v169: 批量重生所有卡片（异步，立即返回）。
     * 仅管理员可操作。后台按每批 10 篇文章分段生成，批间 2s 间隔。
     */
    @PostMapping("/flashcards/regenerate-all")
    @Operation(summary = "批量重生所有卡片", description = "异步重新生成所有知识卡片")
    @Parameter(name = "subjectId", description = "学科ID（不传=全部）")
    public R<?> regenerateAllFlashcards(@RequestParam(required = false) Long subjectId) {
        checkEnabled();
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师/管理员可执行此操作");

        // 统计文章数（不清空也不生成，只做信息确认）
        long count = knowledgeBaseAdminService.countPublishedArticles(subjectId);

        if (count == 0) return R.ok(Map.of("articleCount", 0), "没有已发布的文章");

        // 异步执行（不阻塞请求线程）
        knowledgeBaseAdminService.regenerateAllFlashcardsAsync(subjectId);

        return R.ok(Map.of("articleCount", (int) count), "已提交后台任务，将为 " + count + " 篇文章重新生成卡片。稍后刷新审核队列查看进度。");
    }

    /**
     * v169: 查询异步重生进度
     */
    @GetMapping("/flashcards/regeneration-progress")
    @Operation(summary = "查询重生进度", description = "获取卡片异步重生任务的进度")
    @Parameter(name = "subjectId", description = "学科ID")
    public R<?> regenerationProgress(@RequestParam(required = false) Long subjectId) {
        checkEnabled();
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师/管理员可访问");
        return R.ok(knowledgeBaseAdminService.getRegenerationProgress(subjectId));
    }

    // ======================== 知识清单（学生浏览教师 AI 生成的清单） ========================

    @GetMapping("/checklists")
    @Operation(summary = "分页获取知识清单", description = "学生获取教师发布的知识清单列表，按可访问学科过滤")
    @Parameter(name = "keyword", description = "搜索关键词（匹配标题和内容）")
    @Parameter(name = "page", description = "页码", example = "1")
    @Parameter(name = "size", description = "每页数量", example = "12")
    public R<Map<String, Object>> listChecklists(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        checkEnabled();
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        Long studentId = studentResolver.resolveCurrentStudentId();
        return R.ok(knowledgeBaseStudentService.listChecklists(studentId, keyword, page, size));
    }

    @GetMapping("/checklists/{id}")
    @Operation(summary = "获取知识清单详情", description = "获取知识清单的完整 Markdown 内容")
    @Parameter(name = "id", description = "清单ID", required = true)
    public R<Map<String, Object>> getChecklistDetail(@PathVariable Long id) {
        checkEnabled();
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        Long studentId = studentResolver.resolveCurrentStudentId();
        track("KNOWLEDGE_CHECKLIST_VIEW", Map.of("checklistId", id));
        return R.ok(knowledgeBaseStudentService.getChecklistDetail(id, studentId));
    }
}
