package com.school.teaching.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.annotation.AuditLog;
import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.common.R;
import com.school.teaching.entity.ExamSyllabus;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.ExamSyllabusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/settings/exam-syllabus")
public class ExamSyllabusController {

    @Autowired private ExamSyllabusService examSyllabusService;

    @GetMapping("/list")
    public R<Page<ExamSyllabus>> list(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String examType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(examSyllabusService.list(subjectId, examType, page, size));
    }

    @GetMapping("/{id}")
    public R<ExamSyllabus> getById(@PathVariable Long id) {
        return R.ok(examSyllabusService.getById(id));
    }

    @GetMapping("/by-subject/{subjectId}")
    public R<ExamSyllabus> getBySubject(
            @PathVariable Long subjectId,
            @RequestParam(required = false, defaultValue = "GENERAL") String examType) {
        ExamSyllabus s = examSyllabusService.getBySubject(subjectId, examType);
        return s != null ? R.ok(s) : R.ok(null, "该学科暂无考纲");
    }

    @PostMapping
    @AuditLog(eventType = AuditEventType.OTHER, description = "创建/上传考纲")
    public R<ExamSyllabus> create(@RequestBody ExamSyllabus syllabus) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "仅管理员可操作");
        return R.ok(examSyllabusService.create(syllabus));
    }

    @PutMapping("/{id}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "编辑考纲")
    public R<ExamSyllabus> update(@PathVariable Long id, @RequestBody ExamSyllabus syllabus) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "仅管理员可操作");
        return R.ok(examSyllabusService.update(id, syllabus));
    }

    @DeleteMapping("/{id}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "删除考纲")
    public R<?> delete(@PathVariable Long id) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "仅管理员可操作");
        examSyllabusService.delete(id);
        return R.ok("已删除");
    }

    @GetMapping("/{id}/nodes")
    public R<?> getNodeIds(@PathVariable Long id) {
        return R.ok(examSyllabusService.getNodeIds(id));
    }

    @PutMapping("/{id}/nodes")
    @AuditLog(eventType = AuditEventType.OTHER, description = "更新考纲关联知识点")
    public R<?> saveNodeRelations(@PathVariable Long id, @RequestBody java.util.List<Long> nodeIds) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "仅管理员可操作");
        examSyllabusService.saveNodeRelations(id, nodeIds);
        return R.ok("关联已更新");
    }

    @PutMapping("/{id}/toggle")
    public R<?> toggleStatus(@PathVariable Long id) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "仅管理员可操作");
        examSyllabusService.toggleStatus(id);
        return R.ok("状态已切换");
    }

    @PostMapping("/upload")
    @AuditLog(eventType = AuditEventType.OTHER, description = "上传考纲文件")
    public R<ExamSyllabus> uploadFile(
            @RequestParam Long subjectId,
            @RequestParam(defaultValue = "GENERAL") String examType,
            @RequestParam("file") MultipartFile file) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "仅管理员可操作");
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String fileName = file.getOriginalFilename();
            String title = fileName != null && fileName.contains(".")
                    ? fileName.substring(0, fileName.lastIndexOf('.'))
                    : "考纲";

            // 已有则更新，无则创建
            ExamSyllabus existing = examSyllabusService.getBySubject(subjectId, examType);
            if (existing != null) {
                existing.setTitle(title);
                existing.setContent(content);
                return R.ok(examSyllabusService.update(existing.getId(), existing));
            } else {
                ExamSyllabus s = new ExamSyllabus();
                s.setSubjectId(subjectId);
                s.setExamType(examType);
                s.setTitle(title);
                s.setContent(content);
                return R.ok(examSyllabusService.create(s));
            }
        } catch (Exception e) {
            return R.error(400, "上传失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    /** 考纲健康检查：校验结构化元数据完整性 */
    @GetMapping("/health")
    public R<java.util.Map<String, Object>> healthCheck() {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        return R.ok(examSyllabusService.healthCheck());
    }
}
