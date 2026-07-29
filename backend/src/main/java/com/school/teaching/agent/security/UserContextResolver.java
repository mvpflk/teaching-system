package com.school.teaching.agent.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.Classes;
import com.school.teaching.entity.Student;
import com.school.teaching.entity.Teacher;
import com.school.teaching.entity.TeacherClass;
import com.school.teaching.mapper.ClassesMapper;
import com.school.teaching.mapper.StudentMapper;
import com.school.teaching.mapper.TeacherClassMapper;
import com.school.teaching.mapper.TeacherMapper;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.security.CustomUserDetails;
import com.school.teaching.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserContextResolver {

    private final TeacherMapper teacherMapper;
    private final StudentMapper studentMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final ClassesMapper classesMapper;

    public UserContext resolve() {
        CustomUserDetails user = SecurityUtils.getCurrentUser();
        if (user == null) {
            throw new BusinessException(401, "无法获取当前用户信息");
        }
        return resolve(user);
    }

    public UserContext resolve(CustomUserDetails user) {
        UserContext.UserContextBuilder builder = UserContext.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .roleName(user.getRole())
                .schoolId(user.getSchoolId())
                .stageId(user.getStageId());

        String role = user.getRole();
        if ("TEACHER".equals(role) || "HEAD_TEACHER".equals(role)) {
            fillTeacher(builder, user.getUserId());
        } else if ("STUDENT".equals(role)) {
            fillStudent(builder, user.getUserId());
        }

        return builder.build();
    }

    private void fillTeacher(UserContext.UserContextBuilder builder, Long userId) {
        Teacher teacher = teacherMapper.selectOne(
                new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
        if (teacher == null) {
            builder.accessibleClassIds(Collections.emptySet());
            return;
        }
        builder.teacherId(teacher.getId());

        // 收集 teacher_classes 中的班级 ID 和所授学科
        List<TeacherClass> tcList = teacherClassMapper.selectList(
                new LambdaQueryWrapper<TeacherClass>()
                        .eq(TeacherClass::getTeacherId, teacher.getId()));
        Set<Long> classIds = tcList.stream()
                .map(TeacherClass::getClassId).collect(Collectors.toSet());
        builder.accessibleClassIds(classIds);

        // 收集教师所授学科
        Set<String> teacherSubjects = tcList.stream()
                .map(TeacherClass::getSubject)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toSet());
        builder.subjects(teacherSubjects);

        // 判断是否班主任 + 收集班级名称
        if (!classIds.isEmpty()) {
            List<Classes> classes = classesMapper.selectList(
                    new LambdaQueryWrapper<Classes>().in(Classes::getId, classIds));
            boolean isHead = classes.stream()
                    .anyMatch(c -> c.getHeadTeacherId() != null
                            && c.getHeadTeacherId().equals(teacher.getId()));
            builder.isHeadTeacher(isHead);

            // 收集班级名称，让 AI 知道"我是谁"
            Map<Long, String> classNames = new java.util.LinkedHashMap<>();
            for (Classes c : classes) {
                classNames.put(c.getId(), c.getClassName() != null ? c.getClassName() : "班级" + c.getId());
            }
            builder.classNames(classNames);
        }

        log.debug("fillTeacher: userId={}, subjects={}, isHeadTeacher={}, classCount={}",
                userId, teacherSubjects, builder.build().isHeadTeacher(),
                builder.build().getClassNames().size());
    }

    private void fillStudent(UserContext.UserContextBuilder builder, Long userId) {
        Student student = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId));
        if (student == null) return;

        builder.studentId(student.getId());
        builder.classId(student.getClassId());

        // 查班级专业 → 计算可访问学科
        if (student.getClassId() != null) {
            Classes cls = classesMapper.selectById(student.getClassId());
            if (cls != null && cls.getMajor() != null) {
                builder.major(cls.getMajor());
                builder.accessibleSubjects(
                        MajorSubjectMapping.getSubjects(cls.getMajor()));
                log.debug("fillStudent: userId={}, major={}, subjects={}",
                        userId, cls.getMajor(), builder.build().getAccessibleSubjects());
            }
        }
        // 如果查不到专业，使用默认（仅文化课）
        if (builder.build().getAccessibleSubjects().isEmpty()) {
            builder.accessibleSubjects(MajorSubjectMapping.getDefaultSubjects());
        }
    }
}
