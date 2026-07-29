package com.school.teaching.common;

/**
 * 任务行为 — 教师视角的3种任务分类，后端映射为9种具体 TaskCategory 存入数据库。
 * 前端只暴露这3种选择，降低教师认知负担。
 */
public enum TaskBehavior {
    EXAM,       // 考试/测验 → 默认 FORMATIVE
    HOMEWORK,   // 作业/任务 → 默认 AFTER_CLASS
    SURVEY      // 问卷调查 → 固定 SURVEY
}
