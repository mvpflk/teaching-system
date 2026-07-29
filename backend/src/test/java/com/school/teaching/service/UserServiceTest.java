package com.school.teaching.service;

import com.school.teaching.entity.User;
import com.school.teaching.mapper.UserMapper;
import com.school.teaching.utils.PasswordUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("需要 MySQL 数据库 — 本地无 MySQL 时跳过，CI/部署环境自动运行")
@SpringBootTest
@ActiveProfiles("local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql(scripts = {"/schema.sql", "/data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserServiceTest {

    @Autowired private UserService userService;
    @Autowired private UserMapper userMapper;

    /** 将测试种子数据中的明文密码升级为 BCrypt 哈希（模拟生产环境登录自动升级流程） */
    @BeforeEach
    void upgradePasswords() {
        List<User> users = userMapper.selectList(null);
        for (User u : users) {
            if (u.getPassword() != null && !u.getPassword().startsWith("$2")) {
                u.setPassword(PasswordUtils.encode(u.getPassword()));
                userMapper.updateById(u);
            }
        }
    }

    @Test
    void login_withCorrectCredentials_shouldReturnUser() {
        User user = userService.login("admin", "admin123");
        assertNotNull(user);
        assertEquals("admin", user.getUsername());
        assertEquals("Admin", user.getRealName());
    }

    @Test
    void login_withWrongPassword_shouldReturnNull() {
        User user = userService.login("admin", "wrongpassword");
        assertNull(user);
    }

    @Test
    void login_withNonExistentUser_shouldReturnNull() {
        User user = userService.login("nonexistent", "password");
        assertNull(user);
    }

    @Test
    void login_shouldAutoUpgradePlaintextPassword() {
        // First login — data is BCrypt already from @BeforeEach
        User user = userService.login("teacher1", "test123");
        assertNotNull(user);
        User reloaded = userService.getUserById(user.getId());
        assertNotNull(reloaded.getPassword());
        assertTrue(reloaded.getPassword().startsWith("$2"),
            "Password should be BCrypt, got: " + reloaded.getPassword().substring(0, 3) + "...");
    }

    @Test
    void getUserById_shouldReturnCorrectUser() {
        User user = userService.getUserById(1L);
        assertNotNull(user);
        assertEquals("admin", user.getUsername());
    }

    @Test
    void getUserById_nonExistent_shouldReturnNull() {
        User user = userService.getUserById(999L);
        assertNull(user);
    }

    @Test
    void getUserByUsername_shouldReturnCorrectUser() {
        User user = userService.getUserByUsername("student1");
        assertNotNull(user);
        assertEquals("Student Li", user.getRealName());
    }

    @Test
    void login_withBCryptAfterUpgrade_shouldStillWork() {
        User user1 = userService.login("teacher1", "test123");
        assertNotNull(user1);
        User user2 = userService.login("teacher1", "test123");
        assertNotNull(user2);
    }
}