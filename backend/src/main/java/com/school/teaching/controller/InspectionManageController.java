package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.entity.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.teaching.entity.Task;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inspector/manage")
@PreAuthorize("hasAnyRole('INSPECTOR','ADMIN','SUPER_ADMIN','REGION_ADMIN')")
@RequiredArgsConstructor
public class InspectionManageController {

    private final InspectionRecordService recordService;
    private final InspectionIssueService issueService;
    private final RectificationNoticeService noticeService;
    private final InspectionReportService reportService;
    private final InspectionIssueCommentService commentService;
    private final ClassroomPatrolService classroomPatrolService;
    private final MoralInspectionService moralInspectionService;
    private final ParentFeedbackSummaryService parentFeedbackSummaryService;
    private final TeachingGroupService teachingGroupService;
    private final TaskService taskService;
    private final TeacherService teacherService;
    private final UserService userService;

    private boolean canAccess() {
        return SecurityUtils.isInspector() || SecurityUtils.isAdmin() || SecurityUtils.isRegionAdmin();
    }

    // ── 巡视记录 ───────────────────────────────────

    @PostMapping("/records")
    public R<InspectionRecord> createRecord(@RequestBody InspectionRecord record) {
        return canAccess() ? R.ok(recordService.create(record)) : R.error(403, "无巡视权限");
    }

    @GetMapping("/records")
    public R<?> getRecords(
            @RequestParam(required = false) Long inspectorId,
            @RequestParam(required = false) String recordType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return canAccess() ? R.ok(recordService.getPage(inspectorId, recordType, startDate, endDate, page, size))
            : R.error(403, "无巡视权限");
    }

    @GetMapping("/records/{id}")
    public R<InspectionRecord> getRecord(@PathVariable Long id) {
        return canAccess() ? R.ok(recordService.getById(id)) : R.error(403, "无巡视权限");
    }

    @PutMapping("/records/{id}")
    public R<InspectionRecord> updateRecord(@PathVariable Long id, @RequestBody InspectionRecord record) {
        return canAccess() ? R.ok(recordService.update(id, record)) : R.error(403, "无巡视权限");
    }

    @DeleteMapping("/records/{id}")
    public R<Void> deleteRecord(@PathVariable Long id) {
        if (!canAccess()) return R.error(403, "无巡视权限");
        recordService.delete(id);
        return R.ok(null, "删除成功");
    }

    // ── 问题台账 ───────────────────────────────────

    @PostMapping("/issues")
    public R<InspectionIssue> createIssue(@RequestBody InspectionIssue issue) {
        return canAccess() ? R.ok(issueService.create(issue)) : R.error(403, "无巡视权限");
    }

    @GetMapping("/issues")
    public R<?> getIssues(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Long assignedTo,
            @RequestParam(required = false) Long assignedClassId,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return canAccess() ? R.ok(issueService.getPage(status, category, severity,
            assignedTo, assignedClassId, createdBy, startDate, endDate, page, size))
            : R.error(403, "无巡视权限");
    }

    @GetMapping("/issues/{id}")
    public R<InspectionIssue> getIssue(@PathVariable Long id) {
        return canAccess() ? R.ok(issueService.getById(id)) : R.error(403, "无巡视权限");
    }

    @PutMapping("/issues/{id}")
    public R<InspectionIssue> updateIssue(@PathVariable Long id, @RequestBody InspectionIssue data) {
        return canAccess() ? R.ok(issueService.update(id, data)) : R.error(403, "无巡视权限");
    }

    @DeleteMapping("/issues/{id}")
    public R<Void> deleteIssue(@PathVariable Long id) {
        if (!canAccess()) return R.error(403, "无巡视权限");
        issueService.delete(id);
        return R.ok(null, "删除成功");
    }

    @PostMapping("/issues/{id}/actions/assign")
    public R<InspectionIssue> assignIssue(
            @PathVariable Long id,
            @RequestParam Long teacherId,
            @RequestParam(required = false) String deadline) {
        return canAccess() ? R.ok(issueService.assignIssue(id, teacherId, deadline))
            : R.error(403, "无巡视权限");
    }

    @PostMapping("/issues/{id}/actions/start")
    public R<InspectionIssue> startIssue(
            @PathVariable Long id,
            @RequestParam Long teacherId) {
        return canAccess() ? R.ok(issueService.startProgress(id, teacherId))
            : R.error(403, "无巡视权限");
    }

    @PostMapping("/issues/{id}/actions/resolve")
    public R<InspectionIssue> resolveIssue(
            @PathVariable Long id,
            @RequestParam Long teacherId,
            @RequestParam(required = false) String resolveComment) {
        return canAccess() ? R.ok(issueService.resolveIssue(id, teacherId, resolveComment))
            : R.error(403, "无巡视权限");
    }

    @PostMapping("/issues/{id}/actions/verify")
    public R<InspectionIssue> verifyIssue(
            @PathVariable Long id,
            @RequestParam Long inspectorId,
            @RequestParam boolean approved,
            @RequestParam(required = false) String verifyComment) {
        return canAccess() ? R.ok(issueService.verifyIssue(id, inspectorId, approved, verifyComment))
            : R.error(403, "无巡视权限");
    }

    @GetMapping("/issues/{id}/actions/comment")
    public R<?> getIssueComments(@PathVariable Long id) {
        return canAccess() ? R.ok(commentService.getCommentsByIssue(id)) : R.error(403, "无巡视权限");
    }

    @PostMapping("/issues/{id}/actions/comment")
    public R<?> addIssueComment(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam String content,
            @RequestParam(defaultValue = "0") int isSystem) {
        return canAccess() ? R.ok(commentService.addComment(id, userId, content, isSystem))
            : R.error(403, "无巡视权限");
    }

    @GetMapping("/issues/stats")
    public R<Map<String, Object>> getIssueStats() {
        return canAccess() ? R.ok(issueService.getIssueStats()) : R.error(403, "无巡视权限");
    }

    // ── 整改通知书 ─────────────────────────────────

    @PostMapping("/notices")
    public R<RectificationNotice> createNotice(@RequestBody RectificationNotice notice) {
        return canAccess() ? R.ok(noticeService.create(notice)) : R.error(403, "无巡视权限");
    }

    @GetMapping("/notices")
    public R<?> getNotices(
            @RequestParam Long userId,
            @RequestParam String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return canAccess() ? R.ok(noticeService.getPage(userId, role, page, size))
            : R.error(403, "无巡视权限");
    }

    // ── 巡视报告 ───────────────────────────────────

    @GetMapping("/reports")
    public R<?> getReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return canAccess() ? R.ok(reportService.getPage(page, size)) : R.error(403, "无巡视权限");
    }

    @PostMapping("/reports/actions/generate")
    public R<InspectionReport> generateReport(
            @RequestParam String reportType,
            @RequestParam(required = false) String periodStart,
            @RequestParam(required = false) String periodEnd,
            @RequestParam Long userId) {
        return canAccess() ? R.ok(reportService.generate(reportType,
            periodStart != null ? LocalDate.parse(periodStart) : null,
            periodEnd != null ? LocalDate.parse(periodEnd) : null, userId))
            : R.error(403, "无巡视权限");
    }

    @GetMapping("/reports/{id}")
    public R<InspectionReport> getReport(@PathVariable Long id) {
        return canAccess() ? R.ok(reportService.getById(id)) : R.error(403, "无巡视权限");
    }

    @PostMapping("/reports/{id}/actions/publish")
    public R<InspectionReport> publishReport(@PathVariable Long id) {
        return canAccess() ? R.ok(reportService.publish(id)) : R.error(403, "无巡视权限");
    }

    @DeleteMapping("/reports/{id}")
    public R<Void> deleteReport(@PathVariable Long id) {
        if (!canAccess()) return R.error(403, "无巡视权限");
        reportService.delete(id);
        return R.ok(null, "删除成功");
    }

    // ── 课堂巡课 ───────────────────────────────────

    @PostMapping("/classroom-patrols")
    public R<ClassroomPatrol> createClassroomPatrol(@RequestBody ClassroomPatrol patrol) {
        return canAccess() ? R.ok(classroomPatrolService.create(patrol)) : R.error(403, "无巡视权限");
    }

    @GetMapping("/classroom-patrols")
    public R<?> getClassroomPatrols(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long inspectorId,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return canAccess() ? R.ok(classroomPatrolService.getPage(classId, teacherId, inspectorId,
            subject, startDate, endDate, page, size)) : R.error(403, "无巡视权限");
    }

    @GetMapping("/classroom-patrols/{id}")
    public R<ClassroomPatrol> getClassroomPatrol(@PathVariable Long id) {
        return canAccess() ? R.ok(classroomPatrolService.getById(id)) : R.error(403, "无巡视权限");
    }

    @PutMapping("/classroom-patrols/{id}")
    public R<ClassroomPatrol> updateClassroomPatrol(@PathVariable Long id, @RequestBody ClassroomPatrol data) {
        return canAccess() ? R.ok(classroomPatrolService.update(id, data)) : R.error(403, "无巡视权限");
    }

    @DeleteMapping("/classroom-patrols/{id}")
    public R<Void> deleteClassroomPatrol(@PathVariable Long id) {
        if (!canAccess()) return R.error(403, "无巡视权限");
        classroomPatrolService.delete(id);
        return R.ok(null, "删除成功");
    }

    @GetMapping("/classroom-patrols/recent")
    public R<?> getRecentPatrols(@RequestParam Long classId, @RequestParam(defaultValue = "5") int limit) {
        return canAccess() ? R.ok(classroomPatrolService.getRecentPatrols(classId, limit))
            : R.error(403, "无巡视权限");
    }

    // ── 德育巡视 ───────────────────────────────────

    @PostMapping("/moral-inspections")
    public R<MoralInspection> createMoralInspection(@RequestBody MoralInspection inspection) {
        return canAccess() ? R.ok(moralInspectionService.create(inspection)) : R.error(403, "无巡视权限");
    }

    @GetMapping("/moral-inspections")
    public R<?> getMoralInspections(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long inspectorId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return canAccess() ? R.ok(moralInspectionService.getPage(classId, inspectorId,
            category, startDate, endDate, page, size)) : R.error(403, "无巡视权限");
    }

    @GetMapping("/moral-inspections/{id}")
    public R<MoralInspection> getMoralInspection(@PathVariable Long id) {
        return canAccess() ? R.ok(moralInspectionService.getById(id)) : R.error(403, "无巡视权限");
    }

    @PutMapping("/moral-inspections/{id}")
    public R<MoralInspection> updateMoralInspection(@PathVariable Long id, @RequestBody MoralInspection data) {
        return canAccess() ? R.ok(moralInspectionService.update(id, data)) : R.error(403, "无巡视权限");
    }

    @DeleteMapping("/moral-inspections/{id}")
    public R<Void> deleteMoralInspection(@PathVariable Long id) {
        if (!canAccess()) return R.error(403, "无巡视权限");
        moralInspectionService.delete(id);
        return R.ok(null, "删除成功");
    }

    @GetMapping("/moral-inspections/recent")
    public R<?> getRecentMoralInspections(@RequestParam Long classId, @RequestParam(defaultValue = "5") int limit) {
        return canAccess() ? R.ok(moralInspectionService.getRecentInspections(classId, limit))
            : R.error(403, "无巡视权限");
    }

    // ── 教研活动（功能未完成，已移除） ─────────────────

    // ── 家长反馈 ───────────────────────────────────

    @PostMapping("/parent-feedback")
    public R<ParentFeedbackSummary> createParentFeedback(@RequestBody ParentFeedbackSummary summary) {
        return canAccess() ? R.ok(parentFeedbackSummaryService.create(summary)) : R.error(403, "无巡视权限");
    }

    @GetMapping("/parent-feedback")
    public R<?> getParentFeedback(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String period,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return canAccess() ? R.ok(parentFeedbackSummaryService.getPage(classId, period, page, size))
            : R.error(403, "无巡视权限");
    }

    @GetMapping("/parent-feedback/{id}")
    public R<ParentFeedbackSummary> getParentFeedbackById(@PathVariable Long id) {
        return canAccess() ? R.ok(parentFeedbackSummaryService.getById(id)) : R.error(403, "无巡视权限");
    }

    @PutMapping("/parent-feedback/{id}")
    public R<ParentFeedbackSummary> updateParentFeedback(@PathVariable Long id, @RequestBody ParentFeedbackSummary data) {
        return canAccess() ? R.ok(parentFeedbackSummaryService.update(id, data)) : R.error(403, "无巡视权限");
    }

    @DeleteMapping("/parent-feedback/{id}")
    public R<Void> deleteParentFeedback(@PathVariable Long id) {
        if (!canAccess()) return R.error(403, "无巡视权限");
        parentFeedbackSummaryService.delete(id);
        return R.ok(null, "删除成功");
    }

    @GetMapping("/parent-feedback/latest")
    public R<?> getLatestParentFeedback() {
        return canAccess() ? R.ok(parentFeedbackSummaryService.getLatestByClass())
            : R.error(403, "无巡视权限");
    }

    // ── 审核流水 ─────────────────────────────────────

    @GetMapping("/review-flow")
    public R<?> getReviewFlow(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (!canAccess()) return R.error(403, "无巡视权限");

        IPage<Task> p = taskService.pageTasksForReview(reviewStatus, startDate, endDate, page, size);

        List<Long> teacherIds = p.getRecords().stream()
            .map(Task::getTeacherId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> teacherNameMap = new HashMap<>();
        if (!teacherIds.isEmpty()) {
            List<com.school.teaching.entity.Teacher> teachers = teacherService.getTeachersByIds(teacherIds);
            List<Long> userIds = teachers.stream().map(com.school.teaching.entity.Teacher::getUserId).collect(Collectors.toList());
            if (!userIds.isEmpty()) {
                List<com.school.teaching.entity.User> users = userService.getUsersByIds(userIds);
                Map<Long, String> userMap = users.stream().collect(Collectors.toMap(com.school.teaching.entity.User::getId, com.school.teaching.entity.User::getRealName));
                for (com.school.teaching.entity.Teacher t : teachers) {
                    teacherNameMap.put(t.getId(), userMap.getOrDefault(t.getUserId(), "未知"));
                }
            }
        }

        Map<String, String> statusLabelMap = Map.of(
            "PENDING_GROUP", "待备课组长审核",
            "PENDING_TEACHING", "待教研组长审核",
            "APPROVED", "已通过",
            "REJECTED", "已驳回"
        );

        List<Map<String, Object>> records = new ArrayList<>();
        for (Task task : p.getRecords()) {
            String status = task.getReviewStatus() != null ? task.getReviewStatus() : "";
            long hours = -1;
            if (task.getCreatedAt() != null && task.getUpdatedAt() != null
                && ("APPROVED".equals(status) || "REJECTED".equals(status))) {
                hours = ChronoUnit.HOURS.between(task.getCreatedAt(), task.getUpdatedAt());
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("taskId", task.getId());
            item.put("title", task.getTitle() != null ? task.getTitle() : "");
            item.put("taskType", task.getTaskType() != null ? task.getTaskType() : "");
            item.put("submitterName", teacherNameMap.getOrDefault(task.getTeacherId(), ""));
            item.put("reviewStatus", status);
            item.put("reviewStatusLabel", statusLabelMap.getOrDefault(status, status));
            item.put("submittedAt", task.getCreatedAt() != null ? task.getCreatedAt().toString() : "");
            item.put("updatedAt", task.getUpdatedAt() != null ? task.getUpdatedAt().toString() : "");
            item.put("reviewHours", hours >= 0 ? hours : null);
            records.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", p.getTotal());
        result.put("current", p.getCurrent());
        result.put("size", p.getSize());
        result.put("pages", p.getPages());
        return R.ok(result);
    }

    // ── 审核统计（按教研组维度） ──────────────────────

    @GetMapping("/review-stats")
    public R<List<Map<String, Object>>> getReviewStats() {
        if (!canAccess()) return R.error(403, "无巡视权限");
        List<com.school.teaching.entity.TeachingGroup> groups = teachingGroupService.listAllEntities();
        List<Map<String, Object>> result = new ArrayList<>();
        for (com.school.teaching.entity.TeachingGroup g : groups) {
            List<com.school.teaching.entity.GroupMember> members = teachingGroupService.getMembersByGroupId(g.getId());
            if (members.isEmpty()) continue;
            List<Long> teacherIds = members.stream()
                .map(com.school.teaching.entity.GroupMember::getTeacherId).collect(Collectors.toList());
            List<Task> groupTasks = taskService.getTasksByTeacherIds(teacherIds);
            long pendingCount = groupTasks.stream()
                .filter(t -> "PENDING_GROUP".equals(t.getReviewStatus()) || "PENDING_TEACHING".equals(t.getReviewStatus()))
                .count();
            long approvedCount = groupTasks.stream()
                .filter(t -> "APPROVED".equals(t.getReviewStatus())).count();
            long rejectedCount = groupTasks.stream()
                .filter(t -> "REJECTED".equals(t.getReviewStatus())).count();
            long totalReviewed = approvedCount + rejectedCount;
            double approvalRate = totalReviewed > 0
                ? Math.round(approvedCount * 1000.0 / totalReviewed) / 10.0 : 0;
            result.add(Map.of(
                "groupId", g.getId(),
                "groupName", g.getName(),
                "groupType", "TEACHING",
                "pendingCount", pendingCount,
                "approvedCount", approvedCount,
                "rejectedCount", rejectedCount,
                "approvalRate", approvalRate
            ));
        }
        return R.ok(result);
    }
}
