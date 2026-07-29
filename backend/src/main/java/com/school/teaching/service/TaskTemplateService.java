package com.school.teaching.service;

import com.school.teaching.entity.TaskTemplate;
import java.util.List;
import java.util.Map;

public interface TaskTemplateService {
    List<TaskTemplate> listTemplates(Long userId, String scope, String subject, String taskType, String category);
    TaskTemplate getById(Long id);
    TaskTemplate saveFromTask(Long taskId, String name, String scope, String category, Long userId);
    TaskTemplate createTemplate(TaskTemplate template, Long userId);
    Map<String, Object> createTaskFromTemplate(Long templateId, Long userId);
    void updateScope(Long id, String scope, Long userId);
    void deleteTemplate(Long id, Long userId);
}
