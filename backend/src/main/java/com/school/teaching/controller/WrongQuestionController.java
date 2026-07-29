package com.school.teaching.controller;

import com.school.teaching.annotation.AuditLog;
import com.school.teaching.annotation.AuditEventType;

import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.security.StudentResolver;
import com.school.teaching.service.WrongQuestionService;
import com.school.teaching.entity.PracticeSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wrong")
public class WrongQuestionController {

    @Autowired private WrongQuestionService wrongService;
    @Autowired private StudentResolver studentResolver;

    @GetMapping("/list")
    public R<Map<String, Object>> list(@RequestParam(defaultValue = "0") int mastered,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "30") int pageSize,
                                        @RequestParam(required = false) String sourceType) {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(wrongService.listWrongQuestions(sid, mastered, page, pageSize, sourceType));
    }

    @PutMapping("/{id}/actions/mastered")
    @AuditLog(eventType = AuditEventType.OTHER, description = "标记错题已掌握")
    public R<String> markMastered(@PathVariable Long id) {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        wrongService.markMastered(id, sid);
        return R.ok("已标记为掌握");
    }

    @PutMapping("/{id}/actions/unmastered")
    @AuditLog(eventType = AuditEventType.OTHER, description = "标记错题未掌握")
    public R<String> markUnmastered(@PathVariable Long id) {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        wrongService.markUnmastered(id, sid);
        return R.ok("已取消掌握标记");
    }

    @GetMapping("/actions/practice")
    public R<List<Map<String, Object>>> getPracticeList() {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(wrongService.getPracticeList(sid));
    }

    @PostMapping("/actions/practice/submit")
    @AuditLog(eventType = AuditEventType.OTHER, description = "提交错题练习")
    public R<Map<String, Object>> submitPractice(@RequestBody Map<String, Object> body) {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        Object wrongIdObj = body.get("wrongId");
        if (wrongIdObj == null) return R.error(400, "wrongId 不能为空");
        Long wrongId = Long.valueOf(wrongIdObj.toString());
        String answer = (String) body.getOrDefault("answer", "");
        return R.ok(wrongService.submitPractice(wrongId, answer, sid));
    }

    // ═══════════ 衍生练习 ═══════════

    /** 提交衍生练习生成任务（异步：立即返回sessionId，AI后台出题） */
    @PostMapping("/actions/derived-practice")
    @AuditLog(eventType = AuditEventType.OTHER, description = "生成衍生练习")
    public R<Map<String, Object>> generateDerivedPractice() {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(wrongService.generateDerivedPractice(sid));
    }

    /** 轮询练习会话生成状态 */
    @GetMapping("/practice/{sessionId}/status")
    public R<Map<String, Object>> getPracticeStatus(@PathVariable Long sessionId) {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        PracticeSession session = wrongService.getSessionById(sessionId);
        if (session == null) return R.error(404, "会话不存在");
        if (!session.getStudentId().equals(sid)) return R.error(403, "无权访问");

        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("sessionId", session.getId());
        data.put("status", session.getStatus());
        data.put("totalQuestions", session.getTotalQuestions());

        if ("ongoing".equals(session.getStatus())) {
            Map<String, Object> full = wrongService.getPracticeSession(sessionId, sid);
            data.put("items", full.get("items"));
            data.put("weakPoints", full.get("weakPoints"));
        }
        return R.ok(data);
    }

    /** 获取练习会话（不含正确答案） */
    @GetMapping("/practice/{sessionId}")
    public R<Map<String, Object>> getPracticeSession(@PathVariable Long sessionId) {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(wrongService.getPracticeSession(sessionId, sid));
    }

    /** 提交练习答案 → 自动判分 → 错题回写 */
    @PostMapping("/practice/{sessionId}/submit")
    @AuditLog(eventType = AuditEventType.OTHER, description = "提交衍生练习")
    public R<Map<String, Object>> submitPracticeSession(@PathVariable Long sessionId,
                                                        @RequestBody Map<String, Object> body) {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) body.get("answers");
        if (answers == null || answers.isEmpty()) return R.error("请提交答案");
        return R.ok(wrongService.submitPracticeSession(sessionId, sid, answers));
    }

    /** 删除单条错题 */
    @DeleteMapping("/{id}")
    public R<String> deleteWrong(@PathVariable Long id) {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        wrongService.deleteWrongQuestion(id, sid);
        return R.ok("已删除");
    }

    /** 批量删除错题 */
    @DeleteMapping("/actions/batch-delete")
    public R<String> batchDelete(@RequestBody Map<String, Object> body) {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) body.get("ids");
        if (ids == null || ids.isEmpty()) return R.error("请选择要删除的错题");
        wrongService.batchDeleteWrongQuestions(ids.stream().map(Number::longValue).toList(), sid);
        return R.ok("已删除" + ids.size() + "条");
    }

    /** 薄弱知识点排行 */
    @GetMapping("/weak-points")
    public R<List<Map<String, Object>>> getWeakPoints() {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(wrongService.getWeakPoints(sid));
    }

    /** 学生端主页数据（统计数据 + streak） */
    @GetMapping("/student-stats")
    public R<Map<String, Object>> getStudentStats() {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(wrongService.getStudentStats(sid));
    }

    /** 记录单题练习行为（重做/逐题练习答对/答错后调用） */
    @PostMapping("/{id}/actions/practice")
    public R<String> recordPractice(@PathVariable Long id, @RequestParam(required = false) Boolean correct) {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        wrongService.recordPractice(id, sid, correct);
        return R.ok("ok");
    }

    // ═══════════ 教师监督 ═══════════

    /** 教师端：班级错题汇总统计 */
    @GetMapping("/teacher/summary")
    public R<Map<String, Object>> teacherSummary() {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(wrongService.teacherSummary(userId));
    }

    /** 教师端：按学生维度错题聚合 */
    @GetMapping("/teacher/students")
    public R<List<Map<String, Object>>> teacherStudentList() {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(wrongService.teacherStudentList(userId));
    }

    /** 教师端：班级薄弱知识点聚合 */
    @GetMapping("/teacher/weak-points")
    public R<List<Map<String, Object>>> teacherWeakPoints() {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(wrongService.teacherWeakPoints(userId));
    }
    /** 教师端：查看某学生的错题明细 */
    @GetMapping("/teacher/students/{studentId}/wrong-detail")
    public R<List<Map<String, Object>>> teacherStudentWrongDetail(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "0") int mastered) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(wrongService.teacherStudentWrongDetail(userId, studentId, mastered));
    }

    /** 教师端：发送错题复习提醒 */
    @PostMapping("/teacher/notify/{studentId}")
    public R<String> notifyStudentReview(@PathVariable Long studentId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long userId = SecurityUtils.getCurrentUserId();
        wrongService.notifyStudentReview(userId, studentId);
        return R.ok("已发送复习提醒");
    }

    /** 教师端：薄弱知识点趋势对比 */
    @GetMapping("/teacher/weak-points/trend")
    public R<Map<String, Object>> teacherWeakPointsTrend(
            @RequestParam(defaultValue = "4") int weeks) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(wrongService.teacherWeakPointsTrend(userId, weeks));
    }
}
