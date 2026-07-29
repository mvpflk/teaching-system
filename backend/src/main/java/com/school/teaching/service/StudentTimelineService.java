package com.school.teaching.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.Student;
import com.school.teaching.entity.StudentTimeline;
import com.school.teaching.entity.User;
import com.school.teaching.mapper.StudentMapper;
import com.school.teaching.mapper.StudentTimelineMapper;
import com.school.teaching.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentTimelineService {

    private final StudentTimelineMapper mapper;
    private final StudentMapper studentMapper;
    private final UserMapper userMapper;

    public void recordEvent(Long studentId, String eventType, String title, String description, String link) {
        StudentTimeline t = new StudentTimeline();
        t.setStudentId(studentId); t.setEventType(eventType);
        t.setTitle(title); t.setDescription(description); t.setLink(link);
        mapper.insert(t);
    }

    /** 根据 userId 查找 studentId 并记录 */
    public void recordByUserId(Long userId, String eventType, String title, String description, String link) {
        Student s = studentMapper.selectOne(
            new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId));
        if (s != null) recordEvent(s.getId(), eventType, title, description, link);
    }

    /** 根据 realName 查找 student_id（用于 @提及） */
    public Long findStudentIdByName(String realName) {
        User u = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getRealName, realName));
        if (u == null) return null;
        Student s = studentMapper.selectOne(
            new LambdaQueryWrapper<Student>().eq(Student::getUserId, u.getId()));
        return s != null ? s.getId() : null;
    }

    public List<StudentTimeline> getByStudentId(Long studentId) {
        return mapper.selectList(
            new LambdaQueryWrapper<StudentTimeline>().eq(StudentTimeline::getStudentId, studentId)
                .orderByDesc(StudentTimeline::getCreatedAt));
    }
}
