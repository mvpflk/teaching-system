package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.school.teaching.entity.AuditLog;
import com.school.teaching.entity.CreditTransaction;
import com.school.teaching.entity.Student;
import com.school.teaching.entity.User;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.AuditLogMapper;
import com.school.teaching.mapper.CreditTransactionMapper;
import com.school.teaching.mapper.StudentMapper;
import com.school.teaching.mapper.UserMapper;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.ProfileService;
import com.school.teaching.utils.JwtUtils;
import com.school.teaching.utils.PasswordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class ProfileServiceImpl implements ProfileService {

    @Autowired private UserMapper userMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private CreditTransactionMapper creditTransactionMapper;
    @Autowired private com.school.teaching.mapper.AuditLogMapper auditLogMapper;
    @Autowired private JwtUtils jwtUtils;
    @Autowired(required = false) private CacheManager cacheManager;

    @Value("${teaching.upload-dir:uploads/avatars}")
    private String uploadDir;

    @Override
    public Map<String, Object> getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId()); info.put("username", user.getUsername());
        info.put("realName", user.getRealName()); info.put("email", user.getEmail());
        info.put("phone", user.getPhone()); info.put("avatarUrl", user.getAvatarUrl());
        return info;
    }

    @Override @Transactional
    public Map<String, Object> updateProfile(Long userId, Map<String, Object> body) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");

        // 学生不允许自行修改姓名
        if ("STUDENT".equals(user.getRoleName()) && body.get("realName") != null) {
            throw new BusinessException(403, "学生无法自行修改姓名，请联系班主任或管理员");
        }

        // 记录变更前的值用于审计
        String oldRealName = user.getRealName();
        String oldEmail = user.getEmail();
        String oldPhone = user.getPhone();

        if (body.get("email") != null) user.setEmail((String) body.get("email"));
        if (body.get("phone") != null) user.setPhone((String) body.get("phone"));
        if (body.get("realName") != null) user.setRealName((String) body.get("realName"));
        userMapper.updateById(user);

        // 审计：姓名变更记录 old/new 值
        String newRealName = user.getRealName();
        if (oldRealName != null && !oldRealName.equals(newRealName)) {
            try {
                com.school.teaching.entity.AuditLog al = new com.school.teaching.entity.AuditLog();
                al.setUserId(userId);
                al.setUsername(user.getUsername());
                al.setRole(SecurityUtils.getCurrentRole() != null ? SecurityUtils.getCurrentRole() : "STUDENT");
                al.setEventType("USER_UPDATE");
                al.setDescription("修改姓名");
                al.setTargetTable("users");
                al.setTargetId(userId);
                al.setOldValue("{\"realName\":\"" + oldRealName + "\"}");
                al.setNewValue("{\"realName\":\"" + newRealName + "\"}");
                al.setCreatedTime(java.time.LocalDateTime.now());
                auditLogMapper.insert(al);
            } catch (Exception ignored) { /* 审计失败不影响主流程 */ }
        }

        if (SecurityUtils.isStudent()) {
            Student s = studentMapper.selectOne(new LambdaQueryWrapper<Student>().eq(Student::getUserId, user.getId()));
            if (s != null) {
                if (creditTransactionMapper.selectCount(new LambdaQueryWrapper<CreditTransaction>()
                    .eq(CreditTransaction::getSourceType, "profile").eq(CreditTransaction::getStudentId, s.getId())) == 0) {
                    int old = s.getTotalCredits() != null ? s.getTotalCredits() : 0;
                    s.setTotalCredits(old + 5); studentMapper.updateById(s);
                    CreditTransaction ct = new CreditTransaction();
                    ct.setStudentId(s.getId()); ct.setTransactionType("earn"); ct.setCreditAmount(5);
                    ct.setBalanceAfter(s.getTotalCredits()); ct.setSourceType("profile");
                    ct.setDescription("完善个人资料 +5积分"); ct.setCreateTime(java.time.LocalDateTime.now());
                    creditTransactionMapper.insert(ct);
                }
            }
        }
        return Map.of("updated", true);
    }

    @Override @Transactional
    public void updatePassword(Long userId, String oldPassword, String newPassword, String token) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");
        if (!PasswordUtils.matches(oldPassword, user.getPassword())) throw new BusinessException(400, "旧密码错误");
        user.setPassword(PasswordUtils.encode(newPassword));
        user.setMustChangePassword(0);
        userMapper.updateById(user);
        if (token != null) jwtUtils.blacklist(token);
    }

    @Override @Transactional
    public Map<String, String> uploadAvatar(Long userId, MultipartFile file) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");

        String originalName = file.getOriginalFilename();
        String ext = originalName != null && originalName.contains(".")
            ? originalName.substring(originalName.lastIndexOf(".")).toLowerCase() : "";
        java.util.Set<String> allowedExts = Set.of(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");
        if (!allowedExts.contains(ext)) throw new BusinessException(400, "仅支持 jpg/png/gif/bmp/webp 格式");
        if (file.getSize() > 5 * 1024 * 1024) throw new BusinessException(400, "头像不能超过5MB");

        Student student = null;
        if (SecurityUtils.isStudent()) {
            student = studentMapper.selectOne(new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId));
            if (student == null) throw new BusinessException(404, "学生信息不存在");
            boolean hasAvatar = user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty();
            int cost = hasAvatar ? 40 : 20;
            int updated = studentMapper.update(null, new LambdaUpdateWrapper<Student>()
                .eq(Student::getId, student.getId())
                .ge(Student::getTotalCredits, cost)
                .setSql("total_credits = total_credits - " + cost));
            if (updated == 0) throw new BusinessException(400, "积分不足，需要" + cost + "积分才能" + (hasAvatar ? "更换" : "设置") + "头像");
        }

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            String filename = "avatar_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Files.copy(file.getInputStream(), uploadPath.resolve(filename));
            String avatarUrl = "/api/uploads/" + filename;
            user.setAvatarUrl(avatarUrl); userMapper.updateById(user);
            // 清除班级学生缓存，使智慧大屏等模块能立即看到新头像
            if (student != null && cacheManager != null) {
                Cache cache = cacheManager.getCache("class_students");
                if (cache != null) cache.evict(student.getClassId());
            }
            Map<String, String> result = new HashMap<>();
            result.put("avatarUrl", avatarUrl);
            return result;
        } catch (IOException e) {
            throw new BusinessException(500, "上传失败: " + e.getMessage());
        }
    }

    @Override @Transactional
    public String adminResetPassword(Long targetUserId, String newPassword) {
        User target = userMapper.selectById(targetUserId);
        if (target == null) throw new BusinessException(404, "用户不存在");

        // 保护内置超级管理员账号（id=1, username=admin），禁止通过管理界面由其他管理员重置密码
        // 如需修改 admin 密码，请登录 admin 账号后通过「个人中心 → 修改密码」自行修改
        if (targetUserId == 1L || "admin".equals(target.getUsername())) {
            throw new BusinessException(403, "内置超级管理员账号（admin）的密码无法通过此界面重置，请登录该账号后在个人中心自行修改密码");
        }

        if (newPassword == null || newPassword.isBlank()) {
            newPassword = com.school.teaching.utils.PasswordUtils.generateRandomPassword();
        }
        if (newPassword.length() < 6) throw new BusinessException(400, "密码至少6位");
        target.setPassword(PasswordUtils.encode(newPassword));
        target.setMustChangePassword(1);
        userMapper.updateById(target);
        return newPassword;
    }
}
