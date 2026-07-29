package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.LessonPrepGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class LessonPrepGroupServiceImpl implements LessonPrepGroupService {

    private final LessonPrepGroupMapper groupMapper;
    private final GroupMemberMapper memberMapper;
    private final TeacherMapper teacherMapper;
    @Autowired private UserMapper userMapper;

    @Override
    public List<Map<String, Object>> listAll() {
        List<LessonPrepGroup> groups = groupMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();
        for (LessonPrepGroup g : groups) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", g.getId()); m.put("name", g.getName());
            m.put("teachingGroupId", g.getTeachingGroupId()); m.put("gradeId", g.getGradeId());
            m.put("subjectId", g.getSubjectId()); m.put("classType", g.getClassType());
            m.put("stageId", g.getStageId()); m.put("leaderIds", g.getLeaderIds());
            m.put("leaderName", getFirstLeaderName(g.getLeaderIds()));
            Long memberCount = memberMapper.selectCount(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupType, "LESSON_PREP").eq(GroupMember::getGroupId, g.getId()));
            m.put("memberCount", memberCount);
            result.add(m);
        }
        return result;
    }

    @Override @Transactional
    public LessonPrepGroup create(LessonPrepGroup g) {
        groupMapper.insert(g);
        addLeadersFromJson(g.getId(), g.getLeaderIds());
        return g;
    }

    @Override @Transactional
    public LessonPrepGroup update(Long id, LessonPrepGroup g) {
        LessonPrepGroup existing = groupMapper.selectById(id);
        if (existing == null) throw new BusinessException(404, "备课组不存在");
        if (g.getName() != null) existing.setName(g.getName());
        if (g.getTeachingGroupId() != null) existing.setTeachingGroupId(g.getTeachingGroupId());
        if (g.getGradeId() != null) existing.setGradeId(g.getGradeId());
        if (g.getSubjectId() != null) existing.setSubjectId(g.getSubjectId());
        if (g.getClassType() != null) existing.setClassType(g.getClassType());
        if (g.getStageId() != null) existing.setStageId(g.getStageId());
        if (g.getLeaderIds() != null) {
            existing.setLeaderIds(g.getLeaderIds());
            // 同步组长身份到 group_member 表：先删旧组长记录，再插入新组长
            memberMapper.delete(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupType, "LESSON_PREP")
                .eq(GroupMember::getGroupId, id)
                .eq(GroupMember::getRole, "LEADER"));
            addLeadersFromJson(id, g.getLeaderIds());
        }
        groupMapper.updateById(existing);
        return existing;
    }

    @Override @Transactional
    public void delete(Long id) {
        groupMapper.deleteById(id);
        memberMapper.delete(new LambdaQueryWrapper<GroupMember>()
            .eq(GroupMember::getGroupType, "LESSON_PREP").eq(GroupMember::getGroupId, id));
    }

    @Override @Transactional
    public void addMember(Long groupId, Long teacherId) {
        Long cnt = memberMapper.selectCount(new LambdaQueryWrapper<GroupMember>()
            .eq(GroupMember::getGroupType, "LESSON_PREP").eq(GroupMember::getGroupId, groupId)
            .eq(GroupMember::getTeacherId, teacherId));
        if (cnt > 0) return;
        GroupMember m = new GroupMember(); m.setGroupType("LESSON_PREP");
        m.setGroupId(groupId); m.setTeacherId(teacherId); m.setRole("MEMBER");
        memberMapper.insert(m);
    }

    @Override @Transactional
    public void removeMember(Long groupId, Long teacherId) {
        memberMapper.delete(new LambdaQueryWrapper<GroupMember>()
            .eq(GroupMember::getGroupType, "LESSON_PREP").eq(GroupMember::getGroupId, groupId)
            .eq(GroupMember::getTeacherId, teacherId));
    }

    @Override @Transactional
    public void setLeader(Long groupId, Long teacherId) {
        LessonPrepGroup g = groupMapper.selectById(groupId);
        if (g == null) throw new BusinessException(404, "备课组不存在");
        List<Long> leaders = parseLeaderIds(g.getLeaderIds());
        if (leaders.contains(teacherId)) return;
        if (leaders.size() >= 2) throw new BusinessException(400, "备课组最多设置2名组长");
        leaders.add(teacherId);
        g.setLeaderIds(toJson(leaders)); groupMapper.updateById(g);
        upsertMember(groupId, teacherId, "LEADER");
    }

    @Override
    public LessonPrepGroup findByClassInfo(Long stageId, Long gradeId, Long subjectId) {
        return groupMapper.selectOne(new LambdaQueryWrapper<LessonPrepGroup>()
            .eq(LessonPrepGroup::getStageId, stageId)
            .eq(gradeId != null, LessonPrepGroup::getGradeId, gradeId)
            .eq(subjectId != null, LessonPrepGroup::getSubjectId, subjectId)
            .last("LIMIT 1"));
    }

    @Override
    public Long getFirstLeaderId(Long groupId) {
        LessonPrepGroup g = groupMapper.selectById(groupId);
        if (g == null) return null;
        List<Long> leaders = parseLeaderIds(g.getLeaderIds());
        return leaders.isEmpty() ? null : leaders.get(0);
    }

    // ── 辅助方法 ──
    private List<Long> parseLeaderIds(String json) {
        if (json == null || json.isEmpty()) return new java.util.ArrayList<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<Integer> raw = om.readValue(json, java.util.List.class);
            java.util.List<Long> result = new java.util.ArrayList<>();
            for (Object o : raw) result.add(Long.valueOf(o.toString()));
            return result;
        } catch (Exception e) { return new java.util.ArrayList<>(); }
    }
    private String toJson(java.util.List<Long> ids) {
        try { return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(ids); } catch (Exception e) { return "[]"; }
    }
    private void addLeadersFromJson(Long groupId, String json) {
        if (json == null || json.isEmpty()) return;
        java.util.List<Long> ids = parseLeaderIds(json);
        for (Long tid : ids) upsertMember(groupId, tid, "LEADER");
    }
    private void upsertMember(Long groupId, Long teacherId, String role) {
        GroupMember gm = memberMapper.selectOne(new LambdaQueryWrapper<GroupMember>()
            .eq(GroupMember::getGroupType, "LESSON_PREP").eq(GroupMember::getGroupId, groupId)
            .eq(GroupMember::getTeacherId, teacherId));
        if (gm != null) { gm.setRole(role); memberMapper.updateById(gm); }
        else { gm = new GroupMember(); gm.setGroupType("LESSON_PREP"); gm.setGroupId(groupId); gm.setTeacherId(teacherId); gm.setRole(role); memberMapper.insert(gm); }
    }
    private String getFirstLeaderName(String json) {
        java.util.List<Long> ids = parseLeaderIds(json);
        if (ids.isEmpty()) return "";
        Teacher t = teacherMapper.selectById(ids.get(0));
        if (t != null) {
            User u = userMapper.selectById(t.getUserId());
            return u != null ? u.getRealName() : "";
        }
        return "";
    }

    @Override
    public List<Map<String, Object>> getMembers(Long groupId) {
        List<GroupMember> members = memberMapper.selectList(
            new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupType, "LESSON_PREP")
                .eq(GroupMember::getGroupId, groupId));
        if (members.isEmpty()) return List.of();
        // 批量加载教师和用户
        java.util.Set<Long> teacherIds = members.stream().map(GroupMember::getTeacherId).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        Map<Long, Teacher> teacherMap = teacherIds.isEmpty() ? Map.of() :
            teacherMapper.selectBatchIds(teacherIds).stream().collect(java.util.stream.Collectors.toMap(Teacher::getId, t -> t));
        java.util.Set<Long> userIds = teacherMap.values().stream().map(Teacher::getUserId).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of() :
            userMapper.selectBatchIds(userIds).stream().collect(java.util.stream.Collectors.toMap(User::getId, u -> u));
        List<Map<String, Object>> result = new ArrayList<>();
        for (GroupMember gm : members) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", gm.getId()); m.put("teacherId", gm.getTeacherId()); m.put("role", gm.getRole());
            Teacher t = teacherMap.get(gm.getTeacherId());
            if (t != null) {
                User u = userMap.get(t.getUserId());
                m.put("name", u != null ? u.getRealName() : "未知");
                m.put("teacherNumber", t.getTeacherNumber());
            }
            result.add(m);
        }
        return result;
    }

    @Override
    public List<Long> getGroupIdsForTeacher(Long teacherId) {
        return memberMapper.selectList(new LambdaQueryWrapper<GroupMember>()
            .eq(GroupMember::getGroupType, "LESSON_PREP").eq(GroupMember::getTeacherId, teacherId))
            .stream().map(GroupMember::getGroupId).collect(java.util.stream.Collectors.toList());
    }
}
