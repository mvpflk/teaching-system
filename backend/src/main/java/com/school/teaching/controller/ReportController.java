package com.school.teaching.controller;

import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/report")
public class ReportController {

    @Autowired private ReportService reportService;

    @GetMapping("/actions/exam/{examId}/export")
    public ResponseEntity<byte[]> exportExamScores(@PathVariable Long examId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return ResponseEntity.status(403).build();
        byte[] data = reportService.exportExamScores(examId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''exam_scores.xlsx")
            .contentType(MediaType.APPLICATION_OCTET_STREAM).body(data);
    }

    @GetMapping("/actions/class/{classId}/export")
    public ResponseEntity<byte[]> exportClassScores(@PathVariable Long classId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return ResponseEntity.status(403).build();
        byte[] data = reportService.exportClassScores(classId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''class_scores.xlsx")
            .contentType(MediaType.APPLICATION_OCTET_STREAM).body(data);
    }
}
