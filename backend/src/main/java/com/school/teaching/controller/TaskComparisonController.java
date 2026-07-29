package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.TaskComparisonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teacher/comparison")
@RequiredArgsConstructor
public class TaskComparisonController {

    private final TaskComparisonService comparisonService;

    /**
     * 对比分析 — 支持单任务或多任务聚合
     * GET /teacher/comparison/116            → 单任务
     * GET /teacher/comparison/116?ids=116,117,118 → 多任务聚合
     */
    @GetMapping("/{taskId}")
    public R<?> compareClasses(@PathVariable Long taskId,
                               @RequestParam(required = false) String ids) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        if (ids != null && !ids.isEmpty()) {
            List<Long> taskIds = Arrays.stream(ids.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::parseLong).toList();
            return R.ok(comparisonService.compareMultiTasks(taskIds));
        }
        return R.ok(comparisonService.compareClasses(taskId));
    }

    /**
     * 完整诊断数据 — 对比数据 + 学生明细 + 分数分布 + 知识点薄弱
     * GET /teacher/comparison/116/diagnosis?ids=116,117,118
     * 所有定量数据由后端预计算，不依赖 AI。AI 分析文本另存于 ai_outputs。
     */
    @GetMapping("/{taskId}/diagnosis")
    public R<?> getDiagnosis(@PathVariable Long taskId,
                             @RequestParam(required = false) String ids) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        List<Long> taskIds;
        if (ids != null && !ids.isEmpty()) {
            taskIds = Arrays.stream(ids.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::parseLong).toList();
        } else {
            taskIds = List.of(taskId);
        }
        // AI 分析文本已下沉到 TaskComparisonService.getDiagnosisData() 内部查询
        // （attachDiagnosisAiText），Controller 不再直接注入 Mapper。见 CONVENTIONS.md 铁律。
        return R.ok(comparisonService.getDiagnosisData(taskIds));
    }

    /**
     * 批量摘要 — 为列表预览获取多个任务组的轻量统计（每班均分+及格率）
     * POST /teacher/comparison/summary
     * Body: { "groups": [ { "taskIds": [116,117] }, { "taskIds": [118] } ] }
     */
    @PostMapping("/summary")
    public R<?> getBatchSummary(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");

        List<List<Long>> groups = new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawGroups = (List<Map<String, Object>>) body.getOrDefault("groups", List.of());
            for (Map<String, Object> g : rawGroups) {
                @SuppressWarnings("unchecked")
                List<Integer> rawIds = (List<Integer>) g.getOrDefault("taskIds", List.of());
                List<Long> taskIds = rawIds.stream().map(Long::valueOf).toList();
                groups.add(taskIds);
            }
        } catch (Exception e) {
            return R.error(400, "请求格式错误: groups 应为 [{taskIds:[1,2]},...]");
        }

        return R.ok(comparisonService.getBatchSummary(groups));
    }

    /**
     * 纵向对比 — 同一班级同一学科历次考试趋势。
     * GET /teacher/comparison/trend?classId=1&subject=语文
     */
    @GetMapping("/trend")
    public R<?> getTrend(@RequestParam(required = false) Long classId,
                         @RequestParam(required = false) String subject) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        return R.ok(comparisonService.getTrend(classId, subject));
    }
}
