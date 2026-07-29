package com.school.teaching.controller.task;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.annotation.AuditLog;
import com.school.teaching.common.R;
import com.school.teaching.dto.RubricDetailDTO;
import com.school.teaching.dto.RubricScoreDTO;
import com.school.teaching.dto.request.*;
import com.school.teaching.entity.Task;
import com.school.teaching.entity.TaskSubmission;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 任务评分 / 提交管理 / 重测。
 * 从 TaskController 拆分（W-3）。
 */
@RestController
@RequestMapping("/task")
@Tag(name = "任务管理-评分", description = "任务提交管理、评分、重测、补交")
public class TaskGradingController {

    private static final Logger log = LoggerFactory.getLogger(TaskGradingController.class);

    private final TaskAccessChecker access;
    private final TaskCrudService taskCrudService;
    private final TaskPublishService taskPublishService;
    private final TaskReviewService taskReviewService;
    private final TaskSubmissionService submissionService;
    private final TaskGradingService gradingService;
    private final RubricScoringService rubricScoringService;
    @Autowired(required = false) private com.school.teaching.common.ExamTaskHandler examTaskHandler;

    public TaskGradingController(TaskAccessChecker access,
                                  TaskCrudService taskCrudService,
                                  TaskPublishService taskPublishService,
                                  TaskReviewService taskReviewService,
                                  TaskSubmissionService submissionService,
                                  TaskGradingService gradingService,
                                  RubricScoringService rubricScoringService) {
        this.access = access;
        this.taskCrudService = taskCrudService;
        this.taskPublishService = taskPublishService;
        this.taskReviewService = taskReviewService;
        this.submissionService = submissionService;
        this.gradingService = gradingService;
        this.rubricScoringService = rubricScoringService;
    }

    // ═══════════ 提交查询 ═══════════

    @GetMapping("/{id}/submissions")
    @Operation(summary = "分页获取提交列表", description = "获取任务的提交记录列表")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    @Parameter(name = "page", description = "页码", example = "1")
    @Parameter(name = "size", description = "每页数量", example = "20")
    @Parameter(name = "keyword", description = "搜索关键词")
    public R<?> submissions(@PathVariable Long id,
                            @RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(defaultValue = "20") Integer size,
                            @RequestParam(required = false) String keyword) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        return R.ok(submissionService.pageByTaskId(id, new Page<>(page, size), keyword));
    }

    @GetMapping("/{id}/submission-board")
    @Operation(summary = "获取提交看板", description = "获取任务的提交统计看板数据")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<Map<String, Object>> submissionBoard(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        return R.ok(submissionService.getSubmissionBoard(id));
    }

    @GetMapping("/{taskId}/submissions/{submissionId}/answers")
    @Operation(summary = "获取提交答案详情", description = "获取某次提交的所有题目作答详情")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "123")
    @Parameter(name = "submissionId", description = "提交ID", required = true, example = "456")
    public R<?> submissionAnswers(@PathVariable Long taskId, @PathVariable Long submissionId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        return R.ok(taskReviewService.getSubmissionAnswers(taskId, submissionId));
    }

    // ═══════════ 评分 ═══════════

    @AuditLog(eventType = AuditEventType.TASK_GRADE, description = "评分任务")
    @PostMapping("/{id}/actions/grade")
    @Operation(summary = "教师评分", description = "教师对任务提交进行评分")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<?> grade(@PathVariable Long id, @Valid @RequestBody TaskGradeRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        access.checkGradingPermission(id);
        TaskSubmission graded;
        if ("PASS_FAIL".equals(request.getScoreType()) || request.getScore() == null) {
            graded = submissionService.grade(request.getSubmissionId(), null, request.getGradeLevel(),
                SecurityUtils.getCurrentUserId(), request.getIsExemplar(), request.getComment());
        } else {
            java.math.BigDecimal score = new java.math.BigDecimal(request.getScore().toString());
            graded = submissionService.grade(request.getSubmissionId(), score, request.getGradeLevel(),
                SecurityUtils.getCurrentUserId(), request.getIsExemplar(), request.getComment());
        }
        if (request.getExplanation() != null && !request.getExplanation().isBlank()) {
            try {
                java.util.Map<String, Object> sj = new java.util.LinkedHashMap<>();
                if (request.getComment() != null) sj.put("comment", request.getComment());
                sj.put("explanation", request.getExplanation());
                submissionService.updateScoreJson(graded.getId(),
                    new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(sj));
            } catch (Exception e) {
                log.warn("评分理由序列化失败 submissionId={}: {}", graded.getId(), e.getMessage());
            }
        }
        return R.ok(graded);
    }

    @PostMapping("/{taskId}/submissions/{submissionId}/actions/grade-items")
    @Operation(summary = "逐题评分", description = "对每道主观题分别打分，自动求和为总分")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "123")
    @Parameter(name = "submissionId", description = "提交ID", required = true, example = "456")
    public R<?> gradeItems(@PathVariable Long taskId, @PathVariable Long submissionId,
                           @RequestBody java.util.Map<String, java.math.BigDecimal> scores) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        access.checkGradingPermission(taskId);
        Map<Long, java.math.BigDecimal> answerScores = new java.util.HashMap<>();
        for (var e : scores.entrySet()) answerScores.put(Long.valueOf(e.getKey()), e.getValue());
        return R.ok(gradingService.gradeItems(submissionId, answerScores, SecurityUtils.getCurrentUserId()));
    }

    @AuditLog(eventType = AuditEventType.TASK_GRADE, description = "自动评分(客观题)")
    @PostMapping("/{taskId}/submissions/{submissionId}/actions/auto-grade")
    @Operation(summary = "自动评分", description = "对客观题（选择题/判断题）进行自动评分")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "123")
    @Parameter(name = "submissionId", description = "提交ID", required = true, example = "456")
    public R<?> autoGrade(@PathVariable Long taskId, @PathVariable Long submissionId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        access.checkGradingPermission(taskId);
        int scored = gradingService.autoGradeObjective(submissionId);
        return R.ok(Map.of("scored", scored));
    }

    @AuditLog(eventType = AuditEventType.TASK_GRADE, description = "评分回退")
    @PostMapping("/{taskId}/submissions/{submissionId}/actions/regrade")
    @Operation(summary = "回退评分", description = "将已评分的提交重置为待评分状态")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "123")
    @Parameter(name = "submissionId", description = "提交ID", required = true, example = "456")
    public R<?> regrade(@PathVariable Long taskId, @PathVariable Long submissionId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        access.checkGradingPermission(taskId);
        return R.ok(submissionService.regrade(submissionId));
    }

    @PostMapping("/{taskId}/submissions/{submissionId}/actions/update-meta")
    @Operation(summary = "更新提交元数据", description = "更新已评分提交的评语和推荐标记")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "123")
    @Parameter(name = "submissionId", description = "提交ID", required = true, example = "456")
    public R<?> updateMeta(@PathVariable Long taskId, @PathVariable Long submissionId,
                           @RequestBody SubmissionMetaRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        access.checkGradingPermission(taskId);
        submissionService.updateMeta(submissionId, request.getComment(), request.getIsExemplar());
        return R.ok();
    }

    // ═══════════ 重测 / 补交 / 达标线 ═══════════

    @PostMapping("/{taskId}/student/{studentId}/allow-extra-submit")
    @Operation(summary = "允许补交", description = "教师特许个别学生补交，即使任务已截止")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "123")
    @Parameter(name = "studentId", description = "学生ID", required = true, example = "456")
    public R<String> allowExtraSubmit(@PathVariable Long taskId, @PathVariable Long studentId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        boolean ok = submissionService.allowExtraSubmit(taskId, studentId);
        return ok ? R.ok("已开启补交通道") : R.notFound("该学生无提交记录");
    }

    @AuditLog(eventType = AuditEventType.TASK_UPDATE, description = "手动触发重测")
    @PostMapping("/{taskId}/student/{studentId}/actions/manual-retake")
    @Operation(summary = "手动触发重测", description = "教师手动为学生创建重测机会")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "123")
    @Parameter(name = "studentId", description = "学生ID", required = true, example = "456")
    public R<?> manualRetake(@PathVariable Long taskId, @PathVariable Long studentId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        access.checkOwnership(taskId);
        TaskSubmission retake = submissionService.manualRetake(taskId, studentId,
            SecurityUtils.getCurrentUserId());
        return R.ok(retake, "已创建重测");
    }

    @AuditLog(eventType = AuditEventType.TASK_GRADE, description = "手动标记通过")
    @PostMapping("/{taskId}/student/{studentId}/actions/manual-pass")
    @Operation(summary = "手动标记通过", description = "教师手动标记学生任务通过")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "123")
    @Parameter(name = "studentId", description = "学生ID", required = true, example = "456")
    public R<?> manualPass(@PathVariable Long taskId, @PathVariable Long studentId,
                           @RequestBody(required = false) ManualPassRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        access.checkOwnership(taskId);
        String reason = request != null ? request.getReason() : null;
        submissionService.manualPass(taskId, studentId,
            SecurityUtils.getCurrentUserId(), reason);
        return R.ok(null, "已标记通过");
    }

    @AuditLog(eventType = AuditEventType.TASK_UPDATE, description = "关闭重测通道")
    @PostMapping("/{taskId}/student/{studentId}/actions/close-retake")
    @Operation(summary = "关闭重测通道", description = "教师关闭学生的重测机会")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "123")
    @Parameter(name = "studentId", description = "学生ID", required = true, example = "456")
    public R<?> closeRetake(@PathVariable Long taskId, @PathVariable Long studentId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        access.checkOwnership(taskId);
        submissionService.closeRetake(taskId, studentId,
            SecurityUtils.getCurrentUserId());
        return R.ok(null, "已关闭重测通道");
    }

    @AuditLog(eventType = AuditEventType.TASK_UPDATE, description = "调整达标配置")
    @PostMapping("/{taskId}/actions/adjust-pass-rate")
    @Operation(summary = "调整达标配置", description = "调整任务的达标线、最大重测次数、重测截止时间")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "123")
    public R<?> adjustPassRate(@PathVariable Long taskId, @Valid @RequestBody PassRateRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        access.checkOwnership(taskId);

        Task task = taskCrudService.getById(taskId);
        if (!java.util.List.of("DRAFT", "PUBLISHED").contains(task.getStatus()))
            return R.error(409, "仅草稿或已发布状态可调整达标配置");

        taskCrudService.updatePassRateConfig(taskId, request.getPassRate(), request.getMaxAttempts(), request.getRetakeDeadlineHours());
        return R.ok(null, "达标配置已更新");
    }

    // ═══════════ 提醒 / 重启 ═══════════

    @PostMapping("/{id}/actions/remind-unsubmitted")
    @Operation(summary = "提醒未提交学生", description = "向未提交任务的学生发送提醒通知")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<Map<String, Object>> remindUnsubmitted(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        Map<String, Object> result = submissionService.remindUnsubmitted(id);
        return R.ok(result, "已提醒 " + result.getOrDefault("reminded", 0) + " 名学生");
    }

    @AuditLog(eventType = AuditEventType.DATA_RESET, description = "一键重启未完成学生考试权限")
    @PostMapping("/{id}/actions/restart-unfinished")
    @Operation(summary = "重启未完成学生", description = "重置所有未提交/终止学生的考试权限")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<Map<String, Object>> restartUnfinished(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        Map<String, Object> result = submissionService.restartUnfinished(id);
        return R.ok(result, String.valueOf(result.getOrDefault("message", "操作完成")));
    }

    @PostMapping("/{taskId}/actions/resend-to-pending")
    @Operation(summary = "重发未提交学生", description = "批量开放未提交学生的补交权限并发送通知")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "123")
    public R<Map<String, Object>> resendToPending(@PathVariable Long taskId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        Map<String, Object> result = taskPublishService.resendToPending(taskId);
        int count = (int) result.getOrDefault("count", 0);
        return R.ok(result, "已为 " + count + " 名学生重新开放提交");
    }

    // ═══════════ 重新评分 (legacy paths) ═══════════

    @AuditLog(eventType = AuditEventType.DATA_RESET, description = "重新评分提交")
    @PostMapping("/submissions/{submissionId}/regrade")
    @Operation(summary = "重新评分提交", description = "教师手动触发，逐题重新判分并更正总分")
    @Parameter(name = "submissionId", description = "提交ID", required = true, example = "456")
    public R<Map<String, Object>> regradeSubmission(@PathVariable Long submissionId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        if (examTaskHandler == null) return R.error(500, "评分处理器不可用");
        return R.ok(examTaskHandler.regradeSubmission(submissionId));
    }

    @AuditLog(eventType = AuditEventType.DATA_RESET, description = "批量重新评分")
    @PostMapping("/submissions/batch-regrade")
    @Operation(summary = "批量重新评分", description = "根据题目ID或任务ID批量重新评分受影响的提交")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "重评完成"),
        @ApiResponse(responseCode = "400", description = "请提供submissionIds、questionId或taskId")
    })
    public R<Map<String, Object>> batchRegrade(@RequestBody BatchRegradeRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");

        List<Number> submissionIds = request.getSubmissionIds();
        Number taskIdNum = request.getTaskId();
        if (submissionIds == null || submissionIds.isEmpty()) {
            Number questionId = request.getQuestionId();
            if (questionId != null) {
                submissionIds = taskReviewService.findSubmissionIdsByQuestionId(questionId.longValue())
                    .stream().map(id -> (Number) id).collect(Collectors.toList());
            } else if (taskIdNum != null) {
                submissionIds = taskReviewService.findSubmissionIdsByTaskId(taskIdNum.longValue())
                    .stream().map(id -> (Number) id).collect(Collectors.toList());
            } else {
                return R.error(400, "请提供 submissionIds、questionId 或 taskId");
            }
        }

        if (taskIdNum != null) {
            access.checkGradingPermission(taskIdNum.longValue());
        } else if (!submissionIds.isEmpty()) {
            for (Number sid : submissionIds) {
                TaskSubmission sub = taskReviewService.getSubmissionById(sid.longValue());
                if (sub != null) access.checkGradingPermission(sub.getTaskId());
            }
        }

        List<Long> longIds = submissionIds.stream().map(Number::longValue).collect(Collectors.toList());
        R<Map<String, Object>> result = R.ok(taskReviewService.batchRegrade(longIds));
        for (Long sid : longIds) {
            TaskSubmission sub = taskReviewService.getSubmissionById(sid);
            if (sub != null) access.logTeacherActivity("GRADE", "TASK", sub.getTaskId());
        }
        return result;
    }

    @GetMapping("/{id}/rubric")
    @Operation(summary = "获取任务量规详情", description = "获取任务绑定的评分量规及其维度、等级描述")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<RubricDetailDTO> getTaskRubric(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        return R.ok(rubricScoringService.getTaskRubric(id));
    }

    @PostMapping("/{taskId}/submissions/{submissionId}/actions/grade-rubric")
    @Operation(summary = "量规逐项评分", description = "按量规维度逐项打分，自动加权计算总分")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "123")
    @Parameter(name = "submissionId", description = "提交ID", required = true, example = "456")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "评分成功"),
        @ApiResponse(responseCode = "400", description = "维度ID无效或等级超出范围"),
        @ApiResponse(responseCode = "404", description = "量规不存在")
    })
    public R<RubricScoreDTO> gradeRubric(@PathVariable Long taskId, @PathVariable Long submissionId,
                                          @jakarta.validation.Valid @RequestBody RubricScoreDTO scores) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        access.checkGradingPermission(taskId);
        Task task = taskCrudService.getById(taskId);
        if (task == null || task.getRubricId() == null) return R.error(400, "该任务未绑定评分量规");
        scores.setRubricId(task.getRubricId());
        return R.ok(rubricScoringService.saveRubricScores(submissionId, scores));
    }
}
