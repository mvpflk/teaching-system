package com.school.teaching.service.impl;

import com.school.teaching.entity.Task;
import com.school.teaching.mapper.TaskMapper;
import com.school.teaching.service.TaskService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 场景2: 任务发布→学生可见→提交 核心链路集成测试
 *
 * 测试链路: 创建草稿任务 → 发布 → 验证状态变更 → 验证学生可见 → 验证学生提交
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
@Disabled("需要运行中的 MySQL — 请先启动数据库再运行此测试")
@DisplayName("任务发布集成测试: DRAFT→PUBLISHED→学生可见→提交")
class TaskPublishIntegrationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskMapper taskMapper;

    private static final Long TEACHER_ID = 2L;   // teacher1
    private static final Long STUDENT_ID = 1L;   // student1

    /**
     * 核心链路: 创建草稿 → 发布 → 状态变更正确
     */
    @Test
    @DisplayName("任务发布链路: DRAFT→PUBLISHED 状态变更")
    void publishTask_shouldChangeStatusToPublished() {
        // 1. 创建草稿任务
        Task task = new Task();
        task.setTitle("集成测试-待发布任务");
        task.setDescription("验证发布流程");
        task.setTaskType("AFTER_CLASS");
        task.setTargetType("CLASS");
        task.setTargetId(1L);
        task.setTeacherId(TEACHER_ID);
        task.setSchoolId(1L);
        task.setStageId(4L);
        task.setTotalScore(new BigDecimal("100"));
        task.setDeadline(LocalDateTime.now().plusDays(7));

        Task draft = taskService.create(task);
        assertNotNull(draft.getId());
        assertEquals("DRAFT", draft.getStatus(), "初始状态应为DRAFT");

        // 2. 发布任务
        Task published = taskService.publish(draft.getId());
        assertNotNull(published, "publish()应返回任务对象");
        assertEquals("PUBLISHED", published.getStatus(), "发布后状态应为PUBLISHED");

        // 3. 验证数据库中的状态已更新
        Task fromDb = taskMapper.selectById(draft.getId());
        assertNotNull(fromDb);
        assertEquals("PUBLISHED", fromDb.getStatus(), "数据库中状态应已更新为PUBLISHED");
    }

    /**
     * 学生可查询到已发布的任务
     */
    @Test
    @DisplayName("学生可见性: 已发布任务应出现在学生任务列表中")
    void publishedTask_shouldBeVisibleToStudent() {
        // 创建并发布任务
        Task task = new Task();
        task.setTitle("集成测试-学生可见任务");
        task.setTaskType("AFTER_CLASS");
        task.setTargetType("CLASS");
        task.setTargetId(1L); // CS2025-01，student1所在班级
        task.setTeacherId(TEACHER_ID);
        task.setSchoolId(1L);
        task.setStageId(4L);
        task.setTotalScore(new BigDecimal("100"));
        task.setDeadline(LocalDateTime.now().plusDays(7));

        Task created = taskService.create(task);
        taskService.publish(created.getId());

        // 验证学生任务列表中包含此任务
        List<Task> studentTasks = taskService.getStudentTasks(STUDENT_ID);
        assertNotNull(studentTasks, "学生任务列表不应为null");
        assertFalse(studentTasks.isEmpty(), "学生任务列表不应为空");

        boolean found = studentTasks.stream()
            .anyMatch(t -> t.getId().equals(created.getId()));
        assertTrue(found, "已发布的任务应出现在学生任务列表中");
    }

    /**
     * 草稿状态任务学生不可见
     */
    @Test
    @DisplayName("草稿不可见: DRAFT状态任务不出现在学生列表中")
    void draftTask_shouldNotBeVisibleToStudent() {
        // 创建草稿但不发布
        Task task = new Task();
        task.setTitle("集成测试-草稿不可见");
        task.setTaskType("AFTER_CLASS");
        task.setTargetType("CLASS");
        task.setTargetId(1L);
        task.setTeacherId(TEACHER_ID);
        task.setSchoolId(1L);
        task.setStageId(4L);
        task.setTotalScore(new BigDecimal("100"));
        task.setDeadline(LocalDateTime.now().plusDays(7));

        Task draft = taskService.create(task);
        assertEquals("DRAFT", draft.getStatus());

        // 验证学生可见的任务中不包含此草稿
        List<Task> studentTasks = taskService.getStudentTasks(STUDENT_ID);
        boolean found = studentTasks.stream()
            .anyMatch(t -> t.getId().equals(draft.getId()));
        assertFalse(found, "DRAFT状态任务不应出现在学生任务列表中");
    }

    /**
     * 已关闭的任务学生不可见
     */
    @Test
    @DisplayName("关闭不可见: CLOSED状态任务不出现在学生列表中")
    void closedTask_shouldNotBeVisibleToStudent() {
        // 创建→发布→关闭
        Task task = new Task();
        task.setTitle("集成测试-关闭不可见");
        task.setTaskType("AFTER_CLASS");
        task.setTargetType("CLASS");
        task.setTargetId(1L);
        task.setTeacherId(TEACHER_ID);
        task.setSchoolId(1L);
        task.setStageId(4L);
        task.setTotalScore(new BigDecimal("100"));
        task.setDeadline(LocalDateTime.now().plusDays(7));

        Task created = taskService.create(task);
        taskService.publish(created.getId());
        taskService.close(created.getId());

        // 验证关闭后学生不可见
        Task closed = taskMapper.selectById(created.getId());
        assertEquals("CLOSED", closed.getStatus());

        List<Task> studentTasks = taskService.getStudentTasks(STUDENT_ID);
        boolean found = studentTasks.stream()
            .anyMatch(t -> t.getId().equals(created.getId()));
        assertFalse(found, "CLOSED状态任务不应出现在学生任务列表中");
    }

    /**
     * 重新打开已关闭的任务
     */
    @Test
    @DisplayName("重新打开: CLOSED→PUBLISHED 状态恢复")
    void reopenTask_shouldRestoreToPublished() {
        // 创建→发布→关闭→重新打开
        Task task = new Task();
        task.setTitle("集成测试-重新打开");
        task.setTaskType("AFTER_CLASS");
        task.setTargetType("CLASS");
        task.setTargetId(1L);
        task.setTeacherId(TEACHER_ID);
        task.setSchoolId(1L);
        task.setStageId(4L);
        task.setTotalScore(new BigDecimal("100"));
        task.setDeadline(LocalDateTime.now().plusDays(7));

        Task created = taskService.create(task);
        taskService.publish(created.getId());
        taskService.close(created.getId());

        Task closed = taskMapper.selectById(created.getId());
        assertEquals("CLOSED", closed.getStatus());

        // 重新打开
        taskService.reopen(created.getId());
        Task reopened = taskMapper.selectById(created.getId());
        assertEquals("PUBLISHED", reopened.getStatus(), "重新打开后状态应恢复为PUBLISHED");

        // 学生应能再次看到
        List<Task> studentTasks = taskService.getStudentTasks(STUDENT_ID);
        boolean found = studentTasks.stream()
            .anyMatch(t -> t.getId().equals(created.getId()));
        assertTrue(found, "重新打开后任务应重新对学生可见");
    }
}
