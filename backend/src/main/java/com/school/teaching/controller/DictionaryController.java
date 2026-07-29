package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.DictionaryService;
import com.school.teaching.service.RubricService;
import com.school.teaching.service.TeacherService;
import com.school.teaching.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dictionary")
public class DictionaryController {

    @Autowired private DictionaryService dictionaryService;
    @Autowired private UserProfileService userProfileService;
    @Autowired private TeacherService teacherService;
    @Autowired private RubricService rubricService;

    @GetMapping("/subjects")
    public R<?> subjects() { return R.ok(dictionaryService.getSubjectTree()); }

    @GetMapping("/grades")
    public R<?> grades(@RequestParam(required = false) Long stageId) { return R.ok(dictionaryService.getGradeList(stageId)); }

    @GetMapping("/wuyu-tags")
    public R<?> wuyuTags() { return R.ok(dictionaryService.getWuyuTags()); }

    @GetMapping("/question-types")
    public R<?> questionTypes() { return R.ok(dictionaryService.getQuestionTypes()); }

    // 当前教师任教科目（管理员/超管/巡视员返回全部学科，教师返回所授学科）
    @GetMapping("/actions/my-subjects")
    public R<?> mySubjects() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error("未登录");
        String role = SecurityUtils.getCurrentRole();
        if ("SUPER_ADMIN".equals(role) || "ADMIN".equals(role) || "INSPECTOR".equals(role) || "HEAD_TEACHER".equals(role)) {
            return R.ok(dictionaryService.getActiveSubjects());
        }
        return R.ok(teacherService.getTeachingSubjectsWithIds(userId));
    }

    // 当前教师任教配置（含学科→班级映射，用于组卷赋分联动）
    @GetMapping("/actions/my-teaching-assignments")
    public R<?> myTeachingAssignments() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error("未登录");
        return R.ok(teacherService.getTeachingAssignments(userId));
    }

    @GetMapping("/terms")
    public R<?> terms(@RequestParam(required = false) Long schoolId) { return R.ok(dictionaryService.getTermList(schoolId)); }

    @GetMapping("/rubrics")
    public R<?> rubrics(@RequestParam(required = false) Long schoolId) {
        Long sid = schoolId != null ? schoolId : SecurityUtils.getCurrentSchoolId();
        return R.ok(rubricService.listBySchool(sid));
    }

    // 用户档案快捷入口
    @GetMapping("/actions/my-profile")
    public R<Map<String, Object>> myProfile() { return R.ok(userProfileService.getCurrentProfile()); }
}
