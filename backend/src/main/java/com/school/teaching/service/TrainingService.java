package com.school.teaching.service;

import java.util.Map;

/**
 * 实训中心服务接口 — 复用 Task/TaskSubmission 表，零新表
 */
public interface TrainingService {

    // ── 任务 CRUD ──

    /** 分页查询实训任务列表（教师看自己的，学生看已发布的） */
    Map<String, Object> listTasks(int page, int size);

    /** 任务详情 */
    Map<String, Object> getTaskDetail(Long taskId);

    /** 创建实训任务（含步骤定义） */
    Map<String, Object> createTask(Map<String, Object> body, Long userId);

    /** 更新实训任务（仅 DRAFT 状态） */
    Map<String, Object> updateTask(Long taskId, Map<String, Object> body, Long userId);

    /** 删除实训任务（仅 DRAFT/CLOSED 状态） */
    void deleteTask(Long taskId, Long userId);

    /** 发布实训任务（DRAFT → PUBLISHED） */
    Map<String, Object> publishTask(Long taskId, Long userId);

    // ── 任务库 ──

    /** 实训任务库（模板列表） */
    Map<String, Object> listLibrary();

    /** 从任务库复制模板 */
    Map<String, Object> copyFromLibrary(Long templateId);

    // ── 学生步骤执行 ──

    /** 学生获取任务步骤（含已保存进度） */
    Map<String, Object> getStudentSteps(Long taskId, Long userId);

    /** 保存单步进度 */
    void saveStepProgress(Long taskId, int stepIndex, Map<String, Object> body, Long userId);

    /** 提交全部步骤 */
    Map<String, Object> submitAllSteps(Long taskId, Long userId);

    // ── 教师评分 ──

    /** 教师查看该任务所有学生提交 */
    Map<String, Object> getSubmissions(Long taskId, Long userId);

    /** 教师对某份提交的单步评分 */
    Map<String, Object> gradeStep(Long taskId, Long submissionId, int stepIndex,
                                  Map<String, Object> body, Long userId);

    /** 完成评分（锁定成绩） */
    Map<String, Object> finalizeGrade(Long taskId, Long submissionId,
                                      Map<String, Object> body, Long userId);

    // ── 实训中心 Hub ──

    /** 实训中心仪表盘（统计卡片 + 最近任务） */
    Map<String, Object> getHub();
}
