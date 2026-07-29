package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.PaperImportService;
import com.school.teaching.service.ExamPaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/paper-import")
public class PaperImportController {

    @Autowired private PaperImportService paperImportService;
    @Autowired private ExamPaperService examPaperService;

    /** 上传文件，解析题目（不入库） */
    @PostMapping("/actions/parse")
    public R<Map<String, Object>> parse(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String subject) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师/管理员可操作");
        try {
            byte[] fileBytes = file.getBytes();
            Map<String, Object> result = paperImportService.parse(
                    fileBytes, file.getOriginalFilename(), title, subject);
            return R.ok(result);
        } catch (Exception e) {
            return R.error(400, "试卷解析失败: " + e.getMessage());
        }
    }

    /** 接收解析结果+赋分+配置，创建任务+题目+试卷库 */
    @PostMapping("/actions/create")
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师/管理员可操作");
        try {
            Map<String, Object> result = paperImportService.create(request);
            return R.ok(result, "试卷导入成功");
        } catch (Exception e) {
            return R.error(400, "创建失败: " + e.getMessage());
        }
    }

    // ── 试卷库 ──

    @GetMapping("/library")
    public R<?> library(@RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "12") int pageSize,
                        @RequestParam(required = false) String subject) {
        if (SecurityUtils.getCurrentUserId() == null) return R.error(401, "请先登录");
        return R.ok(examPaperService.pageByCreator(page, pageSize, subject));
    }

    @DeleteMapping("/library/{id}")
    public R<?> deletePaper(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        examPaperService.softDelete(id);
        return R.ok("已删除");
    }

    @PostMapping("/library/{id}/actions/create-task")
    public R<Map<String, Object>> createFromLibrary(@PathVariable Long id,
                                                     @RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        Map<String, Object> result = examPaperService.createTaskFromPaper(id, body);
        return R.ok(result, "任务已创建");
    }
}
