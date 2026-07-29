package com.school.teaching.service;

import com.school.teaching.entity.PracticePlan;
import com.school.teaching.entity.PracticeRubric;

import java.util.List;
import java.util.Map;

public interface PracticePlanService {
    PracticePlan create(PracticePlan plan, List<PracticeRubric> rubrics);
    PracticePlan update(Long id, PracticePlan plan, List<PracticeRubric> rubrics);
    void delete(Long id);
    PracticePlan getById(Long id);
    List<PracticePlan> listByCreator(Long userId);
    Map<String, Object> publishToTask(Long planId, List<Long> classIds, Long teacherId);
    List<PracticeRubric> getRubrics(Long planId);
    void saveRubrics(Long planId, List<PracticeRubric> rubrics);
    Map<String, Object> importFromZip(Long userId, org.springframework.web.multipart.MultipartFile file);
    Map<String, Object> importFromExcel(Long userId, org.springframework.web.multipart.MultipartFile file);
    Map<String, Object> batchImportFromMarkdown(Long userId, String markdown);
    List<PracticePlan> listSharedBySubject(String subject);

    /** 统计指定学科的方案数量 */
    long countBySubject(String subject);

    /** AI 生成实训方案步骤+评分标准 */
    Map<String, Object> aiGeneratePlan(Long userId, String title, String subject,
        String requirements, String stageHint);
}
