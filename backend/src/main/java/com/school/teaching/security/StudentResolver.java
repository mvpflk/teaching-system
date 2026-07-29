package com.school.teaching.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.Student;
import com.school.teaching.entity.StudentClassHistory;
import com.school.teaching.mapper.StudentClassHistoryMapper;
import com.school.teaching.mapper.StudentMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StudentResolver {

    private final StudentMapper studentMapper;
    private final StudentClassHistoryMapper classHistoryMapper;

    public StudentResolver(StudentMapper studentMapper, StudentClassHistoryMapper classHistoryMapper) {
        this.studentMapper = studentMapper;
        this.classHistoryMapper = classHistoryMapper;
    }

    public Long resolveCurrentStudentId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return null;
        Student s = studentMapper.selectOne(
            new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId));
        return s != null ? s.getId() : null;
    }

    public Long resolveStudentIdByUserId(Long userId) {
        if (userId == null) return null;
        Student s = studentMapper.selectOne(
            new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId));
        return s != null ? s.getId() : null;
    }

    /** 解析学生当前班级ID — 先查班级历史（转班场景），回退到 students.class_id */
    public Long resolveCurrentClassId(Long studentId) {
        if (studentId == null) return null;
        List<StudentClassHistory> list = classHistoryMapper.selectList(
            new LambdaQueryWrapper<StudentClassHistory>()
                .eq(StudentClassHistory::getStudentId, studentId)
                .isNull(StudentClassHistory::getEndDate)
                .orderByDesc(StudentClassHistory::getId)
                .last("LIMIT 1"));
        if (!list.isEmpty()) return list.get(0).getClassId();
        Student s = studentMapper.selectById(studentId);
        return s != null ? s.getClassId() : null;
    }
}
