package com.school.teaching.service;

import com.school.teaching.entity.TeacherQuickComment;

import java.util.List;
import java.util.Map;

public interface TeacherService {

    /** 获取教师的 teacher 记录ID */
    Long getTeacherIdByUserId(Long userId);

    /** 根据 userId 获取教师实体 */
    com.school.teaching.entity.Teacher getTeacherEntityByUserId(Long userId);

    /** 获取教师任教的班级ID列表 */
    List<Long> getTeachingClassIds(Long userId);

    /** 获取教师可管理的班级ID列表（任教 + 担任班主任） */
    List<Long> getAccessibleClassIds(Long userId);

    /** 获取教师任教的科目列表 */
    List<String> getTeachingSubjects(Long userId);

    /** 获取教师任教的科目列表（含学科ID，从dict_subject匹配） */
    List<Map<String, Object>> getTeachingSubjectsWithIds(Long userId);

    /** 判断用户是否是班主任 */
    boolean isHeadTeacher(Long userId);

    /** 判断用户是否是教师或管理员 */
    boolean isTeacherOrAdmin(Long roleId);

    /** 获取教师任课配置（含班级名称） */
    List<Map<String, Object>> getTeachingAssignments(Long userId);

    /** 设置任课（先删后增） */
    void setTeachingAssignments(Long userId, List<Map<String, Object>> assignments);

    /** 获取教师的任课摘要（前端用） */
    Map<String, Object> getTeacherSummary(Long userId);

    // Admin management methods
    Map<String, Object> adminListTeachers(String keyword);
    /** 返回简化的教师列表 [{id, name, username}]，用于班级管理下拉框 */
    List<Map<String, Object>> adminListSimpleTeachers();
    Map<String, Object> adminGetTeacher(Long userId);
    Map<String, Object> adminCreateTeacher(Map<String, Object> body);
    void adminUpdateTeacher(Long userId, Map<String, Object> body);
    void adminDeleteTeacher(Long userId);
    void adminSetHeadClass(Long userId, Long classId);

    /** 快捷评语 — 获取教师自己的列表 */
    List<TeacherQuickComment> getQuickComments(Long teacherId);

    /** 快捷评语 — 添加一条 */
    TeacherQuickComment addQuickComment(Long teacherId, String commentText);

    /** 快捷评语 — 删除一条（带教师归属校验） */
    void deleteQuickComment(Long commentId, Long teacherId);

    /** 判断教师是否任教指定班级 */
    boolean isTeacherOfClass(Long teacherId, Long classId);

    /** 判断用户是否任教指定班级（通过 userId） */
    boolean isUserTeacherOfClass(Long userId, Long classId);

    /** 根据 ID 列表批量获取教师 */
    List<com.school.teaching.entity.Teacher> getTeachersByIds(java.util.Collection<Long> ids);
}
