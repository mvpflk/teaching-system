package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.school.teaching.entity.Classes;
import com.school.teaching.entity.Student;
import com.school.teaching.entity.User;
import com.school.teaching.mapper.ClassesMapper;
import com.school.teaching.mapper.StudentMapper;
import com.school.teaching.mapper.UserMapper;
import com.school.teaching.service.UserService;
import com.school.teaching.utils.PasswordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired private UserMapper userMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private ClassesMapper classesMapper;

    @Override
    public User login(String username, String password) {
        User user = userMapper.selectOne(
            new QueryWrapper<User>()
                .eq("username", username)
                .eq("status", 1)
        );
        if (user == null) return null;

        String storedPwd = user.getPassword();
        if (PasswordUtils.matches(password, storedPwd)) {
            // Auto-upgrade old format passwords to BCrypt
            if (!PasswordUtils.isCurrentFormat(storedPwd)) {
                user.setPassword(PasswordUtils.encode(password));
                userMapper.updateById(user);
            }
            return user;
        }
        return null;
    }

    @Override
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public User getUserByUsername(String username) {
        return userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    }

    @Override
    public Map<String, Object> getStudentClassInfo(Long userId) {
        Map<String, Object> info = new HashMap<>();
        Student student = studentMapper.selectOne(
            new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId));
        if (student != null && student.getClassId() != null) {
            info.put("classId", student.getClassId());
            Classes cls = classesMapper.selectById(student.getClassId());
            if (cls != null) {
                info.put("className", cls.getClassName());
                info.put("grade", cls.getGrade());
            }
        }
        return info;
    }

    @Override
    public java.util.List<User> getUsersByIds(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return java.util.List.of();
        return userMapper.selectBatchIds(ids);
    }
}
