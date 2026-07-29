package com.school.teaching.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final ClassesMapper classesMapper;
    private final StudentClassHistoryMapper classHistoryMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final com.school.teaching.security.StudentResolver studentResolver;

    /** 当前用户聚合档案 */
    public Map<String, Object> getCurrentProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return Map.of("error", "未登录");
        User u = userMapper.selectById(userId);
        if (u == null) return Map.of("error", "用户不存在");

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("userId", u.getId()); profile.put("username", u.getUsername());
        profile.put("realName", u.getRealName()); profile.put("roleId", u.getRoleId());
        profile.put("role", SecurityUtils.getCurrentRole());
        profile.put("schoolId", u.getSchoolId()); profile.put("stageId", u.getCurrentStageId());

        // 学生扩展
        if (SecurityUtils.isStudent()) {
            Student s = studentMapper.selectOne(new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId));
            if (s != null) {
                profile.put("studentId", s.getId()); profile.put("studentNumber", s.getStudentNumber());
                Long classId = resolveCurrentClassId(s.getId());
                profile.put("classId", classId);
                if (classId != null) {
                    Classes c = classesMapper.selectById(classId);
                    if (c != null) { profile.put("className", c.getClassName()); profile.put("grade", c.getGrade()); }
                }
            }
        }

        // 教师扩展
        if (SecurityUtils.isTeacherOrAdmin()) {
            Teacher t = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
            if (t != null) {
                profile.put("teacherId", t.getId()); profile.put("teacherSubject", t.getSubject());
                // 任教班级
                List<TeacherClass> tcs = teacherClassMapper.selectList(
                    new LambdaQueryWrapper<TeacherClass>().eq(TeacherClass::getTeacherId, t.getId()));
                if (!tcs.isEmpty()) {
                    Set<Long> cids = tcs.stream().map(TeacherClass::getClassId).collect(Collectors.toSet());
                    List<Classes> cs = classesMapper.selectBatchIds(cids);
                    profile.put("classList", cs.stream().map(c -> Map.of("id",c.getId(),"name",c.getClassName(),"grade",c.getGrade())).collect(Collectors.toList()));
                    profile.put("subjectList", tcs.stream().map(TeacherClass::getSubject).filter(Objects::nonNull).distinct().collect(Collectors.toList()));
                }
            }
        }

        return profile;
    }

    @Cacheable("userClassInfo")
    public Map<String, Object> getStudentClassInfo(Long userId) {
        Student s = studentMapper.selectOne(new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId));
        if (s == null) return Map.of();
        Long classId = resolveCurrentClassId(s.getId());
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("classId", classId);
        if (classId != null) {
            Classes c = classesMapper.selectById(classId);
            if (c != null) { info.put("className", c.getClassName()); info.put("grade", c.getGrade()); }
        }
        return info;
    }

    private Long resolveCurrentClassId(Long studentId) {
        return studentResolver.resolveCurrentClassId(studentId);
    }
}
