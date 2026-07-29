package com.school.teaching.controller;

import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.annotation.AuditLog;
import com.school.teaching.common.R;
import com.school.teaching.dto.ParamBatchUpdateDTO;
import com.school.teaching.dto.ParamUpdateDTO;
import com.school.teaching.dto.request.RemedialClassesRequest;
import com.school.teaching.dto.request.ResetDataRequest;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.AuditLogService;
import com.school.teaching.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 运维与审计控制器：数据重置/管理概览/系统参数/审计日志/偏科班级。
 * 拆分自 SettingsController，路由前缀 /settings 保持不变。
 */
@RestController
@RequestMapping("/settings")
@io.swagger.v3.oas.annotations.tags.Tag(name = "运维审计", description = "数据重置/参数/审计/偏科配置")
public class AdminMaintenanceController {

    @Autowired private SystemService systemService;
    @Autowired private AuditLogService auditLogService;

    /** 数据重置（仅超级管理员） */
    @PostMapping("/actions/reset")
    @AuditLog(eventType = AuditEventType.DATA_RESET)
    public R<Map<String, Object>> resetData(@RequestBody ResetDataRequest request) {
        if (!SecurityUtils.isSuperAdmin()) return R.error(403, "仅超级管理员可执行");
        if (!"CONFIRM_RESET".equals(request.getConfirm()))
            return R.error(400, "请传入 confirm=CONFIRM_RESET 确认重置");
        String target = request.getTarget() != null ? request.getTarget() : "all";
        return R.ok(systemService.resetData(target));
    }

    /** 管理概览 */
    @GetMapping("/admin/overview")
    public R<Map<String, Object>> adminOverview() {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        return R.ok(systemService.getDashboardOverview());
    }

    /** 系统参数分类 */
    @GetMapping("/admin/params/categories")
    public R<?> paramCategories() {
        return R.ok(List.of("task", "credit", "bbs", "security", "system", "feature"));
    }

    /** 系统参数列表 */
    @GetMapping("/admin/params")
    public R<?> adminParams(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
                            @RequestParam(required = false) String category) {
        return R.ok(systemService.getSystemParams(category));
    }

    /** 批量更新系统参数 */
    @PutMapping("/admin/params/batch")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "批量更新系统参数")
    public R<?> adminParamsBatch(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        systemService.updateSystemParams(body);
        return R.ok(null, "已更新");
    }

    /** 运维信息（兼容别名） */
    @GetMapping("/admin/maintenance/info")
    public R<?> maintenanceInfo() {
        return R.ok(systemService.getSystemInfo());
    }

    /** 运维导入数据（实验期未实现） */
    @PostMapping("/admin/maintenance/import")
    @AuditLog(eventType = AuditEventType.DATA_RESET, description = "运维导入数据")
    public R<?> maintenanceImport(@RequestBody Map<String, Object> body) {
        return R.error(501, "功能待实验期结束后实现");
    }

    /** 运维清空数据（实验期未实现） */
    @PostMapping("/admin/maintenance/clear")
    @AuditLog(eventType = AuditEventType.DATA_RESET, description = "运维清空数据")
    public R<?> maintenanceClear(@RequestBody Map<String, Object> body) {
        return R.error(501, "功能待实验期结束后实现");
    }

    // ── 动态参数 ──
    @GetMapping("/system-params")
    public R<List<Map<String, Object>>> getDynamicParams(@RequestParam(required = false) String category) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        return R.ok(systemService.getDynamicParams(category));
    }

    @GetMapping("/system-params/{key}")
    public R<Map<String, Object>> getParamDetail(@PathVariable String key) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        Map<String, Object> detail = systemService.getParamDetail(key);
        return detail != null ? R.ok(detail) : R.notFound("参数不存在");
    }

    @PutMapping("/system-params/{key}")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE)
    public R<String> updateParam(@PathVariable String key, @RequestBody ParamUpdateDTO body) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        systemService.updateParam(key, body.getNewValue());
        return R.ok(null, "已更新");
    }

    @PutMapping("/system-params/actions/batch")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE)
    public R<String> updateParamsBatch(@RequestBody ParamBatchUpdateDTO body) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        List<Map<String, String>> params = new ArrayList<>();
        for (var p : body.getUpdates()) params.add(Map.of("key", String.valueOf(p.getId()), "value", p.getNewValue()));
        systemService.updateParamsBatch(params);
        return R.ok(null, "已批量更新");
    }

    // ── 审计日志 ──
    @GetMapping("/admin/audit-logs")
    public R<?> adminAuditLogs(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) String eventType,
                               @RequestParam(required = false) String username,
                               @RequestParam(required = false) String startTime,
                               @RequestParam(required = false) String endTime) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        var pg = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.school.teaching.entity.AuditLog>(page, size);
        var result = auditLogService.page(pg, eventType, username, startTime, endTime);
        return R.ok(Map.of("records", result.getRecords(), "total", result.getTotal()));
    }

    @GetMapping("/admin/audit-logs/analysis/event-distribution")
    public R<?> auditEventDist() { return R.ok(auditLogService.getEventDistribution()); }
    @GetMapping("/admin/audit-logs/analysis/active-users")
    public R<?> auditActiveUsers() { return R.ok(auditLogService.getActiveUsers()); }
    @GetMapping("/admin/audit-logs/analysis/hourly-trend")
    public R<?> auditHourlyTrend() { return R.ok(auditLogService.getHourlyTrend()); }

    // ── 偏科提分班级配置 ──
    @GetMapping("/remedial-classes")
    public R<Map<String, Object>> getRemedialClasses() {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        List<Map<String, Object>> allClasses = systemService.getAllClassesWithRemedialStatus();
        String ids = systemService.getRemedialClassIds();
        List<Long> enabledIds = (ids == null || ids.isBlank()) ? List.of()
            : java.util.Arrays.stream(ids.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::parseLong).collect(java.util.stream.Collectors.toList());
        return R.ok(Map.of("allClasses", allClasses, "enabledClassIds", enabledIds));
    }

    @PutMapping("/remedial-classes")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "更新偏科提分班级配置")
    public R<String> updateRemedialClasses(@RequestBody RemedialClassesRequest request) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        String classIds = request.getClassIds() == null || request.getClassIds().isEmpty() ? ""
            : request.getClassIds().stream().map(n -> String.valueOf(n.longValue())).collect(java.util.stream.Collectors.joining(","));
        systemService.setRemedialClassIds(classIds);
        return R.ok("已更新");
    }
}
