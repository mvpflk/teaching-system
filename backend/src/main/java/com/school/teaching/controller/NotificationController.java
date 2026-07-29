package com.school.teaching.controller;

import com.school.teaching.annotation.AuditLog;
import com.school.teaching.annotation.AuditEventType;

import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Autowired private NotificationService notificationService;

    @GetMapping("/list")
    public R<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        return R.ok(notificationService.adminListNotifications(userId, page, pageSize));
    }

    @AuditLog(eventType = AuditEventType.OTHER, description = "创建通知/公告")
    @PostMapping("/actions/create")
    public R<String> create(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        String scope = (String) body.getOrDefault("scope", "ALL");
        String type = (String) body.getOrDefault("type", "ANNOUNCEMENT");
        if (title == null || title.isBlank() || content == null || content.isBlank())
            return R.error("标题和内容不能为空");
        if ("CLASS".equals(scope)) {
            Long classId = body.get("targetClassId") != null
                ? Long.valueOf(body.get("targetClassId").toString()) : null;
            if (classId != null) notificationService.notifyClassStudents(classId, type, title, content, null);
        } else {
            // 全校通知仅限管理员
            if (!SecurityUtils.isAdmin()) return R.error(403, "全校通知仅限管理员发送");
            notificationService.notifyAllStudents(type, title, content);
        }
        return R.ok("已发布");
    }

    @PutMapping("/{id}/actions/read")
    @AuditLog(eventType = AuditEventType.OTHER, description = "标记通知已读")
    public R<String> markRead(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        notificationService.markAsRead(id, userId);
        return R.ok("已标记为已读");
    }

    @GetMapping("/unread-count")
    public R<Map<String, Object>> unreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        long count = notificationService.getUnreadCount(userId);
        return R.ok(Map.of("count", count));
    }

    @PutMapping("/actions/read-all")
    @AuditLog(eventType = AuditEventType.OTHER, description = "全部标记已读")
    public R<String> markAllRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        notificationService.markAllAsRead(userId);
        return R.ok("已全部标记为已读");
    }
}
