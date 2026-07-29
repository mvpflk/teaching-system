package com.school.teaching.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.teaching.common.R;
import com.school.teaching.entity.ParentFeedbackForm;
import com.school.teaching.entity.ParentFeedbackResponse;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.ParentFeedbackFormService;
import com.school.teaching.service.ParentFeedbackResponseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ParentFeedbackController {

    @Autowired private ParentFeedbackFormService formService;
    @Autowired private ParentFeedbackResponseService responseService;
    @Autowired private com.school.teaching.service.SystemService systemService;

    // ──────────────── 巡视端 ────────────────

    @PostMapping("/inspector/feedback/forms")
    @PreAuthorize("hasAnyRole('INSPECTOR','ADMIN','SUPER_ADMIN')")
    public R<ParentFeedbackForm> createForm(@RequestBody ParentFeedbackForm form) {
        if (!systemService.getBooleanConfig("feature.parent_feedback_enabled", false)) return R.error(410, "功能已关闭");
        return R.ok(formService.create(form));
    }

    @GetMapping("/inspector/feedback/forms")
    @PreAuthorize("hasAnyRole('INSPECTOR','ADMIN','SUPER_ADMIN')")
    public R<IPage<ParentFeedbackForm>> listForms(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String period,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (!systemService.getBooleanConfig("feature.parent_feedback_enabled", false)) return R.error(410, "功能已关闭");
        return R.ok(formService.getPage(classId, period, page, size));
    }

    @PutMapping("/inspector/feedback/forms/{id}")
    @PreAuthorize("hasAnyRole('INSPECTOR','ADMIN','SUPER_ADMIN')")
    public R<ParentFeedbackForm> updateForm(@PathVariable Long id, @RequestBody ParentFeedbackForm form) {
        if (!systemService.getBooleanConfig("feature.parent_feedback_enabled", false)) return R.error(410, "功能已关闭");
        return R.ok(formService.update(id, form));
    }

    @DeleteMapping("/inspector/feedback/forms/{id}")
    @PreAuthorize("hasAnyRole('INSPECTOR','ADMIN','SUPER_ADMIN')")
    public R<Void> deleteForm(@PathVariable Long id) {
        if (!systemService.getBooleanConfig("feature.parent_feedback_enabled", false)) return R.error(410, "功能已关闭");
        formService.delete(id);
        return R.ok(null);
    }

    @PostMapping("/inspector/feedback/forms/{id}/send")
    @PreAuthorize("hasAnyRole('INSPECTOR','ADMIN','SUPER_ADMIN')")
    public R<Void> sendForm(@PathVariable Long id) {
        if (!systemService.getBooleanConfig("feature.parent_feedback_enabled", false)) return R.error(410, "功能已关闭");
        formService.send(id);
        return R.ok(null);
    }

    @PostMapping("/inspector/feedback/forms/{id}/close")
    @PreAuthorize("hasAnyRole('INSPECTOR','ADMIN','SUPER_ADMIN')")
    public R<Void> closeForm(@PathVariable Long id) {
        if (!systemService.getBooleanConfig("feature.parent_feedback_enabled", false)) return R.error(410, "功能已关闭");
        formService.close(id);
        return R.ok(null);
    }

    @GetMapping("/inspector/feedback/forms/{id}/responses")
    @PreAuthorize("hasAnyRole('INSPECTOR','ADMIN','SUPER_ADMIN')")
    public R<List<ParentFeedbackResponse>> getResponses(@PathVariable Long id) {
        if (!systemService.getBooleanConfig("feature.parent_feedback_enabled", false)) return R.error(410, "功能已关闭");
        return R.ok(responseService.getByForm(id));
    }

    @GetMapping("/inspector/feedback/forms/{id}/stats")
    @PreAuthorize("hasAnyRole('INSPECTOR','ADMIN','SUPER_ADMIN')")
    public R<Map<String, Object>> getStats(@PathVariable Long id) {
        if (!systemService.getBooleanConfig("feature.parent_feedback_enabled", false)) return R.error(410, "功能已关闭");
        return R.ok(formService.getStats(id));
    }

    @PostMapping("/inspector/feedback/forms/{id}/generate-summary")
    @PreAuthorize("hasAnyRole('INSPECTOR','ADMIN','SUPER_ADMIN')")
    public R<Map<String, Object>> generateSummary(@PathVariable Long id) {
        if (!systemService.getBooleanConfig("feature.parent_feedback_enabled", false)) return R.error(410, "功能已关闭");
        return R.ok(formService.generateSummary(id));
    }

    // ──────────────── 家长端 ────────────────

    @GetMapping("/parent/feedback/pending")
    public R<List<Map<String, Object>>> getPendingForms() {
        if (!systemService.getBooleanConfig("feature.parent_feedback_enabled", false)) return R.error(410, "功能已关闭");
        if (!SecurityUtils.isParent() && !SecurityUtils.isAdmin())
            return R.error(403, "仅家长可访问");
        Long parentId = SecurityUtils.getCurrentUserId();
        return R.ok(responseService.getPendingForParent(parentId));
    }

    @PostMapping("/parent/feedback/respond")
    public R<ParentFeedbackResponse> submitResponse(@RequestBody ParentFeedbackResponse response) {
        if (!systemService.getBooleanConfig("feature.parent_feedback_enabled", false)) return R.error(410, "功能已关闭");
        if (!SecurityUtils.isParent() && !SecurityUtils.isAdmin())
            return R.error(403, "仅家长可访问");
        Long parentId = SecurityUtils.getCurrentUserId();
        response.setParentId(parentId);
        return R.ok(responseService.submit(response));
    }
}
