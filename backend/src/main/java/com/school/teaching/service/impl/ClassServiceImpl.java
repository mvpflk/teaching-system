package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.ClassService;
import com.school.teaching.service.NotificationService;
import com.school.teaching.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClassServiceImpl implements ClassService {

    @Autowired
    private ClassesMapper classesMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ClassTypeConfigMapper classTypeConfigMapper;

    @Autowired
    private StudentClassHistoryMapper studentClassHistoryMapper;

    @Autowired
    private StudentClassHistoryMapper classHistoryMapper;

    @Autowired
    private StudentStageChangeLogMapper stageChangeLogMapper;

    @Autowired
    private com.school.teaching.mapper.TaskMapper taskMapper;

    @Autowired
    private com.school.teaching.mapper.TaskSubmissionMapper taskSubmissionMapper;

    @Autowired
    private com.school.teaching.mapper.TeacherMapper teacherMapper;

    @Autowired
    private com.school.teaching.mapper.TeacherClassMapper teacherClassMapper;

    @Autowired
    private TeacherService teacherService;

    @Override
    public List<Classes> getClassList() {
        LambdaQueryWrapper<Classes> w = new LambdaQueryWrapper<>();
        w.orderByDesc(Classes::getCreateTime);
        return classesMapper.selectList(w);
    }

    @Override
    public Classes getClassById(Long id) {
        return classesMapper.selectById(id);
    }

    @Override
    @CacheEvict(value = "classHome", allEntries = true)
    public Classes createClass(Classes classes) {
        classes.setStudentCount(0);
        if (classes.getClassCode() == null || classes.getClassCode().isEmpty()) {
            classes.setClassCode("C" + System.currentTimeMillis() % 100000);
        }
        validateClassType(classes.getClassType());
        classesMapper.insert(classes);
        return classes;
    }

    @Override
    @CacheEvict(value = "classHome", key = "#classes.id")
    public Classes updateClass(Classes classes) {
        if (classes.getClassType() != null) validateClassType(classes.getClassType());
        classesMapper.updateById(classes);
        return classes;
    }

    private void validateClassType(String typeCode) {
        if (typeCode == null || typeCode.isEmpty()) return;
        Long count = classTypeConfigMapper.selectCount(
            new LambdaQueryWrapper<ClassTypeConfig>().eq(ClassTypeConfig::getTypeCode, typeCode));
        if (count == 0) throw new BusinessException(400, "无效的班级类型: " + typeCode);
    }

    @Override @Transactional
    public int batchUpdateClassType(List<Long> classIds, String classType) {
        if (classIds == null || classIds.isEmpty() || classType == null)
            throw new BusinessException(400, "参数错误");
        validateClassType(classType);
        // 批量加载班级
        List<Classes> classes = classesMapper.selectBatchIds(classIds);
        if (classes.isEmpty()) return 0;
        // 批量更新班级
        classes.forEach(c -> c.setClassType(classType));
        for (Classes c : classes) classesMapper.updateById(c);
        // 批量加载所有相关学生
        List<Student> allStudents = studentMapper.selectList(
            new LambdaQueryWrapper<Student>().in(Student::getClassId, classIds));
        if (!allStudents.isEmpty()) {
            List<Long> studentIds = allStudents.stream().map(Student::getId).toList();
            Set<Long> classIdSet = new java.util.HashSet<>(classIds);
            Map<Long, Classes> clsMap = classes.stream().collect(java.util.stream.Collectors.toMap(Classes::getId, c -> c));
            // 批量更新学生 current_type
            for (Student s : allStudents) { s.setCurrentType(classType); }
            for (Student s : allStudents) studentMapper.updateById(s);
            // 批量关闭旧的活跃历史记录
            java.time.LocalDate today = java.time.LocalDate.now();
            List<StudentClassHistory> activeHistories = studentClassHistoryMapper.selectList(
                new LambdaQueryWrapper<StudentClassHistory>()
                    .in(StudentClassHistory::getStudentId, studentIds)
                    .isNull(StudentClassHistory::getEndDate));
            for (StudentClassHistory h : activeHistories) {
                h.setEndDate(today);
                studentClassHistoryMapper.updateById(h);
            }
            // 批量插入新历史记录
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            for (Student s : allStudents) {
                Classes cls = clsMap.get(s.getClassId());
                if (cls == null) continue;
                StudentClassHistory h = new StudentClassHistory();
                h.setStudentId(s.getId()); h.setClassId(s.getClassId());
                h.setStageId(cls.getStageId()); h.setSchoolId(cls.getSchoolId());
                h.setChangeReason("班级批量标记"); h.setOperatorId(null);
                h.setStartDate(today); h.setCreatedAt(now);
                studentClassHistoryMapper.insert(h);
            }
        }
        return classes.size();
    }

    @Override
    @Transactional
    @CacheEvict(value = "classHome", key = "#id")
    public void deleteClass(Long id) {
        // 1. 清空该班级学生的class_id
        studentMapper.update(null, new LambdaUpdateWrapper<Student>()
            .eq(Student::getClassId, id)
            .set(Student::getClassId, null));

        // 2. 删除班级
        classesMapper.deleteById(id);
    }

    @Override
    @Cacheable(value = "class_students", key = "#classId")
    public List<Map<String, Object>> getStudents(Long classId) {
        LambdaQueryWrapper<Student> w = new LambdaQueryWrapper<>();
        w.eq(Student::getClassId, classId);
        w.orderByAsc(Student::getStudentNumber);
        List<Student> students = studentMapper.selectList(w);

        Set<Long> userIds = students.stream().map(Student::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap() :
            userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Student s : students) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", s.getId());
            item.put("userId", s.getUserId());
            item.put("studentNumber", s.getStudentNumber());
            item.put("gender", s.getGender());
            User user = userMap.get(s.getUserId());
            if (user != null) {
                item.put("username", user.getUsername());
                item.put("realName", user.getRealName());
                if (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
                    item.put("avatarUrl", user.getAvatarUrl());
                }
            }
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "class_students", key = "#classId"),
        @CacheEvict(value = "classHome", key = "#classId")
    })
    public void addStudent(Long classId, Long studentId) {
        Student s = studentMapper.selectById(studentId);
        if (s == null) throw new BusinessException(404, "学生不存在");
        // 显式使用 LambdaUpdateWrapper 设置 classId（确保生效）
        studentMapper.update(null,
            new LambdaUpdateWrapper<Student>()
                .eq(Student::getId, studentId)
                .set(Student::getClassId, classId));

        // 更新班级人数
        Classes c = classesMapper.selectById(classId);
        if (c != null) {
            Long count = studentMapper.selectCount(
                new LambdaQueryWrapper<Student>().eq(Student::getClassId, classId));
            c.setStudentCount(count.intValue());
            classesMapper.updateById(c);
        }

        // 班级变更通知
        User su = userMapper.selectById(s.getUserId());
        if (su != null) {
            notificationService.notify(su.getId(), "class_changed",
                "班级变更", "你已被分配到「" + (c != null ? c.getClassName() : "未知") + "」班", classId);
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "class_students", key = "#classId"),
        @CacheEvict(value = "classHome", key = "#classId")
    })
    public void removeStudent(Long classId, Long studentId) {
        // 使用 LambdaUpdateWrapper 显式设置 class_id = NULL
        // （updateById 会忽略 null 字段）
        studentMapper.update(null, new LambdaUpdateWrapper<Student>()
            .eq(Student::getId, studentId)
            .eq(Student::getClassId, classId)
            .set(Student::getClassId, null));

        // 更新班级人数
        Classes c = classesMapper.selectById(classId);
        if (c != null) {
            Long count = studentMapper.selectCount(
                new LambdaQueryWrapper<Student>().eq(Student::getClassId, classId));
            c.setStudentCount(count.intValue());
            classesMapper.updateById(c);
        }

        // 班级移除通知
        Student removedStudent = studentMapper.selectById(studentId);
        if (removedStudent != null) {
            User ru = userMapper.selectById(removedStudent.getUserId());
            if (ru != null && c != null) {
                notificationService.notify(ru.getId(), "class_changed",
                    "班级变更", "你已从「" + c.getClassName() + "」班移除", null);
            }
        }
    }

    @Override
    public List<Map<String, Object>> getAvailableStudents() {
        LambdaQueryWrapper<Student> w = new LambdaQueryWrapper<>();
        w.isNull(Student::getClassId);
        w.isNotNull(Student::getStudentNumber); // [C6] 过滤空学号
        w.ne(Student::getStudentNumber, "");
        w.orderByAsc(Student::getStudentNumber);
        List<Student> students = studentMapper.selectList(w);

        Set<Long> uIds = students.stream().map(Student::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = uIds.isEmpty() ? Collections.emptyMap() :
            userMapper.selectBatchIds(uIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Student s : students) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", s.getId());
            item.put("studentNumber", s.getStudentNumber());
            User user = userMap.get(s.getUserId());
            if (user != null) {
                item.put("realName", user.getRealName());
                item.put("username", user.getUsername());
            }
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> adminListClasses() {
        List<Classes> classes = classesMapper.selectList(null);
        Set<Long> classIds = classes.stream().map(Classes::getId).collect(Collectors.toSet());
        Set<Long> headIds = classes.stream().map(Classes::getHeadTeacherId).filter(Objects::nonNull).collect(Collectors.toSet());

        // Batch load student counts
        Map<Long, Long> countMap = new HashMap<>();
        if (!classIds.isEmpty()) {
            List<Student> all = studentMapper.selectList(
                new LambdaQueryWrapper<Student>().in(Student::getClassId, classIds));
            for (Student s : all) {
                if (s.getClassId() != null) countMap.merge(s.getClassId(), 1L, Long::sum);
            }
        }

        // Batch load head teacher names
        Map<Long, User> userMap = headIds.isEmpty() ? Collections.emptyMap() :
            userMapper.selectBatchIds(headIds).stream().collect(Collectors.toMap(User::getId, u -> u));

        List<Map<String, Object>> records = new ArrayList<>();
        for (Classes c : classes) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId()); item.put("className", c.getClassName()); item.put("classCode", c.getClassCode());
            item.put("grade", c.getGrade()); item.put("major", c.getMajor());
            item.put("academicYear", c.getAcademicYear()); item.put("semester", c.getSemester());
            item.put("headTeacherId", c.getHeadTeacherId()); item.put("status", c.getStatus());
            item.put("stageId", c.getStageId()); item.put("classType", c.getClassType());
            item.put("studentCount", countMap.getOrDefault(c.getId(), 0L));
            if (c.getHeadTeacherId() != null) {
                User t = userMap.get(c.getHeadTeacherId());
                if (t != null) item.put("headTeacherName", t.getRealName());
            }
            records.add(item);
        }
        return Map.of("records", records, "total", (long) records.size());
    }

    @Override
    public Map<String, Object> adminListClassesByTeacher(Long userId) {
        // userId → teacherId 转换
        com.school.teaching.entity.Teacher teacher = teacherMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.teaching.entity.Teacher>()
                .eq(com.school.teaching.entity.Teacher::getUserId, userId));
        if (teacher == null) return Map.of("records", List.of(), "total", 0L);

        List<TeacherClass> tcs = teacherClassMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TeacherClass>()
                .eq(TeacherClass::getTeacherId, teacher.getId()));
        Set<Long> classIds = tcs.stream().map(TeacherClass::getClassId).collect(java.util.stream.Collectors.toSet());
        if (classIds.isEmpty()) return Map.of("records", List.of(), "total", 0L);

        List<Classes> classes = classesMapper.selectBatchIds(classIds);
        // 批量统计学生数
        Map<Long, Long> countMap = new HashMap<>();
        List<Student> all = studentMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Student>()
                .in(Student::getClassId, classIds));
        for (Student s : all) { if (s.getClassId() != null) countMap.merge(s.getClassId(), 1L, Long::sum); }

        // 批量查班主任姓名
        Set<Long> headIds = classes.stream().map(Classes::getHeadTeacherId).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        Map<Long, String> headNames = new HashMap<>();
        if (!headIds.isEmpty()) {
            userMapper.selectBatchIds(headIds).forEach(u -> headNames.put(u.getId(), u.getRealName()));
        }

        List<Map<String, Object>> records = new ArrayList<>();
        for (Classes c : classes) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId()); item.put("className", c.getClassName()); item.put("classCode", c.getClassCode());
            item.put("grade", c.getGrade()); item.put("major", c.getMajor());
            item.put("academicYear", c.getAcademicYear()); item.put("semester", c.getSemester());
            item.put("headTeacherId", c.getHeadTeacherId()); item.put("status", c.getStatus());
            item.put("stageId", c.getStageId()); item.put("classType", c.getClassType());
            item.put("studentCount", countMap.getOrDefault(c.getId(), 0L));
            if (c.getHeadTeacherId() != null) item.put("headTeacherName", headNames.get(c.getHeadTeacherId()));
            records.add(item);
        }
        return Map.of("records", records, "total", (long) records.size());
    }

    @Override
    public String getClassMajor(Long classId) {
        if (classId == null) return null;
        try {
            Classes cls = classesMapper.selectById(classId);
            return cls != null ? cls.getMajor() : null;
        } catch (Exception e) { return null; }
    }

    @Override
    public boolean isTeacherOfClass(Long userId, Long classId) {
        return teacherService.isUserTeacherOfClass(userId, classId);
    }

    @Override
    public boolean isHeadTeacherOfClass(Long userId, Long classId) {
        Classes cls = classesMapper.selectById(classId);
        return cls != null && userId.equals(cls.getHeadTeacherId());
    }

    @Override @Transactional
    public Map<String, Object> changeStudentClass(Long studentId, Long newClassId, String reason, Long approvedBy) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) throw new BusinessException(404, "学生不存在");
        Classes newClass = classesMapper.selectById(newClassId);
        if (newClass == null) throw new BusinessException(404, "目标班级不存在");

        Long oldClassId = student.getClassId();
        Long oldStageId = student.getCurrentStageId();
        Long newStageId = newClass.getStageId();

        closeOldClassHistory(studentId, oldClassId);
        createNewClassHistory(studentId, newClassId, newStageId, newClass.getSchoolId(), reason);
        updateStudentRecord(student, newClassId, oldStageId, newStageId, oldClassId, reason, approvedBy);

        if (oldClassId != null) exemptOldTasks(studentId, oldClassId);
        notifyStudentClassChange(student, newClass, oldStageId, newStageId);
        registerNewTasks(studentId, newClassId);

        Map<String, Object> result = new HashMap<>();
        result.put("studentId", studentId);
        result.put("oldClassId", oldClassId);
        result.put("newClassId", newClassId);
        result.put("stageChanged", !Objects.equals(oldStageId, newStageId));
        return result;
    }

    private void closeOldClassHistory(Long studentId, Long oldClassId) {
        if (oldClassId == null) return;
        classHistoryMapper.update(null,
            new LambdaUpdateWrapper<StudentClassHistory>()
                .eq(StudentClassHistory::getStudentId, studentId)
                .isNull(StudentClassHistory::getEndDate)
                .set(StudentClassHistory::getEndDate, java.time.LocalDate.now()));
    }

    private void createNewClassHistory(Long studentId, Long newClassId, Long newStageId, Long schoolId, String reason) {
        StudentClassHistory history = new StudentClassHistory();
        history.setStudentId(studentId);
        history.setClassId(newClassId);
        history.setStageId(newStageId != null ? newStageId : 4L);
        history.setSchoolId(schoolId != null ? schoolId : 1L);
        history.setStartDate(java.time.LocalDate.now());
        history.setChangeReason(reason);
        history.setCreatedAt(java.time.LocalDateTime.now());
        classHistoryMapper.insert(history);
    }

    private void updateStudentRecord(Student student, Long newClassId, Long newStageId, Long oldStageId,
                                     Long oldClassId, String reason, Long approvedBy) {
        student.setClassId(newClassId);
        if (newStageId != null && !newStageId.equals(oldStageId)) {
            student.setCurrentStageId(newStageId);
            StudentStageChangeLog log = new StudentStageChangeLog();
            log.setStudentId(student.getId());
            log.setFromStageId(oldStageId);
            log.setToStageId(newStageId);
            log.setFromClassId(oldClassId);
            log.setToClassId(newClassId);
            log.setChangeDate(java.time.LocalDate.now());
            log.setReason(reason);
            log.setApprovedBy(approvedBy);
            log.setCreatedAt(java.time.LocalDateTime.now());
            stageChangeLogMapper.insert(log);
        }
        studentMapper.updateById(student);
    }

    private void exemptOldTasks(Long studentId, Long oldClassId) {
        // 统一任务: 豁免进行中提交
        List<com.school.teaching.entity.Task> oldTasks = taskMapper.selectList(
            new LambdaQueryWrapper<com.school.teaching.entity.Task>()
                .eq(com.school.teaching.entity.Task::getTargetType, "CLASS")
                .eq(com.school.teaching.entity.Task::getTargetId, oldClassId)
                .eq(com.school.teaching.entity.Task::getStatus, "ONGOING"));
        if (!oldTasks.isEmpty()) {
            java.util.Set<Long> oldTids = oldTasks.stream().map(com.school.teaching.entity.Task::getId).collect(java.util.stream.Collectors.toSet());
            taskSubmissionMapper.update(null,
                new LambdaUpdateWrapper<com.school.teaching.entity.TaskSubmission>()
                    .in(com.school.teaching.entity.TaskSubmission::getTaskId, oldTids)
                    .eq(com.school.teaching.entity.TaskSubmission::getStudentId, studentId)
                    .set(com.school.teaching.entity.TaskSubmission::getStatus, "EXEMPTED"));
        }
    }

    private void notifyStudentClassChange(Student student, Classes newClass, Long oldStageId, Long newStageId) {
        User u = userMapper.selectById(student.getUserId());
        if (u != null) {
            String msg = "你已被调整到「" + newClass.getClassName() + "」班";
            if (!Objects.equals(oldStageId, newStageId)) msg += "（学段变更）";
            notificationService.notify(u.getId(), "class_changed", "班级调整", msg, newClass.getId());
        }
    }

    private void registerNewTasks(Long studentId, Long newClassId) {
        java.util.List<com.school.teaching.entity.Task> newTasks = taskMapper.selectList(
            new LambdaQueryWrapper<com.school.teaching.entity.Task>()
                .eq(com.school.teaching.entity.Task::getTargetType, "CLASS")
                .eq(com.school.teaching.entity.Task::getTargetId, newClassId)
                .in(com.school.teaching.entity.Task::getStatus, java.util.List.of("PUBLISHED", "ONGOING")));
        List<com.school.teaching.entity.TaskSubmission> subs = new ArrayList<>();
        for (com.school.teaching.entity.Task t : newTasks) {
            com.school.teaching.entity.TaskSubmission sub = new com.school.teaching.entity.TaskSubmission();
            sub.setTaskId(t.getId()); sub.setStudentId(studentId);
            sub.setSchoolId(t.getSchoolId()); sub.setStageId(t.getStageId());
            sub.setStatus("PENDING");
            subs.add(sub);
        }
        for (com.school.teaching.entity.TaskSubmission sub : subs) taskSubmissionMapper.insert(sub);
    }
}
