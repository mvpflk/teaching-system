package com.school.teaching.agent.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {

    private Long userId;
    private String username;
    private String roleName;
    private Long schoolId;
    private Long stageId;

    private Long teacherId;
    private Long studentId;
    private Long classId;

    private Set<Long> accessibleClassIds;

    // ── 学生身份隔离 ──
    /** 学生所属专业（从 classes.major 查） */
    private String major;
    /** 学生可访问的学科集合（文化课 + 专业课） */
    @Builder.Default
    private Set<String> accessibleSubjects = Collections.emptySet();

    // ── 教师学科限制 ──
    /** 教师所授学科集合（从 teacher_classes.subject 收集） */
    @Builder.Default
    private Set<String> subjects = Collections.emptySet();
    /** 是否班主任（classes.head_teacher_id == teacher.id） */
    private boolean isHeadTeacher;

    /** 教师任教班级名称映射（classId → className），注入 system prompt 让 AI 知道"我是谁" */
    @Builder.Default
    private Map<Long, String> classNames = Collections.emptyMap();

    public boolean isTeacher() {
        return "TEACHER".equals(roleName) || "HEAD_TEACHER".equals(roleName);
    }

    public boolean isStudent() {
        return "STUDENT".equals(roleName);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(roleName) || "SUPER_ADMIN".equals(roleName);
    }

    public boolean isInspector() {
        return "INSPECTOR".equals(roleName);
    }

    public boolean isTeacherOrAbove() {
        return isTeacher() || isInspector() || isAdmin();
    }

    /** 检查指定学科是否在用户允许范围内 */
    public boolean canAccessSubject(String subject) {
        if (isAdmin() || isInspector()) return true;
        if (accessibleSubjects.isEmpty() && subjects.isEmpty()) return true; // 未配置则不限制
        Set<String> allowed = isStudent() ? accessibleSubjects : subjects;
        if (allowed.isEmpty()) return true;
        return allowed.stream().anyMatch(s ->
                s.equals(subject) || s.contains(subject) || subject.contains(s));
    }
}
