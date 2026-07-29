package com.school.teaching.common;

import java.util.Map;
import java.util.Set;

/**
 * 行为↔类型双向映射器。
 * 前端传 TaskBehavior → 后端解析为具体 TaskCategory 存入 DB；
 * 编辑时反向映射 → 前端恢复正确的行为面板。
 */
public class TaskBehaviorResolver {

    private static final Map<TaskBehavior, TaskCategory> DEFAULT_MAPPING = Map.of(
        TaskBehavior.EXAM, TaskCategory.FORMATIVE,
        TaskBehavior.HOMEWORK, TaskCategory.AFTER_CLASS,
        TaskBehavior.SURVEY, TaskCategory.SURVEY
    );

    private static final Set<TaskCategory> EXAM_CATEGORIES = Set.of(TaskCategory.FORMATIVE, TaskCategory.SUMMATIVE);
    private static final Set<TaskCategory> HOMEWORK_CATEGORIES = Set.of(
        TaskCategory.PRE_CLASS, TaskCategory.IN_CLASS, TaskCategory.AFTER_CLASS,
        TaskCategory.MORAL, TaskCategory.LABOR
    );

    /** 行为 → 默认具体类型 */
    public static TaskCategory resolveTaskType(TaskBehavior behavior) {
        TaskCategory cat = DEFAULT_MAPPING.get(behavior);
        if (cat == null) throw new IllegalArgumentException("未知任务行为: " + behavior);
        return cat;
    }

    /** 行为 → 默认具体类型字符串 */
    public static String resolveTaskTypeString(TaskBehavior behavior) {
        return resolveTaskType(behavior).name();
    }

    /** 具体类型 → 行为（编辑模式回填），PRACTICE 返回 null */
    public static TaskBehavior resolveBehavior(String taskType) {
        if (taskType == null) return null;
        try {
            TaskCategory cat = TaskCategory.valueOf(taskType);
            if (EXAM_CATEGORIES.contains(cat)) return TaskBehavior.EXAM;
            if (HOMEWORK_CATEGORIES.contains(cat)) return TaskBehavior.HOMEWORK;
            if (cat == TaskCategory.SURVEY) return TaskBehavior.SURVEY;
            return null; // PRACTICE
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 判断是否为考试类行为 */
    public static boolean isExamBehavior(String taskType) {
        return EXAM_CATEGORIES.contains(safeValueOf(taskType));
    }

    /** 判断是否为作业类行为（含德育/劳动） */
    public static boolean isHomeworkBehavior(String taskType) {
        return HOMEWORK_CATEGORIES.contains(safeValueOf(taskType));
    }

    /** 判断是否为问卷行为 */
    public static boolean isSurveyBehavior(String taskType) {
        return TaskCategory.SURVEY == safeValueOf(taskType);
    }

    private static TaskCategory safeValueOf(String taskType) {
        if (taskType == null) return null;
        try { return TaskCategory.valueOf(taskType); }
        catch (IllegalArgumentException e) { return null; }
    }
}
