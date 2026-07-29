package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.TeachingGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeachingGroupServiceImpl implements TeachingGroupService {

    private final TeachingGroupMapper groupMapper;
    private final GroupMemberMapper memberMapper;
    private final TeacherMapper teacherMapper;
    private final UserMapper userMapper;
    private final DictSubjectMapper dictSubjectMapper;

    @Override
    public TeachingGroup getById(Long id) {
        return groupMapper.selectById(id);
    }

    @Override
    public List<Map<String, Object>> listAll() {
        List<TeachingGroup> groups = groupMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();
        for (TeachingGroup g : groups) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", g.getId()); m.put("name", g.getName());
            m.put("subjectIds", g.getSubjectIds()); m.put("stageIds", g.getStageIds());
            m.put("leaderIds", g.getLeaderIds());
            // 组长姓名列表
            if (g.getLeaderIds() != null && !g.getLeaderIds().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    List<Integer> ids = om.readValue(g.getLeaderIds(), List.class);
                    List<String> names = new ArrayList<>();
                    for (Object oid : ids) {
                        Long lid = Long.valueOf(oid.toString());
                        Teacher t = teacherMapper.selectById(lid);
                        if (t != null) {
                            User u = userMapper.selectById(t.getUserId());
                            names.add(u != null ? u.getRealName() : "未知");
                        }
                    }
                    m.put("leaderNames", String.join("、", names));
                } catch (Exception e) { m.put("leaderNames", ""); }
            } else { m.put("leaderNames", ""); }
            // 成员数
            Long memberCount = memberMapper.selectCount(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupType, "TEACHING").eq(GroupMember::getGroupId, g.getId()));
            m.put("memberCount", memberCount);
            result.add(m);
        }
        return result;
    }

    @Override @Transactional
    public TeachingGroup create(TeachingGroup g) {
        groupMapper.insert(g);
        addLeadersFromJson(g.getId(), g.getLeaderIds());
        return g;
    }

    @Override @Transactional
    public TeachingGroup update(Long id, TeachingGroup g) {
        TeachingGroup existing = groupMapper.selectById(id);
        if (existing == null) throw new BusinessException(404, "教研组不存在");
        if (g.getName() != null) existing.setName(g.getName());
        if (g.getSubjectIds() != null) existing.setSubjectIds(g.getSubjectIds());
        if (g.getStageIds() != null) existing.setStageIds(g.getStageIds());
        if (g.getLeaderIds() != null) {
            existing.setLeaderIds(g.getLeaderIds());
            // 同步组长身份到 group_member 表：先删旧组长记录，再插入新组长
            memberMapper.delete(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupType, "TEACHING")
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
            .eq(GroupMember::getGroupType, "TEACHING").eq(GroupMember::getGroupId, id));
    }

    @Override @Transactional
    public void addMember(Long groupId, Long teacherId) {
        Long cnt = memberMapper.selectCount(new LambdaQueryWrapper<GroupMember>()
            .eq(GroupMember::getGroupType, "TEACHING").eq(GroupMember::getGroupId, groupId)
            .eq(GroupMember::getTeacherId, teacherId));
        if (cnt > 0) return;
        GroupMember m = new GroupMember(); m.setGroupType("TEACHING");
        m.setGroupId(groupId); m.setTeacherId(teacherId); m.setRole("MEMBER");
        memberMapper.insert(m);
    }

    @Override @Transactional
    public void removeMember(Long groupId, Long teacherId) {
        memberMapper.delete(new LambdaQueryWrapper<GroupMember>()
            .eq(GroupMember::getGroupType, "TEACHING").eq(GroupMember::getGroupId, groupId)
            .eq(GroupMember::getTeacherId, teacherId));
    }

    @Override @Transactional
    public void setLeader(Long groupId, Long teacherId) {
        TeachingGroup g = groupMapper.selectById(groupId);
        if (g == null) throw new BusinessException(404, "教研组不存在");
        // 解析现组长列表
        List<Long> leaders = parseLeaderIds(g.getLeaderIds());
        if (leaders.contains(teacherId)) return; // 已是组长
        if (leaders.size() >= 2) throw new BusinessException(400, "教研组最多设置2名组长");
        leaders.add(teacherId);
        g.setLeaderIds(toJson(leaders)); groupMapper.updateById(g);
        // 设置组长role
        GroupMember gm = memberMapper.selectOne(new LambdaQueryWrapper<GroupMember>()
            .eq(GroupMember::getGroupType, "TEACHING").eq(GroupMember::getGroupId, groupId)
            .eq(GroupMember::getTeacherId, teacherId));
        if (gm != null) { gm.setRole("LEADER"); memberMapper.updateById(gm); }
        else { gm = new GroupMember(); gm.setGroupType("TEACHING"); gm.setGroupId(groupId); gm.setTeacherId(teacherId); gm.setRole("LEADER"); memberMapper.insert(gm); }
    }

    @Override @Transactional
    public void removeLeader(Long groupId, Long teacherId) {
        TeachingGroup g = groupMapper.selectById(groupId);
        if (g == null) throw new BusinessException(404, "教研组不存在");
        List<Long> leaders = parseLeaderIds(g.getLeaderIds());
        leaders.remove(teacherId);
        g.setLeaderIds(toJson(leaders)); groupMapper.updateById(g);
        // 降级为普通成员
        GroupMember gm = memberMapper.selectOne(new LambdaQueryWrapper<GroupMember>()
            .eq(GroupMember::getGroupType, "TEACHING").eq(GroupMember::getGroupId, groupId)
            .eq(GroupMember::getTeacherId, teacherId));
        if (gm != null) { gm.setRole("MEMBER"); memberMapper.updateById(gm); }
    }

    @Override
    public List<Map<String, Object>> getMyGroupsWithSubjects(Long teacherId) {
        // 查该教师所属的教研组（作为组长或组员）
        List<GroupMember> memberships = memberMapper.selectList(new LambdaQueryWrapper<GroupMember>()
            .eq(GroupMember::getGroupType, "TEACHING").eq(GroupMember::getTeacherId, teacherId));
        Set<Long> groupIds = memberships.stream().map(GroupMember::getGroupId).collect(java.util.stream.Collectors.toSet());
        if (groupIds.isEmpty()) return List.of();

        List<TeachingGroup> groups = groupMapper.selectBatchIds(groupIds);
        // 收集所有 subjectIds
        java.util.Set<Long> allSubjectIds = new java.util.LinkedHashSet<>();
        for (TeachingGroup g : groups) {
            List<Long> sids = parseLeaderIds(g.getSubjectIds()); // 复用parseLeaderIds解析JSON数组
            allSubjectIds.addAll(sids);
        }
        // 查 dict_subject 获取学科名
        List<DictSubject> subjects = dictSubjectMapper.selectBatchIds(allSubjectIds);
        List<Map<String, Object>> result = new ArrayList<>();
        for (DictSubject s : subjects) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId()); m.put("subjectName", s.getSubjectName());
            result.add(m);
        }
        return result;
    }

    // ── 辅助方法 ──
    private List<Long> parseLeaderIds(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Integer> raw = om.readValue(json, List.class);
            List<Long> result = new ArrayList<>();
            for (Object o : raw) result.add(Long.valueOf(o.toString()));
            return result;
        } catch (Exception e) { return new ArrayList<>(); }
    }

    private String toJson(List<Long> ids) {
        try { return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(ids); } catch (Exception e) { return "[]"; }
    }

    private void addLeadersFromJson(Long groupId, String leaderIdsJson) {
        if (leaderIdsJson == null || leaderIdsJson.isEmpty()) return;
        List<Long> ids = parseLeaderIds(leaderIdsJson);
        if (ids.isEmpty()) return;
        // 循环前批量查询已存在成员，避免 N+1
        List<GroupMember> existingMembers = memberMapper.selectList(new LambdaQueryWrapper<GroupMember>()
            .eq(GroupMember::getGroupType, "TEACHING").eq(GroupMember::getGroupId, groupId)
            .in(GroupMember::getTeacherId, ids));
        Map<Long, GroupMember> memberMap = existingMembers.stream()
            .collect(Collectors.toMap(GroupMember::getTeacherId, m -> m, (a, b) -> a));
        for (Long tid : ids) {
            GroupMember gm = memberMap.get(tid);
            if (gm != null) { gm.setRole("LEADER"); memberMapper.updateById(gm); }
            else { gm = new GroupMember(); gm.setGroupType("TEACHING"); gm.setGroupId(groupId); gm.setTeacherId(tid); gm.setRole("LEADER"); memberMapper.insert(gm); }
        }
    }

    @Override
    public List<Long> getGroupIdsForTeacher(Long teacherId) {
        return memberMapper.selectList(new LambdaQueryWrapper<GroupMember>()
            .eq(GroupMember::getGroupType, "TEACHING").eq(GroupMember::getTeacherId, teacherId))
            .stream().map(GroupMember::getGroupId).collect(Collectors.toList());
    }

    @Override
    public Long getFirstLeaderId(Long groupId) {
        TeachingGroup g = groupMapper.selectById(groupId);
        if (g == null) return null;
        List<Long> leaders = parseLeaderIds(g.getLeaderIds());
        return leaders.isEmpty() ? null : leaders.get(0);
    }

    @Override
    public List<Map<String, Object>> getMyGroupsByUserId(Long userId) {
        Teacher t = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
        if (t == null) return List.of();
        return getMyGroupsWithSubjects(t.getId());
    }

    @Override
    public List<Map<String, Object>> getMembers(Long groupId) {
        List<GroupMember> members = memberMapper.selectList(
            new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupType, "TEACHING")
                .eq(GroupMember::getGroupId, groupId));
        if (members.isEmpty()) return List.of();
        // 批量加载教师和用户
        Set<Long> teacherIds = members.stream().map(GroupMember::getTeacherId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Teacher> teacherMap = teacherIds.isEmpty() ? Map.of() :
            teacherMapper.selectBatchIds(teacherIds).stream().collect(Collectors.toMap(Teacher::getId, t -> t));
        Set<Long> userIds = teacherMap.values().stream().map(Teacher::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of() :
            userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
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
    public List<TeachingGroup> listAllEntities() {
        return groupMapper.selectList(null);
    }

    @Override
    public List<GroupMember> getMembersByGroupId(Long groupId) {
        return memberMapper.selectList(
            new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupType, "TEACHING")
                .eq(GroupMember::getGroupId, groupId));
    }
}
