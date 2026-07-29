package com.school.teaching.controller;

import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.annotation.AuditLog;
import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.StudentImportService;
import com.school.teaching.service.StudentGrowthService;
import com.school.teaching.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/student")
public class StudentController {

    @Autowired private StudentService studentService;
    @Autowired private StudentImportService studentImportService;
    @Autowired private StudentGrowthService growthService;
    @Autowired private com.school.teaching.service.ClassService classService;
    @Autowired private com.school.teaching.service.TeacherService teacherService;

    /** 教师是否能编辑该学生数据：管理员全通，班主任限本班，科任教师不可编辑 */
    private boolean canEditStudent(Long studentId) {
        if (SecurityUtils.isAdmin() || SecurityUtils.isSuperAdmin()) return true;
        if (!SecurityUtils.isTeacherOrAdmin()) return false;
        com.school.teaching.entity.Student s = studentService.getStudentById(studentId);
        if (s == null || s.getClassId() == null) return false;
        return classService.isHeadTeacherOfClass(SecurityUtils.getCurrentUserId(), s.getClassId());
    }

    /** 教师是否能访问该学生数据：管理员全通，教师限任教班级 */
    private boolean canAccessStudent(Long studentId) {
        if (SecurityUtils.isAdmin() || SecurityUtils.isSuperAdmin()) return true;
        // REGION_ADMIN 没有班级归属，故意不在此处放行。如需只读查看学生数据，应在 RegionController 中实现。
        if (!SecurityUtils.isTeacherOrAdmin()) return false;
        com.school.teaching.entity.Student s = studentService.getStudentById(studentId);
        if (s == null || s.getClassId() == null) return false;
        return classService.isTeacherOfClass(SecurityUtils.getCurrentUserId(), s.getClassId());
    }

    /** 学生查自己的班级ID（用于跳转班级主页） */
    @GetMapping("/actions/my-class")
    public R<Map<String, Object>> myClass() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        com.school.teaching.entity.Student s = studentService.getStudentByUserId(userId);
        if (s == null || s.getClassId() == null) return R.error(404, "未分配班级");
        return R.ok(Map.of("classId", s.getClassId(), "studentId", s.getId()));
    }

    @GetMapping("/list")
    public R<Map<String, Object>> list(@RequestParam(required = false) Long classId,
                                        @RequestParam(required = false) String grade,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int pageSize) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        // 非管理员教师只返回任教班级学生
        if (!SecurityUtils.isAdmin()) {
            Long userId = SecurityUtils.getCurrentUserId();
            List<Long> accessibleIds = teacherService.getAccessibleClassIds(userId);
            if (accessibleIds.isEmpty())
                return R.ok(Map.of("records", List.of(), "total", 0L));
            if (classId != null && !accessibleIds.contains(classId))
                return R.error(403, "无权限查看该班级学生");
            return R.ok(studentService.adminListStudents(classId, grade, keyword, page, pageSize, accessibleIds));
        }
        return R.ok(studentService.adminListStudents(classId, grade, keyword, page, pageSize, null));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> getStudent(@PathVariable Long id) {
        if (!canAccessStudent(id)) return R.error(403, "无权限访问该学生数据");
        Map<String, Object> s = studentService.adminGetStudent(id);
        return s != null ? R.ok(s) : R.notFound("学生不存在");
    }

    @AuditLog(eventType = AuditEventType.USER_CREATE, description = "创建学生")
    @PostMapping
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        return R.ok(studentService.adminCreateStudent(body));
    }

    @AuditLog(eventType = AuditEventType.USER_UPDATE, description = "编辑学生")
    @PutMapping("/{id}")
    public R<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (!canEditStudent(id)) return R.error(403, "无权限编辑该学生数据");
        return R.ok(studentService.adminUpdateStudent(id, body));
    }

    @AuditLog(eventType = AuditEventType.USER_UPDATE, description = "变更学生状态")
    @PutMapping("/{id}/actions/status")
    public R<String> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        if (!canEditStudent(id)) return R.error(403, "无权限变更该学生状态");
        String status = (String) body.get("status");
        if (status == null || status.trim().isEmpty()) return R.error(400, "状态不能为空");
        studentService.adminUpdateStudentStatus(id, status, userId);
        return R.ok("状态已更新");
    }

    @AuditLog(eventType = AuditEventType.USER_DELETE, description = "删除学生")
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        studentService.adminDeleteStudent(id);
        return R.ok("删除成功");
    }

    @AuditLog(eventType = AuditEventType.USER_DELETE, description = "批量删除学生")
    @PostMapping("/actions/batch-delete")
    public R<String> batchDelete(@RequestBody Map<String, List<Long>> body) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        studentService.adminBatchDelete(body.get("ids"));
        return R.ok("批量删除成功");
    }

    @AuditLog(eventType = AuditEventType.DATA_EXPORT, description = "批量毕业")
    @PostMapping("/actions/batch-graduate")
    public R<Map<String, Object>> batchGraduate(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isSuperAdmin()) return R.error(403, "仅超级管理员可操作");
        String scope = (String) body.getOrDefault("scope", "all");
        Long targetId = body.get("targetId") != null ? Long.valueOf(body.get("targetId").toString()) : null;
        return R.ok(studentService.adminBatchGraduate(scope, targetId));
    }

    @AuditLog(eventType = AuditEventType.DATA_EXPORT, description = "批量导入学生")
    @PostMapping("/actions/batch-import")
    public R<Map<String, Object>> batchImport(@RequestParam("file") MultipartFile file) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        if (file.isEmpty()) return R.error("请选择文件");
        String fn = file.getOriginalFilename();
        if (fn == null || !(fn.endsWith(".xlsx") || fn.endsWith(".xls"))) return R.error("仅支持 .xlsx / .xls 格式");
        return R.ok(studentImportService.batchImport(file));
    }

    @GetMapping("/actions/template/download")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] data = studentService.downloadTemplate();
        return ResponseEntity.ok()
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=student_template.xlsx")
            .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM).body(data);
    }

    @GetMapping("/growth/{studentId}")
    public R<?> getGrowth(@PathVariable Long studentId, @RequestParam(required = false) String subject) {
        if (!SecurityUtils.isTeacherOrAdmin() && !SecurityUtils.getCurrentUserId().equals(studentId))
            return R.error(403, "无权限");
        return R.ok(growthService.getGrowthCurve(studentId, subject));
    }
}