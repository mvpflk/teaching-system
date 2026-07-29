package com.school.teaching.controller;

import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.annotation.AuditLog;
import com.school.teaching.common.R;
import com.school.teaching.dto.request.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.SystemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 系统设置核心控制器
 * 拆分自 SettingsController：系统信息/功能开关/配置/公告/Logo/上传白名单。
 * 字典管理见 {@link DictManagementController}，运维审计见 {@link AdminMaintenanceController}，
 * AI 配置见 {@link AiConfigController}。
 */
@RestController
@RequestMapping("/settings")
public class SettingsController {

    @Autowired private SystemService systemService;

    /** 系统状态信息 */
    @GetMapping("/actions/status")
    public R<Map<String, Object>> status() {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        return R.ok(systemService.getSystemInfo());
    }

    /** 功能开关列表 */
    @GetMapping("/features")
    public R<Map<String, Boolean>> getFeatureFlags() {
        return R.ok(systemService.getFeatureFlags());
    }

    /** 获取单项配置 */
    @GetMapping("/actions/config")
    public R<Map<String, String>> getConfig(@RequestParam String key) {
        Map<String, String> all = systemService.getAllSettings();
        String val = all.getOrDefault(key, "");
        return R.ok(Map.of(key, val));
    }

    /** 更新单项配置 */
    @PutMapping("/actions/update-config")
    public R<String> updateConfig(@RequestBody Map<String, String> body) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        systemService.updateAllSettings(body);
        return R.ok("已更新");
    }

    /** 获取全部配置 */
    @GetMapping
    public R<Map<String, String>> getAll() {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        return R.ok(systemService.getAllSettings());
    }

    /** 批量更新配置 */
    @PutMapping("/actions/update-all")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "批量修改系统设置")
    public R<String> updateAll(@RequestBody Map<String, String> body) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        systemService.updateAllSettings(body);
        return R.ok(null, "已更新");
    }

    // 文件上传白名单（供前端动态获取，避免硬编码不同步）
    private static final List<String> ALLOWED_UPLOAD_EXTS = List.of(
        ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp",
        ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".pdf",
        ".rar", ".zip", ".7z",
        ".mp3", ".wav", ".ogg", ".m4a", ".aac",
        ".mp4", ".webm", ".mov", ".avi", ".mkv", ".flv",
        ".txt", ".csv", ".java", ".py", ".cpp", ".c", ".js", ".ts", ".html", ".css", ".md", ".xml", ".json"
    );

    @GetMapping("/allowed-upload-exts")
    public R<List<String>> getAllowedUploadExts() {
        return R.ok(ALLOWED_UPLOAD_EXTS);
    }

    /** 发布公告 */
    @PostMapping("/announce")
    @AuditLog(eventType = AuditEventType.OTHER, description = "发布公告")
    public R<Map<String, Object>> sendAnnouncement(@Valid @RequestBody AnnouncementRequest request) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        String scope = request.getScope() != null ? request.getScope() : "all";
        int count = systemService.sendAnnouncement(scope, request.getTargetId(), request.getTitle(), request.getContent());
        return R.ok(Map.of("sentCount", count), "已发送给 " + count + " 人");
    }

    // ==================== 系统 Logo 管理 ====================

    @Value("${teaching.upload-dir:/data/uploads}")
    private String logoUploadDir;

    private static final Set<String> LOGO_ALLOWED_EXTS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".bmp");

    /** 上传学校 Logo */
    @PostMapping("/logo")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "上传学校Logo")
    public R<Map<String, String>> uploadLogo(@RequestParam("file") MultipartFile file) throws IOException {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");
        if (file == null || file.isEmpty()) return R.error(400, "请选择图片文件");
        if (file.getSize() > 2 * 1024 * 1024) return R.error(400, "Logo 图片不能超过2MB");

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        if (!LOGO_ALLOWED_EXTS.contains(ext)) {
            return R.error(400, "仅支持图片格式: jpg/jpeg/png/gif/bmp");
        }

        // 魔数校验（复用 UploadController 的逻辑，简化为图片检测）
        byte[] bytes = file.getBytes();
        if (bytes.length < 4) return R.error(400, "文件内容异常");
        int b0 = bytes[0] & 0xFF, b1 = bytes[1] & 0xFF, b2 = bytes[2] & 0xFF, b3 = bytes[3] & 0xFF;
        boolean validMagic = false;
        if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) validMagic = true;           // JPEG
        else if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) validMagic = true; // PNG
        else if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46) validMagic = true;     // GIF
        else if (b0 == 0x42 && b1 == 0x4D) validMagic = true;                    // BMP
        if (!validMagic) return R.error(400, "图片内容与扩展名不匹配");

        // 保存文件
        Path logoDir = Paths.get(logoUploadDir, "logo");
        if (!Files.exists(logoDir)) Files.createDirectories(logoDir);

        // 删除旧 Logo 文件
        String oldUrl = systemService.getLogoUrl();
        if (oldUrl != null && oldUrl.startsWith("/api/uploads/logo/")) {
            try {
                String oldFilename = oldUrl.substring("/api/uploads/logo/".length());
                Files.deleteIfExists(logoDir.resolve(oldFilename));
            } catch (IOException ignored) {
                // 静默降级：旧 Logo 文件删除失败不影响上传
            }
        }

        String filename = "logo_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + ext;
        Files.write(logoDir.resolve(filename), bytes);

        String url = "/api/uploads/logo/" + filename;
        systemService.setLogoUrl(url);

        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        return R.ok(result, "Logo 上传成功");
    }

    /** 删除学校 Logo，恢复默认 */
    @DeleteMapping("/logo")
    @AuditLog(eventType = AuditEventType.PARAM_UPDATE, description = "删除学校Logo")
    public R<String> deleteLogo() {
        if (!SecurityUtils.isAdmin()) return R.error(403, "权限不足");

        String oldUrl = systemService.getLogoUrl();
        if (oldUrl != null && oldUrl.startsWith("/api/uploads/logo/")) {
            try {
                Path logoDir = Paths.get(logoUploadDir, "logo");
                String oldFilename = oldUrl.substring("/api/uploads/logo/".length());
                Files.deleteIfExists(logoDir.resolve(oldFilename));
            } catch (IOException ignored) {
                // 静默降级：旧 Logo 文件删除失败不影响恢复默认
            }
        }

        systemService.setLogoUrl("");
        return R.ok("已恢复默认 Logo");
    }
}
