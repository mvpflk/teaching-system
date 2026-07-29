package com.school.teaching.controller;

import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.annotation.AuditLog;
import com.school.teaching.common.R;
import com.school.teaching.entity.TeachingGroup;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.TeachingGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teaching-group")
public class TeachingGroupController {

    @Autowired private TeachingGroupService service;

    @GetMapping("/list")
    public R<?> list() {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        return R.ok(service.listAll());
    }

    @GetMapping("/actions/my-groups")
    public R<?> myGroups() {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(service.getMyGroupsByUserId(userId));
    }

    @GetMapping("/actions/teacher/{teacherId}/group-ids")
    public R<?> getGroupIds(@PathVariable Long teacherId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        List<Long> ids = service.getGroupIdsForTeacher(teacherId);
        return R.ok(ids);
    }

    @GetMapping("/{id}")
    public R<?> get(@PathVariable Long id) { var opt = service.listAll().stream().filter(m -> m.get("id").equals(id)).findFirst(); return opt.<R<?>>map(R::ok).orElse(R.notFound("不存在")); }

    @PostMapping
    @AuditLog(eventType = AuditEventType.OTHER, description = "创建教研组")
    public R<TeachingGroup> create(@RequestBody TeachingGroup g) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        return R.ok(service.create(g));
    }

    @PutMapping("/{id}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "编辑教研组")
    public R<TeachingGroup> update(@PathVariable Long id, @RequestBody TeachingGroup g) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        return R.ok(service.update(id, g));
    }

    @DeleteMapping("/{id}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "删除教研组")
    public R<String> delete(@PathVariable Long id) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        service.delete(id);
        return R.ok("已删除");
    }

    @PostMapping("/{id}/actions/add-member")
    @AuditLog(eventType = AuditEventType.OTHER, description = "教研组添加成员")
    public R<String> addMember(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        Long teacherId = Long.valueOf(body.get("teacherId").toString());
        service.addMember(id, teacherId);
        return R.ok("已添加");
    }

    @DeleteMapping("/{id}/actions/remove-member/{teacherId}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "教研组移除成员")
    public R<String> removeMember(@PathVariable Long id, @PathVariable Long teacherId) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        service.removeMember(id, teacherId);
        return R.ok("已移除");
    }

    @PostMapping("/{id}/actions/set-leader")
    @AuditLog(eventType = AuditEventType.OTHER, description = "设置教研组长")
    public R<String> setLeader(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        Long teacherId = Long.valueOf(body.get("teacherId").toString());
        service.setLeader(id, teacherId);
        return R.ok("已设置");
    }

    @DeleteMapping("/{id}/actions/remove-leader/{teacherId}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "移除教研组长")
    public R<String> removeLeader(@PathVariable Long id, @PathVariable Long teacherId) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        service.removeLeader(id, teacherId);
        return R.ok("已移除组长");
    }

    @GetMapping("/{id}/members")
    public R<?> getMembers(@PathVariable Long id) { return R.ok(service.getMembers(id)); }
}
