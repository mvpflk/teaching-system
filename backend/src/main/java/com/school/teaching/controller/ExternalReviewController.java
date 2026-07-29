package com.school.teaching.controller;

import com.school.teaching.annotation.AuditLog;
import com.school.teaching.annotation.AuditEventType;

import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.ExternalReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/external-reviews")
public class ExternalReviewController {

    @Autowired private ExternalReviewService service;

    /** 教师生成外部评阅链接 */
    @PostMapping("/actions/generate")
    @AuditLog(eventType = AuditEventType.OTHER, description = "生成外部评阅链接")
    public R<?> generate(@RequestBody Map<String, Object> body) {
        if (true) return R.error(410, "该功能暂未开放，如需启用请联系管理员");
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        Long taskId = Long.valueOf(body.get("taskId").toString());
        Long submissionId = Long.valueOf(body.get("submissionId").toString());
        int hours = body.get("validHours") instanceof Number n ? n.intValue() : 72;
        String reviewer = (String) body.getOrDefault("reviewerName", "外部评审人");
        var r = service.generateLink(taskId, submissionId, hours, reviewer);
        String link = "/access/external-review?token=" + r.getToken();
        return R.ok(Map.of("token", r.getToken(), "link", link, "expiresAt", r.getExpiresAt().toString()));
    }

    /** 教师查看某任务的外部评阅列表 */
    @GetMapping("/list/{taskId}")
    public R<?> list(@PathVariable Long taskId) {
        if (true) return R.error(410, "该功能暂未开放，如需启用请联系管理员");
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        return R.ok(service.listByTask(taskId));
    }

    /** 教师合并外部评阅到最终成绩 */
    @PostMapping("/{reviewId}/actions/merge")
    @AuditLog(eventType = AuditEventType.OTHER, description = "合并外部评阅分数")
    public R<?> merge(@PathVariable Long reviewId, @RequestBody Map<String, Object> body) {
        if (true) return R.error(410, "该功能暂未开放，如需启用请联系管理员");
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        double weight = body.get("weight") instanceof Number n ? n.doubleValue() : 0.3;
        service.mergeToScore(reviewId, weight);
        return R.ok(Map.of("merged", true));
    }

    /** 批量合并某任务所有外部评阅 */
    @PostMapping("/actions/batch-merge/{taskId}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "批量合并外部评阅")
    public R<?> batchMerge(@PathVariable Long taskId, @RequestBody Map<String, Object> body) {
        if (true) return R.error(410, "该功能暂未开放，如需启用请联系管理员");
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        double weight = body.get("weight") instanceof Number n ? n.doubleValue() : 0.3;
        int count = service.batchMergeByTask(taskId, weight);
        return R.ok(Map.of("merged", count));
    }
}
