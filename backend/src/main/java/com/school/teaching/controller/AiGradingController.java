package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.AiGradingService;
import com.school.teaching.service.AiTaskStore;
import com.school.teaching.service.MajorExamService;
import com.school.teaching.service.QuickExamService;
import com.school.teaching.service.impl.AsyncTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiGradingController {

    @Autowired private AiGradingService aiGradingService;
    @Autowired private AiTaskStore taskStore;
    @Autowired private AsyncTaskService asyncTaskService;
    @Autowired private com.school.teaching.service.SystemService systemService;
    @Autowired private QuickExamService quickExamService;
    @Autowired private MajorExamService majorExamService;

    /** 提交 AI 评分任务（异步，立即返回 taskId） */
    @GetMapping("/grading/suggestion")
    public R<?> submitGrading(@RequestParam Long submissionId, @RequestParam Long questionId) {
        if (!systemService.getBooleanConfig("feature.ai_grading_enabled", false))
            return R.error(410, "AI评分功能已关闭");
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        String taskId = taskStore.create();
        try {
            aiGradingService.executeAsync(taskId, submissionId, questionId, SecurityUtils.getCurrentUserId());
        } catch (java.util.concurrent.RejectedExecutionException e) {
            taskStore.fail(taskId, "AI 服务繁忙，请稍后重试");
        }
        return R.ok(Map.of("taskId", taskId, "status", "PENDING"));
    }

    /** 查询 AI 任务结果（出题/评分通用） */
    @GetMapping("/result/{taskId}")
    public R<?> getResult(@PathVariable String taskId) {
        AiTaskStore.TaskEntry entry = taskStore.get(taskId);
        if (entry == null) return R.error(404, "任务不存在或已过期");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskId", entry.taskId);
        data.put("status", entry.status);
        data.put("result", entry.result);
        data.put("error", entry.error);
        return R.ok(data);
    }

    /** 管理员查看 AI 统计 */
    @GetMapping("/admin/stats")
    public R<?> stats() {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        Long schoolId = SecurityUtils.getCurrentSchoolId();
        return R.ok(aiGradingService.getStats(schoolId != null ? schoolId : 1L));
    }

    // ═══════════ 一键示例卷 ═══════════

    /** 获取教师任教学科列表（供前端下拉选择） */
    @GetMapping("/quick-exam/subjects")
    public R<?> getQuickExamSubjects() {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        List<String> subjects = quickExamService.getTeacherSubjects(SecurityUtils.getCurrentUserId());
        return R.ok(subjects);
    }

    /** 提交一键示例卷生成任务 */
    @PostMapping("/quick-exam")
    public R<?> submitQuickExam(@RequestBody(required = false) Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        Long teacherId = SecurityUtils.getCurrentUserId();
        String subject = body != null && body.get("subject") instanceof String s ? s : null;
        Long categoryId = body != null && body.get("categoryId") instanceof Number n ? n.longValue() : null;

        // 未传学科时，取教师第一任教学科
        if (subject == null || subject.isBlank()) {
            List<String> mySubjects = quickExamService.getTeacherSubjects(teacherId);
            if (mySubjects.isEmpty()) return R.error(400, "未找到您的任教学科，请联系管理员配置");
            subject = mySubjects.get(0);
        }

        String taskId = taskStore.create();
        try {
            quickExamService.executeAsync(taskId, teacherId, subject, categoryId);
        } catch (java.util.concurrent.RejectedExecutionException e) {
            taskStore.fail(taskId, "AI 服务繁忙，请稍后重试");
        }
        return R.ok(Map.of("taskId", taskId, "status", "PENDING", "subject", subject));
    }

    // ═══════════ 专业大类综合卷 ═══════════

    /** 获取所有专业大类列表 */
    @GetMapping("/major-exam/majors")
    public R<?> getMajorExamMajors() {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        var majors = majorExamService.getMajors();
        List<Map<String, Object>> list = new ArrayList<>();
        for (var m : majors) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("name", m.getMajorName());
            list.add(item);
        }
        return R.ok(list);
    }

    /** 获取某大类下的专业课学科列表 */
    @GetMapping("/major-exam/subjects")
    public R<?> getMajorExamSubjects(@RequestParam Long majorId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        return R.ok(majorExamService.getMajorProfessionalSubjects(majorId));
    }

    /** 提交专业大类综合卷生成任务 */
    @PostMapping("/major-exam")
    public R<?> submitMajorExam(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        Long majorId = body.get("majorId") instanceof Number n ? n.longValue() : null;
        if (majorId == null) return R.error(400, "请选择专业大类");

        @SuppressWarnings("unchecked")
        Map<String, Integer> typeCounts = body.get("typeCounts") instanceof Map<?, ?> m
            ? ((Map<String, Integer>) m) : null;
        @SuppressWarnings("unchecked")
        Map<String, Integer> difficultyRatio = body.get("difficultyRatio") instanceof Map<?, ?> m
            ? ((Map<String, Integer>) m) : null;

        String taskId = taskStore.create();
        try {
            majorExamService.executeAsync(taskId, SecurityUtils.getCurrentUserId(), majorId, typeCounts, difficultyRatio);
        } catch (java.util.concurrent.RejectedExecutionException e) {
            taskStore.fail(taskId, "AI 服务繁忙，请稍后重试");
        }
        return R.ok(Map.of("taskId", taskId, "status", "PENDING"));
    }

    // ═══════════ 异步任务管理 ═══════════

    /** 取消进行中的任务 */
    @PostMapping("/task/{taskId}/cancel")
    public R<?> cancelTask(@PathVariable String taskId) {
        asyncTaskService.markCancelled(taskId);
        return R.ok("已取消");
    }

    /** 手动重试失败/超时任务 */
    @PostMapping("/task/{taskId}/retry")
    public R<?> retryTask(@PathVariable String taskId) {
        asyncTaskService.retry(taskId);
        return R.ok("已加入重试队列");
    }
}
