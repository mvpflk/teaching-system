package com.school.teaching.service;

import com.school.teaching.entity.Task;

public interface TaskCrudService {

    Task getById(Long id);

    Task create(Task task);

    Task update(Long id, Task task);

    void delete(Long id);

    Task copyTask(Long sourceTaskId, Long userId);

    /** 更新达标配置（绕过 DRAFT 状态检查，调用方需自行校验状态） */
    void updatePassRateConfig(Long taskId, Integer passRate, Integer maxAttempts, Integer retakeDeadlineHours);
}