package com.school.teaching.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilsTest {

    @Test
    void generateRandomPassword_长度为8() {
        String pwd = PasswordUtils.generateRandomPassword();
        assertEquals(8, pwd.length());
    }

    @Test
    void generateRandomPassword_不含易混淆字符() {
        for (int i = 0; i < 100; i++) {
            String pwd = PasswordUtils.generateRandomPassword();
            assertFalse(pwd.contains("I"), "不应包含 I");
            assertFalse(pwd.contains("l"), "不应包含 l");
            assertFalse(pwd.contains("O"), "不应包含 O");
            assertFalse(pwd.contains("0"), "不应包含 0");
            assertFalse(pwd.contains("1"), "不应包含 1");
        }
    }

    @Test
    void generateRandomPassword_每次生成不同() {
        String pwd1 = PasswordUtils.generateRandomPassword();
        String pwd2 = PasswordUtils.generateRandomPassword();
        // 极小概率相同，但 100 次循环足够验证随机性
        assertNotEquals(pwd1, pwd2);
    }

    @Test
    void encode_生成BCrypt哈希() {
        String encoded = PasswordUtils.encode("test123");
        assertNotNull(encoded);
        assertTrue(encoded.startsWith("$2"), "应为 BCrypt 格式");
    }

    @Test
    void encode_相同密码生成不同哈希() {
        String h1 = PasswordUtils.encode("test123");
        String h2 = PasswordUtils.encode("test123");
        assertNotEquals(h1, h2, "BCrypt 含随机盐，相同密码应产生不同哈希");
    }

    @Test
    void matches_正确密码返回true() {
        String raw = "mypassword";
        String encoded = PasswordUtils.encode(raw);
        assertTrue(PasswordUtils.matches(raw, encoded));
    }

    @Test
    void matches_错误密码返回false() {
        String encoded = PasswordUtils.encode("correct");
        assertFalse(PasswordUtils.matches("wrong", encoded));
    }

    @Test
    void matches_null编码返回false() {
        assertFalse(PasswordUtils.matches("test", null));
    }

    @Test
    void isCurrentFormat_BCrypt格式返回true() {
        String encoded = PasswordUtils.encode("test");
        assertTrue(PasswordUtils.isCurrentFormat(encoded));
    }

    @Test
    void isCurrentFormat_null返回false() {
        assertFalse(PasswordUtils.isCurrentFormat(null));
    }

    @Test
    void isCurrentFormat_旧格式返回false() {
        assertFalse(PasswordUtils.isCurrentFormat("abc$def"));
    }
}
