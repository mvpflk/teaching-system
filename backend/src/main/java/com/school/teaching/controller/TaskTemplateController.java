package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.TaskTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/task-templates")
public class TaskTemplateController {

    @Autowired private TaskTemplateService templateService;

    private Long uid() { return SecurityUtils.getCurrentUserId(); }

    /** 模板列表 */
    @GetMapping
    public R<?> list(@RequestParam(defaultValue = "ALL") String scope,
                     @RequestParam(required = false) String subject,
                     @RequestParam(required = false) String taskType,
                     @RequestParam(defaultValue = "ALL") String category) {
        return R.ok(templateService.listTemplates(uid(), scope, subject, taskType, category));
    }

    /** 模板详情 */
    @GetMapping("/{id}")
    public R<?> detail(@PathVariable Long id) {
        return R.ok(templateService.getById(id));
    }

    /** 新建空白模板 */
    @PostMapping
    public R<?> create(@RequestBody com.school.teaching.entity.TaskTemplate template) {
        return R.ok(templateService.createTemplate(template, uid()), "模板已创建");
    }

    /** 从任务保存为模板 */
    @PostMapping("/actions/save-from-task")
    public R<?> saveFromTask(@RequestBody Map<String, Object> body) {
        Long taskId = Long.valueOf(body.get("taskId").toString());
        String name = (String) body.get("name");
        String scope = (String) body.getOrDefault("scope", "PRIVATE");
        String category = (String) body.getOrDefault("category", "TEACHING");
        return R.ok(templateService.saveFromTask(taskId, name, scope, category, uid()));
    }

    /** 从模板创建任务 */
    @PostMapping("/{id}/actions/create-task")
    public R<?> createTask(@PathVariable Long id) {
        return R.ok(templateService.createTaskFromTemplate(id, uid()), "任务已创建");
    }

    /** 修改共享范围 */
    @PutMapping("/{id}/actions/scope")
    public R<?> updateScope(@PathVariable Long id, @RequestBody Map<String, String> body) {
        templateService.updateScope(id, body.get("scope"), uid());
        return R.ok();
    }

    /** 删除模板 */
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        templateService.deleteTemplate(id, uid());
        return R.ok();
    }
}
