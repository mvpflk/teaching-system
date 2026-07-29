package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.TaskTemplateService;
import com.school.teaching.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskTemplateServiceImpl implements TaskTemplateService {

    private static final Logger log = LoggerFactory.getLogger(TaskTemplateServiceImpl.class);

    private final TaskTemplateMapper templateMapper;
    private final TaskMapper taskMapper;
    private final TaskQuestionMapper taskQuestionMapper;
    private final UserMapper userMapper;
    private final TeacherMapper teacherMapper;
    private final ClassesMapper classesMapper;
    private final LessonPrepGroupMapper lpgMapper;
    private final TeachingGroupMapper tgMapper;
    private final GroupMemberMapper gmMapper;

    // ── 班主任判断 ──────────────────────────
    private boolean isHeadTeacher(Long userId) {
        if (userId == null) return false;
        return classesMapper.selectCount(new LambdaQueryWrapper<Classes>()
            .eq(Classes::getHeadTeacherId, userId)) > 0;
    }

    private boolean isAdmin(Long userId) {
        com.school.teaching.entity.User u = userMapper.selectById(userId);
        if (u == null || u.getRoleId() == null) return false;
        return (u.getRoleId() & 9) != 0; // ADMIN=1, SUPER_ADMIN=8
    }

    // ── 列表 ────────────────────────────────
    @Override
    public List<TaskTemplate> listTemplates(Long userId, String scope, String subject, String taskType, String category) {
        LambdaQueryWrapper<TaskTemplate> w = new LambdaQueryWrapper<>();
        if (subject != null && !subject.isEmpty()) w.eq(TaskTemplate::getSubject, subject);
        if (taskType != null && !taskType.isEmpty()) w.eq(TaskTemplate::getTaskType, taskType);
        if (category != null && !category.isEmpty() && !"ALL".equals(category)) w.eq(TaskTemplate::getCategory, category);
        w.orderByDesc(TaskTemplate::getUpdatedAt);

        List<TaskTemplate> all = templateMapper.selectList(w);
        Set<Long> creatorIds = all.stream().map(TaskTemplate::getCreatedBy).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> nameMap = new HashMap<>();
        if (!creatorIds.isEmpty()) userMapper.selectBatchIds(creatorIds).forEach(u -> nameMap.put(u.getId(), u.getRealName()));

        Teacher teacher = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
        Long teacherId = teacher != null ? teacher.getId() : null;
        boolean isHT = isHeadTeacher(userId);
        boolean isAdm = isAdmin(userId);
        Set<Long> myLpgIds = new HashSet<>(), myTgIds = new HashSet<>();
        if (teacherId != null) {
            gmMapper.selectList(new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getTeacherId, teacherId))
                .forEach(gm -> {
                    if ("LESSON_PREP".equals(gm.getGroupType())) myLpgIds.add(gm.getGroupId());
                    else myTgIds.add(gm.getGroupId());
                });
        }

        List<TaskTemplate> result = new ArrayList<>();
        for (TaskTemplate t : all) {
            boolean visible = false;
            String sc = t.getScope() != null ? t.getScope() : "PRIVATE";
            switch (sc) {
                case "PRIVATE":
                    visible = userId.equals(t.getCreatedBy()); break;
                case "LESSON_PREP":
                    visible = myLpgIds.contains(t.getLessonPrepGroupId()); break;
                case "TEACHING_GROUP":
                    visible = myTgIds.contains(t.getTeachingGroupId()); break;
                case "SCHOOL_WIDE":
                    visible = true; break;  // 所有登录教师可见
                case "HEAD_TEACHER_GROUP":
                    visible = isHT; break;   // 仅班主任可见
                default:
                    visible = userId.equals(t.getCreatedBy());
            }
            if (!visible && !userId.equals(t.getCreatedBy()) && !isAdm) continue;
            if (scope != null && !scope.equals("ALL") && !scope.equals(sc)) continue;

            t.setCreatorName(nameMap.getOrDefault(t.getCreatedBy(), ""));
            try { var arr = JsonUtils.MAPPER.readTree(t.getQuestionIds()); t.setQuestionCount(arr.isArray() ? arr.size() : 0); }
            catch (Exception e) { t.setQuestionCount(0); }
            result.add(t);
        }
        return result;
    }

    @Override
    public TaskTemplate getById(Long id) {
        TaskTemplate t = templateMapper.selectById(id);
        if (t == null) throw new BusinessException(404, "模板不存在");
        if (t.getCreatedBy() != null) { User u = userMapper.selectById(t.getCreatedBy()); if (u != null) t.setCreatorName(u.getRealName()); }
        try { var arr = JsonUtils.MAPPER.readTree(t.getQuestionIds()); t.setQuestionCount(arr.isArray() ? arr.size() : 0); }
        catch (Exception e) { t.setQuestionCount(0); }
        return t;
    }

    // ── 保存 ────────────────────────────────
    @Override @Transactional
    public TaskTemplate saveFromTask(Long taskId, String name, String scope, String category, Long userId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");
        List<TaskQuestion> tqs = taskQuestionMapper.selectList(
            new LambdaQueryWrapper<TaskQuestion>().eq(TaskQuestion::getTaskId, taskId));
        List<Long> qids = tqs.stream().map(TaskQuestion::getQuestionId).distinct().toList();

        TaskTemplate t = new TaskTemplate();
        t.setName(name != null ? name : task.getTitle());
        t.setDescription(task.getDescription());
        t.setSubject(task.getSubject());
        t.setTaskType(task.getTaskType());
        t.setScoreType(task.getScoreType());
        t.setCategory(category != null ? category : "TEACHING");
        t.setQuestionIds(JsonUtils.toJson(qids));
        t.setTaskConfig(task.getTaskConfig());
        t.setWuyuTag(task.getWuyuTag());
        t.setTotalScore(task.getTotalScore() != null ? task.getTotalScore().doubleValue() : 100.0);
        t.setScope(scope != null ? scope : "PRIVATE");
        t.setUseCount(0);
        t.setCreatedBy(userId);
        t.setSchoolId(task.getSchoolId());
        t.setStageId(task.getStageId());
        Teacher teacher = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
        if (teacher != null) {
            List<GroupMember> gms = gmMapper.selectList(new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getTeacherId, teacher.getId()));
            for (GroupMember gm : gms) {
                if ("LESSON_PREP".equals(gm.getGroupType())) t.setLessonPrepGroupId(gm.getGroupId());
                else t.setTeachingGroupId(gm.getGroupId());
            }
        }
        templateMapper.insert(t);
        return t;
    }

    // ── 新建空白模板 ──────────────────────────
    @Override @Transactional
    public TaskTemplate createTemplate(TaskTemplate t, Long userId) {
        t.setCreatedBy(userId);
        t.setUseCount(0);
        if (t.getCategory() == null) t.setCategory("TEACHING");
        if (t.getScope() == null) t.setScope("PRIVATE");
        if (t.getTotalScore() == null) t.setTotalScore(100.0);
        if (t.getScoreType() == null) t.setScoreType("POINT_100");
        Teacher teacher = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
        if (teacher != null) {
            List<GroupMember> gms = gmMapper.selectList(new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getTeacherId, teacher.getId()));
            for (GroupMember gm : gms) {
                if ("LESSON_PREP".equals(gm.getGroupType())) t.setLessonPrepGroupId(gm.getGroupId());
                else t.setTeachingGroupId(gm.getGroupId());
            }
        }
        templateMapper.insert(t);
        return t;
    }

    // ── 使用模板创建任务 ─────────────────────
    @Override @Transactional
    public Map<String, Object> createTaskFromTemplate(Long templateId, Long userId) {
        TaskTemplate t = templateMapper.selectById(templateId);
        if (t == null) throw new BusinessException(404, "模板不存在");
        t.setUseCount((t.getUseCount() != null ? t.getUseCount() : 0) + 1);
        templateMapper.updateById(t);

        Task task = new Task();
        task.setTitle(t.getName());
        task.setDescription(t.getDescription());
        task.setSubject("CLASS_MGMT".equals(t.getCategory()) ? null : t.getSubject());
        task.setTaskType(t.getTaskType());
        task.setScoreType(t.getScoreType());
        task.setTaskConfig(t.getTaskConfig());
        task.setWuyuTag(t.getWuyuTag());
        task.setTotalScore(t.getTotalScore() != null ? java.math.BigDecimal.valueOf(t.getTotalScore()) : java.math.BigDecimal.valueOf(100));
        task.setTeacherId(userId);
        task.setStatus("DRAFT");
        task.setTargetType("CLASS");
        task.setReviewStatus("NOT_SUBMITTED");
        task.setSchoolId(t.getSchoolId());
        task.setStageId(t.getStageId());
        task.setCreatedAt(java.time.LocalDateTime.now());

        // 班级管理类 → 自动锁定班主任的班级
        if ("CLASS_MGMT".equals(t.getCategory()) && isHeadTeacher(userId)) {
            List<Classes> headClasses = classesMapper.selectList(
                new LambdaQueryWrapper<Classes>().eq(Classes::getHeadTeacherId, userId));
            if (headClasses.size() == 1) {
                task.setTargetId(headClasses.get(0).getId());
            }
            // 多个班级时不自动设置，让班主任手动选
        }
        taskMapper.insert(task);

        // 拷贝题目关联
        try {
            var arr = JsonUtils.MAPPER.readTree(t.getQuestionIds());
            if (arr.isArray()) {
                for (var node : arr) {
                    TaskQuestion tq = new TaskQuestion();
                    tq.setTaskId(task.getId()); tq.setQuestionId(node.asLong());
                    tq.setScore(java.math.BigDecimal.ONE); taskQuestionMapper.insert(tq);
                }
            }
        } catch (Exception ignored) { log.error("从模板拷贝题目关联失败 templateId={}", templateId, ignored); }

        return Map.of("taskId", task.getId(), "title", task.getTitle());
    }

    // ── 共享范围 ────────────────────────────
    @Override
    public void updateScope(Long id, String scope, Long userId) {
        TaskTemplate t = templateMapper.selectById(id);
        if (t == null) throw new BusinessException(404, "模板不存在");
        boolean isAdm = isAdmin(userId);
        // 管理员可设置任意scope, 普通用户只能设置非SCHOOL_WIDE
        if ("SCHOOL_WIDE".equals(scope) && !isAdm) throw new BusinessException(403, "仅管理员可设为全校共享");
        if (!userId.equals(t.getCreatedBy()) && !isAdm) throw new BusinessException(403, "仅创建者可修改");
        t.setScope(scope);
        templateMapper.updateById(t);
    }

    @Override
    public void deleteTemplate(Long id, Long userId) {
        TaskTemplate t = templateMapper.selectById(id);
        if (t == null) throw new BusinessException(404, "模板不存在");
        if (!userId.equals(t.getCreatedBy()) && !isAdmin(userId))
            throw new BusinessException(403, "仅创建者可删除");
        templateMapper.deleteById(id);
    }
}
