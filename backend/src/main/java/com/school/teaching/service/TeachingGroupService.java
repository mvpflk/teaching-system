package com.school.teaching.service;

import com.school.teaching.entity.GroupMember;
import com.school.teaching.entity.TeachingGroup;
import java.util.List;
import java.util.Map;

public interface TeachingGroupService {
    /** 根据主键 ID 获取教研组 */
    TeachingGroup getById(Long id);
    List<Map<String, Object>> listAll();
    TeachingGroup create(TeachingGroup g);
    TeachingGroup update(Long id, TeachingGroup g);
    void delete(Long id);
    void addMember(Long groupId, Long teacherId);
    void removeMember(Long groupId, Long teacherId);
    void setLeader(Long groupId, Long teacherId);
    void removeLeader(Long groupId, Long teacherId);
    List<Map<String, Object>> getMembers(Long groupId);
    /** 获取教师所属的教研组ID列表（作为组长或组员） */
    List<Long> getGroupIdsForTeacher(Long teacherId);
    /** 获取教研组第一组长ID */
    Long getFirstLeaderId(Long groupId);
    /** 获取教师所属教研组及其涵盖学科 */
    List<Map<String, Object>> getMyGroupsWithSubjects(Long teacherId);

    /** 根据 userId 查 teacherId 后获取其所属教研组及涵盖学科 */
    List<Map<String, Object>> getMyGroupsByUserId(Long userId);

    /** 获取所有教研组实体列表 */
    List<TeachingGroup> listAllEntities();

    /** 获取教研组成员列表 */
    List<GroupMember> getMembersByGroupId(Long groupId);
}