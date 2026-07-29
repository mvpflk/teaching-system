package com.school.teaching.controller;

import com.school.teaching.annotation.AuditLog;
import com.school.teaching.annotation.AuditEventType;

import com.school.teaching.common.R;
import com.school.teaching.entity.ShowcaseComment;
import com.school.teaching.entity.ShowcaseWork;
import com.school.teaching.entity.User;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.ShowcaseCommentService;
import com.school.teaching.service.ShowcaseWorkService;
import com.school.teaching.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/showcase")
public class ShowcaseWorkController {

    @Autowired private ShowcaseWorkService showcaseService;
    @Autowired private ShowcaseCommentService commentService;
    @Autowired private UserService userService;

    /** 推荐作品到展示墙（教师+管理员） */
    @PostMapping
    @AuditLog(eventType = AuditEventType.SHOWCASE_RECOMMEND, description = "推荐优秀作品")
    public R<ShowcaseWork> recommend(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师和管理员可推荐作品");
        return R.ok(showcaseService.recommendWork(body), "推荐成功");
    }

    /** 更新展示作品（推荐教师或管理员） */
    @PutMapping("/{id:\\d+}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "更新展示作品")
    public R<ShowcaseWork> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师和管理员可编辑");
        return R.ok(showcaseService.updateWork(id, body), "更新成功");
    }

    /** 下架展示作品（推荐教师或管理员） */
    @DeleteMapping("/{id:\\d+}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "删除展示作品")
    public R<String> delete(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师和管理员可下架");
        showcaseService.deleteWork(id);
        return R.ok("已下架");
    }

    /** 分页查询展示作品（学生按班级权限过滤，教师/管理员看全部） */
    @GetMapping({"/list", ""})
    public R<Map<String, Object>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "12") Integer pageSize,
                                        @RequestParam(required = false) String sourceType,
                                        @RequestParam(required = false) String subject,
                                        @RequestParam(required = false) Long classId,
                                        @RequestParam(required = false) String grade) {
        Map<String, Object> data = showcaseService.listWorks(pageNum, pageSize, sourceType, subject, classId, grade);
        return R.ok(data);
    }

    /** 获取单个作品详情 */
    @GetMapping("/{id:\\d+}")
    public R<ShowcaseWork> detail(@PathVariable Long id) {
        ShowcaseWork work = showcaseService.getWorkDetail(id);
        if (work == null) return R.notFound("作品不存在或已下架");
        return R.ok(work);
    }

    /** 教师查看自己推荐的作品 */
    @GetMapping("/actions/my-recommended")
    public R<List<ShowcaseWork>> myRecommended() {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师和管理员可查看");
        return R.ok(showcaseService.getMyRecommended());
    }

    /** 点赞 */
    @PostMapping("/{id}/like")
    public R<Map<String, Object>> toggleLike(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "请先登录");

        ShowcaseWork work = showcaseService.getWorkDetail(id);
        if (work == null) return R.error(404, "作品不存在");

        // 原子自增，避免并发丢失
        showcaseService.incrementLikeCount(id);

        Map<String, Object> result = new HashMap<>();
        result.put("liked", true);
        result.put("likeCount", (work.getLikeCount() == null ? 0 : work.getLikeCount()) + 1);
        return R.ok(result);
    }

    /** 本周之星：最近7天点赞数前3 */
    @GetMapping("/weekly-stars")
    public R<List<ShowcaseWork>> weeklyStars() {
        List<ShowcaseWork> list = showcaseService.getWeeklyStars();
        showcaseService.enrichShowcaseWorks(list);
        return R.ok(list);
    }

    // ========== 评论系统 ==========

    /** 获取作品已审核评论 */
    @GetMapping("/{id}/comments")
    public R<List<ShowcaseComment>> getComments(@PathVariable Long id) {
        List<ShowcaseComment> comments = commentService.getApprovedComments(id);
        if (!comments.isEmpty()) {
            Set<Long> uids = comments.stream().map(ShowcaseComment::getUserId).collect(Collectors.toSet());
            List<User> users = userService.getUsersByIds(uids);
            Map<Long, String> nameMap = users.stream().collect(Collectors.toMap(User::getId, u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));
            for (ShowcaseComment c : comments) {
                c.setUserName(nameMap.getOrDefault(c.getUserId(), "匿名"));
            }
        }
        return R.ok(comments);
    }

    /** 提交评论 */
    @PostMapping("/{id}/comments")
    public R<ShowcaseComment> addComment(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "请先登录");
        String content = body.get("content");
        if (content == null || content.trim().isEmpty()) return R.error(400, "评论不能为空");
        if (content.length() > 500) return R.error(400, "评论最多500字");

        ShowcaseComment comment = commentService.addComment(id, userId, content, SecurityUtils.isTeacherOrAdmin());
        return R.ok(comment);
    }

    /** 审核通过评论 */
    @PutMapping("/comments/{commentId}/approve")
    public R<?> approveComment(@PathVariable Long commentId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        commentService.approveComment(commentId, SecurityUtils.getCurrentUserId());
        return R.ok("审核通过");
    }

    /** 拒绝评论 */
    @PutMapping("/comments/{commentId}/reject")
    public R<?> rejectComment(@PathVariable Long commentId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        commentService.rejectComment(commentId, SecurityUtils.getCurrentUserId());
        return R.ok("已拒绝");
    }

    /** 待审核评论列表 */
    @GetMapping("/comments/pending")
    public R<List<ShowcaseComment>> pendingComments() {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        List<ShowcaseComment> list = commentService.getPendingComments();
        if (!list.isEmpty()) {
            Set<Long> uids = list.stream().map(ShowcaseComment::getUserId).collect(Collectors.toSet());
            Set<Long> wids = list.stream().map(ShowcaseComment::getWorkId).collect(Collectors.toSet());
            Map<Long, String> nameMap = userService.getUsersByIds(uids).stream()
                .collect(Collectors.toMap(User::getId,
                    u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));
            Map<Long, String> titleMap = showcaseService.getWorksByIds(wids).stream()
                .collect(Collectors.toMap(ShowcaseWork::getId, ShowcaseWork::getTitle));
            for (ShowcaseComment c : list) {
                c.setUserName(nameMap.getOrDefault(c.getUserId(), "匿名"));
                c.setWorkTitle(titleMap.get(c.getWorkId()));
            }
        }
        return R.ok(list);
    }
}
