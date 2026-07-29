package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.BehaviorTrackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user-events")
public class UserEventController {

    @Autowired private BehaviorTrackService behaviorTrackService;

    /** 记录用户行为事件（异步写入，不阻塞主请求） */
    @PostMapping("/track")
    public R<?> track(@RequestBody Map<String, Object> body) {
        // 必须在同步线程中捕获 userId/roleName，因为 @Async 线程不会继承 SecurityContext
        Long userId = SecurityUtils.getCurrentUserId();
        String roleName = SecurityUtils.getCurrentRole();
        String eventType = (String) body.get("eventType");
        Object eventData = body.getOrDefault("eventData", Map.of());
        behaviorTrackService.trackAsync(userId, roleName, eventType, eventData);
        return R.ok(null);
    }

    /** 查询事件统计（管理员专用） */
    @GetMapping("/stats")
    public R<Map<String, Object>> stats(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false, defaultValue = "30") int days) {

        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        return R.ok(behaviorTrackService.stats(eventType, days));
    }
}
