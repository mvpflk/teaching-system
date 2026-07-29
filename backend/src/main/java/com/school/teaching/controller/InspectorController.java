package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.entity.InspectionAlertRule;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.InspectionAiService;
import com.school.teaching.service.InspectionAlertService;
import com.school.teaching.service.InspectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inspector")
@PreAuthorize("hasAnyRole('INSPECTOR','ADMIN','SUPER_ADMIN','REGION_ADMIN')")
public class InspectorController {

    @Autowired private InspectorService inspectorService;
    @Autowired private InspectionAlertService inspectionAlertService;
    @Autowired private InspectionAiService inspectionAiService;

    @GetMapping("/actions/dashboard")
    public R<Map<String, Object>> dashboard() {
        return R.ok(inspectorService.dashboard());
    }

    // ── 新增分析端点 ──────────────────────────────

    @GetMapping("/actions/score-analysis")
    public R<Map<String, Object>> scoreAnalysis(
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long taskId) {
        return R.ok(inspectorService.scoreAnalysis(stageId, grade, classId, taskId));
    }

    @GetMapping("/actions/peer-review-stats")
    public R<Map<String, Object>> peerReviewStats(
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) String grade) {
        return R.ok(inspectorService.peerReviewStats(stageId, grade));
    }

    @GetMapping("/actions/review-progress")
    public R<Map<String, Object>> reviewProgress() {
        return R.ok(inspectorService.reviewProgress());
    }

    @GetMapping("/actions/credit-stats")
    public R<Map<String, Object>> creditStats() {
        return R.ok(inspectorService.creditStats());
    }

    @GetMapping("/actions/teacher-activity")
    public R<List<Map<String, Object>>> teacherActivity() {
        return R.ok(inspectorService.teacherActivity());
    }

    // ── 预警规则 ────────────────────────────────────

    @GetMapping("/actions/alerts/rules")
    public R<?> getAlertRules() {
        return R.ok(inspectionAlertService.getRules());
    }

    @PutMapping("/actions/alerts/rules/{id}")
    public R<?> updateAlertRule(@PathVariable Long id, @RequestBody InspectionAlertRule rule) {
        try {
            return R.ok(inspectionAlertService.updateRule(id, rule));
        } catch (RuntimeException e) {
            return R.error(404, e.getMessage());
        }
    }

    @GetMapping("/actions/alerts/logs")
    public R<?> getAlertLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean isRead) {
        return R.ok(inspectionAlertService.getLogs(page, size, isRead));
    }

    @PostMapping("/actions/alerts/read/{id}")
    public R<?> markAlertRead(@PathVariable Long id) {
        inspectionAlertService.markAsRead(id);
        return R.ok(null, "已标记已读");
    }

    @PostMapping("/actions/alerts/read-all")
    public R<?> markAllAlertsRead() {
        int count = inspectionAlertService.markAllAsRead();
        return R.ok(count, "已标记" + count + "条已读");
    }

    // ── 趋势与档案 ──────────────────────────────────

    @GetMapping("/actions/dashboard-trend")
    public R<?> getDashboardTrend(@RequestParam(defaultValue = "WEEKLY") String period) {
        return R.ok(inspectorService.getDashboardTrend(period));
    }

    @GetMapping("/actions/teacher-profile/{teacherId}")
    public R<?> getTeacherProfile(@PathVariable Long teacherId) {
        return R.ok(inspectorService.getTeacherProfile(teacherId));
    }

    @GetMapping("/actions/class-profile/{classId}")
    public R<?> getClassProfile(@PathVariable Long classId) {
        return R.ok(inspectorService.getClassProfile(classId));
    }

    // ── AI 助手 ─────────────────────────────────────

    @GetMapping("/ai/weekly-summary")
    public R<String> getWeeklySummary(
            @RequestParam(required = false) String weekStart,
            @RequestParam(required = false) String weekEnd) {
        LocalDate start = weekStart != null ? LocalDate.parse(weekStart) : LocalDate.now().minusDays(7);
        LocalDate end = weekEnd != null ? LocalDate.parse(weekEnd) : LocalDate.now();
        return R.ok(inspectionAiService.generateWeeklySummary(start, end));
    }

    @GetMapping("/ai/anomalies")
    public R<Map<String, Object>> getAnomalies() {
        return R.ok(inspectionAiService.detectAnomalies());
    }

    @GetMapping("/ai/recommendations")
    public R<Map<String, Object>> getRecommendations() {
        return R.ok(inspectionAiService.getRecommendations());
    }

    @GetMapping("/ai/teaching-research-analysis")
    public R<String> getTeachingResearchAnalysis() {
        return R.ok(inspectionAiService.analyzeTeachingResearch());
    }

    @GetMapping("/ai/lesson-prep-analysis")
    public R<String> getLessonPrepAnalysis() {
        return R.ok(inspectionAiService.analyzeLessonPrep());
    }

    // ── 教研质量 ─────────────────────────────────────

    @GetMapping("/teaching-quality")
    public R<List<Map<String, Object>>> getTeachingGroupQuality() {
        return R.ok(inspectorService.getTeachingGroupQuality());
    }

    // ── 实训监控 ─────────────────────────────────────

    @GetMapping("/practice-stats")
    public R<Map<String, Object>> getPracticeStats() {
        return R.ok(inspectorService.getPracticeStats());
    }
}
