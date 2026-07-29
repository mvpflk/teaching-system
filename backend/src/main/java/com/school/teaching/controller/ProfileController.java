package com.school.teaching.controller;

import com.school.teaching.annotation.AuditLog;
import com.school.teaching.annotation.AuditEventType;

import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    @Autowired private ProfileService profileService;

    @GetMapping("/actions/info")
    public R<Map<String, Object>> getProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        return R.ok(profileService.getProfile(userId));
    }

    @PutMapping("/actions/update")
    @AuditLog(eventType = AuditEventType.OTHER, description = "更新个人信息")
    public R<String> updateProfile(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        profileService.updateProfile(userId, body);
        return R.ok("更新成功");
    }

    @PutMapping("/actions/password")
    @AuditLog(eventType = AuditEventType.PASSWORD_CHANGE, description = "修改密码")
    public R<String> updatePassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        if (body.get("oldPassword") == null || body.get("newPassword") == null) return R.error("请填写旧密码和新密码");
        String token = (String) request.getAttribute("JWT_TOKEN");
        profileService.updatePassword(userId, body.get("oldPassword"), body.get("newPassword"), token);
        return R.ok("密码修改成功，请重新登录");
    }

    @PostMapping("/actions/avatar")
    @AuditLog(eventType = AuditEventType.OTHER, description = "上传头像")
    public R<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        if (file.isEmpty()) return R.error("请选择文件");
        return R.ok(profileService.uploadAvatar(userId, file), "上传成功");
    }
}
