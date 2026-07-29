package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.annotation.Audit;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.TaskCrudService;
import com.school.teaching.service.SubmitLockService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.school.teaching.utils.ScoreUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskCrudServiceImpl implements TaskCrudService {

    private final TaskMapper taskMapper;
    private final TaskQuestionMapper taskQuestionMapper;
    private final TaskGroupVisibilityMapper taskGroupVisibilityMapper;
    private final QuestionBankMapper questionBankMapper;
    private final SurveyQuestionMapper surveyQuestionMapper;
    private final StudentAnswerMapper studentAnswerMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final TaskSubmissionMapper submissionMapper;
    private final SubmitLockService submitLockService;
    private final TeacherMapper teacherMapper;

    private static final Logger log = LoggerFactory.getLogger(TaskCrudServiceImpl.class);

    @Override
    public Task getById(Long id) {
        Task t = taskMapper.selectById(id);
        if (t == null) throw new BusinessException(404, "任务不存在");
        loadGroupIds(t);
        loadQuestionIds(t);
        if (t.getTargetId() != null) {
            try {
                t.setTargetIds(java.util.List.of(Long.valueOf(t.getTargetId())));
            } catch (Exception ignored) { /* targetId 为非数字时跳过 targetIds 回填 */ }
        }
        return t;
    }

    @Override
    @Transactional
    @CacheEvict(value = "task_list", allEntries = true)
    public Task create(Task task) {
        if (task.getSchoolId() == null) task.setSchoolId(SecurityUtils.getCurrentSchoolId());
        if (task.getStageId() == null) task.setStageId(SecurityUtils.getCurrentStageId());
        if (task.getTeacherId() == null) task.setTeacherId(resolveTeacherId());
        task.setStatus("DRAFT");
        if (task.getSurveySchema() != null && task.getSurveySchema().isBlank()) task.setSurveySchema(null);
        taskMapper.insert(task);
        
        if (task.getGroupIds() != null && !task.getGroupIds().isEmpty()) {
            saveGroupIds(task.getId(), task.getGroupIds());
        }
        
        if (task.getQuestionIds() != null && !task.getQuestionIds().isEmpty()) {
            List<QuestionBank> questions = questionBankMapper.selectBatchIds(task.getQuestionIds());
            Map<Long, QuestionBank> qMap = questions.stream()
                .collect(Collectors.toMap(QuestionBank::getId, q -> q));
            List<TaskQuestion> tqList = new ArrayList<>();
            int order = 0;
            for (Long qid : task.getQuestionIds()) {
                QuestionBank q = qMap.get(qid);
                if (q == null) continue;
                TaskQuestion tq = new TaskQuestion();
                tq.setTaskId(task.getId());
                tq.setQuestionId(qid);
                tq.setQuestionType(q.getQuestionType());
                tq.setSortOrder(order++);
                tq.setScore(scoreFromPresets(q.getQuestionType(), task.getScorePresets()));
                tq.setCategoryId(q.getCategoryId());
                tqList.add(tq);
            }
            if (!tqList.isEmpty()) {
                for (TaskQuestion tq : tqList) taskQuestionMapper.insert(tq);
            }
            recalcTaskTotalScore(task.getId());
        }
        
        if ("SURVEY".equals(task.getTaskType()) && task.getSurveySchema() != null && !task.getSurveySchema().isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                var schema = om.readValue(task.getSurveySchema(), new com.fasterxml.jackson.core.type.TypeReference<java.util.List<com.fasterxml.jackson.databind.JsonNode>>() {});
                int order = 0;
                for (com.fasterxml.jackson.databind.JsonNode q : schema) {
                    SurveyQuestion sq = new SurveyQuestion();
                    sq.setTaskId(task.getId()); sq.setQuestionOrder(order++);
                    sq.setQuestionType(q.get("type").asText());
                    sq.setTitle(q.get("label").asText());
                    sq.setRequired(q.has("required") ? q.get("required").asInt() : 1);
                    if (q.has("options")) sq.setOptions(q.get("options").toString());
                    surveyQuestionMapper.insert(sq);
                }
            } catch (Exception e) {
                log.warn("问卷题目迁移失败: {}", e.getMessage());
            }
        }
        return task;
    }

    @Override
    @Transactional
    @CacheEvict(value = "task_list", allEntries = true)
    public Task update(Long id, Task data) {
        Task t = getById(id);
        if (!"DRAFT".equals(t.getStatus())) throw new BusinessException(409, "仅草稿状态可编辑");
        var editLock = submitLockService.tryLockTaskEdit(id);
        if (editLock == null) throw new BusinessException(409, "任务正在被其他操作编辑，请稍后重试");
        try {
            data.setId(id);
            if (data.getSurveySchema() != null && data.getSurveySchema().isBlank()) data.setSurveySchema(null);
            taskMapper.updateById(data);
            if (data.getGroupIds() != null) {
                saveGroupIds(id, data.getGroupIds());
            }
            if (data.getQuestionIds() != null) {
                taskQuestionMapper.delete(
                    new LambdaQueryWrapper<TaskQuestion>().eq(TaskQuestion::getTaskId, id));
                if (!data.getQuestionIds().isEmpty()) {
                    List<QuestionBank> questions = questionBankMapper.selectBatchIds(data.getQuestionIds());
                    Map<Long, QuestionBank> qMap = questions.stream()
                        .collect(Collectors.toMap(QuestionBank::getId, q -> q));
                    int order = 0;
                    for (Long qid : data.getQuestionIds()) {
                        QuestionBank q = qMap.get(qid);
                        if (q == null) continue;
                        TaskQuestion tq = new TaskQuestion();
                        tq.setTaskId(id);
                        tq.setQuestionId(qid);
                        tq.setQuestionType(q.getQuestionType());
                        tq.setSortOrder(order++);
                        tq.setScore(scoreFromPresets(q.getQuestionType(), data.getScorePresets()));
                        taskQuestionMapper.insert(tq);
                    }
                }
                recalcTaskTotalScore(id);
            }
            if ("SURVEY".equals(data.getTaskType()) && data.getSurveySchema() != null && !data.getSurveySchema().isBlank()) {
                surveyQuestionMapper.delete(
                    new LambdaQueryWrapper<SurveyQuestion>().eq(SurveyQuestion::getTaskId, id));
                try {
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    var schema = om.readValue(data.getSurveySchema(), new com.fasterxml.jackson.core.type.TypeReference<java.util.List<com.fasterxml.jackson.databind.JsonNode>>() {});
                    int order = 0;
                    for (com.fasterxml.jackson.databind.JsonNode q : schema) {
                        SurveyQuestion sq = new SurveyQuestion();
                        sq.setTaskId(id); sq.setQuestionOrder(order++);
                        sq.setQuestionType(q.get("type").asText());
                        sq.setTitle(q.get("label").asText());
                        sq.setRequired(q.has("required") ? q.get("required").asInt() : 1);
                        if (q.has("options")) sq.setOptions(q.get("options").toString());
                        surveyQuestionMapper.insert(sq);
                    }
                } catch (Exception e) {
                    log.warn("问卷题目重新同步失败: {}", e.getMessage());
                }
            }
            return getById(id);
        } finally {
            editLock.close();
        }
    }

    @Override
    @Transactional
    @Audit("DELETE_TASK")
    @CacheEvict(value = "task_list", allEntries = true)
    public void delete(Long id) {
        Task t = getById(id);
        if (!"DRAFT".equals(t.getStatus()) && !"CLOSED".equals(t.getStatus()))
            throw new BusinessException(409, "仅草稿或已关闭的任务可删除");
        Long gradedCount = submissionMapper.selectCount(new LambdaQueryWrapper<TaskSubmission>()
            .eq(TaskSubmission::getTaskId, id).eq(TaskSubmission::getStatus, "GRADED"));
        if (gradedCount != null && gradedCount > 0)
            throw new BusinessException(409, "存在" + gradedCount + "份已评分提交，不可删除");
        cascadeDelete(id);
        taskMapper.deleteById(id);
    }

    private void cascadeDelete(Long taskId) {
        taskGroupVisibilityMapper.delete(new LambdaQueryWrapper<TaskGroupVisibility>().eq(TaskGroupVisibility::getTaskId, taskId));
        studentAnswerMapper.delete(new LambdaQueryWrapper<StudentAnswer>().eq(StudentAnswer::getTaskId, taskId));
        List<TaskQuestion> tqList = taskQuestionMapper.selectList(
            new LambdaQueryWrapper<TaskQuestion>().eq(TaskQuestion::getTaskId, taskId));
        if (!tqList.isEmpty()) {
            List<Long> qIds = tqList.stream().map(TaskQuestion::getQuestionId).toList();
            List<TaskSubmission> subs = submissionMapper.selectList(
                new LambdaQueryWrapper<TaskSubmission>().eq(TaskSubmission::getTaskId, taskId));
            var studentIds = subs.stream().map(TaskSubmission::getStudentId).distinct().toList();
            if (!studentIds.isEmpty()) {
                wrongQuestionMapper.delete(new LambdaQueryWrapper<WrongQuestion>()
                    .in(WrongQuestion::getQuestionId, qIds)
                    .in(WrongQuestion::getStudentId, studentIds));
            }
        }
        taskQuestionMapper.delete(new LambdaQueryWrapper<TaskQuestion>().eq(TaskQuestion::getTaskId, taskId));
        surveyQuestionMapper.delete(new LambdaQueryWrapper<SurveyQuestion>().eq(SurveyQuestion::getTaskId, taskId));
        submissionMapper.delete(new LambdaQueryWrapper<TaskSubmission>().eq(TaskSubmission::getTaskId, taskId));
    }

    @Override
    @Transactional
    @Audit("COPY_TASK")
    @CacheEvict(value = "task_list", allEntries = true)
    public Task copyTask(Long sourceTaskId, Long userId) {
        Task source = getById(sourceTaskId);
        if (source == null) throw new BusinessException(404, "原任务不存在");

        Task copy = new Task();
        copy.setSchoolId(source.getSchoolId());
        copy.setStageId(source.getStageId());
        copy.setTitle((source.getTitle() != null ? source.getTitle() : "未命名") + " - 副本");
        copy.setDescription(source.getDescription());
        copy.setSubject(source.getSubject());
        copy.setGradeId(source.getGradeId());
        copy.setTeacherId(resolveTeacherId(userId));
        copy.setScoreType(source.getScoreType());
        copy.setTaskType(source.getTaskType());
        copy.setTargetType(source.getTargetType());
        copy.setTargetId(source.getTargetId());
        copy.setViewScope(source.getViewScope());
        copy.setDeadline(source.getDeadline());
        copy.setStatus("DRAFT");
        copy.setTaskConfig(source.getTaskConfig());
        copy.setNotifyParents(source.getNotifyParents());
        copy.setAllowResubmit(source.getAllowResubmit());
        copy.setIsRequired(source.getIsRequired());
        copy.setWuyuTag(source.getWuyuTag());
        copy.setIsCompetitionMode(source.getIsCompetitionMode());
        copy.setAutoWrongbook(source.getAutoWrongbook());
        copy.setIsForced(source.getIsForced());
        copy.setSurveySchema(source.getSurveySchema());
        copy.setTermId(source.getTermId());
        copy.setRubricId(source.getRubricId());
        copy.setAllowCustomSteps(source.getAllowCustomSteps());
        copy.setReferenceImages(source.getReferenceImages());
        copy.setScheduledPublishAt(source.getScheduledPublishAt());
        copy.setDifficultyLevel(source.getDifficultyLevel());
        copy.setPassRate(source.getPassRate());
        copy.setMaxAttempts(source.getMaxAttempts());
        copy.setRetakeDeadlineHours(source.getRetakeDeadlineHours());
        copy.setPassMode(source.getPassMode());
        copy.setSourceTaskId(source.getId());
        taskMapper.insert(copy);

        List<TaskGroupVisibility> srcGroupVis = taskGroupVisibilityMapper.selectList(
                new LambdaQueryWrapper<TaskGroupVisibility>()
                        .eq(TaskGroupVisibility::getTaskId, sourceTaskId));
        for (TaskGroupVisibility gv : srcGroupVis) {
            TaskGroupVisibility newGv = new TaskGroupVisibility();
            newGv.setTaskId(copy.getId());
            newGv.setGroupId(gv.getGroupId());
            taskGroupVisibilityMapper.insert(newGv);
        }

        List<TaskQuestion> questions = taskQuestionMapper.selectList(
                new LambdaQueryWrapper<TaskQuestion>().eq(TaskQuestion::getTaskId, sourceTaskId)
                        .orderByAsc(TaskQuestion::getSortOrder));
        for (TaskQuestion q : questions) {
            TaskQuestion tq = new TaskQuestion();
            tq.setTaskId(copy.getId());
            tq.setQuestionId(q.getQuestionId());
            tq.setQuestionType(q.getQuestionType());
            tq.setSortOrder(q.getSortOrder());
            tq.setScore(q.getScore());
            taskQuestionMapper.insert(tq);
        }

        recalcTaskTotalScore(copy.getId());

        List<SurveyQuestion> sqs = surveyQuestionMapper.selectList(
                new LambdaQueryWrapper<SurveyQuestion>().eq(SurveyQuestion::getTaskId, sourceTaskId)
                        .orderByAsc(SurveyQuestion::getQuestionOrder));
        for (SurveyQuestion sq : sqs) {
            sq.setId(null);
            sq.setTaskId(copy.getId());
            surveyQuestionMapper.insert(sq);
        }

        return getById(copy.getId());
    }

    private void recalcTaskTotalScore(Long taskId) {
        List<TaskQuestion> allQ = taskQuestionMapper.selectList(
            new LambdaQueryWrapper<TaskQuestion>().eq(TaskQuestion::getTaskId, taskId));
        BigDecimal total = allQ.stream()
            .map(q -> q.getScore() != null ? q.getScore() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        Task task = new Task();
        task.setId(taskId);
        task.setTotalScore(total);
        taskMapper.updateById(task);
    }

    private static BigDecimal scoreFromPresets(String questionType, Map<String, Integer> presets) {
        return ScoreUtils.scoreFromPresets(questionType, presets);
    }

    private Long resolveTeacherId() {
        return resolveTeacherId(SecurityUtils.getCurrentUserId());
    }

    private Long resolveTeacherId(Long userId) {
        if (userId == null) return null;
        Teacher t = teacherMapper.selectOne(
            new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
        if (t != null) return t.getId();
        return SecurityUtils.isAdmin() || SecurityUtils.isInspector() ? 0L : null;
    }

    private void loadGroupIds(Task task) {
        List<TaskGroupVisibility> mappings = taskGroupVisibilityMapper.selectList(
                new LambdaQueryWrapper<TaskGroupVisibility>()
                        .eq(TaskGroupVisibility::getTaskId, task.getId()));
        task.setGroupIds(mappings.stream()
                .map(TaskGroupVisibility::getGroupId)
                .collect(Collectors.toList()));
    }

    private void loadQuestionIds(Task task) {
        List<TaskQuestion> tqs = taskQuestionMapper.selectList(
                new LambdaQueryWrapper<TaskQuestion>()
                        .eq(TaskQuestion::getTaskId, task.getId())
                        .orderByAsc(TaskQuestion::getSortOrder));
        if (!tqs.isEmpty()) {
            task.setQuestionIds(tqs.stream().map(TaskQuestion::getQuestionId).collect(Collectors.toList()));
        }
    }

    private void saveGroupIds(Long taskId, List<Long> groupIds) {
        taskGroupVisibilityMapper.delete(
                new LambdaQueryWrapper<TaskGroupVisibility>()
                        .eq(TaskGroupVisibility::getTaskId, taskId));
        if (groupIds != null && !groupIds.isEmpty()) {
            for (Long gid : groupIds) {
                TaskGroupVisibility tgv = new TaskGroupVisibility();
                tgv.setTaskId(taskId);
                tgv.setGroupId(gid);
                taskGroupVisibilityMapper.insert(tgv);
            }
        }
    }

    @Override
    public void updatePassRateConfig(Long taskId, Integer passRate, Integer maxAttempts, Integer retakeDeadlineHours) {
        var uw = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.school.teaching.entity.Task>()
            .eq(com.school.teaching.entity.Task::getId, taskId);
        if (passRate != null) uw.set(com.school.teaching.entity.Task::getPassRate, passRate);
        if (maxAttempts != null) uw.set(com.school.teaching.entity.Task::getMaxAttempts, maxAttempts);
        if (retakeDeadlineHours != null) uw.set(com.school.teaching.entity.Task::getRetakeDeadlineHours, retakeDeadlineHours);
        taskMapper.update(null, uw);
    }
}