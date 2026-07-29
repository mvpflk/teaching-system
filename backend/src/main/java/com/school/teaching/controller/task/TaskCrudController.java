package com.school.teaching.controller.task;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.annotation.AuditLog;
import com.school.teaching.common.R;
import com.school.teaching.common.TaskBehavior;
import com.school.teaching.common.TaskBehaviorResolver;
import com.school.teaching.dto.request.*;
import com.school.teaching.entity.Task;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 任务 CRUD / 发布 / 审核 / 题目管理。
 * 从 TaskController 拆分（W-3）。
 */
@RestController
@RequestMapping("/task")
@Tag(name = "任务管理-CRUD", description = "任务创建、编辑、删除、发布、审核、题目管理")
public class TaskCrudController {

    private static final Logger log = LoggerFactory.getLogger(TaskCrudController.class);

    private final TaskAccessChecker access;
    private final TaskCrudService taskCrudService;
    private final TaskPublishService taskPublishService;
    private final TaskQueryService taskQueryService;
    private final TaskQuestionService taskQuestionService;
    private final TaskReviewService taskReviewService;
    private final TeacherService teacherService;
    @Autowired(required = false) private org.springframework.cache.CacheManager cacheManager;
    @Autowired(required = false) private com.school.teaching.security.StudentResolver studentResolver;

    public TaskCrudController(TaskAccessChecker access,
                               TaskCrudService taskCrudService,
                               TaskPublishService taskPublishService,
                               TaskQueryService taskQueryService,
                               TaskQuestionService taskQuestionService,
                               TaskReviewService taskReviewService,
                               TeacherService teacherService) {
        this.access = access;
        this.taskCrudService = taskCrudService;
        this.taskPublishService = taskPublishService;
        this.taskQueryService = taskQueryService;
        this.taskQuestionService = taskQuestionService;
        this.taskReviewService = taskReviewService;
        this.teacherService = teacherService;
    }

    // ═══════════ 查询 ═══════════

    @GetMapping("/{id:[0-9]+}")
    @Operation(summary = "获取任务详情", description = "根据任务ID获取任务详细信息")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<Task> detail(@PathVariable Long id) {
        Task task = taskCrudService.getById(id);
        if (!access.canAccessTask(task)) return R.error(403, "无权查看该任务");
        return R.ok(task);
    }

    @GetMapping("/forced-preview")
    @Operation(summary = "预览强制任务覆盖范围", description = "返回强制任务影响的班级数、学生数和范围")
    @Parameter(name = "grade", description = "年级筛选", required = false)
    public R<Map<String, Object>> forcedPreview(@RequestParam(required = false) String grade) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        return R.ok(taskPublishService.forcedPreview(grade));
    }

    @GetMapping("/list")
    @Operation(summary = "分页获取任务列表", description = "根据角色返回对应的任务列表，支持按状态和类型筛选")
    @Parameter(name = "page", description = "页码", example = "1")
    @Parameter(name = "size", description = "每页数量", example = "10")
    @Parameter(name = "status", description = "任务状态筛选: DRAFT/PUBLISHED/CLOSED", example = "PUBLISHED")
    @Parameter(name = "taskType", description = "任务类型筛选，多个用逗号分隔", example = "EXAM,HOMEWORK")
    public R<IPage<Task>> list(@RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer size,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) String taskType) {
        Long userId = SecurityUtils.getCurrentUserId();
        java.util.Set<String> typeFilter = (taskType != null && !taskType.isEmpty())
            ? new java.util.HashSet<>(java.util.Arrays.asList(taskType.split(","))) : null;
        if (SecurityUtils.isTeacherOrAdmin()) {
            if (SecurityUtils.isAdmin()) {
                IPage<Task> pg = taskQueryService.pageByAdmin(new Page<>(page, size), status);
                taskQueryService.enrichTasks(pg.getRecords(), null);
                if (typeFilter != null) pg.getRecords().removeIf(t -> !typeFilter.contains(t.getTaskType()));
                return R.ok(pg);
            }
            Long teacherId = teacherService.getTeacherIdByUserId(userId);
            IPage<Task> result = taskQueryService.pageByTeacher(teacherId != null ? teacherId : userId, new Page<>(page, size), status);
            taskQueryService.enrichTasks(result.getRecords(), teacherId);
            if (typeFilter != null) result.getRecords().removeIf(t -> !typeFilter.contains(t.getTaskType()));
            return R.ok(result);
        }
        if (SecurityUtils.isInspector()) {
            IPage<Task> pg = taskQueryService.pageByAdmin(new Page<>(page, size), status);
            taskQueryService.enrichTasks(pg.getRecords(), null);
            if (typeFilter != null) pg.getRecords().removeIf(t -> !typeFilter.contains(t.getTaskType()));
            return R.ok(pg);
        }
        Long studentId = studentResolver.resolveCurrentStudentId();
        IPage<Task> spg = taskQueryService.pageByStudent(studentId != null ? studentId : 0L, new Page<>(page, size), status);
        if (typeFilter != null) spg.getRecords().removeIf(t -> !typeFilter.contains(t.getTaskType()));
        return R.ok(spg);
    }

    // ═══════════ CRUD ═══════════

    @AuditLog(eventType = AuditEventType.TASK_CREATE, description = "创建任务")
    @PostMapping
    @Operation(summary = "创建任务", description = "教师创建新任务，支持通过targetIds批量创建到多个班级")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数校验失败"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    public R<?> create(@RequestBody Task task) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        if (task.getTaskBehavior() != null && !task.getTaskBehavior().isBlank()
                && (task.getTaskType() == null || task.getTaskType().isBlank())) {
            try {
                task.setTaskType(TaskBehaviorResolver.resolveTaskTypeString(
                    TaskBehavior.valueOf(task.getTaskBehavior())));
            } catch (IllegalArgumentException e) {
                return R.error(400, "无效的任务行为: " + task.getTaskBehavior());
            }
        }
        if (task.getTargetIds() != null && !task.getTargetIds().isEmpty()) {
            List<Task> results = new ArrayList<>();
            for (Long targetId : task.getTargetIds()) {
                Task t = new Task();
                t.setTitle(task.getTitle());
                t.setDescription(task.getDescription());
                t.setTaskType(task.getTaskType());
                t.setScoreType(task.getScoreType());
                t.setSubject(task.getSubject());
                t.setGradeId(task.getGradeId());
                t.setTotalScore(task.getTotalScore());
                t.setDeadline(task.getDeadline());
                t.setTaskConfig(task.getTaskConfig());
                t.setTargetType("CLASS");
                t.setTargetId(targetId);
                t.setIsRequired(task.getIsRequired());
                t.setNotifyParents(task.getNotifyParents());
                t.setAllowResubmit(task.getAllowResubmit());
                t.setWuyuTag(task.getWuyuTag());
                t.setIsCompetitionMode(task.getIsCompetitionMode());
                t.setIsForced(task.getIsForced());
                t.setTermId(task.getTermId());
                t.setRubricId(task.getRubricId());
                t.setQuestionIds(task.getQuestionIds());
                t.setSurveySchema(task.getSurveySchema());
                t.setAutoWrongbook(task.getAutoWrongbook());
                t.setAllowCustomSteps(task.getAllowCustomSteps());
                t.setReferenceImages(task.getReferenceImages());
                t.setScheduledPublishAt(task.getScheduledPublishAt());
                t.setDifficultyLevel(task.getDifficultyLevel());
                t.setGroupIds(task.getGroupIds());
                t.setPassRate(task.getPassRate());
                t.setMaxAttempts(task.getMaxAttempts());
                t.setRetakeDeadlineHours(task.getRetakeDeadlineHours());
                t.setPassMode(task.getPassMode());
                t.setIsAnonymous(task.getIsAnonymous());
                t.setSourceTaskId(task.getSourceTaskId());
                t.setTaskBehavior(task.getTaskBehavior());
                t.setViewScope(task.getViewScope());
                results.add(taskCrudService.create(t));
            }
            for (Task item : results) {
                access.logTeacherActivity("CREATE_TASK", "TASK", item.getId());
            }
            return R.ok(results);
        }
        Task created = taskCrudService.create(task);
        access.logTeacherActivity("CREATE_TASK", "TASK", created.getId());
        return R.ok(created);
    }

    @AuditLog(eventType = AuditEventType.TASK_UPDATE, description = "编辑任务")
    @PutMapping("/{id}")
    @Operation(summary = "更新任务", description = "修改任务的基本信息")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<Task> update(@PathVariable Long id, @RequestBody Task task) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        access.checkOwnership(id);
        access.logTeacherActivity("UPDATE_TASK", "TASK", id);
        return R.ok(taskCrudService.update(id, task));
    }

    @AuditLog(eventType = AuditEventType.TASK_DELETE, description = "删除任务")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除任务", description = "删除指定任务（仅创建者或管理员可操作）")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<Void> delete(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        access.checkOwnership(id);
        taskCrudService.delete(id);
        access.logTeacherActivity("DELETE_TASK", "TASK", id);
        return R.ok();
    }

    @PostMapping("/{id}/actions/copy")
    @Operation(summary = "复制任务", description = "复制任务的题目和配置，新任务状态为草稿")
    @Parameter(name = "id", description = "源任务ID", required = true, example = "123")
    public R<Task> copyTask(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        access.checkOwnership(id);
        Task copied = taskCrudService.copyTask(id, SecurityUtils.getCurrentUserId());
        access.logTeacherActivity("CREATE_TASK", "TASK", copied.getId());
        return R.ok(copied);
    }

    // ═══════════ 发布 ═══════════

    @AuditLog(eventType = AuditEventType.TASK_PUBLISH, description = "发布任务")
    @PostMapping("/{id}/actions/publish")
    @Operation(summary = "发布任务", description = "将草稿状态的任务发布为已发布状态")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<Void> publish(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        access.checkOwnership(id);
        taskPublishService.publish(id);
        access.logTeacherActivity("CREATE_TASK", "TASK", id);
        return R.ok();
    }

    @AuditLog(eventType = AuditEventType.TASK_CLOSE, description = "关闭任务")
    @PostMapping("/{id}/actions/close")
    @Operation(summary = "关闭任务", description = "将已发布任务关闭，学生不再能提交")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<Void> close(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        access.checkOwnership(id);
        taskPublishService.close(id);
        access.logTeacherActivity("CREATE_TASK", "TASK", id);
        return R.ok();
    }

    @PostMapping("/actions/clear-cache")
    @Operation(summary = "清除任务列表缓存", description = "手动清除任务列表的Redis缓存")
    public R<Void> clearTaskCache() {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        try {
            org.springframework.cache.Cache cache = cacheManager.getCache("task_list");
            if (cache != null) cache.clear();
        } catch (Exception e) {
            log.warn("清除任务缓存失败: {}", e.getMessage());
        }
        return R.ok();
    }

    @PostMapping("/{id}/actions/reopen")
    @Operation(summary = "重新打开任务", description = "将已关闭任务重新打开为草稿状态")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<Void> reopen(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        access.checkOwnership(id);
        taskPublishService.reopen(id);
        return R.ok();
    }

    // ═══════════ 审核 ═══════════

    @PostMapping("/{id}/actions/submit-review")
    @Operation(summary = "提交审核", description = "将任务提交给管理员审核")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<String> submitReview(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        taskReviewService.submitForReview(id);
        access.logTeacherActivity("CREATE_TASK", "TASK", id);
        return R.ok("已提交审核");
    }

    @PostMapping("/{id}/actions/approve")
    @Operation(summary = "审核通过", description = "管理员审核通过任务")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<String> approveReview(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        taskReviewService.approveReview(id, SecurityUtils.getCurrentUserId());
        return R.ok("已通过");
    }

    @PostMapping("/{id}/actions/reject")
    @Operation(summary = "审核退回", description = "管理员退回任务并附退回原因")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<String> rejectReview(@PathVariable Long id, @RequestBody ReviewRejectRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        taskReviewService.rejectReview(id, SecurityUtils.getCurrentUserId(), request.getReason());
        return R.ok("已退回");
    }

    @GetMapping("/actions/pending-review")
    @Operation(summary = "获取待审核任务", description = "获取当前教师待审核的任务列表")
    public R<?> pendingReview() {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return R.ok(taskReviewService.getPendingReviews(teacherId));
    }

    // ═══════════ 题目管理 ═══════════

    @GetMapping("/{id}/questions")
    @Operation(summary = "获取任务题目", description = "获取任务关联的题目列表")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<?> questions(@PathVariable Long id) {
        Task task = taskCrudService.getById(id);
        if (!access.canAccessTask(task)) return R.error(403, "无权查看该任务");
        if (SecurityUtils.isTeacherOrAdmin()) {
            return R.ok(taskQuestionService.getQuestionsWithDetails(id));
        }
        return R.ok(taskQuestionService.getQuestions(id));
    }

    @AuditLog(eventType = AuditEventType.TASK_UPDATE, description = "向任务添加题目")
    @PostMapping("/{id}/actions/add-questions")
    @Operation(summary = "添加题目到任务", description = "向指定任务添加题目")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<?> addQuestions(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        access.checkOwnership(id);
        taskQuestionService.addQuestions(id, body.get("questionIds"));
        return R.ok();
    }

    @AuditLog(eventType = AuditEventType.TASK_UPDATE, description = "从任务移除题目")
    @PostMapping("/{id}/actions/remove-questions")
    @Operation(summary = "从任务移除题目", description = "从指定任务移除题目")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<?> removeQuestions(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        access.checkOwnership(id);
        taskQuestionService.removeQuestions(id, body.get("questionIds"));
        return R.ok();
    }
}
