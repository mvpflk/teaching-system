package com.school.teaching.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.Task;

import java.util.List;
import java.util.Map;

public interface TaskQueryService {

    List<Task> getAccessibleTasks(Long userId);

    List<Task> getTeacherTasks(Long teacherId);

    List<Task> getStudentTasks(Long studentId);

    List<Task> getStudentCompletedTasks(Long studentId);

    IPage<Task> pageByTeacher(Long teacherId, Page<Task> page, String status);

    IPage<Task> pageByStudent(Long studentId, Page<Task> page, String status);

    IPage<Task> pageByAdmin(Page<Task> page, String status);

    void enrichTasks(List<Task> tasks, Long currentTeacherId);

    Map<String, Object> getPendingCount(Long studentId);

    Map<String, Object> getStudentTasksWithSubmission(Long studentId, int page, int size);

    boolean isTaskAccessibleByStudent(Long taskId, Long studentId);

    List<Task> getActiveClassTasks(Long classId);

    /** 巡视管理：分页查询已提交审核的任务 */
    IPage<Task> pageTasksForReview(String reviewStatus, String startDate, String endDate, int page, int pageSize);

    /** 根据教师 ID 列表获取任务 */
    List<Task> getTasksByTeacherIds(java.util.Collection<Long> teacherIds);
}