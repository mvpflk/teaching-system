package com.school.teaching.service;

import com.school.teaching.entity.SimulationTask;
import java.util.Map;

/**
 * 仿真任务服务接口 — Windows 操作仿真模块核心 API。
 */
public interface SimulationService {

    /** 获取仿真任务定义（taskJson + initialVfs） */
    Map<String, Object> getTaskDefinition(Long simTaskId);

    /** 根据主键 ID 获取仿真任务实体 */
    SimulationTask getById(Long simTaskId);

    /** 教师创建仿真任务（同时创建 Task + SimulationTask） */
    Map<String, Object> createTask(Map<String, Object> request, Long userId);

    /** 学生上报仿真进度（操作事件/时长/自动评分） */
    void reportProgress(Map<String, Object> body, Long studentId);

    /** 获取学生操作录制数据 */
    Map<String, Object> getRecording(Long submissionId);
    /** 教师添加评注 */
    void addNotes(Long recordingId, String notes);
    /** 列出仿真任务（可选按 category 过滤，null 或空则返回全部） */
    java.util.List<Map<String, Object>> listTasks(String category);

    /** 获取实训大厅分类数据（按 category 分组） */
    java.util.Map<String, Object> getTrainingHub();

    /** 教师更新仿真任务 */
    Map<String, Object> updateTask(Long simTaskId, Map<String, Object> request, Long userId);

    /** 教师删除仿真任务 */
    void deleteTask(Long simTaskId, Long userId);

    /** 学生开始考试 — 创建 submission 并返回 submissionId */
    Map<String, Object> startExam(Long simTaskId, Long studentId);
}
