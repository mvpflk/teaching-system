package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.entity.InspectionIssue;
import com.school.teaching.entity.RectificationNotice;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.InspectionIssueService;
import com.school.teaching.service.RectificationNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teacher/inspection")
@PreAuthorize("hasAnyRole('TEACHER','HEAD_TEACHER','ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class TeacherInspectionController {

    private final InspectionIssueService issueService;
    private final RectificationNoticeService noticeService;

    @GetMapping("/issues")
    public R<?> getMyIssues(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return R.ok(issueService.getMyAssignedIssues(teacherId, status, page, size));
    }

    @PostMapping("/issues/{id}/actions/start")
    public R<InspectionIssue> startProgress(@PathVariable Long id) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return R.ok(issueService.startProgress(id, teacherId));
    }

    @PostMapping("/issues/{id}/actions/resolve")
    public R<InspectionIssue> resolveIssue(
            @PathVariable Long id,
            @RequestParam(required = false) String resolveComment) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return R.ok(issueService.resolveIssue(id, teacherId, resolveComment));
    }

    @GetMapping("/notices")
    public R<?> getMyNotices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return R.ok(noticeService.getPage(teacherId, "TEACHER", page, size));
    }

    @PostMapping("/notices/{id}/actions/acknowledge")
    public R<RectificationNotice> acknowledgeNotice(@PathVariable Long id) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return R.ok(noticeService.acknowledge(id, teacherId));
    }

    @PostMapping("/notices/{id}/actions/comply")
    public R<RectificationNotice> complyNotice(@PathVariable Long id) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return R.ok(noticeService.comply(id, teacherId));
    }
}
