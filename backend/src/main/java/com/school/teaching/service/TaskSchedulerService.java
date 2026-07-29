package com.school.teaching.service;

public interface TaskSchedulerService {

    void closeExpiredTasks();

    void sendDeadlineReminders();

    void autoCloseAndGrade();

    /** 扫描考试时长超限的 PENDING 提交，自动终止并记0分 */
    void terminateExpiredExams();
}
