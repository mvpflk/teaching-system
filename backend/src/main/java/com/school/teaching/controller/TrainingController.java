package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.common.practice.ExcelCheckpointEvaluator;
import com.school.teaching.common.practice.PptCheckpointEvaluator;
import com.school.teaching.common.practice.SqlSandboxEvaluator;
import com.school.teaching.entity.Task;
import com.school.teaching.entity.Teacher;
import com.school.teaching.entity.User;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 实训中心统一控制器
 * 权限规则：教师/管理员 → CRUD+评分+AI；学生 → 步骤执行+提交；所有人 → Hub只读
 */
@RestController
@RequestMapping("/training")
public class TrainingController {

    @Autowired
    private TrainingAiService trainingAiService;

    /** 学生步骤执行三端点委托此 Service（进度持久化/恢复/提交校验+自动评分的完整实现在 TrainingServiceImpl） */
    @Autowired
    private TrainingService trainingService;

    @Autowired
    private TaskCrudService taskCrudService;

    @Autowired
    private TaskPublishService taskPublishService;

    @Autowired
    private TaskQueryService taskQueryService;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TaskSubmissionMapper taskSubmissionMapper;

    @Autowired
    private com.school.teaching.security.StudentResolver studentResolver;

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private UserService userService;

    @Value("${teaching.upload-dir:/data/uploads}")
    private String baseUploadDir;

    private static final Set<String> XLSX_EXT = Set.of(".xlsx");
    private static final Logger log = LoggerFactory.getLogger(TrainingController.class);

    // ── 权限守卫 ──

    private void requireTeacher() {
        if (!SecurityUtils.isTeacherOrAdmin())
            throw new BusinessException(403, "仅教师可操作");
    }

    private void requireStudent() {
        if (!SecurityUtils.isStudent())
            throw new BusinessException(403, "仅学生可操作");
    }

    /** 获取当前教师 ID */
    private Long resolveTeacherId() {
        Long userId = SecurityUtils.getCurrentUserId();
        com.school.teaching.entity.Teacher t = teacherService.getTeacherEntityByUserId(userId);
        if (t == null) throw new BusinessException(404, "未找到教师档案");
        return t.getId();
    }

    /** 获取当前教师姓名 */
    private String resolveTeacherName() {
        Long userId = SecurityUtils.getCurrentUserId();
        com.school.teaching.entity.User u = userService.getUserById(userId);
        return u != null ? u.getRealName() : "";
    }

    // ── 任务 CRUD ──

    @GetMapping("/tasks")
    public R<?> listTasks() {
        requireTeacher();
        Long teacherId = resolveTeacherId();
        List<Task> tasks = taskMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Task>()
                .eq(Task::getTeacherId, teacherId)
                .eq(Task::getTaskType, "PRACTICE")
                .orderByDesc(Task::getCreatedAt));
        taskQueryService.enrichTasks(tasks, teacherId);
        List<Map<String, Object>> result = tasks.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("title", t.getTitle());
            m.put("subject", t.getSubject());
            m.put("description", t.getDescription());
            m.put("status", t.getStatus());
            m.put("createdAt", t.getCreatedAt());
            m.put("updatedAt", t.getUpdatedAt());
            m.put("teacherName", t.getTeacherName());
            // 从 taskConfig 解析步骤数
            List<Map<String, Object>> steps = parseStepsFromConfig(t.getTaskConfig());
            m.put("stepCount", steps.size());
            return m;
        }).collect(Collectors.toList());
        return R.ok(result);
    }

    @GetMapping("/tasks/{id}")
    public R<?> getTask(@PathVariable Long id) {
        requireTeacher();
        Task task = taskCrudService.getById(id);
        if (task == null) return R.notFound("任务不存在");
        List<Map<String, Object>> steps = parseStepsFromConfig(task.getTaskConfig());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", task.getId());
        result.put("title", task.getTitle());
        result.put("subject", task.getSubject());
        result.put("description", task.getDescription());
        result.put("status", task.getStatus());
        result.put("steps", steps);
        result.put("createdAt", task.getCreatedAt());
        return R.ok(result);
    }

    @PostMapping("/tasks")
    public R<?> createTask(@RequestBody Map<String, Object> body) {
        requireTeacher();
        Long teacherId = resolveTeacherId();

        String title = (String) body.getOrDefault("title", "");
        if (title.isBlank()) return R.error(400, "任务标题不能为空");

        // 解析目标班级（JSON数字类型不确定Integer/Long，统一走Number，规则#61）
        List<Long> targetIds = body.get("targetIds") instanceof List<?> rawList
            ? rawList.stream().filter(o -> o instanceof Number)
                .map(o -> ((Number) o).longValue()).distinct().toList()
            : List.of();
        if (targetIds.isEmpty()) return R.error(400, "请选择目标班级");

        // 解析步骤
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> steps = (List<Map<String, Object>>) body.get("steps");
        String stepsJson = null;
        if (steps != null && !steps.isEmpty()) {
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("steps", steps);
            try {
                stepsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(config);
            } catch (Exception e) {
                log.warn("序列化步骤配置失败", e);
            }
        }

        // 多班级：每个班级创建一个任务副本
        List<Map<String, Object>> results = new ArrayList<>();
        for (Long targetId : targetIds) {
            Task task = new Task();
            task.setTitle(title);
            task.setSubject((String) body.getOrDefault("subject", ""));
            task.setDescription((String) body.getOrDefault("description", ""));
            task.setTaskType("PRACTICE");
            task.setTeacherId(teacherId);
            task.setStatus("DRAFT");
            task.setSchoolId(1L);
            task.setStageId(4L);
            task.setTargetType("CLASS");
            task.setTargetId(targetId);
            task.setTeacherName(resolveTeacherName());
            if (stepsJson != null) task.setTaskConfig(stepsJson);

            Task created = taskCrudService.create(task);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", created.getId());
            r.put("title", created.getTitle());
            r.put("status", created.getStatus());
            r.put("targetId", targetId);
            results.add(r);
        }

        if (results.size() == 1) {
            return R.ok(results.get(0), "创建成功");
        }
        return R.ok(results, "已为 " + results.size() + " 个班级创建任务");
    }

    @PutMapping("/tasks/{id}")
    public R<?> updateTask(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requireTeacher();
        Task existing = taskCrudService.getById(id);
        if (existing == null) return R.notFound("任务不存在");

        String title = (String) body.getOrDefault("title", "");
        if (!title.isBlank()) existing.setTitle(title);
        if (body.containsKey("subject")) existing.setSubject((String) body.get("subject"));
        if (body.containsKey("description")) existing.setDescription((String) body.get("description"));

        // 更新步骤
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> steps = (List<Map<String, Object>>) body.get("steps");
        if (steps != null) {
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("steps", steps);
            try {
                existing.setTaskConfig(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(config));
            } catch (Exception e) {
                log.warn("序列化步骤配置失败", e);
            }
        }

        // 只允许 DRAFT 状态的任务被编辑
        if (!"DRAFT".equals(existing.getStatus())) {
            return R.error(400, "仅草稿状态可编辑");
        }

        taskCrudService.update(id, existing);
        return R.ok(Map.of("id", id, "status", existing.getStatus()));
    }

    @DeleteMapping("/tasks/{id}")
    public R<?> deleteTask(@PathVariable Long id) {
        requireTeacher();
        Task task = taskCrudService.getById(id);
        if (task == null) return R.notFound("任务不存在");
        taskCrudService.delete(id);
        return R.ok("已删除");
    }

    @PostMapping("/tasks/{id}/publish")
    public R<?> publishTask(@PathVariable Long id) {
        requireTeacher();
        Task task = taskCrudService.getById(id);
        if (task == null) return R.notFound("任务不存在");
        if (!"DRAFT".equals(task.getStatus())) return R.error(409, "仅草稿可发布");

        // 检查是否有步骤
        List<Map<String, Object>> steps = parseStepsFromConfig(task.getTaskConfig());
        if (steps.isEmpty()) return R.error(400, "请先添加步骤再发布");

        try {
            taskPublishService.publish(id);
            return R.ok(Map.of("id", id, "status", "PUBLISHED"));
        } catch (Exception e) {
            log.error("发布失败: taskId={}", id, e);
            return R.error(500, "发布失败: " + e.getMessage());
        }
    }

    // ── 任务库（使用通用任务）──

    @GetMapping("/library")
    public R<?> listLibrary(@RequestParam(required = false) String subject) {
        requireTeacher();
        List<Task> tasks = taskMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Task>()
                .eq(Task::getTaskType, "PRACTICE")
                .orderByDesc(Task::getCreatedAt)
                .last("LIMIT 50"));
        List<Map<String, Object>> result = tasks.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("title", t.getTitle());
            m.put("subject", t.getSubject());
            m.put("description", t.getDescription());
            return m;
        }).filter(m -> subject == null || subject.isBlank() || subject.equals(m.get("subject")))
         .collect(Collectors.toList());
        return R.ok(result);
    }

    @PostMapping("/library/{id}/copy")
    public R<?> copyFromLibrary(@PathVariable Long id) {
        requireTeacher();
        Task source = taskCrudService.getById(id);
        if (source == null) return R.notFound("模板不存在");
        Task copied = taskCrudService.copyTask(id, SecurityUtils.getCurrentUserId());
        List<Map<String, Object>> steps = parseStepsFromConfig(copied.getTaskConfig());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", copied.getId());
        result.put("title", copied.getTitle());
        result.put("subject", copied.getSubject());
        result.put("description", copied.getDescription());
        result.put("steps", steps);
        result.put("status", copied.getStatus());
        return R.ok(result);
    }

    // ── 学生步骤执行（委托 TrainingServiceImpl：进度持久化/恢复/提交校验+自动评分）──

    @GetMapping("/tasks/{taskId}/steps")
    public R<?> getStudentSteps(@PathVariable Long taskId) {
        requireStudent();
        // 返回 {task, steps(含_completed/_data服务端进度), submissionId, status}，与前端 StepPlayer 消费结构一致
        return R.ok(trainingService.getStudentSteps(taskId, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/tasks/{taskId}/steps/{stepIndex}")
    public R<?> saveStepProgress(@PathVariable Long taskId, @PathVariable int stepIndex,
                                  @RequestBody Map<String, Object> body) {
        requireStudent();
        trainingService.saveStepProgress(taskId, stepIndex, body, SecurityUtils.getCurrentUserId());
        return R.ok("已保存");
    }

    @PostMapping("/tasks/{taskId}/submit")
    public R<?> submitAllSteps(@PathVariable Long taskId) {
        requireStudent();
        // Service 版本：校验全部步骤完成 + autoGradeSteps 自动评分 + 更新同一提交行（无重复行风险），
        // 已提交/已评分抛 409（替代此前 Controller 直插 SUBMITTED 新行的简化实现）
        return R.ok(trainingService.submitAllSteps(taskId, SecurityUtils.getCurrentUserId()));
    }

    // ── 教师评分（基础实现）──

    @GetMapping("/tasks/{taskId}/submissions")
    public R<?> getSubmissions(@PathVariable Long taskId) {
        requireTeacher();
        Task task = taskCrudService.getById(taskId);
        if (task == null) return R.notFound("任务不存在");
        // 归属校验：与 finalizeGrade 口径一致，仅任务创建者可查看提交列表（防教师越权读）
        if (!resolveTeacherId().equals(task.getTeacherId()) && !SecurityUtils.isAdmin()) {
            return R.error(403, "无权查看该任务的提交记录");
        }
        List<com.school.teaching.entity.TaskSubmission> subs = taskSubmissionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.teaching.entity.TaskSubmission>()
                .eq(com.school.teaching.entity.TaskSubmission::getTaskId, taskId));
        List<Map<String, Object>> result = subs.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("studentId", s.getStudentId());
            m.put("status", s.getStatus());
            m.put("score", s.getScore());
            m.put("submittedAt", s.getSubmittedAt());
            m.put("gradeLevel", s.getGradeLevel());
            return m;
        }).collect(Collectors.toList());
        return R.ok(result);
    }

    @Transactional
    @PostMapping("/tasks/{taskId}/submissions/{submissionId}/finalize")
    public R<?> finalizeGrade(@PathVariable Long taskId, @PathVariable Long submissionId,
                               @RequestBody Map<String, Object> body) {
        requireTeacher();
        // 校验任务归属：仅任务创建者可评分
        Task task = taskCrudService.getById(taskId);
        if (task == null) return R.notFound("任务不存在");
        Long teacherId = resolveTeacherId();
        if (!teacherId.equals(task.getTeacherId())) {
            return R.error(403, "无权评分该任务");
        }

        com.school.teaching.entity.TaskSubmission sub = taskSubmissionMapper.selectById(submissionId);
        if (sub == null) return R.notFound("提交记录不存在");
        // 校验提交记录属于该任务
        if (!taskId.equals(sub.getTaskId())) {
            return R.error(400, "提交记录与任务不匹配");
        }

        Number scoreRaw = (Number) body.get("overallScore");
        if (scoreRaw != null) {
            sub.setScore(java.math.BigDecimal.valueOf(scoreRaw.doubleValue()));
        }
        sub.setStatus("GRADED");
        sub.setGradeType("TEACHER");
        sub.setGradedBy(SecurityUtils.getCurrentUserId());
        sub.setGradedAt(java.time.LocalDateTime.now());
        if (body.containsKey("comment")) sub.setGradingMessage((String) body.get("comment"));
        taskSubmissionMapper.updateById(sub);
        return R.ok("评分完成");
    }

    // ── AI 评分 ──

    /** 教师手动触发某次提交的 AI 重新评分 */
    @Autowired
    private com.school.teaching.service.impl.TrainingServiceImpl trainingServiceImpl;

    @PostMapping("/tasks/{taskId}/submissions/{submissionId}/ai-grade")
    public R<?> aiGradeSubmission(@PathVariable Long taskId, @PathVariable Long submissionId) {
        requireTeacher();
        Task task = taskCrudService.getById(taskId);
        if (task == null) return R.notFound("任务不存在");
        Long teacherId = resolveTeacherId();
        if (!teacherId.equals(task.getTeacherId()) && !SecurityUtils.isAdmin()) {
            return R.error(403, "无权操作");
        }
        com.school.teaching.entity.TaskSubmission sub = taskSubmissionMapper.selectById(submissionId);
        if (sub == null) return R.notFound("提交记录不存在");

        // 异步触发 AI 评分
        List<Map<String, Object>> stepDefs = trainingServiceImpl.parseSteps(task);
        List<Map<String, Object>> progress = trainingServiceImpl.parseProgress(sub);
        trainingServiceImpl.aiGradeAsync(taskId, stepDefs, progress, submissionId);

        return R.ok(Map.of("status", "AI评分已触发，稍后刷新查看结果"));
    }

    // ── AI 生成 ──

    @PostMapping("/ai/generate-steps")
    public R<?> aiGenerateSteps(@RequestBody Map<String, Object> body) {
        requireTeacher();
        String subject = (String) body.getOrDefault("subject", "");
        @SuppressWarnings("unchecked")
        List<Integer> nodeIdsRaw = (List<Integer>) body.get("nodeIds");
        List<Long> nodeIds = nodeIdsRaw != null
            ? nodeIdsRaw.stream().map(Long::valueOf).toList()
            : List.of();
        int stepCount = body.get("stepCount") instanceof Number
            ? ((Number) body.get("stepCount")).intValue() : 5;

        return R.ok(trainingAiService.generateSteps(
            SecurityUtils.getCurrentUserId(), subject, nodeIds, stepCount));
    }

    @PostMapping("/ai/import-text")
    public R<?> aiImportText(@RequestBody Map<String, Object> body) {
        requireTeacher();
        String text = (String) body.getOrDefault("text", "");
        return R.ok(trainingAiService.importFromText(
            SecurityUtils.getCurrentUserId(), text));
    }

    // ── 素材打包下载 ──

    /**
     * GET /training/materials/download?prefix=/api/uploads/practice/文字处理基础/op_01
     * 按前缀匹配目录下所有相关文件，打包为 ZIP 下载（包含文档、样张、辅助文件等）
     */
    @GetMapping("/materials/download")
    public ResponseEntity<byte[]> downloadMaterials(@RequestParam("prefix") String urlPrefix) {
        try {
            // /api/uploads/practice/文字处理基础/op_01 → uploads/practice/文字处理基础/op_01
            String relative = urlPrefix.replace("/api/", "");
            Path dirPath = Paths.get(relative).getParent();
            String filePrefix = Paths.get(relative).getFileName().toString();

            if (!Files.exists(dirPath)) {
                throw new BusinessException("素材目录不存在");
            }

            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(bos);

            // 收集所有匹配前缀的文件（过滤临时/系统文件）
            java.util.Set<String> added = new java.util.HashSet<>();
            java.io.File[] files = dirPath.toFile().listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                return name.startsWith(filePrefix)
                    && !lower.endsWith(".htm")     // 排除 Request.htm
                    && !lower.endsWith(".db")      // 排除 Thumbs.db
                    && !name.contains("_tmpext");  // 排除临时目录
            });

            if (files != null) {
                for (java.io.File f : files) {
                    String entryName = f.getName();
                    if (!added.add(entryName)) continue;
                    zos.putNextEntry(new java.util.zip.ZipEntry(entryName));
                    zos.write(Files.readAllBytes(f.toPath()));
                    zos.closeEntry();
                }
            }
            zos.close();
            bos.close();

            byte[] zipBytes = bos.toByteArray();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(zipBytes.length);
            String filename = filePrefix + "_素材.zip";
            headers.add("Content-Disposition",
                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" +
                java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"));
            return new ResponseEntity<>(zipBytes, headers, org.springframework.http.HttpStatus.OK);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("打包下载失败: " + e.getMessage());
        }
    }

    // ── 文件上传 + 自动评估 ──

    /**
     * Excel 步骤文件上传 + 自动检查点评估
     * POST /training/upload/excel/{taskId}
     * 请求参数: file (.xlsx), checkpoints (JSON字符串，步骤配置中的检查点)
     */
    @PostMapping("/upload/excel/{taskId}")
    public R<Map<String, Object>> uploadExcel(@PathVariable Long taskId,
                                               @RequestParam("file") MultipartFile file,
                                               @RequestParam(value = "checkpoints", required = false) String checkpointsJson) {
        requireStudent();

        if (file == null || file.isEmpty()) return R.error(400, "请选择文件");
        if (file.getSize() > 10 * 1024 * 1024) return R.error(400, "文件不能超过10MB");

        String ext = extractExt(file.getOriginalFilename());
        if (!XLSX_EXT.contains(ext)) return R.error(400, "仅支持 .xlsx 格式");

        try {
            byte[] fileBytes = file.getBytes();

            // 魔数校验
            if (!isXlsxMagic(fileBytes)) return R.error(400, "文件内容与扩展名不匹配");

            // 保存文件
            Path uploadPath = Paths.get(baseUploadDir, "training", "excel");
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            String filename = "excel_" + taskId + "_" + System.currentTimeMillis() + ext;
            Files.write(uploadPath.resolve(filename), fileBytes);
            String fileUrl = "/api/uploads/training/excel/" + filename;

            // 解析检查点
            List<Map<String, Object>> checkpoints = new ArrayList<>();
            if (checkpointsJson != null && !checkpointsJson.isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(checkpointsJson, List.class);
                    checkpoints = parsed;
                } catch (Exception ignored) {
                    log.warn("实训检查点 JSON 解析失败，跳过");
                }
            }

            // 评估
            Map<String, Object> evalResult = ExcelCheckpointEvaluator.evaluate(fileBytes, checkpoints);
            evalResult.put("fileUrl", fileUrl);

            return R.ok(evalResult, "上传并评估完成");

        } catch (IOException e) {
            return R.error(500, "文件处理失败: " + e.getMessage());
        }
    }

    /** PPT 演示文稿上传 + 自动评估 */
    @PostMapping("/upload/ppt/{taskId}")
    public R<Map<String, Object>> uploadPpt(@PathVariable Long taskId,
                                             @RequestParam("file") MultipartFile file,
                                             @RequestParam(value = "checkpoints", required = false) String checkpointsJson) {
        requireStudent();
        if (file == null || file.isEmpty()) return R.error(400, "请选择文件");
        if (file.getSize() > 20 * 1024 * 1024) return R.error(400, "文件不能超过20MB");
        String ext = extractExt(file.getOriginalFilename());
        if (!".pptx".equals(ext)) return R.error(400, "仅支持 .pptx 格式");
        try {
            byte[] fileBytes = file.getBytes();
            if (!isXlsxMagic(fileBytes)) return R.error(400, "文件内容与扩展名不匹配"); // PPTX 也是 ZIP 格式，魔数相同
            Path uploadPath = Paths.get(baseUploadDir, "training", "ppt");
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            String filename = "ppt_" + taskId + "_" + System.currentTimeMillis() + ext;
            Files.write(uploadPath.resolve(filename), fileBytes);
            String fileUrl = "/api/uploads/training/ppt/" + filename;

            List<Map<String, Object>> checkpoints = parseCheckpoints(checkpointsJson);
            Map<String, Object> evalResult = PptCheckpointEvaluator.evaluate(fileBytes, checkpoints);
            evalResult.put("fileUrl", fileUrl);
            return R.ok(evalResult, "上传并评估完成");
        } catch (IOException e) {
            return R.error(500, "文件处理失败: " + e.getMessage());
        }
    }

    /** SQL 语句沙箱评估 */
    @PostMapping("/eval/sql/{taskId}")
    public R<Map<String, Object>> evalSql(@PathVariable Long taskId,
                                           @RequestBody Map<String, String> body) {
        requireStudent();
        String sql = body.get("sql");
        String expectedSql = body.get("expectedSql");
        String schema = body.get("schema");
        if (sql == null || sql.isBlank()) return R.error(400, "请输入 SQL 语句");
        Map<String, Object> result = SqlSandboxEvaluator.evaluate(sql, expectedSql, schema);
        return R.ok(result, result.get("passed") != null && (Boolean) result.get("passed") ? "正确" : "不正确");
    }

    /** Office 文档上传（Word .docx）— Phase 3 预留 */
    @PostMapping("/upload/office/{taskId}")
    public R<Map<String, String>> uploadOffice(@PathVariable Long taskId,
                                                @RequestParam("file") MultipartFile file) {
        requireStudent();
        if (file == null || file.isEmpty()) return R.error(400, "请选择文件");
        String ext = extractExt(file.getOriginalFilename());
        if (!".docx".equals(ext)) return R.error(400, "仅支持 .docx 格式");
        try {
            byte[] fileBytes = file.getBytes();
            if (!isXlsxMagic(fileBytes)) return R.error(400, "文件内容与扩展名不匹配"); // .docx也是ZIP格式
            Path uploadPath = Paths.get(baseUploadDir, "training", "office");
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            String filename = "office_" + taskId + "_" + System.currentTimeMillis() + ext;
            Files.write(uploadPath.resolve(filename), fileBytes);
            Map<String, String> result = new HashMap<>();
            result.put("fileUrl", "/api/uploads/training/office/" + filename);
            return R.ok(result, "上传成功");
        } catch (IOException e) {
            return R.error(500, "文件处理失败");
        }
    }

    // ── 实训中心 Hub ──

    @GetMapping("/hub")
    public R<?> getHub() {
        return R.error(501, "实训中心 Hub 待实现");
    }

    // ── 工具方法 ──

    /** 从 taskConfig 解析步骤列表 */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseStepsFromConfig(String taskConfig) {
        if (taskConfig == null || taskConfig.isBlank()) return List.of();
        try {
            Map<String, Object> config = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(taskConfig, LinkedHashMap.class);
            Object stepsRaw = config.get("steps");
            if (stepsRaw instanceof List) {
                return (List<Map<String, Object>>) stepsRaw;
            }
        } catch (Exception e) {
            log.debug("解析 taskConfig 步骤失败: {}", e.getMessage());
        }
        return List.of();
    }

    private static String extractExt(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    private static boolean isXlsxMagic(byte[] bytes) {
        return bytes.length >= 4
            && (bytes[0] & 0xFF) == 0x50 && (bytes[1] & 0xFF) == 0x4B
            && (bytes[2] & 0xFF) == 0x03 && (bytes[3] & 0xFF) == 0x04;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> parseCheckpoints(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, List.class);
        } catch (Exception e) {
            log.warn("Training checkpoints JSON解析失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
