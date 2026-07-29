package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.entity.SimulationTask;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.security.StudentResolver;
import com.school.teaching.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/simulation")
public class SimulationController {

    @org.springframework.beans.factory.annotation.Autowired
    private SimulationService simulationService;
    @org.springframework.beans.factory.annotation.Autowired
    private StudentResolver studentResolver;

    @GetMapping("/ping")
    public R<String> ping() {
        if (simulationService == null) return R.error(500, "simulationService is NULL");
        try { return R.ok("pong:" + simulationService.getClass().getSimpleName()); }
        catch (Exception e) { return R.error(500, e.toString()); }
    }

    @GetMapping("/tasks/list")
    public R<?> listTasks(@RequestParam(required = false) String category) {
        return R.ok(simulationService.listTasks(category));
    }

    @GetMapping("/training-hub")
    public R<?> getTrainingHub() {
        return R.ok(simulationService.getTrainingHub());
    }

    @GetMapping("/tasks/{id}/definition")
    public R<Map<String, Object>> getTaskDefinition(@PathVariable Long id) {
        return R.ok(simulationService.getTaskDefinition(id));
    }

    /** 旧仿真数据兼容：将老版 SimulationTask 包装为统一步骤格式 */
    @GetMapping("/tasks/{simTaskId}/legacy")
    public R<?> getLegacySimTask(@PathVariable Long simTaskId) {
        SimulationTask st = simulationService.getById(simTaskId);
        if (st == null) return R.error(404, "仿真任务不存在");

        Map<String, Object> step = new LinkedHashMap<>();
        step.put("type", "sim");
        step.put("title", "仿真操作");
        step.put("config", Map.of(
            "simType", st.getCategory() != null ? st.getCategory() : "win7",
            "taskJson", st.getTaskJson() != null ? st.getTaskJson() : "{}",
            "initialVfs", st.getInitialVfs() != null ? st.getInitialVfs() : "{}",
            "timeLimit", st.getTimeLimit() != null ? st.getTimeLimit() : 300
        ));
        step.put("score", Map.of("method", "auto", "engine", "simulation", "max", 100));

        return R.ok(List.of(step));
    }

    @PostMapping("/tasks")
    public R<Map<String, Object>> createTask(@RequestBody Map<String, Object> request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        return R.ok(simulationService.createTask(request, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/tasks/{id}")
    public R<Map<String, Object>> updateTask(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        return R.ok(simulationService.updateTask(id, request, SecurityUtils.getCurrentUserId()));
    }

    @DeleteMapping("/tasks/{id}")
    public R<?> deleteTask(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        simulationService.deleteTask(id, SecurityUtils.getCurrentUserId());
        return R.ok("已删除");
    }

    @PostMapping("/progress")
    public R<?> reportProgress(@RequestBody Map<String, Object> body) {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        simulationService.reportProgress(body, sid);
        return R.ok("已上报");
    }

    /** 开始考试 — 创建 submission 并返回 submissionId */
    @PostMapping("/tasks/{id}/start-exam")
    public R<Map<String, Object>> startExam(@PathVariable Long id) {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) return R.error(404, "未找到学生信息");
        return R.ok(simulationService.startExam(id, sid));
    }

    @GetMapping("/recordings/{submissionId}")
    public R<Map<String, Object>> getRecording(@PathVariable Long submissionId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        return R.ok(simulationService.getRecording(submissionId));
    }

    @PostMapping("/recordings/{id}/notes")
    public R<?> addNotes(@PathVariable Long id, @RequestBody Map<String, String> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        simulationService.addNotes(id, body.get("notes"));
        return R.ok("已保存");
    }
}
