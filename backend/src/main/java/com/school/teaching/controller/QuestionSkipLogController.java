package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.entity.QuestionSkipLog;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.QuestionSkipLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/question-skip-log")
@RequiredArgsConstructor
public class QuestionSkipLogController {
    private final QuestionSkipLogService skipLogService;

    @PostMapping
    public R<?> logSkip(@RequestBody QuestionSkipLog log) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        skipLogService.logSkip(userId, log);
        return R.ok(null, "已记录");
    }
}
