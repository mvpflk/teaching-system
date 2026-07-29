package com.school.teaching.service;

import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface StudentImportService {
    Map<String, Object> batchImport(MultipartFile file);
}
