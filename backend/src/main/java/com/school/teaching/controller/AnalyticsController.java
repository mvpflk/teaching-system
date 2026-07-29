package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.security.StudentResolver;
import com.school.teaching.service.AnalyticsService;
import com.school.teaching.service.KnowledgeNodeService;
import com.school.teaching.service.PracticePlanService;
import com.school.teaching.service.TeacherActivityService;
import com.school.teaching.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.*;

/**
 * 成长分析 Controller — 学生端自动取当前用户，教师端传入 studentId/classId 并校验归属
 */
@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    @Autowired private AnalyticsService analyticsService;
    @Autowired private StudentResolver studentResolver;
    @Autowired(required = false) private com.school.teaching.precision.PrecisionService precisionService;
    @Autowired(required = false) private TeacherActivityService teacherActivityService;
    @Autowired private TeacherService teacherService;
    @Autowired private KnowledgeNodeService knowledgeNodeService;
    @Autowired private PracticePlanService practicePlanService;

    // ═══════════ 学生端 ═══════════
    @GetMapping("/student/growth-curve")
    public R<?> studentGrowthCurve(@RequestParam(required = false) String subject) {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(analyticsService.getGrowthCurve(sid, subject));
    }

    @GetMapping("/student/knowledge-radar")
    public R<?> studentKnowledgeRadar(@RequestParam(required = false) String subject) {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(analyticsService.getKnowledgeRadar(sid, subject));
    }

    @GetMapping("/student/achievements")
    public R<?> studentAchievements() {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(analyticsService.getAchievements(sid));
    }

    @GetMapping("/student/daily-encouragement")
    public R<?> studentDailyEncouragement() {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(analyticsService.getDailyEncouragement(sid));
    }

    @GetMapping("/student/summary")
    public R<?> studentSummary(@RequestParam(required = false) String subject) {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(analyticsService.getStudentSummary(sid, subject));
    }

    @GetMapping("/student/available-subjects")
    public R<?> studentAvailableSubjects() {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(analyticsService.getStudentAvailableSubjects(sid));
    }

    // ═══════════ 教师端 ═══════════
    @GetMapping("/teacher/growth-curve")
    public R<?> teacherGrowthCurve(@RequestParam Long studentId, @RequestParam(required = false) String subject) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        if (precisionService != null) precisionService.assertTeacherOwnsStudent(SecurityUtils.getCurrentUserId(), studentId);
        return R.ok(analyticsService.getGrowthCurve(studentId, subject));
    }

    @GetMapping("/teacher/knowledge-radar")
    public R<?> teacherKnowledgeRadar(@RequestParam Long studentId, @RequestParam(required = false) String subject) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        if (precisionService != null) precisionService.assertTeacherOwnsStudent(SecurityUtils.getCurrentUserId(), studentId);
        return R.ok(analyticsService.getKnowledgeRadar(studentId, subject));
    }

    @GetMapping("/teacher/encouragement-preview")
    public R<?> teacherEncouragementPreview(@RequestParam Long studentId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        if (precisionService != null) precisionService.assertTeacherOwnsStudent(SecurityUtils.getCurrentUserId(), studentId);
        return R.ok(analyticsService.getDailyEncouragement(studentId));
    }

    @GetMapping("/teacher/class-growth-curves")
    public R<?> teacherClassGrowthCurves(@RequestParam Long classId, @RequestParam(required = false) String subject) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        if (precisionService != null) precisionService.assertTeacherOwnsClass(SecurityUtils.getCurrentUserId(), classId);
        logTeacherActivity("VIEW_ANALYTICS", "CLASS", classId);
        return R.ok(analyticsService.getClassGrowthCurves(classId, subject));
    }

    @GetMapping("/teacher/export-scores")
    public ResponseEntity<byte[]> exportScores(@RequestParam Long classId,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean blinded) {
        if (!SecurityUtils.isTeacherOrAdmin()) return ResponseEntity.status(403).build();
        logTeacherActivity("VIEW_ANALYTICS", "ANALYSIS", classId);
        String csv = analyticsService.exportScores(classId, subject, startDate, endDate, blinded);
        byte[] bytes = (csv != null ? csv : "").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + (blinded ? "scores-blinded.csv" : "scores.csv"))
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(bytes);
    }

    // ── E5: 课题研究数据导出 ──
    @GetMapping("/research/export")
    public ResponseEntity<byte[]> exportResearchData(
            @RequestParam(defaultValue = "false") boolean blinded) {
        if (!SecurityUtils.isTeacherOrAdmin()) return ResponseEntity.status(403).build();
        logTeacherActivity("VIEW_ANALYTICS", "ANALYSIS", 0L);
        byte[] bytes = analyticsService.exportResearchData(blinded);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + (blinded ? "research-data-blinded.csv" : "research-data.csv"))
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(bytes);
    }

    // ── E6: 知识点掌握趋势 ──
    @GetMapping("/teacher/knowledge-trend")
    public R<?> knowledgeTrend(@RequestParam Long classId,
            @RequestParam(required = false) Long knowledgeNodeId,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        logTeacherActivity("VIEW_ANALYTICS", "ANALYSIS", classId);
        return R.ok(analyticsService.getKnowledgeTrend(classId, knowledgeNodeId, subject, startDate, endDate));
    }

    // ═══════════ 考纲覆盖度 ═══════════

    @GetMapping("/syllabus-coverage")
    public R<List<Map<String, Object>>> getSyllabusCoverage(@RequestParam Integer subjectId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");

        // 获取该学科所有L2/L3节点
        List<KnowledgeNode> allNodes = knowledgeNodeService.listBySubjectId(subjectId.longValue());
        if (allNodes.isEmpty()) return R.ok(Collections.emptyList());

        // 获取已有实训方案（按学科匹配）
        boolean hasSubjectPlans = false;
        // 从 knowledge_nodes 获取学科名称
        String subjectName = allNodes.stream()
            .filter(n -> n.getLevel() == 1)
            .findFirst()
            .map(KnowledgeNode::getName)
            .orElse("");
        if (!subjectName.isEmpty()) {
            long planCount = practicePlanService.countBySubject(subjectName);
            hasSubjectPlans = planCount > 0;
        }

        // 按L2分组构建数据
        List<Map<String, Object>> result = new ArrayList<>();
        Map<Long, List<KnowledgeNode>> l2Groups = new LinkedHashMap<>();

        for (KnowledgeNode node : allNodes) {
            if (node.getLevel() == 2) {
                l2Groups.putIfAbsent(node.getId(), new ArrayList<>());
            }
            if (node.getLevel() == 3 && node.getParentId() != null) {
                l2Groups.computeIfAbsent(node.getParentId(), k -> new ArrayList<>()).add(node);
            }
        }

        // 统计L4数
        Map<Long, Long> l3ToL4Count = new HashMap<>();
        for (KnowledgeNode node : allNodes) {
            if (node.getLevel() == 4 && node.getParentId() != null) {
                l3ToL4Count.merge(node.getParentId(), 1L, Long::sum);
            }
        }

        for (Map.Entry<Long, List<KnowledgeNode>> entry : l2Groups.entrySet()) {
            Long l2Id = entry.getKey();
            List<KnowledgeNode> l3Nodes = entry.getValue();
            if (l3Nodes.isEmpty()) continue;

            KnowledgeNode l2Node = allNodes.stream().filter(n -> n.getId().equals(l2Id)).findFirst().orElse(null);
            String unitName = l2Node != null ? l2Node.getName() : "单元" + l2Id;

            Map<String, Object> unitMap = new LinkedHashMap<>();
            unitMap.put("name", unitName);
            List<Map<String, Object>> tasks = new ArrayList<>();

            for (KnowledgeNode l3 : l3Nodes) {
                Map<String, Object> taskMap = new LinkedHashMap<>();
                taskMap.put("name", l3.getName());
                taskMap.put("nodeId", l3.getId());
                long kpCount = l3ToL4Count.getOrDefault(l3.getId(), 0L);
                taskMap.put("kpCount", (int) kpCount);

                // 覆盖度判定：该学科有实训方案即标记为已覆盖
                int practiceCount = hasSubjectPlans ? 1 : 0;
                // 检查题库是否覆盖该L3任务（通过 category_id 关联）
                int questionCount = 0; // 默认值，后续可通过 question_bank 表查询优化
                taskMap.put("practiceCount", practiceCount);
                taskMap.put("questionCount", questionCount);
                // level: 0=无覆盖(红), 1=仅有题库(黄), 2=有实训(绿)
                int level = practiceCount > 0 ? 2 : (questionCount > 0 ? 1 : 0);
                taskMap.put("level", level);

                tasks.add(taskMap);
            }

            unitMap.put("tasks", tasks);
            result.add(unitMap);
        }

        return R.ok(result);
    }

    private void logTeacherActivity(String action, String targetType, Long targetId) {
        if (teacherActivityService == null) return;
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            Long teacherId = teacherService.getTeacherIdByUserId(userId);
            if (teacherId != null) teacherActivityService.log(teacherId, action, targetType, targetId);
        } catch (Exception ignored) {
            // 静默降级：教师活动日志为辅助功能，不影响主流程
        }
    }
}
