package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.dto.request.*;
import com.school.teaching.entity.PracticePlan;
import com.school.teaching.entity.PracticeRubric;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.security.StudentResolver;
import com.school.teaching.service.PracticePlanService;
import com.school.teaching.service.PracticeService;
import com.school.teaching.service.OfficeCompareService;
import com.school.teaching.service.PracticeTemplateService;
import com.school.teaching.service.TeacherService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;

import com.school.teaching.service.AiTaskStore;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/practice")
public class PracticeController {

    @Autowired private PracticeService practiceService;
    @Autowired private PracticePlanService planService;
    @Autowired private StudentResolver studentResolver;
    @Autowired private TeacherService teacherService;
    @Autowired private PracticeTemplateService templateService;
    @Autowired private OfficeCompareService officeCompareService;

    private Long requireStudent() {
        Long sid = studentResolver.resolveCurrentStudentId();
        if (sid == null) throw new BusinessException(404, "未找到学生信息");
        return sid;
    }

    private void requireTeacher() {
        if (!SecurityUtils.isTeacherOrAdmin()) throw new BusinessException(403, "仅教师可操作");
    }

    /** 新增步骤 */
    @PostMapping("/step")
    public R<Map<String, Object>> createStep(@RequestBody Map<String, Object> params) {
        return R.ok(practiceService.createStep(requireStudent(), params));
    }

    /** 更新步骤 */
    @PutMapping("/step/{stepId}")
    public R<String> updateStep(@PathVariable Long stepId, @RequestBody Map<String, Object> params) {
        practiceService.updateStep(stepId, requireStudent(), params);
        return R.ok("已更新");
    }

    /** 删除步骤 */
    @DeleteMapping("/step/{stepId}")
    public R<String> deleteStep(@PathVariable Long stepId) {
        practiceService.deleteStep(stepId, requireStudent());
        return R.ok("已删除");
    }

    /** 调整步骤顺序 */
    @PostMapping("/order/{taskId}")
    public R<String> reorderSteps(@PathVariable Long taskId, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> idsRaw = (List<Integer>) body.get("stepIds");
        if (idsRaw == null || idsRaw.isEmpty()) return R.error(400, "stepIds 不能为空");
        List<Long> stepIds = idsRaw.stream().map(Long::valueOf).toList();
        practiceService.reorderSteps(taskId, requireStudent(), stepIds);
        return R.ok("顺序已更新");
    }

    /** 获取步骤列表（学生调用时无需传参；教师调用时传 studentId） */
    @GetMapping("/task/{taskId}/steps")
    public R<List<Map<String, Object>>> listSteps(@PathVariable Long taskId,
                                                   @RequestParam(required = false) Long studentId) {
        Long sid;
        if (studentId != null && SecurityUtils.isTeacherOrAdmin()) {
            sid = studentId;
        } else {
            sid = requireStudent();
        }
        return R.ok(practiceService.listSteps(taskId, sid));
    }

    /** 提交实训 */
    @PostMapping("/submit/{taskId}")
    public R<Map<String, Object>> submit(@PathVariable Long taskId) {
        return R.ok(practiceService.submit(taskId, requireStudent()));
    }

    /** 撤回提交 */
    @PostMapping("/withdraw/{taskId}")
    public R<String> withdraw(@PathVariable Long taskId) {
        practiceService.withdraw(taskId, requireStudent());
        return R.ok("已撤回");
    }

    // ═══════════ 教师评分 ═══════════

    /** 教师评分 */
    @PostMapping("/actions/grade")
    public R<Map<String, Object>> grade(@Valid @RequestBody PracticeGradeRequest request) {
        requireTeacher();
        String overallComment = request.getOverallComment() != null ? request.getOverallComment() : "";
        List<Map<String, Object>> stepGrades = request.getStepGrades() != null ? request.getStepGrades() : List.of();
        return R.ok(practiceService.grade(request.getSubmissionId(), request.getOverallScore(),
                overallComment, stepGrades));
    }

    // ═══════════ 教师下载 ═══════════

    /** 生成实训下载ZIP（教师，异步） */
    @PostMapping("/task/{taskId}/download")
    public R<Map<String, Object>> startDownload(@PathVariable Long taskId,
                                                 @RequestParam Long classId,
                                                 @RequestParam(required = false) List<Long> studentIds) {
        requireTeacher();
        String tId = practiceService.startDownload(taskId, classId, studentIds);
        return R.ok(Map.of("taskId", tId, "status", "PENDING"));
    }

    /** 查询下载任务状态 */
    @GetMapping("/download/status/{taskId}")
    public R<Map<String, Object>> downloadStatus(@PathVariable String taskId) {
        requireTeacher();
        AiTaskStore.TaskEntry entry = practiceService.getDownloadStatus(taskId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskId", entry.taskId);
        data.put("status", entry.status);
        data.put("result", entry.result);
        data.put("error", entry.error);
        return R.ok(data);
    }

    /** 获取实训提交列表（教师用） */
    @GetMapping("/task/{taskId}/submissions")
    public R<List<Map<String, Object>>> getSubmissions(@PathVariable Long taskId) {
        requireTeacher();
        return R.ok(practiceService.getSubmissions(taskId));
    }

    // ═══════════ 实训方案 CRUD ═══════════

    @PostMapping("/plans")
    public R<PracticePlan> createPlan(@Valid @RequestBody PracticePlanRequest request) {
        requireTeacher();
        Long userId = SecurityUtils.getCurrentUserId();
        PracticePlan plan = new PracticePlan();
        plan.setTitle(request.getTitle());
        plan.setDescription(request.getDescription());
        plan.setPrerequisites(request.getPrerequisites());
        plan.setEnvironment(request.getEnvironment());
        plan.setSafetyNotes(request.getSafetyNotes());
        plan.setTroubleshooting(request.getTroubleshooting());
        plan.setTeamRoles(request.getTeamRoles());
        plan.setScoringModel(request.getScoringModel() != null ? request.getScoringModel() : "DUAL_DIMENSION");
        plan.setCreatedBy(userId);
        List<PracticeRubric> rubrics = parseRubrics(request.getRubrics());
        return R.ok(planService.create(plan, rubrics));
    }

    @GetMapping("/plans")
    public R<List<PracticePlan>> listPlans() {
        requireTeacher();
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(planService.listByCreator(userId));
    }

    @GetMapping("/plans/{id}")
    public R<PracticePlan> getPlan(@PathVariable Long id) {
        requireTeacher();
        return R.ok(planService.getById(id));
    }

    @PutMapping("/plans/{id}")
    public R<?> updatePlan(@PathVariable Long id, @RequestBody PracticePlanRequest request) {
        requireTeacher();
        PracticePlan plan = new PracticePlan();
        plan.setTitle(request.getTitle());
        plan.setDescription(request.getDescription());
        plan.setPrerequisites(request.getPrerequisites());
        plan.setEnvironment(request.getEnvironment());
        plan.setSafetyNotes(request.getSafetyNotes());
        plan.setTroubleshooting(request.getTroubleshooting());
        plan.setTeamRoles(request.getTeamRoles());
        plan.setScoringModel(request.getScoringModel() != null ? request.getScoringModel() : "DUAL_DIMENSION");
        List<PracticeRubric> rubrics = parseRubrics(request.getRubrics());
        return R.ok(planService.update(id, plan, rubrics));
    }

    @DeleteMapping("/plans/{id}")
    public R<?> deletePlan(@PathVariable Long id) {
        requireTeacher();
        planService.delete(id);
        return R.ok("已删除");
    }

    @GetMapping("/plans/{planId}/rubrics")
    public R<List<PracticeRubric>> getRubrics(@PathVariable Long planId) {
        requireTeacher();
        return R.ok(planService.getRubrics(planId));
    }

    /** AI 生成实训方案 */
    @PostMapping("/plans/ai-generate")
    public R<?> aiGeneratePlan(@Valid @RequestBody AiPlanGenerateRequest request) {
        requireTeacher();
        Long userId = SecurityUtils.getCurrentUserId();
        String stageHint = request.getStageHint() != null ? request.getStageHint() : "中职";
        return R.ok(planService.aiGeneratePlan(userId, request.getTitle(), request.getSubject(),
                request.getRequirements(), stageHint));
    }

    @PostMapping("/plans/{planId}/actions/publish")
    public R<?> publishPlan(@PathVariable Long planId, @RequestBody PlanPublishRequest request) {
        requireTeacher();
        Long teacherId = SecurityUtils.getCurrentUserId();
        List<Long> classIds = request.getClassIds() != null
            ? request.getClassIds().stream().map(Long::valueOf).toList() : List.of();
        return R.ok(planService.publishToTask(planId, classIds, teacherId));
    }

    // ═══════════ 模板库 ═══════════

    /** 模板库列表 */
    @GetMapping("/templates")
    public R<?> listTemplates(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String source) {
        requireTeacher();
        return R.ok(templateService.list(subject, category, source));
    }

    /** 模板详情 */
    @GetMapping("/templates/{id}")
    public R<?> getTemplate(@PathVariable Long id) {
        requireTeacher();
        return R.ok(templateService.getById(id));
    }

    /** 从模板创建方案草稿 */
    @PostMapping("/templates/{id}/apply")
    public R<?> applyTemplate(@PathVariable Long id) {
        requireTeacher();
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(templateService.applyTemplate(id, userId));
    }

    /** 将方案保存为模板 */
    @PostMapping("/plans/{planId}/save-as-template")
    public R<?> saveAsTemplate(@PathVariable Long planId) {
        requireTeacher();
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(templateService.saveAsTemplate(planId, userId));
    }

    /** 下载模板素材文件 */
    @GetMapping("/templates/download")
    public ResponseEntity<byte[]> downloadTemplateResource(
            @RequestParam String path) {
        try {
            String cleanPath = path;
            if (cleanPath.startsWith("/uploads/")) {
                cleanPath = cleanPath.substring(9);
            }
            
            Path uploadBasePath = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath();
            if (!java.nio.file.Files.exists(uploadBasePath)) {
                uploadBasePath = Paths.get("uploads").toAbsolutePath();
            }
            
            Path filePath = uploadBasePath.resolve(cleanPath).normalize();
            
            if (!filePath.startsWith(uploadBasePath)) {
                throw new BusinessException("非法路径访问");
            }
            if (!Files.exists(filePath)) {
                throw new BusinessException("文件不存在: " + filePath);
            }
            
            byte[] content = Files.readAllBytes(filePath);
            String filename = filePath.getFileName().toString();
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(content.length);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.add("Content-Disposition", "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename);
            
            return new ResponseEntity<>(content, headers, HttpStatus.OK);
        } catch (Exception e) {
            throw new BusinessException("下载失败: " + e.getMessage());
        }
    }

    // ═══════════ 导入 ═══════════

    @PostMapping("/plans/actions/import-zip")
    public R<?> importZip(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        requireTeacher();
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(planService.importFromZip(userId, file));
    }

    @PostMapping("/plans/actions/import-excel")
    public R<?> importExcel(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        requireTeacher();
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(planService.importFromExcel(userId, file));
    }

    @PostMapping("/plans/actions/batch-import")
    public R<?> batchImport(@RequestBody PlanBatchImportRequest request) {
        requireTeacher();
        Long userId = SecurityUtils.getCurrentUserId();
        String markdown = request.getMarkdown() != null ? request.getMarkdown() : "";
        if (markdown.isBlank()) return R.error(400, "内容为空");
        return R.ok(planService.batchImportFromMarkdown(userId, markdown));
    }

    @GetMapping("/plans/shared")
    public R<?> listShared() {
        requireTeacher();
        com.school.teaching.entity.Teacher teacher = teacherService.getTeacherEntityByUserId(SecurityUtils.getCurrentUserId());
        String subject = teacher != null ? teacher.getSubject() : null;
        return R.ok(planService.listSharedBySubject(subject));
    }

    // ═══════════ 自动评分 ═══════════

    private String toStr(Object v) {
        if (v instanceof String s) return s;
        if (v != null) { try { return om.writeValueAsString(v); } catch (Exception e) { return null; } }
        return null;
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();

    /** 文档比对（教师预览用） */
    @PostMapping("/grade/compare")
    public R<?> compareDocument(@Valid @RequestBody DocumentCompareRequest request) {
        requireTeacher();
        String fileType = request.getFileType() != null ? request.getFileType() : "xlsx";
        return R.ok(officeCompareService.compare(request.getTemplatePath(), request.getStudentFilePath(), fileType));
    }

    private List<PracticeRubric> parseRubrics(List<Map<String, Object>> rubricsRaw) {
        if (rubricsRaw == null) return List.of();
        return rubricsRaw.stream().map(r -> {
            PracticeRubric ru = new PracticeRubric();
            ru.setDimension((String) r.get("dimension"));
            ru.setDimensionLabel((String) r.get("dimensionLabel"));
            ru.setWeight(r.get("weight") != null ? BigDecimal.valueOf(((Number) r.get("weight")).doubleValue()) : BigDecimal.ZERO);
            ru.setCriteria((String) r.get("criteria"));
            ru.setSortOrder(r.get("sortOrder") != null ? ((Number) r.get("sortOrder")).intValue() : 0);
            return ru;
        }).toList();
    }
}
