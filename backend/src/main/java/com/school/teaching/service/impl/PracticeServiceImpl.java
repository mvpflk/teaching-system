package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.AiTaskStore;
import com.school.teaching.service.PracticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class PracticeServiceImpl implements PracticeService {

    @Autowired private PracticeStepMapper stepMapper;
    @Autowired private PracticeStepImageMapper imageMapper;
    @Autowired private PracticeStepFileMapper fileMapper;
    @Autowired private PracticeSubmissionMapper submissionMapper;
    @Autowired private PracticeStepGradeMapper gradeMapper;
    @Autowired private com.school.teaching.mapper.TaskMapper taskMapper;
    @Autowired private com.school.teaching.mapper.TaskSubmissionMapper taskSubmissionMapper;
    @Autowired private AiTaskStore taskStore;
    @Autowired private com.school.teaching.mapper.StudentMapper studentMapper;
    @Autowired private com.school.teaching.mapper.ClassesMapper classesMapper;
    @Autowired private com.school.teaching.mapper.UserMapper userMapper;
    @Autowired private com.school.teaching.common.practice.PracticeScoringModelSelector scoringModelSelector;
    @Autowired private com.school.teaching.mapper.PracticePlanMapper practicePlanMapper;
    @Autowired private com.school.teaching.mapper.PracticeRubricMapper practiceRubricMapper;
    @Autowired private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Autowired(required = false)
    private com.school.teaching.service.ShowcaseWorkService showcaseService;
    @org.springframework.context.annotation.Lazy
    @Autowired private PracticeServiceImpl self;

    @Value("${teaching.upload-dir:/data/uploads}")
    private String uploadDir;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // ═══════════ 步骤 CRUD ═══════════

    @Override
    @Transactional
    public Map<String, Object> createStep(Long studentId, Map<String, Object> params) {
        Long taskId = toLong(params.get("taskId"));
        if (taskId == null) throw new BusinessException(400, "taskId 不能为空");

        // 计算当前最大 step_index
        List<PracticeStep> existing = stepMapper.selectList(
            new LambdaQueryWrapper<PracticeStep>()
                .eq(PracticeStep::getTaskId, taskId)
                .eq(PracticeStep::getStudentId, studentId)
                .orderByDesc(PracticeStep::getStepIndex)
                .last("LIMIT 1"));
        int nextIndex = existing.isEmpty() ? 0 : existing.get(0).getStepIndex() + 1;

        PracticeStep step = new PracticeStep();
        step.setTaskId(taskId);
        step.setStudentId(studentId);
        step.setStepIndex(nextIndex);
        step.setTitle((String) params.getOrDefault("title", ""));
        step.setDescription((String) params.getOrDefault("description", ""));
        step.setVersion(1);

        // images JSON 数组
        Object imgs = params.get("images");
        if (imgs instanceof List<?> imgList && !imgList.isEmpty()) {
            step.setImages(toJson(imgList));
        }
        // files JSON 数组
        Object fls = params.get("files");
        if (fls instanceof List<?> fileList && !fileList.isEmpty()) {
            step.setFiles(toJson(fileList));
        }

        stepMapper.insert(step);

        // 同步明细表
        saveStepImages(step.getId(), params.get("images"));
        saveStepFiles(step.getId(), params.get("files"));

        return Map.of("stepId", step.getId(), "stepIndex", step.getStepIndex());
    }

    @Override
    @Transactional
    public void updateStep(Long stepId, Long studentId, Map<String, Object> params) {
        PracticeStep step = checkOwnStep(stepId, studentId);
        if (params.containsKey("title")) step.setTitle((String) params.get("title"));
        if (params.containsKey("description")) step.setDescription((String) params.get("description"));
        if (params.containsKey("images")) {
            Object imgs = params.get("images");
            step.setImages(imgs instanceof List<?> l && !l.isEmpty() ? toJson(l) : null);
        }
        if (params.containsKey("files")) {
            Object fls = params.get("files");
            step.setFiles(fls instanceof List<?> l && !l.isEmpty() ? toJson(l) : null);
        }
        step.setVersion(step.getVersion() != null ? step.getVersion() + 1 : 1);
        stepMapper.updateById(step);

        // 更新明细表：删旧插新
        imageMapper.delete(new LambdaQueryWrapper<PracticeStepImage>().eq(PracticeStepImage::getStepId, stepId));
        fileMapper.delete(new LambdaQueryWrapper<PracticeStepFile>().eq(PracticeStepFile::getStepId, stepId));
        saveStepImages(stepId, params.get("images"));
        saveStepFiles(stepId, params.get("files"));
    }

    @Override
    @Transactional
    public void deleteStep(Long stepId, Long studentId) {
        PracticeStep step = checkOwnStep(stepId, studentId);
        Long taskId = step.getTaskId();
        int removedIndex = step.getStepIndex();

        // 删除步骤及明细
        imageMapper.delete(new LambdaQueryWrapper<PracticeStepImage>().eq(PracticeStepImage::getStepId, stepId));
        fileMapper.delete(new LambdaQueryWrapper<PracticeStepFile>().eq(PracticeStepFile::getStepId, stepId));
        gradeMapper.delete(new LambdaQueryWrapper<PracticeStepGrade>().eq(PracticeStepGrade::getStepId, stepId));
        stepMapper.deleteById(stepId);

        // 后续步骤 index -1
        List<PracticeStep> later = stepMapper.selectList(
            new LambdaQueryWrapper<PracticeStep>()
                .eq(PracticeStep::getTaskId, taskId)
                .eq(PracticeStep::getStudentId, studentId)
                .gt(PracticeStep::getStepIndex, removedIndex)
                .orderByAsc(PracticeStep::getStepIndex));
        for (PracticeStep s : later) {
            s.setStepIndex(s.getStepIndex() - 1);
            stepMapper.updateById(s);
        }
    }

    @Override
    @Transactional
    public void reorderSteps(Long taskId, Long studentId, List<Long> stepIds) {
        List<PracticeStep> steps = stepMapper.selectList(
            new LambdaQueryWrapper<PracticeStep>()
                .eq(PracticeStep::getTaskId, taskId)
                .eq(PracticeStep::getStudentId, studentId));
        if (steps.size() != stepIds.size()) {
            throw new BusinessException(400, "步骤数量不匹配");
        }
        Set<Long> existingIds = new HashSet<>();
        for (PracticeStep s : steps) existingIds.add(s.getId());
        for (Long id : stepIds) {
            if (!existingIds.contains(id)) throw new BusinessException(400, "步骤 " + id + " 不属于当前学生");
        }
        Map<Long, PracticeStep> stepMap = new HashMap<>();
        for (PracticeStep s : steps) stepMap.put(s.getId(), s);
        for (int i = 0; i < stepIds.size(); i++) {
            PracticeStep s = stepMap.get(stepIds.get(i));
            if (s != null && !s.getStepIndex().equals(i)) {
                s.setStepIndex(i);
                stepMapper.updateById(s);
            }
        }
    }

    @Override
    @Transactional
    public List<Map<String, Object>> listSteps(Long taskId, Long studentId) {
        List<PracticeStep> steps = stepMapper.selectList(
            new LambdaQueryWrapper<PracticeStep>()
                .eq(PracticeStep::getTaskId, taskId)
                .eq(PracticeStep::getStudentId, studentId)
                .orderByAsc(PracticeStep::getStepIndex));

        // 如果学生没有个人步骤，从模板拷贝一份
        if (steps.isEmpty()) {
            List<PracticeStep> templates = stepMapper.selectList(
                new LambdaQueryWrapper<PracticeStep>()
                    .eq(PracticeStep::getTaskId, taskId)
                    .isNull(PracticeStep::getStudentId)
                    .orderByAsc(PracticeStep::getStepIndex));
            if (!templates.isEmpty()) {
                for (PracticeStep tpl : templates) {
                    PracticeStep copy = new PracticeStep();
                    copy.setTaskId(taskId);
                    copy.setStudentId(studentId);
                    copy.setStepIndex(tpl.getStepIndex());
                    copy.setTitle(tpl.getTitle());
                    copy.setDescription(tpl.getDescription());
                    copy.setImages(tpl.getImages());
                    copy.setFiles(tpl.getFiles());
                    copy.setVersion(1);
                    stepMapper.insert(copy);
                    steps.add(copy);
                }
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (PracticeStep s : steps) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("stepId", s.getId());
            m.put("taskId", s.getTaskId());
            m.put("stepIndex", s.getStepIndex());
            m.put("title", s.getTitle());
            m.put("description", s.getDescription());
            m.put("images", parseJsonList(s.getImages()));
            m.put("files", parseJsonList(s.getFiles()));
            m.put("version", s.getVersion());
            result.add(m);
        }

        // 附加评分信息（学生端可见）
        PracticeSubmission sub = submissionMapper.selectOne(
            new LambdaQueryWrapper<PracticeSubmission>()
                .eq(PracticeSubmission::getTaskId, taskId)
                .eq(PracticeSubmission::getStudentId, studentId));
        if (sub != null && "GRADED".equals(sub.getStatus())) {
            List<PracticeStepGrade> grades = gradeMapper.selectList(
                new LambdaQueryWrapper<PracticeStepGrade>()
                    .eq(PracticeStepGrade::getSubmissionId, sub.getId()));
            Map<Long, PracticeStepGrade> gradeMap = new HashMap<>();
            for (PracticeStepGrade g : grades) gradeMap.put(g.getStepId(), g);
            for (Map<String, Object> m : result) {
                Long sid = (Long) m.get("stepId");
                PracticeStepGrade g = gradeMap.get(sid);
                if (g != null) {
                    m.put("stepScore", g.getStepScore());
                    m.put("stepComment", g.getStepComment());
                }
            }
            // 构建评分结果 Map（包含维度得分供雷达图使用）
            Map<String, Object> subMap = new LinkedHashMap<>();
            subMap.put("status", "GRADED");
            subMap.put("overallScore", sub.getOverallScore());
            subMap.put("overallComment", sub.getOverallComment() != null ? sub.getOverallComment() : "");

            try {
                com.school.teaching.entity.Task task = taskMapper.selectById(taskId);
                if (task != null) {
                    PracticePlan plan = practicePlanMapper.selectOne(
                        new LambdaQueryWrapper<PracticePlan>()
                            .eq(PracticePlan::getTaskId, taskId).last("LIMIT 1"));
                    if (plan != null) {
                        List<com.school.teaching.entity.PracticeRubric> rubrics = practiceRubricMapper.selectList(
                            new LambdaQueryWrapper<com.school.teaching.entity.PracticeRubric>()
                                .eq(com.school.teaching.entity.PracticeRubric::getPlanId, plan.getId()));
                        List<PracticeStepGrade> savedGrades = gradeMapper.selectList(
                            new LambdaQueryWrapper<PracticeStepGrade>()
                                .eq(PracticeStepGrade::getSubmissionId, sub.getId()));
                        com.school.teaching.common.practice.ScoringModelType modelType;
                        try { modelType = com.school.teaching.common.practice.ScoringModelType.valueOf(plan.getScoringModel()); }
                        catch (Exception e) { modelType = com.school.teaching.common.practice.ScoringModelType.DUAL_DIMENSION; }
                        Map<String, Object> computed = scoringModelSelector.get(modelType).compute(savedGrades, rubrics, sub);
                        if (computed.containsKey("skillScore")) subMap.put("skillScore", computed.get("skillScore"));
                        if (computed.containsKey("profScore")) subMap.put("profScore", computed.get("profScore"));
                        if (computed.containsKey("valueScore")) subMap.put("valueScore", computed.get("valueScore"));
                        if (computed.containsKey("innovScore")) subMap.put("innovScore", computed.get("innovScore"));
                        if (computed.containsKey("teamScore")) subMap.put("teamScore", computed.get("teamScore"));
                        if (computed.containsKey("processScore")) subMap.put("processScore", computed.get("processScore"));
                        if (computed.containsKey("productScore")) subMap.put("productScore", computed.get("productScore"));
                    }
                }
            } catch (Exception ignored) { log.warn("评分计算失败: {}", ignored.getMessage()); }

            result.add(0, Map.of("_submission", subMap));
        }

        return result;
    }

    // ═══════════ 提交/撤回 ═══════════

    @Override
    @Transactional
    public Map<String, Object> submit(Long taskId, Long studentId) {
        // 检查是否已提交
        PracticeSubmission existing = submissionMapper.selectOne(
            new LambdaQueryWrapper<PracticeSubmission>()
                .eq(PracticeSubmission::getTaskId, taskId)
                .eq(PracticeSubmission::getStudentId, studentId));
        if (existing != null && !"RETURNED".equals(existing.getStatus())) {
            throw new BusinessException(400, "已提交，请勿重复操作");
        }

        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setStatus("SUBMITTED");
            existing.setSubmittedAt(now);
            existing.setOverallScore(null);
            existing.setOverallComment(null);
            existing.setGradedAt(null);
            submissionMapper.updateById(existing);
            syncTaskSubmission(taskId, studentId, "SUBMITTED", null);
            return Map.of("submissionId", existing.getId(), "status", "SUBMITTED");
        }

        PracticeSubmission sub = new PracticeSubmission();
        sub.setTaskId(taskId);
        sub.setStudentId(studentId);
        sub.setSubmittedAt(now);
        sub.setStatus("SUBMITTED");
        submissionMapper.insert(sub);
        syncTaskSubmission(taskId, studentId, "SUBMITTED", null);
        return Map.of("submissionId", sub.getId(), "status", "SUBMITTED");
    }

    @Override
    @Transactional
    public void withdraw(Long taskId, Long studentId) {
        PracticeSubmission sub = submissionMapper.selectOne(
            new LambdaQueryWrapper<PracticeSubmission>()
                .eq(PracticeSubmission::getTaskId, taskId)
                .eq(PracticeSubmission::getStudentId, studentId));
        if (sub == null) throw new BusinessException(404, "未找到提交记录");
        if ("GRADED".equals(sub.getStatus())) {
            throw new BusinessException(400, "已评分，无法撤回");
        }
        if (!"SUBMITTED".equals(sub.getStatus())) {
            throw new BusinessException(400, "当前状态不可撤回");
        }
        sub.setStatus("PENDING");
        sub.setSubmittedAt(null);
        submissionMapper.updateById(sub);
        // A1: 撤回时同步 task_submissions 表，保持教师看板数据准确
        syncTaskSubmission(taskId, studentId, "PENDING", null);
    }

    // ═══════════ 教师评分 ═══════════

    @Override
    @Transactional
    public Map<String, Object> grade(Long submissionId, BigDecimal overallScore,
        String overallComment, List<Map<String, Object>> stepGrades) {
        PracticeSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BusinessException(404, "提交记录不存在");
        if ("GRADED".equals(sub.getStatus())) throw new BusinessException(409, "已评分");

        sub.setStatus("GRADED");
        sub.setOverallScore(overallScore);
        sub.setOverallComment(overallComment);
        sub.setGradedAt(LocalDateTime.now());
        submissionMapper.updateById(sub);

        if (stepGrades != null) {
            for (Map<String, Object> sg : stepGrades) {
                Long stepId = toLong(sg.get("stepId"));
                if (stepId == null) continue;
                BigDecimal stepScore = sg.get("stepScore") instanceof Number n
                    ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO;
                String stepComment = (String) sg.getOrDefault("stepComment", "");

                PracticeStepGrade existing = gradeMapper.selectOne(
                    new LambdaQueryWrapper<PracticeStepGrade>()
                        .eq(PracticeStepGrade::getSubmissionId, submissionId)
                        .eq(PracticeStepGrade::getStepId, stepId));
                if (existing != null) {
                    existing.setStepScore(stepScore);
                    existing.setStepComment(stepComment);
                    gradeMapper.updateById(existing);
                } else {
                    PracticeStepGrade grade = new PracticeStepGrade();
                    grade.setSubmissionId(submissionId);
                    grade.setStepId(stepId);
                    grade.setStepScore(stepScore);
                    grade.setStepComment(stepComment);
                    gradeMapper.insert(grade);
                }
            }
        }

        // 评分引擎自动计算总分
        List<PracticeStepGrade> savedGrades = gradeMapper.selectList(
            new LambdaQueryWrapper<PracticeStepGrade>()
                .eq(PracticeStepGrade::getSubmissionId, submissionId));
        if (sub.getTaskId() != null) {
            com.school.teaching.entity.Task task = taskMapper.selectById(sub.getTaskId());
            if (task != null) {
                PracticePlan plan = practicePlanMapper.selectOne(
                    new LambdaQueryWrapper<PracticePlan>()
                        .eq(PracticePlan::getTaskId, task.getId()).last("LIMIT 1"));
                if (plan != null) {
                    List<com.school.teaching.entity.PracticeRubric> rubrics = practiceRubricMapper.selectList(
                        new LambdaQueryWrapper<com.school.teaching.entity.PracticeRubric>()
                            .eq(com.school.teaching.entity.PracticeRubric::getPlanId, plan.getId()));

                    com.school.teaching.common.practice.ScoringModelType modelType;
                    try { modelType = com.school.teaching.common.practice.ScoringModelType.valueOf(plan.getScoringModel()); }
                    catch (Exception e) { modelType = com.school.teaching.common.practice.ScoringModelType.DUAL_DIMENSION; }

                    Map<String, Object> computed = scoringModelSelector.get(modelType)
                        .compute(savedGrades, rubrics, sub);

                    // A7: 仅当教师未传分（null）时自动计算，有意给 0 分时尊重教师决定
                    if (overallScore == null) {
                        Number cs = (Number) computed.get("overallScore");
                        BigDecimal autoScore = BigDecimal.valueOf(cs.doubleValue());
                        sub.setOverallScore(autoScore);
                        submissionMapper.updateById(sub);
                    }
                }
            }
        }

        // 发布积分事件
        try {
            com.school.teaching.entity.Task task = sub.getTaskId() != null ? taskMapper.selectById(sub.getTaskId()) : null;
            if (task != null) {
                eventPublisher.publishEvent(com.school.teaching.event.TaskEvent.graded(this,
                    sub.getTaskId(), com.school.teaching.common.TaskCategory.valueOf(task.getTaskType()),
                    sub.getStudentId(), null,
                    Map.of("submissionId", submissionId, "score",
                        sub.getOverallScore() != null ? sub.getOverallScore().toString() : "0")));
            }
        } catch (Exception ignored) { log.warn("积分事件发布失败: submissionId={}", submissionId); }

        // 推荐到展示墙（总分 ≥ 90）
        if (showcaseService != null && sub.getOverallScore() != null
                && sub.getOverallScore().compareTo(BigDecimal.valueOf(90)) >= 0) {
            try {
                com.school.teaching.entity.Task t = sub.getTaskId() != null ? taskMapper.selectById(sub.getTaskId()) : null;
                if (t != null) {
                    com.school.teaching.entity.Student st = studentMapper.selectById(sub.getStudentId());
                    String studentName = null;
                    if (st != null && st.getUserId() != null) {
                        com.school.teaching.entity.User u = userMapper.selectById(st.getUserId());
                        if (u != null) studentName = u.getRealName();
                    }
                    Long classId = st != null ? st.getClassId() : null;
                    Map<String, Object> showcaseReq = new HashMap<>();
                    showcaseReq.put("title", t.getTitle() + " — " + (studentName != null ? studentName : "学生"));
                    showcaseReq.put("sourceType", "PRACTICE");
                    showcaseReq.put("sourceId", t.getId());
                    showcaseReq.put("studentId", sub.getStudentId());
                    showcaseReq.put("subject", t.getSubject());
                    showcaseReq.put("teacherComment", overallComment != null ? overallComment : "");
                    showcaseReq.put("showScope", "CLASS");
                    if (classId != null) showcaseReq.put("classId", classId);
                    showcaseService.recommendWork(showcaseReq);
                }
            } catch (Exception ignored) { log.warn("展示墙推荐失败: {}", ignored.getMessage()); }
        }

        // 同步到统一任务提交表（教师任务列表/待批计数使用）
        syncTaskSubmission(sub.getTaskId(), sub.getStudentId(), "GRADED", sub.getOverallScore());

        return Map.of("submissionId", submissionId, "status", "GRADED");
    }

    /** 同步写入 task_submissions 表，确保任务列表和待批计数能正确显示 */
    private void syncTaskSubmission(Long taskId, Long studentId, String status, BigDecimal score) {
        try {
            com.school.teaching.entity.TaskSubmission ts = taskSubmissionMapper.selectOne(
                new LambdaQueryWrapper<com.school.teaching.entity.TaskSubmission>()
                    .eq(com.school.teaching.entity.TaskSubmission::getTaskId, taskId)
                    .eq(com.school.teaching.entity.TaskSubmission::getStudentId, studentId));
            if (ts == null) {
                ts = new com.school.teaching.entity.TaskSubmission();
                ts.setTaskId(taskId);
                ts.setStudentId(studentId);
                ts.setStatus(status != null ? status : "SUBMITTED");
                ts.setScore(score);
                ts.setSubmittedAt(LocalDateTime.now());
                taskSubmissionMapper.insert(ts);
            } else {
                if (status != null) ts.setStatus(status);
                if (score != null) ts.setScore(score);
                if ("GRADED".equals(status)) ts.setGradedAt(LocalDateTime.now());
                taskSubmissionMapper.updateById(ts);
            }
        } catch (Exception e) {
            // A2: 记录同步失败但不影响实训主流程
            log.warn("同步 task_submissions 失败 taskId={} studentId={} status={}", taskId, studentId, status, e);
        }
    }

    @Override
    public List<Map<String, Object>> getSubmissions(Long taskId) {
        List<PracticeSubmission> subs = submissionMapper.selectList(
            new LambdaQueryWrapper<PracticeSubmission>()
                .eq(PracticeSubmission::getTaskId, taskId)
                .orderByDesc(PracticeSubmission::getSubmittedAt));
        List<Map<String, Object>> result = new ArrayList<>();
        for (PracticeSubmission sub : subs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", sub.getId());
            m.put("studentId", sub.getStudentId());
            m.put("status", sub.getStatus() != null ? sub.getStatus() : "PENDING");
            m.put("overallScore", sub.getOverallScore());
            com.school.teaching.entity.Student st = studentMapper.selectById(sub.getStudentId());
            String studentName = null;
            if (st != null && st.getUserId() != null) {
                com.school.teaching.entity.User u = userMapper.selectById(st.getUserId());
                if (u != null) studentName = u.getRealName();
            }
            m.put("studentName", studentName != null ? studentName : "学生" + sub.getStudentId());
            result.add(m);
        }
        return result;
    }

    // ═══════════ 工具方法 ═══════════

    private PracticeStep checkOwnStep(Long stepId, Long studentId) {
        PracticeStep step = stepMapper.selectById(stepId);
        if (step == null) throw new BusinessException(404, "步骤不存在");
        if (!step.getStudentId().equals(studentId)) throw new BusinessException(403, "无权操作");
        return step;
    }

    private void saveStepImages(Long stepId, Object images) {
        if (!(images instanceof List<?> list)) return;
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            String url = item instanceof Map<?,?> m ? (String) m.get("url") : item.toString();
            PracticeStepImage img = new PracticeStepImage();
            img.setStepId(stepId);
            img.setImageUrl(url);
            img.setOrderIndex(i);
            imageMapper.insert(img);
        }
    }

    @SuppressWarnings("unchecked")
    private void saveStepFiles(Long stepId, Object files) {
        if (!(files instanceof List<?> list)) return;
        for (Object item : list) {
            if (!(item instanceof Map)) continue;
            Map<String, Object> m = (Map<String, Object>) item;
            PracticeStepFile f = new PracticeStepFile();
            f.setStepId(stepId);
            f.setFileUrl((String) m.get("url"));
            f.setOriginalName((String) m.get("name"));
            Object size = m.get("size");
            f.setFileSize(size instanceof Number n ? n.longValue() : null);
            fileMapper.insert(f);
        }
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "[]"; }
    }

    private List<?> parseJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, List.class); }
        catch (Exception e) { return List.of(); }
    }

    private Long toLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) {
            try { return Long.parseLong(s); } catch (Exception ignored) { log.debug("ID解析失败: {}", s); }
        }
        return null;
    }

    // ═══════════ ZIP 下载 ═══════════

    @Override
    public String startDownload(Long taskId, Long classId, List<Long> studentIds) {
        String tId = taskStore.create(com.school.teaching.common.AsyncTaskType.ZIP_EXPORT, 300, null);
        self.generateDownloadZip(tId, taskId, classId, studentIds);
        return tId;
    }

    @Override
    public AiTaskStore.TaskEntry getDownloadStatus(String taskId) {
        AiTaskStore.TaskEntry entry = taskStore.get(taskId);
        if (entry == null) throw new BusinessException(404, "任务不存在或已过期");
        return entry;
    }

    @Async
    public void generateDownloadZip(String taskId, Long taskIdParam, Long classId, List<Long> studentIds) {
        taskStore.markRunning(taskId);
        try {
            // 1. 查班级信息
            Classes cls = classesMapper.selectById(classId);
            String className = cls != null && cls.getClassName() != null ? cls.getClassName() : "class_" + classId;

            // 2. 查提交记录
            LambdaQueryWrapper<PracticeSubmission> subW = new LambdaQueryWrapper<PracticeSubmission>()
                .eq(PracticeSubmission::getTaskId, taskIdParam);
            if (studentIds != null && !studentIds.isEmpty())
                subW.in(PracticeSubmission::getStudentId, studentIds);
            subW.eq(PracticeSubmission::getStatus, "SUBMITTED");
            List<PracticeSubmission> submissions = submissionMapper.selectList(subW);
            if (submissions.isEmpty()) {
                taskStore.fail(taskId, "无已提交的学生");
                return;
            }

            // 3. 创建临时ZIP
            Path tmpDir = Files.createTempDirectory("practice_");
            Path zipPath = tmpDir.resolve(className + "_实训_" + taskIdParam + ".zip");

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                for (PracticeSubmission sub : submissions) {
                    addStudentToZip(zos, className, sub, taskIdParam);
                }
            }

            // 4. 移到downloads目录
            Path downloadDir = Paths.get(uploadDir, "downloads");
            Files.createDirectories(downloadDir);
            String filename = "practice_" + taskIdParam + "_" + UUID.randomUUID().toString().substring(0, 8) + ".zip";
            Path finalPath = downloadDir.resolve(filename);
            Files.move(zipPath, finalPath);
            // 清理临时目录
            try { Files.deleteIfExists(tmpDir); } catch (Exception ignored) { log.debug("临时目录清理失败", ignored); }

            taskStore.complete(taskId, Map.of("downloadUrl", "/api/uploads/downloads/" + filename,
                "filename", filename, "studentCount", submissions.size()));

        } catch (Exception e) {
            taskStore.fail(taskId, "生成失败: " + e.getMessage());
        }
    }

    private void addStudentToZip(ZipOutputStream zos, String className, PracticeSubmission sub, Long taskId) throws Exception {
        // 学生信息
        Student st = studentMapper.selectById(sub.getStudentId());
        String studentName = (st != null && st.getUserId() != null)
            ? userMapper.selectById(st.getUserId()).getRealName()
            : "student_" + sub.getStudentId();
        if (studentName == null) studentName = "student_" + sub.getStudentId();

        // 步骤列表
        List<PracticeStep> steps = stepMapper.selectList(
            new LambdaQueryWrapper<PracticeStep>()
                .eq(PracticeStep::getTaskId, taskId)
                .eq(PracticeStep::getStudentId, sub.getStudentId())
                .orderByAsc(PracticeStep::getStepIndex));

        for (PracticeStep step : steps) {
            String stepDir = className + "/" + studentName + "/"
                + pad(step.getStepIndex()) + "_" + safeName(step.getTitle()) + "/";

            // 描述.txt
            String desc = (step.getTitle() != null ? step.getTitle() : "") + "\n\n"
                + (step.getDescription() != null ? step.getDescription() : "");
            zos.putNextEntry(new ZipEntry(stepDir + "描述.txt"));
            zos.write(desc.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zos.closeEntry();

            // 图片（从本地路径读取）
            List<PracticeStepImage> images = imageMapper.selectList(
                new LambdaQueryWrapper<PracticeStepImage>()
                    .eq(PracticeStepImage::getStepId, step.getId())
                    .orderByAsc(PracticeStepImage::getOrderIndex));
            int imgIdx = 1;
            for (PracticeStepImage img : images) {
                byte[] data = readFileFromUrl(img.getImageUrl());
                if (data != null) {
                    zos.putNextEntry(new ZipEntry(stepDir + pad(imgIdx) + ".jpg"));
                    zos.write(data);
                    zos.closeEntry();
                    imgIdx++;
                }
            }

            // 附件
            List<PracticeStepFile> files = fileMapper.selectList(
                new LambdaQueryWrapper<PracticeStepFile>()
                    .eq(PracticeStepFile::getStepId, step.getId()));
            for (PracticeStepFile f : files) {
                byte[] data = readFileFromUrl(f.getFileUrl());
                if (data != null) {
                    String fname = f.getOriginalName() != null ? f.getOriginalName() : "file";
                    zos.putNextEntry(new ZipEntry(stepDir + fname));
                    zos.write(data);
                    zos.closeEntry();
                }
            }
        }
    }

    /** 从URL路径读取本地文件内容 */
    private byte[] readFileFromUrl(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            // URL格式: /api/uploads/xxx/yyy.ext → 本地路径: uploadDir/xxx/yyy.ext
            String relativePath = url;
            if (relativePath.startsWith("/api/uploads/"))
                relativePath = relativePath.substring("/api/uploads/".length());
            else if (relativePath.startsWith("/uploads/"))
                relativePath = relativePath.substring("/uploads/".length());
            // S1: 预防路径穿越攻击 — normalize 后校验仍在上传目录内
            Path filePath = Paths.get(uploadDir, relativePath).normalize().toAbsolutePath();
            if (!filePath.startsWith(Paths.get(uploadDir).normalize().toAbsolutePath())) {
                log.warn("拒绝可疑文件路径: {}", relativePath);
                return null;
            }
            if (Files.exists(filePath)) return Files.readAllBytes(filePath);
        } catch (Exception e) {
            log.warn("读取文件失败 url={}: {}", url, e.getMessage());
        }
        return null;
    }

    private static String pad(int n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }

    private static String safeName(String s) {
        if (s == null || s.isBlank()) return "step";
        return s.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "_");
    }
}
