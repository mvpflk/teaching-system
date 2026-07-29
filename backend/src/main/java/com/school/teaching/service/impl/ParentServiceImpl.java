package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.ParentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ParentServiceImpl implements ParentService {

    @Autowired private ParentChildRelationMapper relationMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private ClassesMapper classesMapper;
    @Autowired private TaskSubmissionMapper submissionMapper;
    @Autowired private TaskMapper taskMapper;
    @Autowired private StudentTimelineMapper timelineMapper;
    @Autowired private AlertRecordMapper alertRecordMapper;
    @Autowired private com.school.teaching.service.AlertService alertService;
    @Autowired private PracticeSubmissionMapper practiceSubmissionMapper;

    @Override
    public boolean isMyChild(Long parentUserId, Long studentId) {
        return relationMapper.selectCount(new LambdaQueryWrapper<ParentChildRelation>()
                .eq(ParentChildRelation::getParentId, parentUserId)
                .eq(ParentChildRelation::getStudentId, studentId)) > 0;
    }

    @Override
    public List<Map<String, Object>> getMyChildren(Long parentUserId) {
        List<ParentChildRelation> relations = relationMapper.selectList(
                new LambdaQueryWrapper<ParentChildRelation>().eq(ParentChildRelation::getParentId, parentUserId));
        if (relations.isEmpty()) return List.of();

        Set<Long> studentIds = relations.stream().map(ParentChildRelation::getStudentId).collect(Collectors.toSet());
        List<Student> students = studentMapper.selectBatchIds(studentIds);
        if (students.isEmpty()) return List.of();

        Set<Long> userIds = students.stream().map(Student::getUserId).collect(Collectors.toSet());
        Set<Long> classIds = students.stream().map(Student::getClassId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> uMap = userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Classes> cMap = classIds.isEmpty() ? Map.of() : classesMapper.selectBatchIds(classIds).stream().collect(Collectors.toMap(Classes::getId, c -> c));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ParentChildRelation rel : relations) {
            Student s = students.stream().filter(st -> st.getId().equals(rel.getStudentId())).findFirst().orElse(null);
            if (s == null) continue;
            User u = uMap.get(s.getUserId());
            Classes c = cMap.get(s.getClassId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("studentId", s.getId());
            item.put("studentNumber", s.getStudentNumber());
            item.put("realName", u != null ? u.getRealName() : "?");
            item.put("className", c != null ? c.getClassName() : null);
            item.put("grade", c != null ? c.getGrade() : null);
            item.put("relation", rel.getRelation());
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getChildGrades(Long parentUserId, Long studentId) {
        if (!isMyChild(parentUserId, studentId)) throw new BusinessException(403, "无权查看该学生的成绩");

        List<TaskSubmission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getStudentId, studentId)
                        .orderByDesc(TaskSubmission::getSubmittedAt)
                        .last("LIMIT 10"));

        if (submissions.isEmpty()) return List.of();

        Set<Long> taskIds = submissions.stream().map(TaskSubmission::getTaskId).collect(Collectors.toSet());
        Map<Long, Task> taskMap = taskMapper.selectBatchIds(taskIds).stream().collect(Collectors.toMap(Task::getId, t -> t));

        List<Map<String, Object>> result = new ArrayList<>();
        for (TaskSubmission sub : submissions) {
            Task task = taskMap.get(sub.getTaskId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", sub.getId());
            item.put("taskTitle", task != null ? task.getTitle() : "?");
            item.put("taskType", task != null ? task.getTaskType() : null);
            item.put("score", sub.getScore());
            item.put("totalScore", task != null ? task.getTotalScore() : null);
            item.put("status", sub.getStatus());
            item.put("gradeLevel", sub.getGradeLevel());
            item.put("submittedAt", sub.getSubmittedAt());
            item.put("gradedAt", sub.getGradedAt());
            // 从task.description提取teacher评语（教师批阅时常在此字段附言）
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getChildTimeline(Long parentUserId, Long studentId) {
        if (!isMyChild(parentUserId, studentId)) throw new BusinessException(403, "无权查看该学生的成长足迹");

        List<StudentTimeline> timelines = timelineMapper.selectList(
                new LambdaQueryWrapper<StudentTimeline>()
                        .eq(StudentTimeline::getStudentId, studentId)
                        .orderByDesc(StudentTimeline::getCreatedAt)
                        .last("LIMIT 20"));

        return timelines.stream().map(t -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", t.getId());
            item.put("eventType", t.getEventType());
            item.put("title", t.getTitle());
            item.put("description", t.getDescription());
            item.put("createdAt", t.getCreatedAt());
            return item;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> bindChild(Long parentUserId, String studentNumber, String studentName, String relation) {
        if (studentNumber == null || studentNumber.isBlank()) throw new BusinessException(400, "学号不能为空");
        if (studentName == null || studentName.isBlank()) throw new BusinessException(400, "学生姓名不能为空");

        Student student = studentMapper.selectOne(
            new LambdaQueryWrapper<Student>().eq(Student::getStudentNumber, studentNumber).last("LIMIT 1"));
        if (student == null) throw new BusinessException(404, "未找到该学号的学生");
        if (!"active".equals(student.getStatus())) throw new BusinessException(400, "该学生当前不在校");

        User studentUser = userMapper.selectById(student.getUserId());
        if (studentUser == null) throw new BusinessException(404, "学生用户信息不存在");
        if (!studentName.equals(studentUser.getRealName())) throw new BusinessException(400, "姓名与学号不匹配");

        long existing = relationMapper.selectCount(
            new LambdaQueryWrapper<ParentChildRelation>()
                .eq(ParentChildRelation::getParentId, parentUserId)
                .eq(ParentChildRelation::getStudentId, student.getId()));
        if (existing > 0) throw new BusinessException(409, "已绑定该学生");

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        long recentCount = relationMapper.selectCount(
            new LambdaQueryWrapper<ParentChildRelation>()
                .eq(ParentChildRelation::getParentId, parentUserId)
                .ge(ParentChildRelation::getCreateTime, since));
        if (recentCount >= 5) throw new BusinessException(429, "24小时内绑定请求已达上限（5次）");

        ParentChildRelation rel = new ParentChildRelation();
        rel.setParentId(parentUserId);
        rel.setStudentId(student.getId());
        rel.setRelation(relation != null ? relation : "GUARDIAN");
        rel.setCreateTime(LocalDateTime.now());
        relationMapper.insert(rel);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", student.getId());
        result.put("studentName", studentUser.getRealName());
        result.put("relation", rel.getRelation());
        return result;
    }

    @Override
    public List<Map<String, Object>> getChildHomework(Long parentUserId, Long studentId) {
        if (!isMyChild(parentUserId, studentId)) throw new BusinessException(403, "无权查看该学生的作业");

        // 获取学生班级
        Student student = studentMapper.selectById(studentId);
        if (student == null) return List.of();
        Long classId = student.getClassId();

        // 查询班级最近30天的任务
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Task> tasks = taskMapper.selectList(
            new LambdaQueryWrapper<Task>()
                .eq(Task::getTargetType, "CLASS")
                .eq(classId != null, Task::getTargetId, classId)
                .ge(Task::getDeadline, since)
                .orderByDesc(Task::getDeadline));

        if (tasks.isEmpty()) return List.of();

        // 查询该学生的所有提交
        Set<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toSet());
        List<TaskSubmission> submissions = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getStudentId, studentId)
                .in(TaskSubmission::getTaskId, taskIds));
        Map<Long, TaskSubmission> subMap = submissions.stream()
            .collect(Collectors.toMap(TaskSubmission::getTaskId, s -> s, (a, b) -> b));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Task task : tasks) {
            TaskSubmission sub = subMap.get(task.getId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", task.getId());
            item.put("title", task.getTitle());
            item.put("subject", task.getSubject());
            item.put("taskType", task.getTaskType());
            item.put("deadline", task.getDeadline());
            item.put("totalScore", task.getTotalScore());
            item.put("submitted", sub != null);
            item.put("score", sub != null ? sub.getScore() : null);
            item.put("status", sub != null ? sub.getStatus() : null);
            item.put("submittedAt", sub != null ? sub.getSubmittedAt() : null);
            result.add(item);
        }
        return result;
    }

    @Override
    public void acknowledgeAlert(Long parentUserId, Long alertId) {
        AlertRecord record = alertRecordMapper.selectById(alertId);
        if (record == null) throw new BusinessException(404, "预警记录不存在");
        if (!isMyChild(parentUserId, record.getStudentId()))
            throw new BusinessException(403, "无权处理该预警");
        alertService.handleAlert(alertId, "ACKNOWLEDGED", parentUserId);
    }

    @Override
    public List<Map<String, Object>> getChildPractices(Long parentUserId, Long studentId) {
        if (!isMyChild(parentUserId, studentId)) throw new BusinessException(403, "无权查看该学生的实训");
        Student st = studentMapper.selectById(studentId);
        if (st == null || st.getClassId() == null) return List.of();
        Long classId = st.getClassId();

        List<Task> tasks = taskMapper.selectList(
            new LambdaQueryWrapper<Task>()
                .eq(Task::getTargetType, "CLASS")
                .eq(Task::getTaskType, "PRACTICE"));
        tasks = tasks.stream().filter(t -> classId.equals(t.getTargetId())).toList();
        if (tasks.isEmpty()) return List.of();

        Set<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toSet());
        List<PracticeSubmission> subs = practiceSubmissionMapper.selectList(
            new LambdaQueryWrapper<PracticeSubmission>()
                .in(PracticeSubmission::getTaskId, taskIds)
                .eq(PracticeSubmission::getStudentId, studentId));
        Map<Long, PracticeSubmission> subMap = subs.stream()
            .collect(Collectors.toMap(PracticeSubmission::getTaskId, s -> s, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Task t : tasks) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("taskId", t.getId());
            item.put("title", t.getTitle());
            item.put("deadline", t.getDeadline());
            PracticeSubmission sub = subMap.get(t.getId());
            item.put("status", sub != null ? sub.getStatus() : "NOT_SUBMITTED");
            item.put("score", sub != null ? sub.getOverallScore() : null);
            item.put("comment", sub != null ? sub.getOverallComment() : null);
            result.add(item);
        }
        return result;
    }
}
