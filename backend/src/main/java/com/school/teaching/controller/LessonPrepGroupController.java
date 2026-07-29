package com.school.teaching.controller;

import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.annotation.AuditLog;
import com.school.teaching.common.R;
import com.school.teaching.entity.LessonPrepGroup;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.LessonPrepGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/lesson-prep-group")
public class LessonPrepGroupController {

    @Autowired private LessonPrepGroupService service;

    @GetMapping("/list")
    public R<?> list() { return R.ok(service.listAll()); }

    @PostMapping
    @AuditLog(eventType = AuditEventType.OTHER, description = "创建备课组")
    public R<LessonPrepGroup> create(@RequestBody LessonPrepGroup g) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        return R.ok(service.create(g));
    }

    @PutMapping("/{id}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "编辑备课组")
    public R<LessonPrepGroup> update(@PathVariable Long id, @RequestBody LessonPrepGroup g) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        return R.ok(service.update(id, g));
    }

    @DeleteMapping("/{id}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "删除备课组")
    public R<String> delete(@PathVariable Long id) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        service.delete(id);
        return R.ok("已删除");
    }

    @PostMapping("/{id}/actions/add-member")
    @AuditLog(eventType = AuditEventType.OTHER, description = "备课组添加成员")
    public R<String> addMember(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        Long teacherId = Long.valueOf(body.get("teacherId").toString());
        service.addMember(id, teacherId);
        return R.ok("已添加");
    }

    @DeleteMapping("/{id}/actions/remove-member/{teacherId}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "备课组移除成员")
    public R<String> removeMember(@PathVariable Long id, @PathVariable Long teacherId) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        service.removeMember(id, teacherId);
        return R.ok("已移除");
    }

    @PostMapping("/{id}/actions/set-leader")
    @AuditLog(eventType = AuditEventType.OTHER, description = "设置备课组长")
    public R<String> setLeader(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        Long teacherId = Long.valueOf(body.get("teacherId").toString());
        service.setLeader(id, teacherId);
        return R.ok("已设置");
    }

    @GetMapping("/{id}/members")
    public R<?> getMembers(@PathVariable Long id) { return R.ok(service.getMembers(id)); }

    @GetMapping("/actions/teacher/{teacherId}/group-ids")
    public R<?> getGroupIds(@PathVariable Long teacherId) { return R.ok(service.getGroupIdsForTeacher(teacherId)); }
}
