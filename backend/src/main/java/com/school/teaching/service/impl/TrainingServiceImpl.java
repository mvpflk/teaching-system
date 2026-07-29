package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.TrainingAiGradingService;
import com.school.teaching.security.StudentResolver;
import com.school.teaching.service.TrainingAiService;
import com.school.teaching.service.TrainingService;
import com.school.teaching.common.practice.SqlSandboxEvaluator;
import com.school.teaching.common.practice.ExcelCheckpointEvaluator;
import com.school.teaching.common.practice.PptCheckpointEvaluator;
import com.school.teaching.common.practice.WordCheckpointEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 实训中心服务实现 — 复用 Task/TaskSubmission 表，零新表
 */
@Slf4j
@Service
public class TrainingServiceImpl implements TrainingService {

    @Autowired private TaskMapper taskMapper;
    @Autowired private TaskSubmissionMapper submissionMapper;
    @Autowired private PracticeTemplateMapper templateMapper;
    @Autowired private TeacherMapper teacherMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private ClassesMapper classMapper;
    @Autowired private StudentResolver studentResolver;
    @Autowired private TrainingAiService trainingAiService;
    @Autowired private TrainingAiGradingService aiGradingService;
    @Autowired @Lazy private TrainingServiceImpl self;  // 自注入，供 @Async 穿透代理

    private static final ObjectMapper om = new ObjectMapper();
    private static final String TASK_TYPE = "TRAINING";

    // ═══════════════════════════════════════════════════════════
    // 辅助方法
    // ═══════════════════════════════════════════════════════════

    /** 从 taskConfig JSON 解析步骤列表 */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> parseSteps(Task task) {
        if (task.getTaskConfig() == null || task.getTaskConfig().isBlank()) return List.of();
        try {
            Map<String, Object> config = om.readValue(task.getTaskConfig(), Map.class);
            Object steps = config.get("steps");
            if (steps instanceof List<?> list) return (List<Map<String, Object>>) list;
        } catch (Exception e) {
            log.warn("解析 taskConfig.steps 失败: taskId={}", task.getId(), e);
        }
        return List.of();
    }

    /** 计算步骤总分 = ∑ step.score.max */
    private int calcTotalScore(List<Map<String, Object>> steps) {
        int total = 0;
        for (Map<String, Object> step : steps) {
            Object scoreObj = step.get("score");
            if (scoreObj instanceof Map<?, ?> score) {
                Object max = score.get("max");
                if (max instanceof Number n) total += n.intValue();
            }
        }
        return total;
    }

    /** 查询学生的唯一提交记录（LIMIT 1 防御：历史脏数据可能存在多行，selectOne 遇 2 行抛 TooManyResultsException，检查清单#2；取最新一行） */
    private TaskSubmission findSubmission(Long taskId, Long studentId) {
        return submissionMapper.selectOne(new LambdaQueryWrapper<TaskSubmission>()
            .eq(TaskSubmission::getTaskId, taskId)
            .eq(TaskSubmission::getStudentId, studentId)
            .orderByDesc(TaskSubmission::getId)
            .last("LIMIT 1"));
    }

    /** 查找或创建 IN_PROGRESS 提交 */
    private TaskSubmission findOrCreateSubmission(Long taskId, Long studentId) {
        TaskSubmission sub = findSubmission(taskId, studentId);
        if (sub != null) return sub;
        sub = new TaskSubmission();
        sub.setTaskId(taskId);
        sub.setStudentId(studentId);
        sub.setStatus("IN_PROGRESS");
        sub.setSchoolId(1L);
        sub.setStageId(4L);
        // 业务字段显式赋值不靠 DB 默认值（检查清单#10 / R115 isOfficial 口径）
        sub.setIsOfficial(true);
        sub.setAttemptNumber(1);
        sub.setContent("{\"steps\":[]}");
        submissionMapper.insert(sub);
        return sub;
    }

    /** 解析提交中已保存的步骤进度 */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> parseProgress(TaskSubmission sub) {
        if (sub.getContent() == null || sub.getContent().isBlank()) return List.of();
        try {
            Map<String, Object> content = om.readValue(sub.getContent(), Map.class);
            Object steps = content.get("steps");
            if (steps instanceof List<?> list) return (List<Map<String, Object>>) list;
        } catch (Exception e) {
            log.warn("解析 submission.content 失败: subId={}", sub.getId(), e);
        }
        return List.of();
    }

    /** 解析评分 JSON */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseScoreJson(TaskSubmission sub) {
        if (sub.getScoreJson() == null || sub.getScoreJson().isBlank()) {
            Map<String, Object> def = new LinkedHashMap<>();
            def.put("steps", new ArrayList<>());
            def.put("totalScore", 0);
            def.put("comment", "");
            return def;
        }
        try {
            return om.readValue(sub.getScoreJson(), Map.class);
        } catch (Exception e) {
            log.warn("解析 scoreJson 失败: subId={}", sub.getId(), e);
            Map<String, Object> def = new LinkedHashMap<>();
            def.put("steps", new ArrayList<>());
            def.put("totalScore", 0);
            def.put("comment", "");
            return def;
        }
    }

    /** 所有权校验：管理员放行，否则比对 teacherId */
    private Task checkOwnership(Long taskId, Long userId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");
        if (SecurityUtils.isAdmin()) return task;
        Long teacherId = resolveTeacherId(userId);
        if (teacherId == null || !teacherId.equals(task.getTeacherId()))
            throw new BusinessException(403, "无权操作此任务");
        return task;
    }

    /** userId → teacherId */
    private Long resolveTeacherId(Long userId) {
        if (userId == null) return null;
        Teacher t = teacherMapper.selectOne(
            new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
        if (t != null) return t.getId();
        return SecurityUtils.isAdmin() ? 0L : null;
    }

    /** 通过 userId 查 studentId */
    private Long resolveStudentId(Long userId) {
        if (userId == null) return null;
        return studentResolver.resolveStudentIdByUserId(userId);
    }

    /** 通过 studentId 查姓名（从 users 表 real_name 字段） */
    private String resolveStudentName(Long studentId) {
        if (studentId == null) return "未知";
        Student stu = studentMapper.selectById(studentId);
        if (stu == null || stu.getUserId() == null) return "未知";
        User user = userMapper.selectById(stu.getUserId());
        if (user == null) return "未知";
        return user.getRealName() != null && !user.getRealName().isBlank()
            ? user.getRealName() : user.getUsername();
    }

    /** 通过 studentId 查班级名称 */
    private String resolveClassName(Long studentId) {
        if (studentId == null) return "";
        Student stu = studentMapper.selectById(studentId);
        if (stu == null || stu.getClassId() == null) return "";
        Classes cls = classMapper.selectById(stu.getClassId());
        return cls != null ? cls.getClassName() : "";
    }

    // ═══════════════════════════════════════════════════════════
    // 任务 CRUD
    // ═══════════════════════════════════════════════════════════

    @Override
    public Map<String, Object> listTasks(int page, int size) {
        String role = SecurityUtils.getCurrentRole();
        Long userId = SecurityUtils.getCurrentUserId();
        LambdaQueryWrapper<Task> qw = new LambdaQueryWrapper<Task>()
            .eq(Task::getTaskType, TASK_TYPE);

        if ("STUDENT".equals(role)) {
            qw.eq(Task::getStatus, "PUBLISHED");
        } else if (SecurityUtils.isTeacherOrAdmin()) {
            Long teacherId = resolveTeacherId(userId);
            if (!SecurityUtils.isAdmin() && teacherId != null) {
                qw.eq(Task::getTeacherId, teacherId);
            }
        }

        qw.orderByDesc(Task::getCreatedAt);
        Page<Task> result = taskMapper.selectPage(new Page<>(page, size), qw);

        List<Map<String, Object>> records = result.getRecords().stream().map(task -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", task.getId());
            m.put("title", task.getTitle());
            m.put("subject", task.getSubject());
            m.put("description", task.getDescription());
            m.put("status", task.getStatus());
            m.put("totalScore", task.getTotalScore());
            m.put("createdAt", task.getCreatedAt());
            m.put("stepCount", parseSteps(task).size());
            return m;
        }).toList();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("records", records);
        resp.put("total", result.getTotal());
        return resp;
    }

    @Override
    public Map<String, Object> getTaskDetail(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");
        List<Map<String, Object>> steps = parseSteps(task);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", task.getId());
        m.put("title", task.getTitle());
        m.put("subject", task.getSubject());
        m.put("description", task.getDescription());
        m.put("status", task.getStatus());
        m.put("totalScore", task.getTotalScore());
        m.put("scoreType", task.getScoreType());
        m.put("teacherId", task.getTeacherId());
        m.put("deadline", task.getDeadline());
        m.put("createdAt", task.getCreatedAt());
        m.put("steps", steps);
        return m;
    }

    @Override
    @Transactional
    public Map<String, Object> createTask(Map<String, Object> body, Long userId) {
        String title = (String) body.get("title");
        if (title == null || title.isBlank()) throw new BusinessException(400, "任务标题不能为空");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> steps = (List<Map<String, Object>>) body.get("steps");
        if (steps == null || steps.isEmpty()) throw new BusinessException(400, "至少需要一个实训步骤");

        // 校验每步必填字段
        for (int i = 0; i < steps.size(); i++) {
            Map<String, Object> step = steps.get(i);
            String stepTitle = (String) step.get("title");
            if (stepTitle == null || stepTitle.isBlank())
                throw new BusinessException(400, "步骤" + (i + 1) + "标题不能为空");
            String type = (String) step.get("type");
            if (type == null || type.isBlank())
                throw new BusinessException(400, "步骤" + (i + 1) + "类型不能为空");
            Object scoreObj = step.get("score");
            if (scoreObj instanceof Map<?, ?> score) {
                Object max = score.get("max");
                if (max instanceof Number && ((Number) max).intValue() < 0)
                    throw new BusinessException(400, "步骤" + (i + 1) + "分值不能为负");
            }
        }

        Task task = new Task();
        task.setSchoolId(SecurityUtils.getCurrentSchoolId() != null
            ? SecurityUtils.getCurrentSchoolId() : 1L);
        task.setStageId(SecurityUtils.getCurrentStageId() != null
            ? SecurityUtils.getCurrentStageId() : 1L);
        task.setTitle(title);
        task.setSubject((String) body.getOrDefault("subject", ""));
        task.setDescription((String) body.getOrDefault("description", ""));
        task.setTaskType(TASK_TYPE);
        task.setStatus("DRAFT");
        task.setScoreType("POINT_100");
        task.setTeacherId(resolveTeacherId(userId));

        // 目标班级（可选）
        Object targetClassId = body.get("targetClassId");
        if (targetClassId instanceof Number) {
            task.setTargetType("CLASS");
            task.setTargetId(((Number) targetClassId).longValue());
        }

        // deadline（可选）
        Object deadlineStr = body.get("deadline");
        if (deadlineStr instanceof String s && !s.isBlank()) {
            try { task.setDeadline(LocalDateTime.parse(s)); } catch (Exception ignored) {}
        }

        // 序列化 taskConfig
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("steps", steps);
        Object saveToLibrary = body.get("saveToLibrary");
        config.put("saveToLibrary", Boolean.TRUE.equals(saveToLibrary));
        try {
            task.setTaskConfig(om.writeValueAsString(config));
        } catch (Exception e) {
            throw new BusinessException(500, "步骤序列化失败");
        }

        int totalScore = calcTotalScore(steps);
        task.setTotalScore(BigDecimal.valueOf(totalScore));

        taskMapper.insert(task);

        // 若勾选保存到任务库，创建 PracticeTemplate
        if (Boolean.TRUE.equals(saveToLibrary)) {
            PracticeTemplate tmpl = new PracticeTemplate();
            tmpl.setTitle(title + " [模板]");
            tmpl.setSubject((String) body.getOrDefault("subject", ""));
            tmpl.setDescription((String) body.getOrDefault("description", ""));
            tmpl.setCategory("TRAINING");
            tmpl.setSource("TEACHER");
            tmpl.setUseCount(0);
            try {
                tmpl.setStepsJson(om.writeValueAsString(steps));
            } catch (Exception ignored) { log.debug("模板步骤序列化失败", ignored); }
            templateMapper.insert(tmpl);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", task.getId());
        return resp;
    }

    @Override
    @Transactional
    public Map<String, Object> updateTask(Long taskId, Map<String, Object> body, Long userId) {
        Task task = checkOwnership(taskId, userId);
        if (!"DRAFT".equals(task.getStatus()))
            throw new BusinessException(409, "仅草稿状态可编辑");

        String title = (String) body.get("title");
        if (title != null && !title.isBlank()) task.setTitle(title);
        if (body.containsKey("subject")) task.setSubject((String) body.getOrDefault("subject", ""));
        if (body.containsKey("description")) task.setDescription((String) body.getOrDefault("description", ""));

        // deadline
        if (body.containsKey("deadline")) {
            Object dl = body.get("deadline");
            if (dl instanceof String s && !s.isBlank()) {
                try { task.setDeadline(LocalDateTime.parse(s)); } catch (Exception ignored) { log.warn("截止日期解析失败: {}", s); }
            } else {
                task.setDeadline(null);
            }
        }

        // 更新步骤
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> steps = (List<Map<String, Object>>) body.get("steps");
        if (steps != null) {
            if (steps.isEmpty()) throw new BusinessException(400, "至少需要一个实训步骤");
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("steps", steps);
            config.put("saveToLibrary", body.getOrDefault("saveToLibrary", false));
            try {
                task.setTaskConfig(om.writeValueAsString(config));
            } catch (Exception e) {
                throw new BusinessException(500, "步骤序列化失败");
            }
            int totalScore = calcTotalScore(steps);
            task.setTotalScore(BigDecimal.valueOf(totalScore));
        }

        taskMapper.updateById(task);
        return getTaskDetail(taskId);
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId, Long userId) {
        Task task = checkOwnership(taskId, userId);
        if (!"DRAFT".equals(task.getStatus()) && !"CLOSED".equals(task.getStatus()))
            throw new BusinessException(409, "仅草稿或已关闭的任务可删除");

        Long gradedCount = submissionMapper.selectCount(new LambdaQueryWrapper<TaskSubmission>()
            .eq(TaskSubmission::getTaskId, taskId)
            .eq(TaskSubmission::getStatus, "GRADED"));
        if (gradedCount != null && gradedCount > 0)
            throw new BusinessException(409, "存在" + gradedCount + "份已评分提交，不可删除");

        // 级联删除提交记录
        submissionMapper.delete(new LambdaQueryWrapper<TaskSubmission>()
            .eq(TaskSubmission::getTaskId, taskId));
        taskMapper.deleteById(taskId);
    }

    @Override
    @Transactional
    public Map<String, Object> publishTask(Long taskId, Long userId) {
        Task task = checkOwnership(taskId, userId);
        if (!"DRAFT".equals(task.getStatus()))
            throw new BusinessException(409, "仅草稿状态可发布");

        List<Map<String, Object>> steps = parseSteps(task);
        if (steps.isEmpty())
            throw new BusinessException(400, "至少需要一个实训步骤才能发布");

        task.setStatus("PUBLISHED");
        taskMapper.updateById(task);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", task.getId());
        resp.put("status", "PUBLISHED");
        return resp;
    }

    // ═══════════════════════════════════════════════════════════
    // 任务库
    // ═══════════════════════════════════════════════════════════

    @Override
    public Map<String, Object> listLibrary() {
        List<PracticeTemplate> templates = templateMapper.selectList(
            new LambdaQueryWrapper<PracticeTemplate>()
                .eq(PracticeTemplate::getSource, "TEACHER")
                .orderByDesc(PracticeTemplate::getUseCount));

        List<Map<String, Object>> records = templates.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("title", t.getTitle());
            m.put("subject", t.getSubject());
            m.put("description", t.getDescription());
            m.put("category", t.getCategory());
            m.put("useCount", t.getUseCount() != null ? t.getUseCount() : 0);
            // 解析 stepsJson
            if (t.getStepsJson() != null && !t.getStepsJson().isBlank()) {
                try {
                    List<?> parsed = om.readValue(t.getStepsJson(), List.class);
                    m.put("steps", parsed);
                    m.put("stepCount", parsed.size());
                } catch (Exception e) {
                    m.put("steps", List.of());
                    m.put("stepCount", 0);
                }
            } else {
                m.put("steps", List.of());
                m.put("stepCount", 0);
            }
            return m;
        }).toList();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("records", records);
        resp.put("total", records.size());
        return resp;
    }

    @Override
    @Transactional
    public Map<String, Object> copyFromLibrary(Long templateId) {
        PracticeTemplate tmpl = templateMapper.selectById(templateId);
        if (tmpl == null) throw new BusinessException(404, "模板不存在");

        // 增加使用计数
        int newCount = (tmpl.getUseCount() != null ? tmpl.getUseCount() : 0) + 1;
        tmpl.setUseCount(newCount);
        templateMapper.updateById(tmpl);

        // 解析步骤
        List<?> steps = List.of();
        if (tmpl.getStepsJson() != null && !tmpl.getStepsJson().isBlank()) {
            try {
                steps = om.readValue(tmpl.getStepsJson(), List.class);
            } catch (Exception ignored) { log.debug("模板步骤解析失败", ignored); }
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("title", tmpl.getTitle() != null ? tmpl.getTitle().replace(" [模板]", "") : "");
        resp.put("subject", tmpl.getSubject());
        resp.put("description", tmpl.getDescription());
        resp.put("steps", steps);
        return resp;
    }

    // ═══════════════════════════════════════════════════════════
    // 学生步骤执行
    // ═══════════════════════════════════════════════════════════

    @Override
    public Map<String, Object> getStudentSteps(Long taskId, Long userId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");
        if (!"PUBLISHED".equals(task.getStatus()))
            throw new BusinessException(400, "任务未发布");

        Long studentId = resolveStudentId(userId);
        if (studentId == null) throw new BusinessException(403, "仅学生可查看任务步骤");

        List<Map<String, Object>> stepDefs = parseSteps(task);
        TaskSubmission sub = findOrCreateSubmission(taskId, studentId);
        List<Map<String, Object>> progress = parseProgress(sub);

        // 合并步骤定义 + 已保存进度 → 每步标注 _completed 和 _data
        List<Map<String, Object>> mergedSteps = new ArrayList<>();
        for (int i = 0; i < stepDefs.size(); i++) {
            Map<String, Object> step = new LinkedHashMap<>(stepDefs.get(i));
            step.put("stepIndex", i);

            // 查找该步的进度
            Map<String, Object> saved = null;
            for (Map<String, Object> p : progress) {
                Object idx = p.get("stepIndex");
                if (idx instanceof Number n && n.intValue() == i) {
                    saved = p;
                    break;
                }
            }

            step.put("_completed", saved != null && Boolean.TRUE.equals(saved.get("completed")));
            step.put("_data", saved != null ? saved.get("data") : null);
            mergedSteps.add(step);
        }

        Map<String, Object> taskInfo = new LinkedHashMap<>();
        taskInfo.put("id", task.getId());
        taskInfo.put("title", task.getTitle());
        taskInfo.put("subject", task.getSubject());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("task", taskInfo);
        resp.put("steps", mergedSteps);
        resp.put("submissionId", sub.getId());
        resp.put("status", sub.getStatus());
        return resp;
    }

    @Override
    @Transactional
    public void saveStepProgress(Long taskId, int stepIndex, Map<String, Object> body, Long userId) {
        Long studentId = resolveStudentId(userId);
        if (studentId == null) throw new BusinessException(403, "仅学生可保存进度");

        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");

        List<Map<String, Object>> stepDefs = parseSteps(task);
        if (stepIndex < 0 || stepIndex >= stepDefs.size())
            throw new BusinessException(400, "步骤索引超出范围");

        TaskSubmission sub = findOrCreateSubmission(taskId, studentId);
        if (!"IN_PROGRESS".equals(sub.getStatus()) && !"PENDING".equals(sub.getStatus())) {
            // 已提交/已评分则不允许修改
            if ("SUBMITTED".equals(sub.getStatus()) || "GRADED".equals(sub.getStatus()))
                throw new BusinessException(409, "任务已提交，不可修改");
            sub.setStatus("IN_PROGRESS");
        }

        List<Map<String, Object>> progress = parseProgress(sub);
        Object stepData = body; // 前端直接发送步骤数据作为请求体，body 本身就是 data

        // 更新或插入该步进度
        boolean found = false;
        for (Map<String, Object> p : progress) {
            Object idx = p.get("stepIndex");
            if (idx instanceof Number n && n.intValue() == stepIndex) {
                p.put("completed", true);
                p.put("data", stepData);
                found = true;
                break;
            }
        }
        if (!found) {
            Map<String, Object> newStep = new LinkedHashMap<>();
            newStep.put("stepIndex", stepIndex);
            newStep.put("completed", true);
            newStep.put("data", stepData);
            progress.add(newStep);
        }

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("steps", progress);
        try {
            sub.setContent(om.writeValueAsString(content));
        } catch (Exception e) {
            throw new BusinessException(500, "进度保存失败");
        }
        submissionMapper.updateById(sub);
    }

    @Override
    @Transactional
    public Map<String, Object> submitAllSteps(Long taskId, Long userId) {
        Long studentId = resolveStudentId(userId);
        if (studentId == null) throw new BusinessException(403, "仅学生可提交");

        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");

        List<Map<String, Object>> stepDefs = parseSteps(task);
        TaskSubmission sub = findSubmission(taskId, studentId);
        if (sub == null) throw new BusinessException(400, "尚未开始答题");

        if ("SUBMITTED".equals(sub.getStatus()) || "GRADED".equals(sub.getStatus()))
            throw new BusinessException(409, "任务已提交，不可重复提交");

        // 校验所有步骤 completed
        List<Map<String, Object>> progress = parseProgress(sub);
        Set<Integer> completedIndexes = progress.stream()
            .filter(p -> Boolean.TRUE.equals(p.get("completed")))
            .map(p -> {
                Object idx = p.get("stepIndex");
                return idx instanceof Number n ? n.intValue() : -1;
            })
            .collect(Collectors.toSet());

        for (int i = 0; i < stepDefs.size(); i++) {
            if (!completedIndexes.contains(i))
                throw new BusinessException(409, "步骤 " + (i + 1) + " 尚未完成，请完成后提交");
        }

        // 阶段1: 检查点规则评分（毫秒级，事务内）
        autoGradeSteps(taskId, stepDefs, progress, sub, false);

        sub.setStatus("SUBMITTED");
        sub.setSubmittedAt(LocalDateTime.now());
        submissionMapper.updateById(sub);

        // 阶段2: AI 评分异步执行（事务提交后，不阻塞学生）
        final Long finalTaskId = taskId;
        final Long finalSubId = sub.getId();
        try {
            self.aiGradeAsync(finalTaskId, stepDefs, progress, finalSubId);
        } catch (Exception e) {
            log.warn("启动AI异步评分失败(已跳过): taskId={}, err={}", taskId, e.getMessage());
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("submissionId", sub.getId());
        resp.put("status", "SUBMITTED");
        return resp;
    }

    // ═══════════════════════════════════════════════════════════
    // 教师评分
    // ═══════════════════════════════════════════════════════════

    @Override
    public Map<String, Object> getSubmissions(Long taskId, Long userId) {
        Task task = checkOwnership(taskId, userId);
        List<Map<String, Object>> stepDefs = parseSteps(task);

        List<TaskSubmission> submissions = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, taskId)
                .in(TaskSubmission::getStatus, List.of("IN_PROGRESS", "SUBMITTED", "GRADED"))
                .orderByDesc(TaskSubmission::getId));

        int pending = 0, graded = 0;
        Set<String> classSet = new LinkedHashSet<>();
        List<Map<String, Object>> subList = new ArrayList<>();
        for (TaskSubmission sub : submissions) {
            String studentName = resolveStudentName(sub.getStudentId());
            String className = resolveClassName(sub.getStudentId());
            if (!className.isBlank()) classSet.add(className);
            List<Map<String, Object>> rawProgress = parseProgress(sub);
            // 扁平化：提取每个步骤的 data 字段，使得前端直接访问 studentStepData[i]?.content
            List<Map<String, Object>> flatStepData = new ArrayList<>();
            for (Map<String, Object> p : rawProgress) {
                Object d = p.get("data");
                if (d instanceof Map<?, ?> dm) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) dm;
                    flatStepData.add(dataMap);
                } else {
                    flatStepData.add(new LinkedHashMap<>());
                }
            }
            Map<String, Object> scoreInfo = parseScoreJson(sub);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", sub.getId());
            m.put("studentId", sub.getStudentId());
            m.put("studentName", studentName);
            m.put("className", className);
            m.put("status", sub.getStatus());
            m.put("submittedAt", sub.getSubmittedAt());
            m.put("stepData", flatStepData);
            m.put("stepScores", scoreInfo.get("steps"));
            m.put("totalScore", scoreInfo.getOrDefault("totalScore", 0));
            m.put("comment", scoreInfo.getOrDefault("comment", ""));
            // AI 评分信息：按 stepIndex 索引的扁平化 map
            Map<Integer, Map<String, Object>> aiGrades = new LinkedHashMap<>();
            Object steps = scoreInfo.get("steps");
            if (steps instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> sm) {
                        Object idx = sm.get("stepIndex");
                        if (idx instanceof Number n && Boolean.TRUE.equals(sm.get("aiGraded"))) {
                            Map<String, Object> ag = new LinkedHashMap<>();
                            ag.put("score", sm.get("score"));
                            ag.put("confidence", sm.get("confidence"));
                            ag.put("reason", sm.get("comment"));
                            aiGrades.put(n.intValue(), ag);
                        }
                    }
                }
            }
            m.put("aiGrades", aiGrades);
            subList.add(m);

            if ("SUBMITTED".equals(sub.getStatus())) pending++;
            else if ("GRADED".equals(sub.getStatus())) graded++;
        }

        Map<String, Object> taskInfo = new LinkedHashMap<>();
        taskInfo.put("id", task.getId());
        taskInfo.put("title", task.getTitle());
        taskInfo.put("subject", task.getSubject());
        taskInfo.put("steps", stepDefs);  // 步骤定义（标题/类型/分值等），前端据此渲染评分界面

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", submissions.size());
        stats.put("pending", pending);
        stats.put("graded", graded);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("task", taskInfo);
        resp.put("stepDefs", stepDefs);
        resp.put("submissions", subList);
        resp.put("stats", stats);
        resp.put("classes", new ArrayList<>(classSet));  // 班级列表供前端筛选
        return resp;
    }

    @Override
    @Transactional
    public Map<String, Object> gradeStep(Long taskId, Long submissionId, int stepIndex,
                                          Map<String, Object> body, Long userId) {
        // 验证任务所有权
        checkOwnership(taskId, userId);

        TaskSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BusinessException(404, "提交不存在");
        if (!sub.getTaskId().equals(taskId))
            throw new BusinessException(400, "提交不属于该任务");

        Task task = taskMapper.selectById(taskId);
        List<Map<String, Object>> stepDefs = parseSteps(task);
        if (stepIndex < 0 || stepIndex >= stepDefs.size())
            throw new BusinessException(400, "步骤索引超出范围");

        // 解析当前评分
        Map<String, Object> scoreInfo = parseScoreJson(sub);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stepScores = (List<Map<String, Object>>) scoreInfo.get("steps");
        if (stepScores == null) stepScores = new ArrayList<>();

        // 获取该步满分
        Map<String, Object> stepDef = stepDefs.get(stepIndex);
        int maxScore = 10;
        Object scoreDef = stepDef.get("score");
        if (scoreDef instanceof Map<?, ?> sd) {
            Object mx = sd.get("max");
            if (mx instanceof Number n) maxScore = n.intValue();
        }

        // 评分值
        Object scoreVal = body.get("score");
        int score = scoreVal instanceof Number n ? n.intValue() : 0;
        if (score < 0) score = 0;
        if (score > maxScore) score = maxScore;
        String comment = (String) body.getOrDefault("comment", "");

        // 更新或插入该步评分
        boolean found = false;
        for (Map<String, Object> s : stepScores) {
            Object idx = s.get("stepIndex");
            if (idx instanceof Number n && n.intValue() == stepIndex) {
                s.put("score", score);
                s.put("maxScore", maxScore);
                s.put("comment", comment);
                found = true;
                break;
            }
        }
        if (!found) {
            Map<String, Object> newScore = new LinkedHashMap<>();
            newScore.put("stepIndex", stepIndex);
            newScore.put("score", score);
            newScore.put("maxScore", maxScore);
            newScore.put("comment", comment);
            stepScores.add(newScore);
        }

        // 重算总分
        int totalScore = 0;
        for (Map<String, Object> s : stepScores) {
            Object sc = s.get("score");
            if (sc instanceof Number n) totalScore += n.intValue();
        }
        scoreInfo.put("totalScore", totalScore);

        try {
            sub.setScoreJson(om.writeValueAsString(scoreInfo));
        } catch (Exception e) {
            throw new BusinessException(500, "评分保存失败");
        }
        submissionMapper.updateById(sub);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("stepIndex", stepIndex);
        resp.put("score", score);
        resp.put("maxScore", maxScore);
        resp.put("totalScore", totalScore);
        return resp;
    }

    @Override
    @Transactional
    public Map<String, Object> finalizeGrade(Long taskId, Long submissionId,
                                              Map<String, Object> body, Long userId) {
        checkOwnership(taskId, userId);

        TaskSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BusinessException(404, "提交不存在");
        if (!sub.getTaskId().equals(taskId))
            throw new BusinessException(400, "提交不属于该任务");
        if (!"SUBMITTED".equals(sub.getStatus()))
            throw new BusinessException(409, "仅已提交状态可完成评分");

        Map<String, Object> scoreInfo = parseScoreJson(sub);
        String finalComment = (String) body.getOrDefault("comment", "");
        scoreInfo.put("comment", finalComment);

        int totalScore = ((Number) scoreInfo.getOrDefault("totalScore", 0)).intValue();

        try {
            sub.setScoreJson(om.writeValueAsString(scoreInfo));
        } catch (Exception e) {
            throw new BusinessException(500, "评分保存失败");
        }
        sub.setScore(BigDecimal.valueOf(totalScore));
        sub.setStatus("GRADED");
        sub.setGradedAt(LocalDateTime.now());
        sub.setGradedBy(resolveTeacherId(userId));
        submissionMapper.updateById(sub);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("submissionId", sub.getId());
        resp.put("status", "GRADED");
        resp.put("totalScore", totalScore);
        return resp;
    }

    // ═══════════════════════════════════════════════════════════
    // 自动评分辅助方法
    // ═══════════════════════════════════════════════════════════

    /** 对已校验全部完成的提交执行自动评分 + 重算总分 */
    @SuppressWarnings("unchecked")
    public void autoGradeSteps(Long taskId, List<Map<String, Object>> stepDefs,
                                 List<Map<String, Object>> progress, TaskSubmission sub,
                                 boolean includeAi) {
        Map<String, Object> scoreInfo = parseScoreJson(sub);
        List<Map<String, Object>> stepScores = (List<Map<String, Object>>) scoreInfo.get("steps");
        if (stepScores == null) stepScores = new ArrayList<>();

        for (int i = 0; i < stepDefs.size(); i++) {
            Map<String, Object> stepDef = stepDefs.get(i);
            String stepType = String.valueOf(stepDef.getOrDefault("type", ""));
            boolean isCheckpoint = "sql".equals(stepType) || "excel".equals(stepType)
                || "ppt".equals(stepType) || "office".equals(stepType);
            boolean isAi = "text".equals(stepType) || "file".equals(stepType)
                || "sim".equals(stepType) || "web".equals(stepType);

            if (!isCheckpoint && !isAi) continue;
            // 阶段1 (includeAi=false): 只做检查点评分
            // 阶段2 (includeAi=true): 只做 AI 评分
            if (!includeAi && !isCheckpoint) continue;
            if (includeAi && !isAi) continue;

            Map<String, Object> stepData = findStepData(progress, i);
            if (stepData == null) continue;

            try {
                Map<String, Object> autoResult = autoEvaluate(stepType, stepDef, stepData);
                if (autoResult != null) {
                    int maxScore = resolveMaxScore(stepDef);
                    int autoScore = autoResult.get("score") instanceof Number n ? n.intValue() : 0;
                    if (autoScore < 0) autoScore = 0;
                    if (autoScore > maxScore) autoScore = maxScore;
                    String comment = autoResult.get("reason") instanceof String s ? s
                        : autoResult.get("comment") instanceof String s2 ? s2 : "自动评分";
                    boolean aiGraded = Boolean.TRUE.equals(autoResult.get("aiGraded"));
                    updateStepScore(stepScores, i, autoScore, maxScore, comment, true);
                    if (aiGraded && autoResult.get("confidence") instanceof Number conf) {
                        for (Map<String, Object> s : stepScores) {
                            Object idx = s.get("stepIndex");
                            if (idx instanceof Number n && n.intValue() == i) {
                                s.put("confidence", conf.doubleValue());
                                s.put("aiGraded", true);
                                break;
                            }
                        }
                    }
                    log.info("自动评分: taskId={}, step={}, type={}, score={}/{} aiGraded={}", taskId, i, stepType, autoScore, maxScore, aiGraded);
                }
            } catch (Exception e) {
                log.warn("自动评分失败(已跳过): taskId={}, step={}, type={}, err={}", taskId, i, stepType, e.getMessage());
            }
        }

        // 重算总分
        int totalScore = 0;
        for (Map<String, Object> s : stepScores) {
            Object sc = s.get("score");
            if (sc instanceof Number n) totalScore += n.intValue();
        }
        scoreInfo.put("totalScore", totalScore);
        try {
            sub.setScoreJson(om.writeValueAsString(scoreInfo));
        } catch (Exception e) {
            log.warn("自动评分JSON序列化失败(已跳过): {}", e.getMessage());
        }
    }

    /** 从进度列表中按 stepIndex 查找步骤数据 */
    private Map<String, Object> findStepData(List<Map<String, Object>> progress, int stepIndex) {
        for (Map<String, Object> p : progress) {
            Object idx = p.get("stepIndex");
            if (idx instanceof Number n && n.intValue() == stepIndex) return p;
        }
        return null;
    }

    /** 判断该步骤类型是否支持自动评分（检查点引擎 + AI评分） */
    private boolean isAutoGradable(String stepType) {
        return "sql".equals(stepType) || "excel".equals(stepType) || "ppt".equals(stepType) || "office".equals(stepType)
            || "text".equals(stepType) || "file".equals(stepType) || "sim".equals(stepType) || "web".equals(stepType);
    }

    /**
     * 异步 AI 评分 — 事务提交后执行，不阻塞学生提交
     * 包含 1 次重试 + 评分写入后的 scoreJson 持久化
     */
    @Async
    public void aiGradeAsync(Long taskId, List<Map<String, Object>> stepDefs,
                              List<Map<String, Object>> progress, Long submissionId) {
        log.info("AI异步评分开始: taskId={}, submissionId={}", taskId, submissionId);
        TaskSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) { log.warn("AI异步评分: submission不存在 id={}", submissionId); return; }

        // 先做 AI 评分（autoGradeSteps with includeAi=true）
        autoGradeSteps(taskId, stepDefs, progress, sub, true);

        // 重算总分（合并检查点评分 + AI 评分）
        Map<String, Object> scoreInfo = parseScoreJson(sub);
        List<Map<String, Object>> stepScores = (List<Map<String, Object>>) scoreInfo.get("steps");
        if (stepScores != null) {
            int totalScore = 0;
            for (Map<String, Object> s : stepScores) {
                Object sc = s.get("score");
                if (sc instanceof Number n) totalScore += n.intValue();
            }
            scoreInfo.put("totalScore", totalScore);
        }
        try {
            sub.setScoreJson(om.writeValueAsString(scoreInfo));
        } catch (Exception e) {
            log.warn("AI异步评分JSON序列化失败: {}", e.getMessage());
        }
        submissionMapper.updateById(sub);
        log.info("AI异步评分完成: taskId={}, submissionId={}", taskId, submissionId);
    }

    /** 获取步骤满分 */
    private int resolveMaxScore(Map<String, Object> stepDef) {
        Object scoreObj = stepDef.get("score");
        if (scoreObj instanceof Map<?, ?> score) {
            Object max = score.get("max");
            if (max instanceof Number n) return n.intValue();
        }
        return 10;
    }

    /** 自动评分执行器 — 根据步骤类型分派到对应的检查点评估器 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> autoEvaluate(String stepType, Map<String, Object> stepDef, Map<String, Object> stepData) {
        Object dataObj = stepData.get("data");
        if (!(dataObj instanceof Map<?, ?> data)) return null;

        return switch (stepType) {
            case "sql" -> autoEvaluateSql(stepDef, (Map<String, Object>) data);
            case "excel" -> autoEvaluateFile(stepDef, (Map<String, Object>) data, "excel");
            case "ppt" -> autoEvaluateFile(stepDef, (Map<String, Object>) data, "ppt");
            case "office" -> autoEvaluateFile(stepDef, (Map<String, Object>) data, "word");
            // AI 评分类型：text / file / sim / web
            case "text", "file", "sim", "web" -> aiEvaluateStep(stepDef, (Map<String, Object>) data);
            default -> null;
        };
    }

    /** AI 评分：对非检查点类型步骤进行语义评估 */
    private Map<String, Object> aiEvaluateStep(Map<String, Object> stepDef, Map<String, Object> data) {
        String stepType = String.valueOf(stepDef.getOrDefault("type", "text"));
        String stepDesc = String.valueOf(stepDef.getOrDefault("description", ""));
        int maxScore = resolveMaxScore(stepDef);
        try {
            return aiGradingService.gradeStep(stepType, stepDesc, maxScore, data);
        } catch (Exception e) {
            log.warn("AI评分异常(回退手动): stepType={}, err={}", stepType, e.getMessage());
            return null;
        }
    }

    /** SQL 检查点自动评分 */
    private Map<String, Object> autoEvaluateSql(Map<String, Object> stepDef, Map<String, Object> data) {
        String studentSql = (String) data.get("sql");
        if (studentSql == null || studentSql.isBlank()) {
            return Map.of("score", 0, "comment", "未提交 SQL");
        }
        // 从步骤配置中读取预期 SQL（教师提供的参考查询，与 studentSql 比对结果集）
        Object configObj = stepDef.get("config");
        String expectedSql = null;
        String dbType = "mysql";
        if (configObj instanceof Map<?, ?> config) {
            expectedSql = (String) config.get("expectedResult");
            if (config.get("dbType") instanceof String dt) dbType = dt;
        }
        if (expectedSql == null || expectedSql.isBlank()) {
            return Map.of("score", 0, "comment", "未配置预期 SQL，需教师手动评分");
        }
        return SqlSandboxEvaluator.evaluate(studentSql, expectedSql, dbType);
    }

    /** 文件型检查点自动评分（Excel/PPT/Word） */
    private Map<String, Object> autoEvaluateFile(Map<String, Object> stepDef, Map<String, Object> data, String type) {
        String filePath = (String) data.get("filePath");
        if (filePath == null || filePath.isBlank()) {
            return Map.of("score", 0, "comment", "未上传文件");
        }
        Object configObj = stepDef.get("config");
        List<Map<String, Object>> checkpoints = List.of();
        if (configObj instanceof Map<?, ?> config) {
            Object cp = config.get("checkpoints");
            if (cp instanceof List<?> list) {
                checkpoints = (List<Map<String, Object>>) list;
            }
        }
        if (checkpoints.isEmpty()) {
            return Map.of("score", 0, "comment", "未配置检查点，需教师手动评分");
        }
        try {
            byte[] fileBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath));
            return switch (type) {
                case "excel" -> ExcelCheckpointEvaluator.evaluate(fileBytes, checkpoints);
                case "ppt" -> PptCheckpointEvaluator.evaluate(fileBytes, checkpoints);
                case "word" -> WordCheckpointEvaluator.evaluate(fileBytes, checkpoints);
                default -> null;
            };
        } catch (Exception e) {
            log.warn("文件型自动评分失败: path={}, err={}", filePath, e.getMessage());
            return Map.of("score", 0, "comment", "文件读取失败: " + e.getMessage());
        }
    }

    /** 更新/插入步骤评分记录，标记是否为自动评分 */
    private void updateStepScore(List<Map<String, Object>> stepScores, int stepIndex,
                                  int score, int maxScore, String comment, boolean autoGraded) {
        for (Map<String, Object> s : stepScores) {
            Object idx = s.get("stepIndex");
            if (idx instanceof Number n && n.intValue() == stepIndex) {
                s.put("score", score);
                s.put("maxScore", maxScore);
                s.put("comment", comment);
                s.put("autoGraded", autoGraded);
                return;
            }
        }
        Map<String, Object> newScore = new LinkedHashMap<>();
        newScore.put("stepIndex", stepIndex);
        newScore.put("score", score);
        newScore.put("maxScore", maxScore);
        newScore.put("comment", comment);
        newScore.put("autoGraded", autoGraded);
        stepScores.add(newScore);
    }

    // ═══════════════════════════════════════════════════════════
    // 实训中心 Hub
    // ═══════════════════════════════════════════════════════════

    @Override
    public Map<String, Object> getHub() {
        List<Map<String, Object>> cards = new ArrayList<>();

        // 实训任务数（已发布）
        Long trainingCount = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
            .eq(Task::getTaskType, TASK_TYPE)
            .eq(Task::getStatus, "PUBLISHED"));
        cards.add(card("training", "实训任务", "分步指导 + 自动评估 + 教师评分", "Monitor", trainingCount));

        // 仿真任务数
        Long simCount = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
            .eq(Task::getTaskType, "SIMULATION")
            .eq(Task::getStatus, "PUBLISHED"));
        cards.add(card("simulation", "仿真实训", "仿真环境操作练习", "Cpu", simCount));

        // 打字竞赛统计（有 typing_competitions 表则查询）
        Long typingCount = 0L;
        try {
            typingCount = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getTaskType, "TYPING")
                .eq(Task::getStatus, "PUBLISHED"));
        } catch (Exception ignored) { log.debug("竞赛统计查询失败", ignored); }
        cards.add(card("typing", "打字竞赛", "在线打字速度比赛", "EditPen", typingCount));

        // 最近实训任务（前5条已发布的）
        List<Task> recentTasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
            .eq(Task::getTaskType, TASK_TYPE)
            .eq(Task::getStatus, "PUBLISHED")
            .orderByDesc(Task::getCreatedAt)
            .last("LIMIT 5"));

        List<Map<String, Object>> recentList = recentTasks.stream().map(task -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", task.getId());
            m.put("title", task.getTitle());
            m.put("subject", task.getSubject());
            m.put("createdAt", task.getCreatedAt());
            m.put("stepCount", parseSteps(task).size());
            return m;
        }).toList();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("cards", cards);
        resp.put("recentTasks", recentList);
        return resp;
    }

    private Map<String, Object> card(String key, String title, String description, String icon, Long count) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("key", key);
        c.put("title", title);
        c.put("description", description);
        c.put("icon", icon);
        c.put("count", count);
        return c;
    }
}
