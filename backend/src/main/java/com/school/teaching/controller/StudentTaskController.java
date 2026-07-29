package com.school.teaching.controller;

import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.annotation.AuditLog;
import com.school.teaching.common.R;
import com.school.teaching.entity.Task;
import com.school.teaching.entity.TaskSubmission;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.security.StudentResolver;
import com.school.teaching.service.TaskService;
import com.school.teaching.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import com.school.teaching.service.TaskSubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/student/tasks")
@Tag(name = "学生任务", description = "学生端任务查看、答题、提交、防作弊、反思")
public class StudentTaskController {

    @Autowired private TaskService taskService;
    @Autowired private com.school.teaching.service.TaskSubmissionService submissionService;
    @Autowired private StudentResolver studentResolver;
    @Autowired private com.school.teaching.service.WrongQuestionService wrongQuestionService;
    @Autowired private com.school.teaching.service.StudentLearningProfileService profileService;
    @Autowired private StudentService studentService;

    // 切屏上报频率限制：每学生每任务每秒最多1次
    private final java.util.concurrent.ConcurrentHashMap<String, java.util.List<Long>> cheatRateLimiter = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long CHEAT_RATE_WINDOW_MS = 1000;
    private static final int CHEAT_RATE_MAX = 1;
    // 定期清理过期频率限制条目（每10分钟），防止内存泄漏
    private java.util.concurrent.ScheduledExecutorService rateLimiterCleaner;

    @PostConstruct
    void startRateLimiterCleanup() {
        rateLimiterCleaner = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cheat-rate-limiter-cleaner");
            t.setDaemon(true);
            return t;
        });
        rateLimiterCleaner.scheduleWithFixedDelay(() -> {
            long now = System.currentTimeMillis();
            cheatRateLimiter.entrySet().removeIf(e -> {
                java.util.List<Long> timestamps = e.getValue();
                synchronized (timestamps) {
                    timestamps.removeIf(t -> now - t > CHEAT_RATE_WINDOW_MS);
                    return timestamps.isEmpty();
                }
            });
        }, 10, 10, java.util.concurrent.TimeUnit.MINUTES);
    }

    @PreDestroy
    void stopRateLimiterCleanup() {
        if (rateLimiterCleaner != null && !rateLimiterCleaner.isShutdown()) {
            rateLimiterCleaner.shutdownNow();
        }
    }

    /** 待办任务计数 — 按紧急程度分级返回 {count, urgent, warning} */
    @GetMapping("/pending-count")
    @Operation(summary = "获取待办任务计数", description = "按紧急程度返回待完成任务数量")
    public R<Map<String, Object>> pendingCount() {
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        Long studentId = studentResolver.resolveCurrentStudentId();
        if (studentId == null) return R.ok(Map.of("count", 0, "urgent", 0, "warning", 0));
        return R.ok(taskService.getPendingCount(studentId));
    }

    /** 待完成任务列表（支持分页） */
    @GetMapping
    @Operation(summary = "获取学生任务列表", description = "分页获取学生的待完成任务列表")
    @Parameter(name = "page", description = "页码", example = "1")
    @Parameter(name = "size", description = "每页数量", example = "20")
    public R<?> listTasks(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size) {
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        Long studentId = studentResolver.resolveCurrentStudentId();
        if (studentId == null) return R.ok(Map.of("records", List.of(), "total", 0));
        return R.ok(taskService.getStudentTasksWithSubmission(studentId, page, size));
    }

    @GetMapping("/pending")
    @Operation(summary = "获取待完成任务", description = "获取学生所有待完成的任务")
    public R<List<Task>> pendingTasks() {
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        Long studentId = studentResolver.resolveCurrentStudentId();
        List<Task> tasks = taskService.getStudentTasks(studentId);
        taskService.enrichTasks(tasks, null);
        return R.ok(tasks);
    }

    @GetMapping("/completed")
    @Operation(summary = "获取已完成任务", description = "获取学生所有已完成的任务")
    public R<List<Task>> completedTasks() {
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        Long studentId = studentResolver.resolveCurrentStudentId();
        List<Task> tasks = taskService.getStudentCompletedTasks(studentId);
        taskService.enrichTasks(tasks, null);
        return R.ok(tasks);
    }

    @GetMapping("/{id:[0-9]+}")
    @Operation(summary = "获取任务详情", description = "学生查看任务详情（含提交状态）")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<Task> detail(@PathVariable Long id) {
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        Long studentId = studentResolver.resolveCurrentStudentId();
        Task t = taskService.getById(id);
        if (t != null) {
            taskService.enrichTasks(java.util.List.of(t), null);
            TaskSubmission sub = submissionService.getByTaskAndStudent(id, studentId);
            if (sub != null) {
                t.setSubmissionId(sub.getId());
                t.setSubmissionStatus(sub.getStatus());
                t.setScore(sub.getScore());
                t.setScoreJson(sub.getScoreJson());
                t.setGradingMessage(sub.getGradingMessage());
                t.setCheatTerminated(sub.getCheatTerminated());
                t.setContent(sub.getContent());
                t.setAttachments(sub.getAttachments());
                t.setReflection(sub.getReflection());
            }
        }
        return R.ok(t);
    }

    @GetMapping("/{id:[0-9]+}/questions")
    @Operation(summary = "获取任务题目", description = "学生获取任务的答题题目")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<?> questions(@PathVariable Long id) {
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        // 校验学生是否属于该任务的目标班级
        Long studentId = studentResolver.resolveCurrentStudentId();
        if (studentId != null) {
            com.school.teaching.entity.Student student = studentService.getById(studentId);
            com.school.teaching.entity.Task task = taskService.getById(id);
            if (student != null && task != null && task.getTargetId() != null
                && !task.getTargetId().equals(student.getClassId())) {
                return R.error(403, "您不在该任务的班级中");
            }
        }
        return R.ok(taskService.getStudentQuestions(id));
    }

    /** 任务提交状态（缓存 30 秒） */
    @GetMapping("/{taskId:[0-9]+}/status")
    @Operation(summary = "获取任务提交状态", description = "查询学生对指定任务的提交状态")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "123")
    public R<Map<String, Object>> taskStatus(@PathVariable Long taskId) {
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        Long studentId = studentResolver.resolveCurrentStudentId();
        if (studentId == null) return R.ok(Map.of("submitted", false, "status", "PENDING"));
        TaskSubmission sub = submissionService.getByTaskAndStudent(taskId, studentId);
        if (sub == null) return R.ok(Map.of("submitted", false, "status", "PENDING"));
        return R.ok(Map.of(
            "submitted", true,
            "status", sub.getStatus(),
            "score", sub.getScore() != null ? sub.getScore() : 0,
            "submissionId", sub.getId()
        ));
    }

    /** 开始答题 — 创建 PENDING 状态提交记录，启用防作弊追踪 */
    @PostMapping("/{id:[0-9]+}/actions/start")
    @Operation(summary = "开始答题", description = "创建PENDING状态提交记录，启用防作弊追踪")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<Map<String, Object>> startExam(@PathVariable Long id) {
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        Long studentId = studentResolver.resolveCurrentStudentId();
        if (studentId == null) return R.error(404, "未找到学生信息");
        return R.ok(submissionService.startExam(id, studentId));
    }

    @AuditLog(eventType = AuditEventType.TASK_SUBMIT, description = "提交任务")
    @PostMapping("/{id:[0-9]+}/actions/submit")
    @Operation(summary = "提交任务", description = "学生提交任务答案")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "提交成功"),
        @ApiResponse(responseCode = "403", description = "非学生或无权限")
    })
    public R<TaskSubmission> submit(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        Long studentId = studentResolver.resolveCurrentStudentId();
        return R.ok(submissionService.submit(id, studentId, payload));
    }

    /** 切屏警告上报/状态同步（考试防作弊） */
    @PostMapping("/{taskId:[0-9]+}/actions/cheat-warning")
    @Operation(summary = "上报切屏警告", description = "考试防作弊切屏警告上报或状态同步")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "123")
    public R<Map<String, Object>> reportCheatWarning(@PathVariable Long taskId,
                                                      @RequestBody(required = false) Map<String, Object> body) {
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        Long studentId = studentResolver.resolveCurrentStudentId();
        if (studentId == null) return R.error(404, "未找到学生信息");

        String eventType = body != null && body.containsKey("eventType")
            ? body.get("eventType").toString() : "UNKNOWN";

        // sync=true 仅查询当前状态不递增计数（前端 flushPending 定期同步用）
        boolean syncOnly = body != null && Boolean.TRUE.equals(body.get("sync"));

        if (!syncOnly) {
            // 频率限制：每学生每任务每秒最多1次（仅真实切屏事件受限）
            String rateKey = studentId + ":" + taskId;
            long now = System.currentTimeMillis();
            java.util.List<Long> timestamps = cheatRateLimiter.computeIfAbsent(rateKey, k -> new java.util.ArrayList<>());
            synchronized (timestamps) {
                timestamps.removeIf(t -> now - t > CHEAT_RATE_WINDOW_MS);
                if (timestamps.size() >= CHEAT_RATE_MAX) {
                    return R.ok(java.util.Map.of("cheatWarnings", 0, "maxCheatWarnings", 0, "terminated", false, "rateLimited", true));
                }
                timestamps.add(now);
            }
        }

        return R.ok(taskService.recordCheatWarning(taskId, studentId, eventType, syncOnly));
    }

    /** 保存学习反思 */
    @PutMapping("/{taskId:[0-9]+}/submissions/{submissionId}/reflection")
    @Operation(summary = "保存学习反思", description = "学生保存任务的学习反思")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "123")
    @Parameter(name = "submissionId", description = "提交ID", required = true, example = "456")
    public R<String> saveReflection(@PathVariable Long taskId, @PathVariable Long submissionId,
                                    @RequestBody Map<String, String> body) {
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        Long studentId = studentResolver.resolveCurrentStudentId();
        if (studentId == null) return R.error(404, "未找到学生信息");
        String reflection = body.get("reflection");
        if (reflection == null || reflection.isBlank()) return R.error(400, "反思内容不能为空");
        submissionService.saveReflection(submissionId, studentId, reflection.trim());
        return R.ok("反思已保存");
    }

    /** 薄弱知识点分析 */
    @GetMapping("/wrong-questions/weakness-analysis")
    @Operation(summary = "薄弱知识点分析", description = "分析学生的薄弱知识点")
    @Parameter(name = "subject", description = "学科筛选", required = false)
    public R<List<Map<String, Object>>> weaknessAnalysis(@RequestParam(required = false) String subject) {
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        Long studentId = studentResolver.resolveCurrentStudentId();
        if (studentId == null) return R.error(404, "未找到学生信息");
        return R.ok(wrongQuestionService.weaknessAnalysis(studentId, subject));
    }

    /** 学习画像 */
    @GetMapping("/profile/learning")
    @Operation(summary = "获取学习画像", description = "获取学生的学习画像数据")
    @Parameter(name = "subject", description = "学科筛选", required = false)
    public R<Map<String, Object>> getLearningProfile(@RequestParam(required = false) String subject) {
        if (!SecurityUtils.isStudent()) return R.error(403, "仅学生可访问");
        Long studentId = studentResolver.resolveCurrentStudentId();
        if (studentId == null) return R.error(404, "未找到学生信息");
        return R.ok(profileService.getLearningProfile(studentId, subject));
    }
}
