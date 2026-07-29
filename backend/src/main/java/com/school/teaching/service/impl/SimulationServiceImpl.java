package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 仿真任务服务实现 — Windows 操作仿真核心逻辑。
 */
@Service
@RequiredArgsConstructor
public class SimulationServiceImpl implements SimulationService {

    private final SimulationTaskMapper simTaskMapper;
    private final SimulationRecordingMapper recordingMapper;
    private final TaskMapper taskMapper;
    private final TeacherMapper teacherMapper;
    private final TaskSubmissionMapper taskSubmissionMapper;
    private static final Logger log = LoggerFactory.getLogger(SimulationServiceImpl.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public SimulationTask getById(Long simTaskId) {
        return simTaskMapper.selectById(simTaskId);
    }

    @Override
    public Map<String, Object> getTaskDefinition(Long simTaskId) {
        SimulationTask st = simTaskMapper.selectById(simTaskId);
        if (st == null) throw new BusinessException(404, "仿真任务不存在");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("simTaskId", st.getId());
        result.put("taskId", st.getTaskId());
        result.put("nodeId", st.getNodeId());
        try {
            result.put("taskJson", objectMapper.readTree(st.getTaskJson()));
            if (st.getInitialVfs() != null) {
                result.put("initialVfs", objectMapper.readTree(st.getInitialVfs()));
            }
        } catch (Exception e) {
            throw new BusinessException(500, "JSON 解析失败");
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> createTask(Map<String, Object> request, Long userId) {
        Teacher teacher = teacherMapper.selectOne(
            new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
        boolean isAdmin = com.school.teaching.security.SecurityUtils.isAdmin() || com.school.teaching.security.SecurityUtils.isSuperAdmin();
        if (teacher == null && !isAdmin) throw new BusinessException(403, "仅教师可操作");
        Long teacherId = teacher != null ? teacher.getId() : 1L; // Admin fallback

        // 创建任务
        Task task = new Task();
        task.setTitle((String) request.getOrDefault("title", "仿真任务"));
        task.setTaskType("FORMATIVE");
        task.setSubject((String) request.getOrDefault("subject", "信息技术应用基础"));
        task.setTeacherId(teacherId);
        // 尊重 publish 参数：true → PUBLISHED，否则 DRAFT
        boolean publish = Boolean.TRUE.equals(request.get("publish"));
        task.setStatus(publish ? "PUBLISHED" : "DRAFT");
        task.setSchoolId(1L);
        task.setStageId(4L);
        // 截止日期（可选）
        String deadlineStr = (String) request.get("deadline");
        if (deadlineStr != null && !deadlineStr.isBlank()) {
            try { task.setDeadline(java.time.LocalDateTime.parse(deadlineStr)); }
            catch (Exception e) { log.warn("deadline 解析失败: {}", deadlineStr); }
        }
        taskMapper.insert(task);

        // 创建仿真任务
        SimulationTask st = buildSimTask(request, task.getId(), userId);
        simTaskMapper.insert(st);

        return Map.of("taskId", task.getId(), "simTaskId", st.getId());
    }

    @Override
    @Transactional
    public Map<String, Object> updateTask(Long simTaskId, Map<String, Object> request, Long userId) {
        SimulationTask st = simTaskMapper.selectById(simTaskId);
        if (st == null) throw new BusinessException(404, "仿真任务不存在");

        // 更新 Task
        Task task = taskMapper.selectById(st.getTaskId());
        if (task == null) throw new BusinessException(404, "关联任务不存在");

        if (request.containsKey("title")) task.setTitle((String) request.get("title"));
        if (request.containsKey("subject")) task.setSubject((String) request.get("subject"));
        boolean publish = Boolean.TRUE.equals(request.get("publish"));
        if (publish) task.setStatus("PUBLISHED");
        // 更新截止日期
        if (request.containsKey("deadline")) {
            String dl = (String) request.get("deadline");
            if (dl != null && !dl.isBlank()) {
                try { task.setDeadline(java.time.LocalDateTime.parse(dl)); }
                catch (Exception e) { log.warn("deadline 解析失败: {}", dl); }
            } else {
                task.setDeadline(null);
            }
        }
        taskMapper.updateById(task);

        // 更新 SimulationTask
        if (request.get("taskJson") != null) {
            try { st.setTaskJson(objectMapper.writeValueAsString(request.get("taskJson"))); }
            catch (Exception e) { throw new BusinessException(500, "JSON 序列化失败"); }
        }
        if (request.get("initialVfs") != null) {
            try { st.setInitialVfs(objectMapper.writeValueAsString(request.get("initialVfs"))); }
            catch (Exception e) { throw new BusinessException(500, "JSON 序列化失败"); }
        }
        if (request.containsKey("mode")) st.setMode((String) request.get("mode"));
        if (request.containsKey("category")) st.setCategory((String) request.get("category"));
        if (request.get("difficulty") instanceof Number) st.setDifficulty(((Number) request.get("difficulty")).intValue());
        if (request.get("timeLimit") instanceof Number) st.setTimeLimit(((Number) request.get("timeLimit")).intValue());
        if (request.get("nodeId") != null) {
            st.setNodeId(request.get("nodeId") instanceof Number ? ((Number) request.get("nodeId")).longValue() : null);
        }
        simTaskMapper.updateById(st);

        return Map.of("taskId", task.getId(), "simTaskId", st.getId());
    }

    @Override
    @Transactional
    public void deleteTask(Long simTaskId, Long userId) {
        SimulationTask st = simTaskMapper.selectById(simTaskId);
        if (st == null) throw new BusinessException(404, "仿真任务不存在");

        // 删除关联 Task
        if (st.getTaskId() != null) {
            taskMapper.deleteById(st.getTaskId());
        }
        // 删除 SimulationTask
        simTaskMapper.deleteById(simTaskId);
    }

    private SimulationTask buildSimTask(Map<String, Object> request, Long taskId, Long userId) {
        SimulationTask st = new SimulationTask();
        st.setTaskId(taskId);
        st.setNodeId(request.get("nodeId") instanceof Number ? ((Number) request.get("nodeId")).longValue() : null);
        try {
            st.setTaskJson(objectMapper.writeValueAsString(request.get("taskJson")));
            if (request.get("initialVfs") != null) {
                st.setInitialVfs(objectMapper.writeValueAsString(request.get("initialVfs")));
            }
        } catch (Exception e) {
            throw new BusinessException(500, "JSON 序列化失败");
        }
        st.setMode((String) request.getOrDefault("mode", "practice"));
        st.setCategory((String) request.getOrDefault("category", "win7"));
        st.setDifficulty(request.get("difficulty") instanceof Number ? ((Number) request.get("difficulty")).intValue() : 1);
        st.setTimeLimit(request.get("timeLimit") instanceof Number ? ((Number) request.get("timeLimit")).intValue() : 120);
        st.setCreatedBy(userId);
        st.setSchoolId(1L);
        return st;
    }

    @Override
    public void reportProgress(Map<String, Object> body, Long studentId) {
        Long submissionId = body.get("submissionId") instanceof Number ? ((Number) body.get("submissionId")).longValue() : null;
        if (submissionId == null) return;

        SimulationRecording rec = recordingMapper.selectOne(
            new LambdaQueryWrapper<SimulationRecording>().eq(SimulationRecording::getSubmissionId, submissionId));
        if (rec == null) {
            rec = new SimulationRecording();
            rec.setSubmissionId(submissionId);
            rec.setStudentId(studentId);
            rec.setSchoolId(1L);
        }

        if (body.get("events") != null) {
            try {
                rec.setEventsJson(objectMapper.writeValueAsString(body.get("events")));
                if (body.get("events") instanceof java.util.List) {
                    rec.setEventCount(((java.util.List<?>) body.get("events")).size());
                }
            } catch (Exception ignored) { log.debug("序列化events JSON失败: {}", ignored.getMessage()); }
        }
        if (body.get("durationSeconds") instanceof Number) {
            rec.setDurationSeconds(((Number) body.get("durationSeconds")).intValue());
        }
        if (body.get("autoScore") instanceof Number) {
            rec.setAutoScore(BigDecimal.valueOf(((Number) body.get("autoScore")).doubleValue()));
        }
        if (body.get("success") instanceof Boolean) {
            rec.setSuccess((Boolean) body.get("success") ? 1 : 0);
        }

        boolean isNew = rec.getId() == null;
        if (isNew) recordingMapper.insert(rec);
        else recordingMapper.updateById(rec);

        // TODO Phase4: 完成后联动积分 + 掌握度 (CreditService / PrecisionService)
    }

    @Override
    public java.util.List<Map<String, Object>> listTasks(String category) {
        LambdaQueryWrapper<SimulationTask> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            wrapper.eq(SimulationTask::getCategory, category);
        }
        java.util.List<SimulationTask> list = simTaskMapper.selectList(wrapper);
        return buildTaskList(list);
    }

    @Override
    public java.util.Map<String, Object> getTrainingHub() {
        java.util.Map<String, Object> result = new LinkedHashMap<>();
        java.util.List<java.util.Map<String, Object>> categories = new java.util.ArrayList<>();

        // 分类定义（按 simType 维度）
        java.util.List<java.util.Map<String, String>> categoryDefs = java.util.List.of(
            java.util.Map.of("key", "win7", "name", "Windows 操作实训", "icon", "Monitor",
                "description", "掌握 Windows 7 操作系统的基本操作技能"),
            java.util.Map.of("key", "network", "name", "网络命令实训", "icon", "Connection",
                "description", "掌握 ping/tracert/ipconfig/netstat 等网络命令")
        );

        for (java.util.Map<String, String> def : categoryDefs) {
            String key = def.get("key");
            java.util.List<SimulationTask> simTasks = simTaskMapper.selectList(
                new LambdaQueryWrapper<SimulationTask>().eq(SimulationTask::getCategory, key));
            java.util.List<java.util.Map<String, Object>> tasks = buildTaskList(simTasks);

            java.util.Map<String, Object> cat = new LinkedHashMap<>();
            cat.put("key", key);
            cat.put("name", def.get("name"));
            cat.put("icon", def.get("icon"));
            cat.put("description", def.get("description"));
            cat.put("taskCount", tasks.size());
            cat.put("tasks", tasks);
            categories.add(cat);
        }

        result.put("categories", categories);
        return result;
    }

    /** 组装任务列表（从 SimulationTask 提取摘要信息） */
    private java.util.List<java.util.Map<String, Object>> buildTaskList(java.util.List<SimulationTask> list) {
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (SimulationTask st : list) {
            try {
                Task task = taskMapper.selectById(st.getTaskId());
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", st.getId());
                m.put("taskId", st.getTaskId());
                m.put("mode", st.getMode());
                m.put("category", st.getCategory());
                m.put("difficulty", st.getDifficulty());
                m.put("timeLimit", st.getTimeLimit());
                m.put("title", task != null ? task.getTitle() : "未知");
                try {
                    if (st.getTaskJson() != null) {
                        var node = objectMapper.readTree(st.getTaskJson());
                        m.put("description", node.has("description") ? node.get("description").asText() : "");
                    }
                } catch (Exception e) { m.put("description", ""); }
                result.add(m);
            } catch (Exception e) {
                log.error("buildTaskList failed for simTask {}: {}", st.getId(), e.getMessage(), e);
            }
        }
        return result;
    }

    @Override
    public Map<String, Object> getRecording(Long submissionId) {
        SimulationRecording rec = recordingMapper.selectOne(
            new LambdaQueryWrapper<SimulationRecording>().eq(SimulationRecording::getSubmissionId, submissionId));
        if (rec == null) throw new BusinessException(404, "录制数据不存在");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("submissionId", rec.getSubmissionId());
        result.put("studentId", rec.getStudentId());
        result.put("eventCount", rec.getEventCount());
        result.put("durationSeconds", rec.getDurationSeconds());
        result.put("autoScore", rec.getAutoScore());
        try {
            if (rec.getEventsJson() != null) {
                result.put("events", objectMapper.readTree(rec.getEventsJson()));
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    @Override
    public void addNotes(Long recordingId, String notes) {
        SimulationRecording rec = recordingMapper.selectById(recordingId);
        if (rec == null) throw new BusinessException(404, "录制数据不存在");
        rec.setTeacherNotes(notes);
        recordingMapper.updateById(rec);
    }

    @Override
    @Transactional
    public Map<String, Object> startExam(Long simTaskId, Long studentId) {
        SimulationTask st = simTaskMapper.selectById(simTaskId);
        if (st == null) throw new BusinessException(404, "仿真任务不存在");
        if (st.getTaskId() == null) throw new BusinessException(400, "任务未关联 taskId");

        // 复用 TaskSubmission 表：创建一条 submission 记录
        TaskSubmission sub = new TaskSubmission();
        sub.setTaskId(st.getTaskId());
        sub.setStudentId(studentId);
        sub.setStatus("SUBMITTED");
        sub.setSchoolId(1L);
        taskSubmissionMapper.insert(sub);

        log.info("仿真考试开始: simTaskId={}, taskId={}, studentId={}, submissionId={}",
            simTaskId, st.getTaskId(), studentId, sub.getId());
        return Map.of("submissionId", sub.getId());
    }
}
