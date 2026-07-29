package com.school.teaching.controller;

import com.school.teaching.annotation.AuditLog;
import com.school.teaching.annotation.AuditEventType;

import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.security.StudentResolver;
import com.school.teaching.service.PeerReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/peer-reviews")
public class PeerReviewController {

    @Autowired private PeerReviewService peerReviewService;
    @Autowired private StudentResolver studentResolver;
    @Autowired private com.school.teaching.service.SystemService systemService;

    private void requireEnabled() {
        if (!systemService.getBooleanConfig("feature.re_review_enabled", false)) {
            throw new com.school.teaching.exception.BusinessException(410, "互评功能暂未开放，如需启用请联系管理员");
        }
    }

    /** 教师触发互评分配 */
    @PostMapping("/{taskId}/actions/assign")
    @AuditLog(eventType = AuditEventType.OTHER, description = "分配互评任务")
    public R<Map<String, Object>> assign(@PathVariable Long taskId) {
        requireEnabled();
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        int count = peerReviewService.assignReviews(taskId);
        return R.ok(Map.of("assigned", count));
    }

    /** 学生查看待评列表 */
    @GetMapping("/pending")
    public R<?> pending() {
        requireEnabled();
        Long studentId = studentResolver.resolveCurrentStudentId();
        if (studentId == null) return R.error(403, "学生未登录");
        return R.ok(peerReviewService.getPendingReviews(studentId));
    }

    /** 学生提交互评 */
    @PostMapping("/{reviewId}/actions/submit")
    @AuditLog(eventType = AuditEventType.OTHER, description = "提交互评")
    public R<?> submit(@PathVariable Long reviewId, @RequestBody Map<String, Object> body) {
        requireEnabled();
        Long studentId = studentResolver.resolveCurrentStudentId();
        if (studentId == null) return R.error(403, "未登录");
        return R.ok(peerReviewService.submitReview(reviewId, body, studentId));
    }

    /** 教师查看互评进度 */
    @GetMapping("/{taskId}/progress")
    public R<?> progress(@PathVariable Long taskId) {
        requireEnabled();
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        return R.ok(peerReviewService.getProgress(taskId));
    }

    /** 查看某提交的互评详情 */
    @GetMapping("/submissions/{submissionId}")
    public R<?> details(@PathVariable Long submissionId) {
        requireEnabled();
        if (!SecurityUtils.isTeacherOrAdmin()) {
            Long studentId = studentResolver.resolveCurrentStudentId();
            if (studentId == null) return R.error(403, "无权限");
            // 校验该提交属于当前学生
            if (!peerReviewService.isSubmissionOwner(submissionId, studentId))
                return R.error(403, "无权查看该提交");
        }
        return R.ok(peerReviewService.getReviewDetails(submissionId));
    }

    /** 教师触发分数融合 */
    @PostMapping("/{taskId}/actions/fuse-scores")
    @AuditLog(eventType = AuditEventType.OTHER, description = "融合互评分数")
    public R<?> fuseScores(@PathVariable Long taskId) {
        requireEnabled();
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        int count = peerReviewService.fuseScores(taskId);
        return R.ok(Map.of("updated", count));
    }

    /** 互评质量分析 */
    @GetMapping("/{taskId}/actions/quality")
    public R<?> quality(@PathVariable Long taskId) {
        requireEnabled();
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        return R.ok(peerReviewService.getQualityAnalysis(taskId));
    }

    /** 学生查看自己提交收到的互评评语 */
    @GetMapping("/submissions/{submissionId}/peer-comments")
    public R<?> peerComments(@PathVariable Long submissionId) {
        requireEnabled();
        Long studentId = studentResolver.resolveCurrentStudentId();
        if (studentId == null) return R.error(403, "学生未登录");
        return R.ok(peerReviewService.getPeerCommentsForStudent(submissionId, studentId));
    }
}