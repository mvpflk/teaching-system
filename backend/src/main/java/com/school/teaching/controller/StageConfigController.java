package com.school.teaching.controller;

import com.school.teaching.annotation.AuditLog;
import com.school.teaching.annotation.AuditEventType;

import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.StageConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/stage-config")
public class StageConfigController {

    @Autowired private StageConfigService stageConfigService;

    // ── P1: 批量修改班级类型 ──
    @PutMapping("/classes/actions/batch-update-type")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "批量更新班级类型")
    public R<?> batchUpdateClassType(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("classIds");
        String type = (String) body.get("classType");
        if (ids == null || ids.isEmpty() || type == null) return R.error(400, "参数错误");
        int updated = stageConfigService.batchUpdateClassType(ids, type);
        return R.ok(Map.of("updated", updated));
    }

    // ── P2: 学段转换记录查询 ──
    @GetMapping("/student/actions/stage-change-logs")
    public R<?> stageChangeLogs(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        return R.ok(stageConfigService.pageStageChangeLogs(page, size));
    }

    // ── P3: 教师跨类统计 ──
    @GetMapping("/teacher/actions/cross-type-stats")
    public R<?> teacherCrossTypeStats() {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        return R.ok(stageConfigService.getTeacherCrossTypeStats());
    }

    // ── P4: 学段类型统计 ──
    @GetMapping("/dashboard/actions/stage-stats")
    public R<?> stageStats() {
        return R.ok(stageConfigService.getStageStats());
    }

    // ── P5: 数据一致性校验 ──
    @PostMapping("/actions/check-data-consistency")
    @AuditLog(eventType = AuditEventType.OTHER, description = "检查数据一致性")
    public R<?> checkDataConsistency() {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        List<Map<String, Object>> mismatches = stageConfigService.checkDataConsistency();
        return R.ok(Map.of("mismatches", mismatches, "total", mismatches.size()));
    }

    // ── P6: edu_stage_config 开关 ──
    @GetMapping("/actions/configs")
    public R<?> listConfigs() {
        return R.ok(stageConfigService.listConfigs());
    }

    @PutMapping("/actions/configs/{id}")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "切换阶段配置")
    public R<?> toggleConfig(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "无权限");
        Integer enabled = body.get("enabled") != null ? (Integer) body.get("enabled") : null;
        stageConfigService.toggleConfig(id, enabled);
        return R.ok();
    }
}
