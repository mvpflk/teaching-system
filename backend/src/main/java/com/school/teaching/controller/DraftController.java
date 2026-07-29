package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.security.StudentResolver;
import com.school.teaching.service.DraftService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/student/drafts")
public class DraftController {

    @Autowired private DraftService draftService;
    @Autowired private StudentResolver studentResolver;

    /** 保存草稿 */
    @PostMapping("/save")
    public R<String> save(@RequestBody Map<String, Object> body) {
        Long studentId = studentResolver.resolveCurrentStudentId();
        if (studentId == null) return R.error(401, "未登录");
        Long taskId = Long.valueOf(body.get("taskId").toString());
        String content = (String) body.get("content");
        draftService.save(studentId, taskId, content);
        return R.ok("已保存");
    }

    /** 加载草稿 */
    @GetMapping("/{taskId}")
    public R<Map<String, Object>> load(@PathVariable Long taskId) {
        Long studentId = studentResolver.resolveCurrentStudentId();
        if (studentId == null) return R.error(401, "未登录");
        String content = draftService.load(studentId, taskId);
        return R.ok(content != null ? Map.of("content", content) : Map.of());
    }

    /** 删除草稿（提交成功后调用） */
    @DeleteMapping("/{taskId}")
    public R<String> delete(@PathVariable Long taskId) {
        Long studentId = studentResolver.resolveCurrentStudentId();
        if (studentId == null) return R.error(401, "未登录");
        draftService.delete(studentId, taskId);
        return R.ok("已删除");
    }
}
