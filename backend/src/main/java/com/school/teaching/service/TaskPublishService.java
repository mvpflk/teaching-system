package com.school.teaching.service;

import com.school.teaching.entity.Task;

import java.util.Map;

public interface TaskPublishService {

    Task publish(Long id);

    Task close(Long id);

    Task reopen(Long id);

    int publishScheduledTasks();

    Map<String, Object> resendToPending(Long taskId);

    Map<String, Object> forcedPreview(String grade);

    /** 学生班级变动时处理进行中任务：旧班级任务豁免 + 新班级任务注册 */
    Map<String, Object> handleStudentClassChange(Long studentId, Long oldClassId, Long newClassId);
}