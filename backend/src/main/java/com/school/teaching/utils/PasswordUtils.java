package com.school.teaching.utils;

import java.security.SecureRandom;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordUtils {

    private static final SecureRandom RNG = new SecureRandom();

    /** 生成8位随机密码（大小写字母+数字） */
    public static String generateRandomPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) sb.append(chars.charAt(RNG.nextInt(chars.length())));
        return sb.toString();
    }

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        if (encodedPassword == null) {
            return false;
        }
        // BCrypt hash starts with $2a$, $2b$, or $2y$
        if (encodedPassword.startsWith("$2")) {
            return ENCODER.matches(rawPassword, encodedPassword);
        }
        // Legacy: old custom MD5+salt format
        if (encodedPassword.contains("$")) {
            return LegacyPasswordUtils.matches(rawPassword, encodedPassword);
        }
        return false;
    }

    /** Check if a password hash uses the current BCrypt format. */
    public static boolean isCurrentFormat(String encodedPassword) {
        return encodedPassword != null && encodedPassword.startsWith("$2");
    }

    /** Legacy MD5+salt verification, kept for migration. */
    private static class LegacyPasswordUtils {
        static boolean matches(String rawPassword, String encodedPassword) {
            String[] parts = encodedPassword.split("\\$");
            if (parts.length != 2) return false;
            String salt = parts[0];
            String expected = parts[1];
            String actual = org.springframework.util.DigestUtils.md5DigestAsHex(
                (salt + rawPassword + "teaching_system_salt").getBytes());
            return expected.equals(actual);
        }
    }
}
