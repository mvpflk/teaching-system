package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.TeacherActivityService;
import com.school.teaching.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/teacher/activity")
public class TeacherActivityController {

    @Autowired private TeacherActivityService activityService;
    @Autowired private TeacherService teacherService;

    @GetMapping("/me")
    public R<?> myActivities() {
        Long userId = SecurityUtils.getCurrentUserId();
        Long teacherId = teacherService.getTeacherIdByUserId(userId);
        if (teacherId == null) return R.error(403, "非教师用户");
        return R.ok(activityService.listByTeacher(teacherId, 50));
    }

    // ── P0: 教师渗透自报（对照班教师每日勾选"是否参考了AI诊断"）──

    /** 查询今日是否已勾选 */
    @GetMapping("/penetration-check/today")
    public R<Map<String, Object>> getTodayCheck() {
        Long userId = SecurityUtils.getCurrentUserId();
        Long teacherId = teacherService.getTeacherIdByUserId(userId);
        if (teacherId == null) return R.error(403, "非教师用户");
        Boolean checked = activityService.getTodayCheck(teacherId);
        return R.ok(Map.of("checked", checked != null, "referencedAi", checked != null && checked));
    }

    /** 提交今日渗透自报 */
    @PostMapping("/penetration-check")
    public R<?> submitCheck(@RequestBody Map<String, Boolean> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long teacherId = teacherService.getTeacherIdByUserId(userId);
        if (teacherId == null) return R.error(403, "非教师用户");
        if (activityService.hasCheckedToday(teacherId)) {
            return R.error(409, "今日已提交，无需重复勾选");
        }
        boolean referencedAi = Boolean.TRUE.equals(body.get("referencedAi"));
        activityService.log(teacherId, "PENETRATION_CHECK", referencedAi ? "yes" : "no", 0L);
        return R.ok(Map.of("recorded", true));
    }

    /** 查询渗透自报历史 */
    @GetMapping("/penetration-check/history")
    public R<?> getCheckHistory(@RequestParam(defaultValue = "30") int limit) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long teacherId = teacherService.getTeacherIdByUserId(userId);
        if (teacherId == null) return R.error(403, "非教师用户");
        return R.ok(activityService.listPenetrationChecks(teacherId, limit));
    }
}
