package com.school.teaching.controller;

/**
 * W-3: 已拆分为 3 个 Controller（2026-07-09）。
 *
 * 原方法迁移对照：
 *   CRUD + 发布 + 审核 + 题目 → controller/task/TaskCrudController.java
 *   评分 + 提交管理 + 重测      → controller/task/TaskGradingController.java
 *   统计 + 导出 + 问卷 + 衍生  → controller/task/TaskAnalyticsController.java
 *
 * 公共权限检查逻辑 → controller/task/TaskAccessChecker.java
 */
@Deprecated
public class TaskController {
    // 所有 43 个端点方法已迁移至 controller/task/ 子包下的 3 个 Controller。
    // 每个 Controller 均使用 @RequestMapping("/task")，路径与原来完全一致。
    // 前端无需任何改动。
}
