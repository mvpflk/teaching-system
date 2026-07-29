package com.school.teaching.controller;

import com.school.teaching.annotation.AuditLog;
import com.school.teaching.annotation.AuditEventType;

import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.ExamShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/exam-share")
public class ExamShareController {

    @Autowired private ExamShareService shareService;

    @PostMapping("/actions/create")
    @AuditLog(eventType = AuditEventType.OTHER, description = "分享试卷")
    public R<Map<String, Object>> createShare(@RequestParam Long taskId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可分享试卷");
        return R.ok(shareService.createShare(taskId, userId));
    }

    @GetMapping("/actions/my-shares")
    public R<List<Map<String, Object>>> myShares() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error("未登录");
        return R.ok(shareService.myShares(userId));
    }

    @DeleteMapping("/{shareId}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "删除试卷分享")
    public R<String> deleteShare(@PathVariable Long shareId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        shareService.deleteShare(shareId, userId);
        return R.ok("已取消分享");
    }

    @PostMapping("/actions/import")
    @AuditLog(eventType = AuditEventType.OTHER, description = "导入分享试卷")
    public R<Map<String, Object>> importShared(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error("未登录");
        if (body.get("shareCode") == null) return R.error("请输入分享码");
        String code = body.get("shareCode").toString().trim().toUpperCase();
        Object targetIdObj = body.get("targetClassId");
        Long targetClassId = targetIdObj instanceof Number ? ((Number) targetIdObj).longValue() : null;
        return R.ok(shareService.importShared(code, userId, targetClassId));
    }

    /** 预览分享试卷题目（不导入） */
    @GetMapping("/actions/preview")
    public R<Map<String, Object>> preview(@RequestParam String shareCode) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        return R.ok(shareService.previewShare(shareCode.trim().toUpperCase()));
    }

    /** 上传试卷文件(word/excel) → 解析题目 → 创建任务 → 自动分享 */
    @PostMapping("/actions/upload")
    public R<Map<String, Object>> uploadExam(@RequestParam("file") MultipartFile file,
                                              @RequestParam String title,
                                              @RequestParam(required = false) String subject) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        return R.ok(shareService.uploadExam(file, title, subject, userId));
    }

    /** 确认上传：接收赋分后创建任务并分享 */
    @PostMapping("/actions/confirm-upload")
    public R<Map<String, Object>> confirmUpload(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        String title = body.get("title") != null ? body.get("title").toString() : "";
        String subject = body.get("subject") != null ? body.get("subject").toString() : "";
        @SuppressWarnings("unchecked")
        Map<String, Object> scoresRaw = body.get("scores") instanceof Map ? (Map<String, Object>) body.get("scores") : Map.of();
        Map<Long, java.math.BigDecimal> scores = new java.util.HashMap<>();
        for (var e : scoresRaw.entrySet()) {
            scores.put(Long.valueOf(e.getKey()), new java.math.BigDecimal(e.getValue().toString()));
        }
        Object tidObj = body.get("targetClassId");
        Long targetClassId = tidObj instanceof Number ? ((Number) tidObj).longValue() : null;
        return R.ok(shareService.confirmUpload(title, subject, userId, targetClassId, scores));
    }

    @GetMapping("/actions/library")
    public R<List<Map<String, Object>>> library() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error("未登录");
        return R.ok(shareService.library(userId));
    }
}
