package com.school.teaching.service;

import com.school.teaching.entity.LessonPrepGroup;
import java.util.List;
import java.util.Map;

public interface LessonPrepGroupService {
    List<Map<String, Object>> listAll();
    LessonPrepGroup create(LessonPrepGroup g);
    LessonPrepGroup update(Long id, LessonPrepGroup g);
    void delete(Long id);
    void addMember(Long groupId, Long teacherId);
    void removeMember(Long groupId, Long teacherId);
    void setLeader(Long groupId, Long teacherId);
    /** 根据班级信息查找匹配的备课组 */
    LessonPrepGroup findByClassInfo(Long stageId, Long gradeId, Long subjectId);
    /** 获取备课组第一组长teacher_id */
    Long getFirstLeaderId(Long groupId);
    /** 获取备课组成员列表 */
    List<Map<String, Object>> getMembers(Long groupId);
    /** 获取教师所属的备课组ID列表 */
    List<Long> getGroupIdsForTeacher(Long teacherId);
}
