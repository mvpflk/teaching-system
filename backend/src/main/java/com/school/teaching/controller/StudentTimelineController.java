package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.entity.Student;
import com.school.teaching.entity.StudentTimeline;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.StudentService;
import com.school.teaching.service.StudentTimelineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/student")
public class StudentTimelineController {

    @Autowired private StudentTimelineService timelineService;
    @Autowired private StudentService studentService;

    @GetMapping("/timeline")
    public R<List<StudentTimeline>> myTimeline() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        Student s = studentService.getStudentByUserId(userId);
        if (s == null) return R.error(404, "未绑定学生");
        return R.ok(timelineService.getByStudentId(s.getId()));
    }
}
