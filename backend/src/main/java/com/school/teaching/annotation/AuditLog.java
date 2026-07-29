package com.school.teaching.annotation;

import java.lang.annotation.*;

/**
 * 审计日志注解 — 标记需要自动记录审计日志的方法。
 *
 * 被标记的方法执行成功后（@AfterReturning），由 AuditLogAspect 自动：
 *   1. 从请求头 JWT 中提取当前用户信息
 *   2. 构造 AuditLog 实体
 *   3. 异步写入数据库
 *
 * 使用示例：
 *   @AuditLog(eventType = AuditEventType.EXAM_PUBLISH, description = "发布考试")
 *   public R<?> publishExam(...) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /** 事件类型 */
    AuditEventType eventType() default AuditEventType.OTHER;

    /** 操作描述（支持 SpEL 表达式，如 "'删除学生#' + #id"） */
    String description() default "";

    /** 目标表名，可选 */
    String targetTable() default "";
}
