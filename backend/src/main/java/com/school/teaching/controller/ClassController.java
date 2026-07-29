package com.school.teaching.controller;

import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.annotation.AuditLog;
import com.school.teaching.common.R;
import com.school.teaching.entity.Classes;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.ClassService;
import com.school.teaching.service.StudentService;
import com.school.teaching.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/class")
public class ClassController {

    @Autowired private ClassService classService;
    @Autowired private TeacherService teacherService;
    @Autowired private StudentService studentService;
    @Autowired private com.school.teaching.service.ClassHomeService classHomeService;

    /** 检查当前用户是否有权限访问指定班级的数据 */
    private boolean canAccessClass(Long classId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return false;
        // 管理员/巡视员可访问所有班级
        if (SecurityUtils.isAdmin() || SecurityUtils.isInspector()) return true;
        // 教师/班主任：检查是否为该班班主任或任教
        if (SecurityUtils.isTeacherOrAdmin()) {
            Classes c = classService.getClassById(classId);
            if (c != null && c.getHeadTeacherId() != null && c.getHeadTeacherId().equals(userId)) return true;
            return teacherService.isUserTeacherOfClass(userId, classId);
        }
        // 学生：检查是否在该班级
        if (SecurityUtils.isStudent()) {
            return studentService.isUserInClass(userId, classId);
        }
        return false;
    }

    @GetMapping("/list")
    public R<Map<String, Object>> list(@RequestParam(required = false) String grade) {
        Long userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> result;
        if (SecurityUtils.isAdmin()) {
            result = classService.adminListClasses();
        } else if (SecurityUtils.isTeacherOrAdmin()) {
            Long teacherId = teacherService.getTeacherIdByUserId(userId);
            result = teacherId != null ? classService.adminListClassesByTeacher(userId) : Map.of("records", List.of(), "total", 0L);
        } else {
            result = Map.of("records", List.of(), "total", 0L);
        }
        // 可选按年级过滤（包装为可变 Map，因为 service 返回 Map.of() 不可变）
        if (grade != null && !grade.isBlank()) {
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> records = (java.util.List<Map<String, Object>>) result.get("records");
            if (records != null) {
                result = new java.util.HashMap<>(result);
                records = records.stream().filter(c -> grade.equals(c.get("grade"))).collect(java.util.stream.Collectors.toList());
                result.put("records", records);
                result.put("total", records.size());
            }
        }
        return R.ok(result);
    }

    @GetMapping("/teachers")
    public R<List<Map<String, Object>>> teachers() {
        return R.ok(teacherService.adminListSimpleTeachers());
    }

    @GetMapping("/actions/my")
    public R<List<Map<String, Object>>> myClasses() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error("未登录");

        if (SecurityUtils.isAdmin()) {
            List<Classes> all = classService.getClassList();
            List<Map<String, Object>> records = new ArrayList<>();
            for (Classes c : all) records.add(Map.of("id", c.getId(), "className", c.getClassName(), "grade", c.getGrade() != null ? c.getGrade() : ""));
            return R.ok(records);
        }

        List<Long> classIds = teacherService.getTeachingClassIds(userId);
        Set<Long> added = new HashSet<>();
        List<Map<String, Object>> records = new ArrayList<>();
        // 批量加载班级避免 N+1
        if (!classIds.isEmpty()) {
            List<Classes> classes = classService.getClassList().stream()
                .filter(c -> classIds.contains(c.getId())).toList();
            for (Classes c : classes) {
                records.add(Map.of("id", c.getId(), "className", c.getClassName(), "grade", c.getGrade() != null ? c.getGrade() : ""));
                added.add(c.getId());
            }
        }
        for (Classes c : classService.getClassList()) {
            if (c.getHeadTeacherId() != null && c.getHeadTeacherId().equals(userId) && !added.contains(c.getId())) {
                records.add(Map.of("id", c.getId(), "className", c.getClassName(), "grade", c.getGrade() != null ? c.getGrade() : ""));
            }
        }
        return R.ok(records);
    }

    @GetMapping("/{id:\\d+}")
    public R<Classes> getClass(@PathVariable Long id) {
        Classes c = classService.getClassById(id);
        return c != null ? R.ok(c) : R.notFound("班级不存在");
    }

    /** 学生成绩趋势 — 历次考试分数+班级均分 */
    @GetMapping("/{id}/actions/student-scores")
    public R<List<Map<String, Object>>> studentScores(@PathVariable Long id,
            @RequestParam Long studentId, @RequestParam(required = false) String subject) {
        if (!canAccessClass(id)) return R.error(403, "无权访问该班级数据");
        return R.ok(classHomeService.getStudentScoreTrend(id, studentId, subject));
    }

    /** 班级主页聚合数据 — 班主任+学生共用入口 */
    @GetMapping("/{id}/actions/home")
    public R<Map<String, Object>> classHome(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        if (!canAccessClass(id)) return R.error(403, "无权访问该班级主页");
        Map<String, Object> data = classHomeService.getClassHomeData(id, userId);
        // 注入当前用户与班级的关系标记
        data.put("isHeadTeacher", classHomeService.isHeadTeacherOfClass(id, userId));
        data.put("isClassStudent", classHomeService.isStudentOfClass(id, userId));
        return R.ok(data);
    }

    /** 班级任务详情聚合 — 供班级主页任务详情面板使用 */
    @GetMapping("/{classId}/actions/task-detail")
    public R<Map<String, Object>> taskDetail(@PathVariable Long classId,
            @RequestParam Long taskId) {
        if (!canAccessClass(classId)) return R.error(403, "无权访问该班级数据");
        return R.ok(classHomeService.getTaskDetail(taskId));
    }

    @AuditLog(eventType = AuditEventType.OTHER, description = "创建班级")
    @PostMapping
    public R<Classes> createClass(@RequestBody Classes cls) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        return R.ok(classService.createClass(cls));
    }

    @AuditLog(eventType = AuditEventType.OTHER, description = "编辑班级")
    @PutMapping("/{id}")
    public R<Classes> updateClass(@PathVariable Long id, @RequestBody Classes cls) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        cls.setId(id);
        return R.ok(classService.updateClass(cls));
    }

    @AuditLog(eventType = AuditEventType.OTHER, description = "删除班级")
    @DeleteMapping("/{id}")
    public R<String> deleteClass(@PathVariable Long id) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        classService.deleteClass(id);
        return R.ok("删除成功");
    }

    @GetMapping("/{classId}/students")
    public R<List<Map<String, Object>>> getStudents(@PathVariable Long classId) {
        if (!canAccessClass(classId)) return R.error(403, "无权访问");
        return R.ok(classService.getStudents(classId));
    }

    /** 班级学生管理权限：管理员 或 该班班主任 */
    private boolean canManageClassStudents(Long classId) {
        if (SecurityUtils.isAdmin()) return true;
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return false;
        Classes c = classService.getClassById(classId);
        return c != null && c.getHeadTeacherId() != null && c.getHeadTeacherId().equals(userId);
    }

    @AuditLog(eventType = AuditEventType.OTHER, description = "批量添加学生到班级")
    @PostMapping("/{classId}/students")
    public R<String> addStudents(@PathVariable Long classId, @RequestBody Map<String, Object> body) {
        if (!canManageClassStudents(classId)) return R.error(403, "仅管理员或班主任可操作");
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("studentIds");
        if (ids == null || ids.isEmpty()) return R.error(400, "studentIds不能为空");
        for (Integer id : ids) classService.addStudent(classId, id.longValue());
        return R.ok("已添加 " + ids.size() + " 名学生");
    }

    @AuditLog(eventType = AuditEventType.OTHER, description = "添加学生到班级")
    @PostMapping("/{classId}/students/{studentId}")
    public R<String> addStudent(@PathVariable Long classId, @PathVariable Long studentId) {
        if (!canManageClassStudents(classId)) return R.error(403, "仅管理员或班主任可操作");
        classService.addStudent(classId, studentId);
        return R.ok("添加成功");
    }

    @AuditLog(eventType = AuditEventType.OTHER, description = "从班级移除学生")
    @DeleteMapping("/{classId}/students/{studentId}")
    public R<String> removeStudent(@PathVariable Long classId, @PathVariable Long studentId) {
        if (!canManageClassStudents(classId)) return R.error(403, "仅管理员或班主任可操作");
        classService.removeStudent(classId, studentId);
        return R.ok("移除成功");
    }

    @GetMapping("/actions/available-students")
    public R<List<Map<String, Object>>> availableStudents() {
        if (!SecurityUtils.isAdmin()) return R.error(403, "仅管理员可操作");
        return R.ok(classService.getAvailableStudents());
    }

    @AuditLog(eventType = AuditEventType.CLASS_TYPE_CONFIG, description = "批量更新班级类型")
    @PutMapping("/actions/batch-update-type")
    public R<Map<String, Object>> batchUpdateType(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isSuperAdmin()) return R.error(403, "仅超级管理员可操作");
        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) body.get("classIds");
        if (rawIds == null || rawIds.isEmpty()) return R.error(400, "请选择班级");
        String classType = (String) body.get("classType");
        if (classType == null || classType.isEmpty()) return R.error(400, "请选择班级类型");
        List<Long> ids = rawIds.stream().map(Integer::longValue).toList();
        int updated = classService.batchUpdateClassType(ids, classType);
        return R.ok(Map.of("updated", updated));
    }

    @AuditLog(eventType = AuditEventType.OTHER, description = "学生调班")
    @PutMapping("/actions/change-student-class")
    public R<Map<String, Object>> changeStudentClass(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "仅管理员可操作");
        Long studentId = Long.valueOf(body.get("studentId").toString());
        Long newClassId = Long.valueOf(body.get("newClassId").toString());
        String reason = (String) body.getOrDefault("reason", "MANUAL");
        Long approvedBy = SecurityUtils.getCurrentUserId();
        return R.ok(classService.changeStudentClass(studentId, newClassId, reason, approvedBy));
    }
}