package com.school.teaching.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface ExamShareService {
    Map<String, Object> createShare(Long taskId, Long userId);
    List<Map<String, Object>> myShares(Long userId);
    void deleteShare(Long shareId, Long userId);
    Map<String, Object> importShared(String shareCode, Long userId, Long targetClassId);
    Map<String, Object> previewShare(String shareCode);
    Map<String, Object> uploadExam(MultipartFile file, String title, String subject, Long userId);
    Map<String, Object> confirmUpload(String title, String subject, Long userId, Long targetClassId, Map<Long, java.math.BigDecimal> scores);
    List<Map<String, Object>> library(Long userId);
}
