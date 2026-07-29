package com.school.teaching.agent.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserContextTest {

    @Test
    @DisplayName("isTeacher: TEACHER 和 HEAD_TEACHER 返回 true")
    void teacherRoles() {
        UserContext teacher = UserContext.builder().roleName("TEACHER").build();
        UserContext headTeacher = UserContext.builder().roleName("HEAD_TEACHER").build();
        assertTrue(teacher.isTeacher());
        assertTrue(headTeacher.isTeacher());
    }

    @Test
    @DisplayName("isTeacher: STUDENT/ADMIN/INSPECTOR 返回 false")
    void nonTeacherRoles() {
        assertFalse(UserContext.builder().roleName("STUDENT").build().isTeacher());
        assertFalse(UserContext.builder().roleName("ADMIN").build().isTeacher());
        assertFalse(UserContext.builder().roleName("INSPECTOR").build().isTeacher());
    }

    @Test
    @DisplayName("isStudent: STUDENT 返回 true，其他角色 false")
    void studentRole() {
        assertTrue(UserContext.builder().roleName("STUDENT").build().isStudent());
        assertFalse(UserContext.builder().roleName("TEACHER").build().isStudent());
        assertFalse(UserContext.builder().roleName("ADMIN").build().isStudent());
    }

    @Test
    @DisplayName("isAdmin: ADMIN 和 SUPER_ADMIN 返回 true")
    void adminRoles() {
        assertTrue(UserContext.builder().roleName("ADMIN").build().isAdmin());
        assertTrue(UserContext.builder().roleName("SUPER_ADMIN").build().isAdmin());
        assertFalse(UserContext.builder().roleName("TEACHER").build().isAdmin());
    }

    @Test
    @DisplayName("isInspector: INSPECTOR 返回 true")
    void inspectorRole() {
        assertTrue(UserContext.builder().roleName("INSPECTOR").build().isInspector());
        assertFalse(UserContext.builder().roleName("TEACHER").build().isInspector());
    }

    @Test
    @DisplayName("isTeacherOrAbove: 教师/巡视员/管理员均返回 true")
    void teacherOrAbove() {
        assertTrue(UserContext.builder().roleName("TEACHER").build().isTeacherOrAbove());
        assertTrue(UserContext.builder().roleName("INSPECTOR").build().isTeacherOrAbove());
        assertTrue(UserContext.builder().roleName("ADMIN").build().isTeacherOrAbove());
        assertFalse(UserContext.builder().roleName("STUDENT").build().isTeacherOrAbove());
    }

    @Test
    @DisplayName("canAccessSubject: 管理员/巡视员可访问任何学科")
    void adminCanAccessAnySubject() {
        assertTrue(UserContext.builder().roleName("ADMIN").build().canAccessSubject("数学"));
        assertTrue(UserContext.builder().roleName("INSPECTOR").build().canAccessSubject("任何学科"));
    }

    @Test
    @DisplayName("canAccessSubject: 无限制时返回 true")
    void noRestrictionReturnsTrue() {
        assertTrue(UserContext.builder().roleName("TEACHER").build().canAccessSubject("数学"));
    }

    @Test
    @DisplayName("canAccessSubject: 教师只能访问所授学科")
    void teacherSubjectRestriction() {
        UserContext teacher = UserContext.builder()
                .roleName("TEACHER")
                .subjects(Set.of("计算机应用基础"))
                .build();
        assertTrue(teacher.canAccessSubject("计算机应用基础"));
        assertFalse(teacher.canAccessSubject("数学"));
    }

    @Test
    @DisplayName("canAccessSubject: 学生只能访问其专业关联学科")
    void studentSubjectRestriction() {
        UserContext student = UserContext.builder()
                .roleName("STUDENT")
                .accessibleSubjects(Set.of("语文", "数学", "计算机应用基础"))
                .build();
        assertTrue(student.canAccessSubject("语文"));
        assertTrue(student.canAccessSubject("数学"));
        assertFalse(student.canAccessSubject("英语"));
    }
}