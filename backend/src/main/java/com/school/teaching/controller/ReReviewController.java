package com.school.teaching.controller;

import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.annotation.AuditLog;
import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.security.StudentResolver;
import com.school.teaching.service.ReReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/re-reviews")
public class ReReviewController {

    @Autowired private ReReviewService reReviewService;
    @Autowired private StudentResolver studentResolver;

    @AuditLog(eventType = AuditEventType.OTHER, description = "申请复议")
    @PostMapping("/actions/request")
    public R<?> requestReReview(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isStudent()) return R.error(403, "无权限");
        Long studentId = studentResolver.resolveCurrentStudentId();
        if (studentId == null) return R.error(401, "未登录");
        Long submissionId = Long.valueOf(body.get("submissionId").toString());
        String reason = (String) body.get("reason");
        return R.ok(reReviewService.requestReReview(submissionId, studentId, reason));
    }

    @AuditLog(eventType = AuditEventType.OTHER, description = "处理复议")
    @PutMapping("/{requestId}/actions/resolve")
    public R<?> resolveReReview(@PathVariable Long requestId, @RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        String teacherComment = (String) body.get("teacherComment");
        return R.ok(reReviewService.resolveReReview(requestId, approved, teacherComment, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/actions/my-requests")
    public R<?> myRequests() {
        Long studentId = studentResolver.resolveCurrentStudentId();
        if (studentId == null) return R.error(401, "未登录");
        return R.ok(reReviewService.getStudentRequests(studentId));
    }
}
