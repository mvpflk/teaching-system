package com.school.teaching.controller;

import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.annotation.AuditLog;
import com.school.teaching.common.R;
import com.school.teaching.dto.request.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.SystemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 字典管理控制器：年级/学科/专业/学期。
 * 拆分自 SettingsController，路由前缀 /settings 保持不变。
 */
@RestController
@RequestMapping("/settings")
@io.swagger.v3.oas.annotations.tags.Tag(name = "字典管理", description = "年级/学科/专业/学期 CRUD")
public class DictManagementController {

    @Autowired private SystemService systemService;

    // ── 年级 ──
    @GetMapping("/grades")
    public R<List<Map<String, Object>>> getGrades(@RequestParam(required = false) Long stageId) {
        return R.ok(systemService.getGrades(stageId));
    }

    @PostMapping("/grades")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "新增年级")
    public R<Map<String, Object>> createGrade(@Valid @RequestBody GradeRequest request) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        return R.ok(systemService.addDictGrade(request.getGradeName()));
    }

    @PostMapping("/grades/batch")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "批量导入年级")
    public R<?> batchCreateGrades(@RequestBody List<Map<String, Object>> list) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        int count = systemService.batchAddDictGrades(list);
        return R.ok(Map.of("count", count), "成功导入" + count + "条");
    }

    @PutMapping("/grades/{id}")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "编辑年级")
    public R<String> updateGrade(@PathVariable Long id, @Valid @RequestBody GradeRequest request) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        systemService.updateDictGrade(id, request.getGradeName());
        return R.ok(null, "已更新");
    }

    @DeleteMapping("/grades/{id}")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "删除年级")
    public R<String> deleteGrade(@PathVariable Long id) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        systemService.deleteDictGrade(id);
        return R.ok(null, "已删除");
    }

    // ── 学科 ──
    @GetMapping("/subjects")
    public R<List<Map<String, Object>>> getSubjects() {
        return R.ok(systemService.getSubjects());
    }

    @PostMapping("/subjects")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "新增学科")
    public R<Map<String, Object>> createSubject(@Valid @RequestBody SubjectRequest request) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        return R.ok(systemService.addDictSubject(request.getSubjectName()));
    }

    @PostMapping("/subjects/batch")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "批量导入学科")
    public R<?> batchCreateSubjects(@RequestBody List<Map<String, Object>> list) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        int count = systemService.batchAddDictSubjects(list);
        return R.ok(Map.of("count", count), "成功导入" + count + "条");
    }

    @PutMapping("/subjects/{id}")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "编辑学科")
    public R<String> updateSubject(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        systemService.updateDictSubject(id, request.getSubjectName());
        return R.ok(null, "已更新");
    }

    @DeleteMapping("/subjects/{id}")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "删除学科")
    public R<String> deleteSubject(@PathVariable Long id) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        systemService.deleteDictSubject(id);
        return R.ok(null, "已删除");
    }

    // ── 专业 ──
    @GetMapping("/majors")
    public R<List<Map<String, Object>>> getMajors() {
        return R.ok(systemService.getDictMajors());
    }

    @PostMapping("/majors")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "新增专业")
    public R<Map<String, Object>> createMajor(@Valid @RequestBody MajorRequest request) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        return R.ok(systemService.addDictMajor(request.getMajorName()));
    }

    @PostMapping("/majors/batch")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "批量导入专业")
    public R<?> batchCreateMajors(@RequestBody List<Map<String, Object>> list) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        int count = systemService.batchAddDictMajors(list);
        return R.ok(Map.of("count", count), "成功导入" + count + "条");
    }

    @PutMapping("/majors/{id}")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "编辑专业")
    public R<String> updateMajor(@PathVariable Long id, @Valid @RequestBody MajorRequest request) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        systemService.updateDictMajor(id, request.getMajorName());
        return R.ok(null, "已更新");
    }

    @DeleteMapping("/majors/{id}")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "删除专业")
    public R<String> deleteMajor(@PathVariable Long id) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        systemService.deleteDictMajor(id);
        return R.ok(null, "已删除");
    }

    /** 查询某专业已关联的学科列表 */
    @GetMapping("/majors/{majorId}/subjects")
    public R<List<Map<String, Object>>> getMajorSubjects(@PathVariable Long majorId) {
        return R.ok(systemService.getMajorSubjects(majorId));
    }

    /** 设置某专业的学科列表（全量替换：先删后插） */
    @PutMapping("/majors/{majorId}/subjects")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "设置专业学科关联")
    public R<String> setMajorSubjects(@PathVariable Long majorId, @RequestBody MajorSubjectsRequest request) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        List<Long> subjectIds = request.getSubjectIds() == null ? List.of()
            : request.getSubjectIds().stream().map(Number::longValue).toList();
        systemService.setMajorSubjects(majorId, subjectIds);
        return R.ok(null, "已更新");
    }

    // ── 学期 ──
    @GetMapping("/terms")
    public R<?> getTerms() {
        if (SecurityUtils.getCurrentUserId() == null) return R.error(401, "请先登录");
        return R.ok(systemService.getTerms());
    }

    @PostMapping("/terms")
    @AuditLog(eventType = AuditEventType.OTHER, description = "创建学期")
    public R<?> createTerm(@RequestBody TermRequest request) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("termName", request.getTermName());
        body.put("startDate", request.getStartDate());
        body.put("endDate", request.getEndDate());
        body.put("status", request.getStatus());
        return R.ok(systemService.addTerm(body));
    }

    @PutMapping("/terms/{id}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "编辑学期")
    public R<String> updateTerm(@PathVariable Long id, @RequestBody TermRequest request) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("termName", request.getTermName());
        body.put("startDate", request.getStartDate());
        body.put("endDate", request.getEndDate());
        body.put("status", request.getStatus());
        systemService.updateTerm(id, body);
        return R.ok("已更新");
    }

    @DeleteMapping("/terms/{id}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "删除学期")
    public R<String> deleteTerm(@PathVariable Long id) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        systemService.deleteTerm(id);
        return R.ok("已删除");
    }
}
