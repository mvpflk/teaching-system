package com.school.teaching.controller;

import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.annotation.AuditLog;
import com.school.teaching.common.R;
import com.school.teaching.entity.TeacherQuickComment;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teacher")
public class TeacherController {

    @Autowired private TeacherService teacherService;

    @GetMapping("/list")
    public R<Map<String, Object>> list(@RequestParam(required = false) String keyword) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        return R.ok(teacherService.adminListTeachers(keyword));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> getTeacher(@PathVariable Long id) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        Map<String, Object> result = teacherService.adminGetTeacher(id);
        return result != null ? R.ok(result) : R.notFound("教师不存在");
    }

    @AuditLog(eventType = AuditEventType.USER_CREATE, description = "创建教师")
    @PostMapping
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        return R.ok(teacherService.adminCreateTeacher(body), "创建成功");
    }

    @AuditLog(eventType = AuditEventType.USER_UPDATE, description = "编辑教师")
    @PutMapping("/{userId}")
    public R<String> update(@PathVariable Long userId, @RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        teacherService.adminUpdateTeacher(userId, body);
        return R.ok("更新成功");
    }

    @AuditLog(eventType = AuditEventType.USER_DELETE, description = "删除教师")
    @DeleteMapping("/{userId}")
    public R<String> delete(@PathVariable Long userId) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        teacherService.adminDeleteTeacher(userId);
        return R.ok("删除成功");
    }

    @GetMapping("/{userId}/assignments")
    public R<List<Map<String, Object>>> getAssignments(@PathVariable Long userId) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        return R.ok(teacherService.getTeachingAssignments(userId));
    }

    @AuditLog(eventType = AuditEventType.OTHER, description = "设置任教配置")
    @PutMapping("/{userId}/actions/assignments")
    public R<String> setAssignments(@PathVariable Long userId, @RequestBody List<Map<String, Object>> assignments) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        teacherService.setTeachingAssignments(userId, assignments);
        return R.ok("配置成功");
    }

    @Autowired private com.school.teaching.service.ProfileService profileService;

    @AuditLog(eventType = AuditEventType.OTHER, description = "设置班主任")
    @PutMapping("/{userId}/actions/head-class")
    public R<String> setHeadClass(@PathVariable Long userId, @RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        Long classId = body.get("classId") != null ? Long.valueOf(body.get("classId").toString()) : null;
        teacherService.adminSetHeadClass(userId, classId);
        return R.ok("设置成功");
    }

    @AuditLog(eventType = AuditEventType.PASSWORD_CHANGE, description = "管理员重置用户密码")
    @PutMapping("/{userId}/actions/reset-password")
    public R<Map<String, String>> adminResetPassword(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        String newPassword = body.get("newPassword");
        String generated = profileService.adminResetPassword(userId, newPassword);
        return R.ok(Map.of("newPassword", generated), "密码已重置");
    }

    /** 快捷评语 — 获取当前教师的列表 */
    @GetMapping("/quick-comments")
    public R<List<TeacherQuickComment>> getQuickComments() {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        Long teacherId = teacherService.getTeacherIdByUserId(SecurityUtils.getCurrentUserId());
        if (teacherId == null) return R.ok(null, "非教师账号");
        return R.ok(teacherService.getQuickComments(teacherId));
    }

    /** 快捷评语 — 添加一条 */
    @PostMapping("/quick-comments")
    public R<TeacherQuickComment> addQuickComment(@RequestBody Map<String, String> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        Long teacherId = teacherService.getTeacherIdByUserId(SecurityUtils.getCurrentUserId());
        if (teacherId == null) return R.ok(null, "非教师账号");
        String text = body.get("commentText");
        if (text == null || text.isBlank()) return R.error(400, "评语内容不能为空");
        if (text.length() > 500) return R.error(400, "评语内容不能超过500字");
        return R.ok(teacherService.addQuickComment(teacherId, text), "已添加");
    }

    /** 快捷评语 — 删除一条 */
    @DeleteMapping("/quick-comments/{id}")
    public R<String> deleteQuickComment(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        Long teacherId = teacherService.getTeacherIdByUserId(SecurityUtils.getCurrentUserId());
        if (teacherId == null) return R.ok(null, "非教师账号");
        teacherService.deleteQuickComment(id, teacherId);
        return R.ok("已删除");
    }
}
