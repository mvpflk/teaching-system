package com.school.teaching.controller.task;

import com.school.teaching.entity.Task;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.TaskCrudService;
import com.school.teaching.service.TaskQueryService;
import com.school.teaching.service.TeacherActivityService;
import com.school.teaching.service.TeacherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 任务访问权限检查 — 从 TaskController 提取的公共方法，
 * 供 TaskCrudController / TaskGradingController / TaskAnalyticsController 共享。
 */
@Slf4j
@Component
public class TaskAccessChecker {

    @Autowired private TaskCrudService taskCrudService;
    @Autowired private TeacherService teacherService;
    @Autowired private TaskQueryService taskQueryService;
    @Autowired(required = false) private com.school.teaching.security.StudentResolver studentResolver;
    @Autowired(required = false) private TeacherActivityService teacherActivityService;

    /** 检查当前用户是否有权访问指定任务（task 为 null 时返回 false） */
    public boolean canAccessTask(Task task) {
        if (task == null) return false;
        if (SecurityUtils.isAdmin() || SecurityUtils.isInspector()) return true;
        Long userId = SecurityUtils.getCurrentUserId();
        if (SecurityUtils.isTeacherOrAdmin()) {
            Long teacherId = teacherService.getTeacherIdByUserId(userId);
            if (teacherId != null && teacherId.equals(task.getTeacherId())) return true;
            if (teacherId != null && task.getTargetId() != null) {
                List<Map<String, Object>> classes = teacherService.getTeachingAssignments(teacherId);
                if (classes != null && classes.stream().anyMatch(c ->
                    task.getTargetId().equals(c.get("classId"))))
                    return true;
            }
            return false;
        }
        if (SecurityUtils.isStudent()) {
            Long studentId = studentResolver.resolveCurrentStudentId();
            if (studentId == null) return false;
            return taskQueryService.isTaskAccessibleByStudent(task.getId(), studentId);
        }
        return false;
    }

    /** 检查是否任务创建者或管理员，班主任无编辑权限。超级管理员可删除任意任务 */
    public void checkOwnership(Long taskId) {
        Task t = taskCrudService.getById(taskId);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Long teacherId = teacherService.getTeacherIdByUserId(currentUserId);
        if (SecurityUtils.isAdmin()) return;
        if (teacherId != null && teacherId.equals(t.getTeacherId())) return;
        throw new BusinessException(403, "仅任务创建者可操作");
    }

    /** 检查评分权限：仅任务创建者可评分，管理员/超级管理员也不能替别人评分 */
    public void checkGradingPermission(Long taskId) {
        Task t = taskCrudService.getById(taskId);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Long teacherId = teacherService.getTeacherIdByUserId(currentUserId);
        if (SecurityUtils.isAdmin()) return;
        if (teacherId != null && teacherId.equals(t.getTeacherId())) return;
        throw new BusinessException(403, "仅任务创建者或管理员可评分");
    }

    /** 记录教师行为日志 */
    public void logTeacherActivity(String action, String targetType, Long targetId) {
        if (teacherActivityService == null) return;
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            Long teacherId = teacherService.getTeacherIdByUserId(userId);
            if (teacherId != null) teacherActivityService.log(teacherId, action, targetType, targetId);
        } catch (Exception ignored) { log.debug("教师行为日志记录失败", ignored); }
    }
}
