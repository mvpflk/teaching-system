package com.school.teaching.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.common.R;
import com.school.teaching.entity.Feedback;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public R<?> submit(@RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        feedbackService.submit(userId, body);
        return R.ok("感谢反馈，我们会尽快处理");
    }

    @GetMapping
    public R<?> list(@RequestParam(defaultValue = "1") int page,
                     @RequestParam(defaultValue = "20") int size,
                     @RequestParam(required = false) String status) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        Page<Feedback> pg = feedbackService.list(page, size, status);
        return R.ok(Map.of("records", pg.getRecords(), "total", pg.getTotal()));
    }

    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        feedbackService.update(id, body, SecurityUtils.getCurrentUserId());
        return R.ok("已更新");
    }
}
