package com.school.teaching.service;

import com.school.teaching.entity.GroupMember;
import com.school.teaching.entity.StudentGroup;

import java.util.List;
import java.util.Map;

public interface GroupService {
    /** 获取班级的所有分组 */
    List<StudentGroup> getGroups(Long classId);

    /** 创建分组 */
    StudentGroup createGroup(Long classId, String name, Long operatorUserId);

    /** 删除分组 */
    void deleteGroup(Long groupId, Long operatorUserId);

    /** 获取分组成员（学生ID列表） */
    List<Long> getMemberIds(Long groupId);

    /** 获取分组成员详情（含姓名、学号） */
    List<Map<String, Object>> getMembers(Long groupId);

    /** 添加学生到分组 */
    void addMember(Long groupId, Long studentId, Long operatorUserId);

    /** 从分组移除学生 */
    void removeMember(Long groupId, Long studentId, Long operatorUserId);

    /** 获取学生在各班级的所有分组ID */
    List<Long> getStudentGroupIds(Long studentId);
}
