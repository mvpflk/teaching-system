package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.entity.StudentGroup;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/groups")
public class GroupController {

    @Autowired private GroupService groupService;

    @GetMapping("/class/{classId}")
    public R<List<StudentGroup>> getGroups(@PathVariable Long classId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        return R.ok(groupService.getGroups(classId));
    }

    @PostMapping("/class/{classId}")
    public R<StudentGroup> createGroup(@PathVariable Long classId, @RequestBody Map<String, String> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        return R.ok(groupService.createGroup(classId, body.get("name"), SecurityUtils.getCurrentUserId()));
    }

    @DeleteMapping("/{groupId}")
    public R<?> deleteGroup(@PathVariable Long groupId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        groupService.deleteGroup(groupId, SecurityUtils.getCurrentUserId());
        return R.ok(null, "已删除");
    }

    @GetMapping("/{groupId}/members")
    public R<List<Map<String, Object>>> getMembers(@PathVariable Long groupId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.ok(groupService.getMembers(groupId));
        return R.ok(groupService.getMembers(groupId));
    }

    @PostMapping("/{groupId}/members")
    public R<?> addMember(@PathVariable Long groupId, @RequestBody Map<String, Long> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        groupService.addMember(groupId, body.get("studentId"), SecurityUtils.getCurrentUserId());
        return R.ok(null, "已添加");
    }

    @DeleteMapping("/{groupId}/members/{studentId}")
    public R<?> removeMember(@PathVariable Long groupId, @PathVariable Long studentId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        groupService.removeMember(groupId, studentId, SecurityUtils.getCurrentUserId());
        return R.ok(null, "已移除");
    }
}
