package com.school.teaching.controller;

import com.school.teaching.annotation.AuditLog;
import com.school.teaching.annotation.AuditEventType;

import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 上传控制器 — 文件扩展名白名单 + MIME魔数校验 + 可执行文件黑名单 + 存储目录隔离。
 */
@RestController
@RequestMapping("/upload")
public class UploadController {

    @Value("${teaching.upload-dir:/data/uploads}")
    private String baseUploadDir;

    /** 通用文件白名单 — 办公文档 + 图片 + 音频 + 压缩包 */
    private static final Set<String> ALLOWED_EXTS = Set.of(
        // 文档类
        ".doc", ".docx", ".pdf", ".txt",
        // 表格类
        ".xls", ".xlsx",
        // 演示类
        ".ppt", ".pptx",
        // 图片类
        ".jpg", ".jpeg", ".png", ".gif", ".bmp",
        // 音频类
        ".mp3", ".wav", ".ogg", ".m4a", ".aac", ".webm",
        // 压缩包
        ".zip", ".rar", ".7z"
    );

    /** 图片专用白名单 */
    private static final Set<String> IMAGE_EXTS = Set.of(
        ".jpg", ".jpeg", ".png", ".gif", ".bmp");

    /** 严格拦截的可执行/脚本后缀（防御纵深，白名单已排除但显式报错更友好） */
    private static final Set<String> BLOCKED_EXTS = Set.of(
        ".exe", ".sh", ".bat", ".cmd",
        ".js", ".php", ".jsp", ".asp", ".aspx", ".py", ".rb", ".pl", ".cgi",
        ".jar", ".war"
    );

    /** 作业附件上传，支持白名单内类型，最多8个 */
    @PostMapping("/actions/homework")
    @AuditLog(eventType = AuditEventType.OTHER, description = "上传作业附件")
    public R<List<String>> uploadHomeworkFiles(@RequestParam("files") MultipartFile[] files) throws IOException {
        if (!SecurityUtils.isStudent() && !SecurityUtils.isTeacherOrAdmin()) return R.error(401, "请先登录");
        if (files == null || files.length == 0) return R.error(400, "请选择文件");
        if (files.length > 8) return R.error(400, "最多上传8个文件");

        long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB 单文件（PPT/压缩包可能较大）
        long MAX_TOTAL_SIZE = 80 * 1024 * 1024; // 80MB 总大小

        long totalSize = 0;
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            if (file.getSize() > MAX_FILE_SIZE) return R.error(400, "单个文件不能超过20MB");
            totalSize += file.getSize();
        }
        if (totalSize > MAX_TOTAL_SIZE) return R.error(400, "文件总大小不能超过80MB");

        List<String> urls = new ArrayList<>();
        Path uploadPath = Paths.get(baseUploadDir, "homework");
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String ext = extractExt(file.getOriginalFilename());
            String error = validateExt(ext);
            if (error != null) return R.error(400, error);

            byte[] fileBytes = file.getBytes();
            if (probeContentType(fileBytes, ext) == null) {
                return R.error(400, "文件内容与扩展名不匹配，拒绝上传");
            }

            String filename = "hw_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + ext;
            Files.write(uploadPath.resolve(filename), fileBytes);
            urls.add("/api/uploads/homework/" + filename);
        }
        return R.ok(urls, "上传成功(" + urls.size() + "个)");
    }

    /** BBS图片上传（单张） */
    @PostMapping("/actions/bbs")
    @AuditLog(eventType = AuditEventType.OTHER, description = "上传BBS图片")
    public R<Map<String, String>> uploadBbsImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (SecurityUtils.getCurrentUserId() == null) return R.error(401, "请先登录");
        if (file == null || file.isEmpty()) return R.error(400, "请选择文件");
        if (file.getSize() > 5 * 1024 * 1024) return R.error(400, "图片不能超过5MB");

        String ext = extractExt(file.getOriginalFilename());
        if (!IMAGE_EXTS.contains(ext)) {
            return R.error(400, "仅支持图片格式: jpg/jpeg/png/gif/bmp");
        }
        if (BLOCKED_EXTS.contains(ext)) {
            return R.error(400, "禁止上传可执行文件: " + ext);
        }

        Path uploadPath = Paths.get(baseUploadDir, "bbs");
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        byte[] imgBytes = file.getBytes();
        if (probeContentType(imgBytes, ext) == null) {
            return R.error(400, "图片内容与扩展名不匹配");
        }

        String filename = "bbs_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + ext;
        Files.write(uploadPath.resolve(filename), imgBytes);

        Map<String, String> result = new HashMap<>();
        result.put("url", "/api/uploads/bbs/" + filename);
        return R.ok(result, "上传成功");
    }

    /** 实训/练习文件上传（单文件，支持文档/图片/压缩包） */
    @PostMapping("/actions/practice")
    @AuditLog(eventType = AuditEventType.OTHER, description = "上传实训文件")
    public R<Map<String, String>> uploadPracticeFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (SecurityUtils.getCurrentUserId() == null) return R.error(401, "请先登录");
        if (file == null || file.isEmpty()) return R.error(400, "请选择文件");
        if (file.getSize() > 20 * 1024 * 1024) return R.error(400, "文件不能超过20MB");

        String ext = extractExt(file.getOriginalFilename());
        String error = validateExt(ext);
        if (error != null) return R.error(400, error);

        byte[] fileBytes = file.getBytes();
        if (probeContentType(fileBytes, ext) == null) {
            return R.error(400, "文件内容与扩展名不匹配，拒绝上传");
        }

        Path uploadPath = Paths.get(baseUploadDir, "practice");
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        String filename = "prac_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + ext;
        Files.write(uploadPath.resolve(filename), fileBytes);

        Map<String, String> result = new HashMap<>();
        result.put("url", "/api/uploads/practice/" + filename);
        return R.ok(result, "上传成功");
    }

    /** 扩展名校验，返回错误消息（null=通过） */
    private static String validateExt(String ext) {
        if (BLOCKED_EXTS.contains(ext)) {
            return "禁止上传可执行/脚本文件: " + ext;
        }
        if (!ALLOWED_EXTS.contains(ext)) {
            return "不支持的文件类型: " + ext
                + "，仅支持 文档(doc/docx/pdf/txt) 表格(xls/xlsx) 演示(ppt/pptx) 图片(jpg/png/gif/bmp) 压缩包(zip/rar/7z)";
        }
        return null;
    }

    /** 从文件名提取小写扩展名（含点号），无扩展名返回空串 */
    private static String extractExt(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    /** 魔数检测 — 仅对白名单类型校验，不匹配返回 null */
    private static String probeContentType(byte[] bytes, String ext) {
        if (bytes == null || bytes.length < 4) return null;
        int b0 = bytes[0] & 0xFF, b1 = bytes[1] & 0xFF, b2 = bytes[2] & 0xFF, b3 = bytes[3] & 0xFF;

        // JPEG: FF D8 FF
        if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) return "image/jpeg";
        // PNG: 89 50 4E 47
        if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) return "image/png";
        // GIF: 47 49 46
        if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46) return "image/gif";
        // BMP: 42 4D
        if (b0 == 0x42 && b1 == 0x4D) return "image/bmp";
        // PDF: 25 50 44 46
        if (b0 == 0x25 && b1 == 0x50 && b2 == 0x44 && b3 == 0x46) return "application/pdf";
        // ZIP-based: DOCX / XLSX / PPTX / ZIP — 50 4B 03 04
        if (b0 == 0x50 && b1 == 0x4B && b2 == 0x03 && b3 == 0x04) {
            if (ext.equals(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            if (ext.equals(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            if (ext.equals(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            if (ext.equals(".zip")) return "application/zip";
            return null; // 未识别的ZIP格式拒绝
        }
        // OLE2: DOC / XLS / PPT — D0 CF 11 E0
        if (b0 == 0xD0 && b1 == 0xCF && b2 == 0x11 && b3 == 0xE0) {
            if (ext.equals(".doc")) return "application/msword";
            if (ext.equals(".xls")) return "application/vnd.ms-excel";
            if (ext.equals(".ppt")) return "application/vnd.ms-powerpoint";
            return null;
        }
        // RAR: 52 61 72 21
        if (b0 == 0x52 && b1 == 0x61 && b2 == 0x72 && b3 == 0x21) {
            return ext.equals(".rar") ? "application/x-rar" : null;
        }
        // 7z: 37 7A BC AF 27 1C
        if (b0 == 0x37 && b1 == 0x7A && b2 == 0xBC && b3 == 0xAF
            && bytes.length > 5 && bytes[4] == 0x27 && bytes[5] == 0x1C) {
            return ext.equals(".7z") ? "application/x-7z-compressed" : null;
        }
        // — 音频格式 —
        // MP3: ID3 tag (49 44 33) or sync frame (FF FB / FF F3 / FF F2)
        if ((b0 == 0x49 && b1 == 0x44 && b2 == 0x33) ||
            (b0 == 0xFF && (b1 == 0xFB || b1 == 0xF3 || b1 == 0xF2))) {
            return ext.equals(".mp3") ? "audio/mpeg" : null;
        }
        // WAV: RIFF (52 49 46 46)
        if (b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46) {
            return ext.equals(".wav") ? "audio/wav" : null;
        }
        // OGG: OggS (4F 67 67 53)
        if (b0 == 0x4F && b1 == 0x67 && b2 == 0x67 && b3 == 0x53) {
            return ext.equals(".ogg") ? "audio/ogg" : null;
        }
        // WebM: EBML header (1A 45 DF A3)
        if (b0 == 0x1A && b1 == 0x45 && b2 == 0xDF && b3 == 0xA3) {
            return ext.equals(".webm") ? "audio/webm" : null;
        }
        // M4A / AAC: ISO base media ftyp box (… 66 74 79 70 at offset 4)
        if (bytes.length > 8 && bytes[4] == 0x66 && bytes[5] == 0x74
            && bytes[6] == 0x79 && bytes[7] == 0x70) {
            if (ext.equals(".m4a")) return "audio/mp4";
            if (ext.equals(".aac")) return "audio/aac";
            return null;
        }
        // TXT: 纯文本无固定魔数，NULL字节检测
        if (ext.equals(".txt")) {
            return isPrintableText(bytes) ? "text/plain" : null;
        }
        return null;
    }

    /** 检查前512字节是否无NULL字节（拒绝二进制伪装.txt） */
    private static boolean isPrintableText(byte[] bytes) {
        int limit = Math.min(bytes.length, 512);
        for (int i = 0; i < limit; i++) {
            if ((bytes[i] & 0xFF) == 0) return false;
        }
        return true;
    }
}
