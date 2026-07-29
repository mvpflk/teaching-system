package com.school.teaching.annotation;

import java.lang.annotation.*;

/**
 * 审计注解 — Service 层方法审计。
 *
 * 被标记的方法由 AuditAspect 环绕处理：
 *   成功 → 写入 SUCCESS 记录
 *   异常 → 写入 FAILURE 记录并附带异常信息
 *
 * 使用示例：
 *   @Audit("DELETE_TASK")
 *   public void deleteTask(Long id) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audit {

    /** 操作类型，如 "DELETE_TASK"、"COPY_TASK"、"GRADE_SUBMISSION" */
    String value();
}
