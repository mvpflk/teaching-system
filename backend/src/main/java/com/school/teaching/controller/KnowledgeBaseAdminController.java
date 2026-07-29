package com.school.teaching.controller;

import com.school.teaching.exception.BusinessException;
import com.school.teaching.common.R;
import com.school.teaching.entity.KnowledgeArticle;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.KnowledgeNodeContentService;
import com.school.teaching.service.impl.KnowledgeBaseAdminService;
import com.school.teaching.service.impl.KnowledgeBaseStudentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/knowledge-base/admin")
public class KnowledgeBaseAdminController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseAdminController.class);

    @Autowired private KnowledgeBaseAdminService knowledgeBaseAdminService;
    @Autowired private KnowledgeBaseStudentService knowledgeBaseStudentService;
    @Autowired private KnowledgeNodeContentService contentService;

    private void checkAdminOrTeacher() {
        if (!SecurityUtils.isTeacherOrAdmin())
            throw new BusinessException(403, "仅教师/管理员可操作");
    }

    @GetMapping("/articles")
    public R<Map<String, Object>> list(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String chapter,
            @RequestParam(required = false) String task,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        checkAdminOrTeacher();
        return R.ok(knowledgeBaseAdminService.getAdminArticleList(subjectId, status, chapter, task, page, size));
    }

    @GetMapping("/articles/{id}")
    public R<?> get(@PathVariable Long id) {
        checkAdminOrTeacher();
        Map<String, Object> detail = knowledgeBaseStudentService.getArticleDetail(id, null);
        Object articleObj = detail.get("article");
        if (articleObj instanceof KnowledgeArticle ka) return R.ok(ka);
        return R.error(500, "文章数据异常");
    }

    @PostMapping("/articles")
    public R<KnowledgeArticle> create(@RequestBody KnowledgeArticle article) {
        checkAdminOrTeacher();
        return R.ok(knowledgeBaseAdminService.createArticle(article), "创建成功");
    }

    @PutMapping("/articles/{id}")
    public R<KnowledgeArticle> update(@PathVariable Long id, @RequestBody KnowledgeArticle article) {
        checkAdminOrTeacher();
        return R.ok(knowledgeBaseAdminService.updateArticle(id, article), "更新成功");
    }

    @DeleteMapping("/articles/{id}")
    public R<?> delete(@PathVariable Long id) {
        checkAdminOrTeacher();
        knowledgeBaseAdminService.deleteArticle(id);
        return R.ok(null, "删除成功");
    }

    @PostMapping("/import")
    public R<?> importMarkdown(@RequestBody Map<String, Object> body) {
        checkAdminOrTeacher();
        String basePath = (String) body.get("basePath");
        Long subjectId = body.get("subjectId") != null
            ? ((Number) body.get("subjectId")).longValue() : null;
        if (basePath == null) return R.error(400, "缺少 basePath 参数");
        return R.ok(knowledgeBaseAdminService.importFromMarkdown(basePath, subjectId));
    }

    @GetMapping("/stats")
    public R<?> stats(@RequestParam(required = false) Long subjectId) {
        checkAdminOrTeacher();
        return R.ok(knowledgeBaseAdminService.getAdminStats(subjectId));
    }

    @PostMapping("/articles/{id}/generate-flashcards")
    public R<?> generateFlashcards(@PathVariable Long id) {
        checkAdminOrTeacher();
        int count = knowledgeBaseAdminService.generateFlashcards(id);
        return R.ok(Map.of("generated", count), "已生成 " + count + " 张记忆卡片");
    }

    @PostMapping("/generate-all-flashcards")
    public R<?> generateAllFlashcards(@RequestParam(defaultValue = "0") int limit) {
        checkAdminOrTeacher();
        int total = knowledgeBaseAdminService.generateFlashcardsBatch(limit);
        return R.ok(Map.of("totalGenerated", total), "批量生成完成，共 " + total + " 篇文章");
    }

    /**
     * 知识节点内容填充 — DeepSeek 批量生成结构化教学内容。
     * 预览模式：preview=true 仅返回生成结果不写库。
     * 正式模式：preview=false 写入 knowledge_nodes.content。
     *
     * @param subjectId 学科 ID（1=数学 4=语文[职高] 5=英语[职高] 6=语文[教材同步]）
     * @param preview   true=预览 false=写入
     * @param maxNodes  限制节点数（null=全部）
     */
    @PostMapping("/generate-node-content")
    public R<?> generateNodeContent(
            @RequestParam Long subjectId,
            @RequestParam(defaultValue = "true") boolean preview,
            @RequestParam(required = false) Integer maxNodes) {
        checkAdminOrTeacher();
        Map<String, Object> report = contentService.generateContent(subjectId, preview, maxNodes);
        return R.ok(report, (preview ? "【预览模式】" : "【正式生成】")
                + "共 " + report.get("generated") + " 成功, "
                + report.get("failed") + " 失败");
    }

    /**
     * 数学知识节点内容增强 — 在已有定义+说明+常见错基础上，追加例题+考法+教材出处。
     * 保留原内容不动，只补缺失部分。
     */
    @PostMapping("/enhance-node-content")
    public R<?> enhanceNodeContent(
            @RequestParam(defaultValue = "22") Long subjectId,
            @RequestParam(defaultValue = "true") boolean preview,
            @RequestParam(required = false) Integer maxNodes) {
        checkAdminOrTeacher();
        Map<String, Object> report = contentService.enhanceContent(subjectId, preview, maxNodes);
        return R.ok(report, (preview ? "【预览模式】" : "【正式增强】")
                + "共 " + report.get("enhanced") + " 成功, "
                + report.get("failed") + " 失败");
    }
}
