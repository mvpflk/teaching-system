package com.school.teaching.service;

import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {
    Map<String, Object> getProfile(Long userId);
    Map<String, Object> updateProfile(Long userId, Map<String, Object> body);
    void updatePassword(Long userId, String oldPassword, String newPassword, String token);
    Map<String, String> uploadAvatar(Long userId, MultipartFile file);
    /** 管理员重置用户密码，返回明文密码 */
    String adminResetPassword(Long targetUserId, String newPassword);
}
