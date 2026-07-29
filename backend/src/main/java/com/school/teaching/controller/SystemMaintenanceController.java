package com.school.teaching.controller;

import com.school.teaching.annotation.AuditLog;
import com.school.teaching.annotation.AuditEventType;

import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/system-maintenance")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SystemMaintenanceController {

    @Autowired private SystemService systemService;

    @GetMapping("/actions/info")
    public R<Map<String, Object>> systemInfo() {
        if (!SecurityUtils.isSuperAdmin()) return R.error(403, "仅超级管理员可访问");
        return R.ok(systemService.getSystemInfo());
    }

    @GetMapping("/actions/backup")
    public ResponseEntity<byte[]> backupDatabase() {
        if (!SecurityUtils.isSuperAdmin()) return ResponseEntity.status(403).build();
        byte[] data = systemService.exportBackup();
        String filename = "teaching_system_backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM).body(data);
    }

    @PostMapping("/actions/import")
    @AuditLog(eventType = AuditEventType.OTHER, description = "导入数据库备份")
    public R<String> importDatabase(@RequestParam("password") String password,
                                     @RequestParam("file") String fileContent) {
        if (!SecurityUtils.isSuperAdmin()) return R.error(403, "仅超级管理员可操作");
        int count = systemService.importBackup(fileContent.getBytes());
        return R.ok(null, "导入完成，执行 " + count + " 条SQL");
    }

    @PostMapping("/actions/clear")
    @AuditLog(eventType = AuditEventType.OTHER, description = "清空业务数据")
    public R<Map<String, Object>> clearData(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isSuperAdmin()) return R.error(403, "仅超级管理员可操作");
        return R.ok(systemService.clearAllData());
    }
}
