package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired private DashboardService dashboardService;

    @GetMapping("/actions/teacher")
    public R<Map<String, Object>> teacherStats(@RequestParam(required = false) Long classId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        return R.ok(dashboardService.teacherStats(classId));
    }

    @GetMapping("/actions/exam/{examId}/analysis")
    public R<Map<String, Object>> examAnalysis(@PathVariable Long examId,
                                                @RequestParam(required = false) Long classId) {
        return R.error(410, "旧考试模块已废弃，请使用统一任务系统");
    }
}
