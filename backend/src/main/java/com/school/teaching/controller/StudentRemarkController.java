package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.entity.Classes;
import com.school.teaching.entity.Student;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.ClassService;
import com.school.teaching.service.StudentService;
import com.school.teaching.service.StudentRemarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class StudentRemarkController {

    @Autowired private StudentRemarkService studentRemarkService;
    @Autowired private StudentService studentService;
    @Autowired private ClassService classService;

    // ── 班主任：班级学生+评语 ──────────────────────────

    @GetMapping("/class/{classId}/students-with-remarks")
    public R<List<Map<String, Object>>> studentsWithRemarks(@PathVariable Long classId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        Classes cls = classService.getClassById(classId);
        if (cls == null) return R.notFound("班级不存在");
        // 权限：班主任（headTeacherId 存的是 users.id）或管理员
        boolean isAdmin = SecurityUtils.isAdmin();
        if (!isAdmin && !userId.equals(cls.getHeadTeacherId()))
            return R.error(403, "仅班主任或管理员可查看");

        return R.ok(studentRemarkService.studentsWithRemarks(classId));
    }

    // ── 班主任：修改评语 ──────────────────────────────

    @PutMapping("/student/{studentId}/remark")
    public R<String> updateRemark(@PathVariable Long studentId, @RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        Student s = studentService.getById(studentId);
        if (s == null) return R.notFound("学生不存在");
        Classes cls = classService.getClassById(s.getClassId());
        boolean isAdmin = SecurityUtils.isAdmin();
        if (!isAdmin && (cls == null || !userId.equals(cls.getHeadTeacherId())))
            return R.error(403, "仅班主任或管理员可操作");

        String remark = body.getOrDefault("remark", "");
        studentRemarkService.updateRemark(studentId, remark);
        return R.ok("已保存");
    }

    // ── 学生：成长报告 ────────────────────────────────

    @GetMapping("/student/report")
    public R<Map<String, Object>> growthReport() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");

        Map<String, Object> report = studentRemarkService.growthReport(userId);
        if (report == null) return R.notFound("未绑定学生");
        return R.ok(report);
    }
}
