package com.school.teaching.controller;

import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.annotation.AuditLog;
import com.school.teaching.common.R;
import com.school.teaching.entity.*;

import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bbs")
public class BbsController {

    @Autowired private BbsService bbsService;
    @Autowired private UserService userService;
    @Autowired private TeacherService teacherService;
    @Autowired private StudentTimelineService studentTimelineService;

    // ==================== 版块 ====================

    @GetMapping("/categories")
    public R<List<BbsCategory>> getCategories() {
        return R.ok(bbsService.getCategories());
    }

    // ==================== 热帖 & 活跃用户 ====================

    @GetMapping("/hot-posts")
    public R<List<Map<String, Object>>> getHotPosts(@RequestParam(defaultValue = "5") int limit) {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(bbsService.getHotPosts(limit, userId));
    }

    @GetMapping("/active-users")
    public R<List<Map<String, Object>>> getActiveUsers(@RequestParam(defaultValue = "5") int limit) {
        return R.ok(bbsService.getActiveUsers(limit));
    }

    // ==================== 帖子 ====================

    @GetMapping("/posts")
    public R<Map<String, Object>> getPosts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) Boolean highlightOnly) {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(bbsService.getPosts(categoryId, keyword, page, pageSize, sort, highlightOnly, userId));
    }

    @GetMapping("/posts/{id}")
    public R<Map<String, Object>> getPostDetail(@PathVariable Long id) {
        bbsService.incrementViewCount(id);
        Long userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> detail = bbsService.getPostDetail(id, userId);
        if (detail == null) return R.notFound("帖子不存在");
        return R.ok(detail);
    }

    @AuditLog(eventType = AuditEventType.BBS_POST, description = "发帖")
    @PostMapping("/posts")
    public R<BbsPost> createPost(@RequestBody BbsPost post) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error("未登录");
        if (bbsService.isMuted(userId)) return R.error("您已被禁言，无法发帖");
        post.setAuthorId(userId);
        return R.ok(bbsService.createPost(post), "发帖成功");
    }

    @AuditLog(eventType = AuditEventType.BBS_POST, description = "编辑帖子")
    @PutMapping("/posts/{id}")
    public R<BbsPost> updatePost(@PathVariable Long id, @RequestBody BbsPost post) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error("未登录");
        post.setId(id);
        post.setAuthorId(userId);
        return R.ok(bbsService.updatePost(post), "编辑成功");
    }

    @AuditLog(eventType = AuditEventType.BBS_POST, description = "删除帖子")
    @DeleteMapping("/posts/{id}")
    public R<String> deletePost(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentRole();
        if (userId == null) return R.error("未登录");
        bbsService.deletePost(id, userId, role);
        return R.ok("删除成功");
    }

    /** 置顶/取消置顶（教师） */
    @AuditLog(eventType = AuditEventType.OTHER, description = "置顶/取消置顶")
    @PostMapping("/posts/{id}/actions/sticky")
    public R<String> toggleSticky(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        bbsService.toggleSticky(id);
        return R.ok("操作成功");
    }

    /** 使用置顶券置顶帖子（学生作者） */
    @AuditLog(eventType = AuditEventType.OTHER, description = "置顶券置顶")
    @PostMapping("/posts/{id}/actions/sticky-coupon")
    public R<String> toggleStickyWithCoupon(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error("未登录");
        bbsService.useStickyCoupon(id, userId);
        return R.ok("已使用置顶券，帖子已置顶");
    }

    /** 加精/取消加精（教师） */
    @AuditLog(eventType = AuditEventType.OTHER, description = "加精/取消加精")
    @PostMapping("/posts/{id}/actions/highlight")
    public R<String> toggleHighlight(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        bbsService.toggleHighlight(id);
        return R.ok("操作成功");
    }

    // ==================== 回复 ====================

    @GetMapping("/posts/{id}/replies")
    public R<Map<String, Object>> getReplies(@PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(bbsService.getReplies(id, userId, page, pageSize));
    }

    @PostMapping("/posts/{id}/replies")
    @AuditLog(eventType = AuditEventType.BBS_POST, description = "回复帖子")
    public R<Map<String, Object>> createReply(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error("未登录");
        if (bbsService.isMuted(userId)) return R.error("您已被禁言，无法回复");
        String content = (String) body.get("content");
        if (content == null || content.trim().isEmpty()) return R.error("回复内容不能为空");
        Number parentIdNum = (Number) body.get("parentId");
        Long parentId = parentIdNum != null ? parentIdNum.longValue() : null;
        User user = userService.getUserById(userId);
        String userName = user != null ? user.getRealName() : "未知";
        return R.ok(bbsService.createReply(id, parentId, content, userId, userName), "回复成功");
    }

    @DeleteMapping("/replies/{replyId}")
    @AuditLog(eventType = AuditEventType.BBS_POST, description = "删除回复")
    public R<String> deleteReply(@PathVariable Long replyId) {
        Long userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentRole();
        if (userId == null) return R.error("未登录");
        bbsService.deleteReply(replyId, userId, role);
        return R.ok("已删除");
    }

    // ==================== 点赞 ====================

    @AuditLog(eventType = AuditEventType.OTHER, description = "点赞/取消点赞")
    @PostMapping("/actions/toggle-like")
    public R<Map<String, Object>> toggleLike(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error("未登录");
        Long targetId = Long.valueOf(body.get("targetId").toString());
        String targetType = (String) body.get("targetType");
        return R.ok(bbsService.toggleLike(userId, targetId, targetType));
    }

    // ==================== 收藏 ====================

    @AuditLog(eventType = AuditEventType.OTHER, description = "收藏/取消收藏")
    @PostMapping("/actions/toggle-bookmark")
    public R<Map<String, Object>> toggleBookmark(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error("未登录");
        Long postId = Long.valueOf(body.get("postId").toString());
        return R.ok(bbsService.toggleBookmark(userId, postId));
    }

    // ==================== 我的 ====================

    @GetMapping("/actions/my-posts")
    public R<List<Map<String, Object>>> getMyPosts() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error("未登录");
        return R.ok(bbsService.getMyPosts(userId));
    }

    @GetMapping("/actions/my-bookmarks")
    public R<List<Map<String, Object>>> getMyBookmarks() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error("未登录");
        return R.ok(bbsService.getMyBookmarks(userId));
    }

    @GetMapping("/actions/my-replies")
    public R<List<Map<String, Object>>> getMyReplies() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error("未登录");
        return R.ok(bbsService.getMyReplies(userId));
    }

    // ==================== 禁言（教师） ====================

    @AuditLog(eventType = AuditEventType.OTHER, description = "禁言用户")
    @PostMapping("/actions/mute")
    public R<String> muteUser(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error("权限不足");
        Long targetUserId = Long.valueOf(body.get("userId").toString());
        Long operatorId = SecurityUtils.getCurrentUserId();
        String reason = (String) body.get("reason");
        Integer durationDays = body.get("durationDays") != null ? ((Number) body.get("durationDays")).intValue() : null;
        bbsService.muteUser(targetUserId, operatorId, reason, durationDays);
        return R.ok("已禁言");
    }

    @AuditLog(eventType = AuditEventType.OTHER, description = "解除禁言")
    @PostMapping("/actions/unmute")
    public R<String> unmuteUser(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error("权限不足");
        Long targetUserId = Long.valueOf(body.get("userId").toString());
        bbsService.unmuteUser(targetUserId);
        return R.ok("已解禁");
    }

    // ==================== 德育行为表扬 ====================

    @PostMapping("/actions/post/moral")
    @AuditLog(eventType = AuditEventType.MORAL_PRAISE, description = "德育表扬")
    public R<Map<String, Object>> createMoralPost(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师/管理员可发布德育表扬");

        if (body.get("content") == null || body.get("praisedStudentId") == null || body.get("classId") == null)
            return R.error(400, "content/praisedStudentId/classId 为必填项");

        Long classId = Long.valueOf(body.get("classId").toString());
        if (!SecurityUtils.isAdmin()) {
            if (!teacherService.getTeachingClassIds(userId).contains(classId))
                return R.error(403, "只能表扬自己任教班级的学生");
        }

        // 成长足迹：德育表扬
        try {
            Long praisedStudentId = Long.valueOf(body.get("praisedStudentId").toString());
            String reason = body.get("content") != null ? body.get("content").toString() : "德育表扬";
            if (reason.length() > 100) reason = reason.substring(0, 100);
            studentTimelineService.recordEvent(praisedStudentId, "moral", "获得德育表扬", reason, null);
        } catch (Exception ignored) {
            // 静默降级：成长足迹记录为辅助功能
        }

        return R.ok(bbsService.createMoralPost(body, userId, teacherService.getTeachingClassIds(userId)));
    }
}
