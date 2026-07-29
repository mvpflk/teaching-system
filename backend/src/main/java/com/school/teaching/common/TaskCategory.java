package com.school.teaching.common;

/**
 * 统一任务类型 — 从教学环节视角分为 9 类。
 * exams → SUMMATIVE（默认）
 * homework_assignments → AFTER_CLASS（默认）
 */
public enum TaskCategory {
    PRE_CLASS,      // 课前预习
    IN_CLASS,       // 课中活动
    AFTER_CLASS,    // 课后巩固
    FORMATIVE,      // 形成性评价（单元测验）
    SUMMATIVE,      // 终结性评价（期中/期末）
    MORAL,          // 德育作业
    LABOR,          // 劳动作业
    SURVEY,         // 问卷调查
    PRACTICE,       // 实训任务
    SIMULATION      // 仿真任务（Windows 操作仿真）
}
