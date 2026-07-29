package com.school.teaching.common;

import com.school.teaching.entity.TaskSubmission;

import java.util.Map;
import java.util.Set;

/**
 * 任务类型处理器 — 策略接口，可插拔扩展新任务类型。
 *
 * 每种 TaskCategory 可有独立 Handler；同一个 Handler 可覆盖多个 Category。
 * 新增 Handler 实现此接口并注册为 @Component，无需修改 TaskService。
 */
public interface TaskTypeHandler {

    /** 返回此 Handler 覆盖的任务分类集合 */
    Set<TaskCategory> getCategories();

    /** 任务发布时的回调 */
    default void onPublish(TaskContext ctx) {}

    /** 学生提交时的回调。返回保存后的 TaskSubmission */
    default TaskSubmission onSubmit(TaskContext ctx) { return null; }

    /** 教师评分时的回调 */
    default void onGrade(TaskContext ctx) {}

    /**
     * 任务上下文 — 携带操作涉及的全部数据。
     * extras 可存放 handler-specific 数据（如 answers JSON、cheat warning count 等）。
     */
    record TaskContext(
        Long taskId,
        Long studentId,
        Long teacherId,
        Map<String, Object> taskConfig,
        Map<String, Object> submission,
        Map<String, Object> extras
    ) {}
}
