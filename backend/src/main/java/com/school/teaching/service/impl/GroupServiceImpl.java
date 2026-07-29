package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupServiceImpl implements GroupService {

    @Autowired private StudentGroupMapper groupMapper;
    @Autowired private StudentGroupMemberMapper memberMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private ClassesMapper classesMapper;

    private void checkClassOwnership(Long classId, Long operatorUserId) {
        if (SecurityUtils.isAdmin()) return;
        Classes c = classesMapper.selectById(classId);
        if (c == null) throw new BusinessException(404, "班级不存在");
        if (!Objects.equals(c.getHeadTeacherId(), operatorUserId))
            throw new BusinessException(403, "仅班主任可管理分组");
    }

    @Override
    public List<StudentGroup> getGroups(Long classId) {
        return groupMapper.selectList(new LambdaQueryWrapper<StudentGroup>()
                .eq(StudentGroup::getClassId, classId)
                .orderByAsc(StudentGroup::getSortOrder));
    }

    @Override
    @Transactional
    public StudentGroup createGroup(Long classId, String name, Long operatorUserId) {
        checkClassOwnership(classId, operatorUserId);
        Long cnt = groupMapper.selectCount(new LambdaQueryWrapper<StudentGroup>()
                .eq(StudentGroup::getClassId, classId)
                .eq(StudentGroup::getName, name));
        if (cnt > 0) throw new BusinessException(409, "该班级已有同名分组");
        StudentGroup g = new StudentGroup();
        g.setClassId(classId);
        g.setName(name);
        g.setSortOrder(0);
        groupMapper.insert(g);
        return g;
    }

    @Override
    @Transactional
    public void deleteGroup(Long groupId, Long operatorUserId) {
        StudentGroup g = groupMapper.selectById(groupId);
        if (g == null) return;
        checkClassOwnership(g.getClassId(), operatorUserId);
        memberMapper.delete(new LambdaQueryWrapper<StudentGroupMember>().eq(StudentGroupMember::getGroupId, groupId));
        groupMapper.deleteById(groupId);
    }

    @Override
    public List<Long> getMemberIds(Long groupId) {
        return memberMapper.selectList(new LambdaQueryWrapper<StudentGroupMember>()
                .eq(StudentGroupMember::getGroupId, groupId))
                .stream().map(StudentGroupMember::getStudentId).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getMembers(Long groupId) {
        List<StudentGroupMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<StudentGroupMember>().eq(StudentGroupMember::getGroupId, groupId));
        if (members.isEmpty()) return List.of();
        Set<Long> sids = members.stream().map(StudentGroupMember::getStudentId).collect(Collectors.toSet());
        List<Student> students = studentMapper.selectBatchIds(sids);
        Set<Long> uids = students.stream().map(Student::getUserId).collect(Collectors.toSet());
        Map<Long, User> uMap = userMapper.selectBatchIds(uids).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        List<Map<String, Object>> result = new ArrayList<>();
        for (StudentGroupMember m : members) {
            Student s = students.stream().filter(st -> st.getId().equals(m.getStudentId())).findFirst().orElse(null);
            if (s == null) continue;
            User u = uMap.get(s.getUserId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("studentId", s.getId());
            item.put("studentName", u != null ? u.getRealName() : "?");
            item.put("studentNumber", s.getStudentNumber());
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional
    public void addMember(Long groupId, Long studentId, Long operatorUserId) {
        StudentGroup g = groupMapper.selectById(groupId);
        if (g == null) throw new BusinessException(404, "分组不存在");
        checkClassOwnership(g.getClassId(), operatorUserId);
        Long cnt = memberMapper.selectCount(new LambdaQueryWrapper<StudentGroupMember>()
                .eq(StudentGroupMember::getGroupId, groupId)
                .eq(StudentGroupMember::getStudentId, studentId));
        if (cnt > 0) return;
        StudentGroupMember m = new StudentGroupMember();
        m.setGroupId(groupId);
        m.setStudentId(studentId);
        memberMapper.insert(m);
    }

    @Override
    @Transactional
    public void removeMember(Long groupId, Long studentId, Long operatorUserId) {
        StudentGroup g = groupMapper.selectById(groupId);
        if (g == null) return;
        checkClassOwnership(g.getClassId(), operatorUserId);
        memberMapper.delete(new LambdaQueryWrapper<StudentGroupMember>()
                .eq(StudentGroupMember::getGroupId, groupId)
                .eq(StudentGroupMember::getStudentId, studentId));
    }

    @Override
    public List<Long> getStudentGroupIds(Long studentId) {
        return memberMapper.selectList(new LambdaQueryWrapper<StudentGroupMember>()
                .eq(StudentGroupMember::getStudentId, studentId))
                .stream().map(StudentGroupMember::getGroupId).collect(Collectors.toList());
    }
}
