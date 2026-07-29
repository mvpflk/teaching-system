package com.school.teaching.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String role) {
        CustomUserDetails details = new CustomUserDetails(
                1L, "testuser", role, 1L, "jti-1", 1L, 1L);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getCurrentUser_未登录返回null() {
        assertNull(SecurityUtils.getCurrentUser());
    }

    @Test
    void getCurrentUserId_已登录返回ID() {
        loginAs("STUDENT");
        assertEquals(1L, SecurityUtils.getCurrentUserId());
    }

    @Test
    void getCurrentUsername_已登录返回用户名() {
        loginAs("TEACHER");
        assertEquals("testuser", SecurityUtils.getCurrentUsername());
    }

    @Test
    void getCurrentRole_已登录返回角色() {
        loginAs("ADMIN");
        assertEquals("ADMIN", SecurityUtils.getCurrentRole());
    }

    @Test
    void isAdmin_ADMIN角色返回true() {
        loginAs("ADMIN");
        assertTrue(SecurityUtils.isAdmin());
    }

    @Test
    void isAdmin_STUDENT角色返回false() {
        loginAs("STUDENT");
        assertFalse(SecurityUtils.isAdmin());
    }

    @Test
    void isSuperAdmin_SUPER_ADMIN返回true() {
        loginAs("SUPER_ADMIN");
        assertTrue(SecurityUtils.isSuperAdmin());
    }

    @Test
    void isSuperAdmin_ADMIN返回false() {
        loginAs("ADMIN");
        assertFalse(SecurityUtils.isSuperAdmin());
    }

    @Test
    void isTeacherOrAdmin_教师和管理员返回true() {
        loginAs("TEACHER");
        assertTrue(SecurityUtils.isTeacherOrAdmin());
        loginAs("HEAD_TEACHER");
        assertTrue(SecurityUtils.isTeacherOrAdmin());
        loginAs("ADMIN");
        assertTrue(SecurityUtils.isTeacherOrAdmin());
    }

    @Test
    void isStudent_STUDENT返回true() {
        loginAs("STUDENT");
        assertTrue(SecurityUtils.isStudent());
    }

    @Test
    void isParent_PARENT返回true() {
        loginAs("PARENT");
        assertTrue(SecurityUtils.isParent());
    }

    @Test
    void isAdminByRole_字符串参数() {
        assertTrue(SecurityUtils.isAdminByRole("ADMIN"));
        assertTrue(SecurityUtils.isAdminByRole("SUPER_ADMIN"));
        assertFalse(SecurityUtils.isAdminByRole("TEACHER"));
    }

    @Test
    void isTeacherByRole_字符串参数() {
        assertTrue(SecurityUtils.isTeacherByRole("TEACHER"));
        assertTrue(SecurityUtils.isTeacherByRole("HEAD_TEACHER"));
        assertTrue(SecurityUtils.isTeacherByRole("ADMIN"));
        assertFalse(SecurityUtils.isTeacherByRole("STUDENT"));
    }

    @Test
    void getCurrentSchoolId_已登录返回学校ID() {
        loginAs("STUDENT");
        assertEquals(1L, SecurityUtils.getCurrentSchoolId());
    }

    @Test
    void 方法在无认证时返回null() {
        assertNull(SecurityUtils.getCurrentUserId());
        assertNull(SecurityUtils.getCurrentUsername());
        assertNull(SecurityUtils.getCurrentRole());
    }
}
