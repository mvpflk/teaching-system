package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.entity.AlertRule;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/alert")
public class AlertController {

    @Autowired private AlertService alertService;

    /** 获取规则列表 */
    @GetMapping("/rules")
    public R<List<AlertRule>> getRules() {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        return R.ok(alertService.getRules(SecurityUtils.getCurrentUserId()));
    }

    /** 保存规则（新增或修改） */
    @PostMapping("/rules")
    public R<AlertRule> saveRule(@RequestBody AlertRule rule) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        return R.ok(alertService.saveRule(rule, SecurityUtils.getCurrentUserId()));
    }

    /** 删除自定义规则 */
    @DeleteMapping("/rules/{id}")
    public R<?> deleteRule(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        alertService.deleteRule(id, SecurityUtils.getCurrentUserId());
        return R.ok(null, "已删除");
    }

    /** 预警记录列表 */
    @GetMapping("/records")
    public R<Map<String, Object>> getRecords(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String handledStatus,
            @RequestParam(required = false) String studentName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        return R.ok(alertService.getAlertRecords(SecurityUtils.getCurrentUserId(),
                classId, alertType, handledStatus, studentName, page, pageSize));
    }

    /** 处理预警 */
    @PutMapping("/records/{id}/handle")
    public R<?> handleRecord(@PathVariable Long id, @RequestBody Map<String, String> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        String status = body.getOrDefault("status", "READ");
        alertService.handleAlert(id, status, SecurityUtils.getCurrentUserId());
        return R.ok(null, "已处理");
    }

    /** 手动触发扫描（管理员全量 / 班主任仅本班） */
    @PostMapping("/actions/scan")
    public R<Map<String, Object>> triggerScan() {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        int count;
        if (SecurityUtils.isAdmin()) {
            count = alertService.scanAllStudents();
        } else {
            count = alertService.scanTeacherClasses(SecurityUtils.getCurrentUserId());
        }
        return R.ok(Map.of("alertCount", count));
    }

    /** 近7天预警趋势 */
    @GetMapping("/trend")
    public R<List<Map<String, Object>>> getTrend() {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        return R.ok(alertService.getAlertTrend(SecurityUtils.getCurrentUserId()));
    }
}
