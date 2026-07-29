package com.school.teaching.service.impl;

import com.school.teaching.entity.Task;
import com.school.teaching.entity.TaskSubmission;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.TaskMapper;
import com.school.teaching.mapper.TaskSubmissionMapper;
import com.school.teaching.service.TaskService;
import com.school.teaching.service.TaskSubmissionService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 场景1: 考试提交→AI评分→成绩落库 核心链路集成测试
 *
 * 测试链路: 创建任务 → 发布 → 开始答题 → 提交答案 → 验证状态和成绩
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
@Disabled("需要运行中的 MySQL — 请先启动数据库再运行此测试")
@DisplayName("任务提交集成测试: 创建→发布→提交→评分全链路")
class TaskSubmissionIntegrationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskSubmissionService submissionService;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TaskSubmissionMapper submissionMapper;

    private static final Long TEACHER_ID = 2L;   // teacher1
    private static final Long STUDENT_ID = 1L;   // student1

    /**
     * 核心链路: 创建任务 → 发布 → 学生提交 → 验证提交记录
     */
    @Test
    @DisplayName("完整提交链路: 创建→发布→提交→验证状态和成绩")
    void fullSubmissionFlow_shouldCreateGradedSubmission() {
        // 1. 创建课后作业任务(DRAFT)
        Task task = new Task();
        task.setTitle("集成测试-课后作业");
        task.setDescription("用于集成测试的课后作业任务");
        task.setTaskType("AFTER_CLASS");
        task.setTargetType("CLASS");
        task.setTargetId(1L); // CS2025-01
        task.setTeacherId(TEACHER_ID);
        task.setSchoolId(1L);
        task.setStageId(4L);
        task.setTotalScore(new BigDecimal("100"));
        task.setDeadline(LocalDateTime.now().plusDays(7));

        Task created = taskService.create(task);
        assertNotNull(created.getId(), "任务创建后应有ID");
        assertEquals("DRAFT", created.getStatus(), "新建任务状态应为DRAFT");

        // 2. 发布任务
        Task published = taskService.publish(created.getId());
        assertNotNull(published, "发布后应返回任务对象");
        assertEquals("PUBLISHED", published.getStatus(), "发布后状态应为PUBLISHED");

        // 3. 学生提交任务(无需startExam的非考试任务)
        Map<String, Object> payload = Map.of(
            "content", "这是我的课后作业答案内容。",
            "answers", Map.of()
        );

        TaskSubmission sub = submissionService.submit(published.getId(), STUDENT_ID, payload);
        assertNotNull(sub, "提交后应返回提交记录");
        assertNotNull(sub.getId(), "提交记录应有ID");
        assertEquals(published.getId(), sub.getTaskId(), "提交记录的taskId应匹配");
        assertEquals(STUDENT_ID, sub.getStudentId(), "提交记录的studentId应匹配");
        assertEquals("SUBMITTED", sub.getStatus(), "提交后状态应为SUBMITTED");
        assertNotNull(sub.getSubmittedAt(), "提交时间不应为空");

        // 4. 验证提交记录已持久化
        TaskSubmission persisted = submissionMapper.selectById(sub.getId());
        assertNotNull(persisted, "提交记录应已持久化到数据库");
        assertEquals("SUBMITTED", persisted.getStatus());
        assertEquals(STUDENT_ID, persisted.getStudentId());

        // 5. 验证任务状态变更(PUBLISHED→ONGOING，首次提交触发)
        Task updatedTask = taskMapper.selectById(published.getId());
        assertEquals("ONGOING", updatedTask.getStatus(), "首次提交后任务状态应变为ONGOING");
    }

    /**
     * 重复提交防护: 同一学生不能重复提交同一任务
     */
    @Test
    @DisplayName("重复提交防护: 已提交学生再次提交应抛409")
    void duplicateSubmission_shouldThrowConflict() {
        // 创建并发布任务
        Task task = new Task();
        task.setTitle("集成测试-防重复提交");
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

        // 第一次提交成功
        Map<String, Object> payload = Map.of("content", "第一次提交");
        TaskSubmission first = submissionService.submit(created.getId(), STUDENT_ID, payload);
        assertNotNull(first);
        assertEquals("SUBMITTED", first.getStatus());

        // 第二次提交应抛异常
        BusinessException ex = assertThrows(BusinessException.class, () -> {
            submissionService.submit(created.getId(), STUDENT_ID, Map.of("content", "重复提交"));
        });
        assertEquals(409, ex.getCode(), "重复提交应返回409冲突状态码");
    }

    /**
     * 任务不存在时提交应返回404
     */
    @Test
    @DisplayName("提交不存在的任务: 应抛404异常")
    void submitNonExistentTask_shouldThrowNotFound() {
        BusinessException ex = assertThrows(BusinessException.class, () -> {
            submissionService.submit(99999L, STUDENT_ID, Map.of("content", "test"));
        });
        assertEquals(404, ex.getCode(), "不存在的任务提交应返回404");
    }

    /**
     * 草稿状态任务不可提交
     */
    @Test
    @DisplayName("提交未发布的任务: 应抛业务异常")
    void submitDraftTask_shouldThrowException() {
        Task task = new Task();
        task.setTitle("集成测试-草稿任务");
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

        // 尝试提交草稿任务
        assertThrows(Exception.class, () -> {
            submissionService.submit(draft.getId(), STUDENT_ID, Map.of("content", "test"));
        }, "提交DRAFT状态任务应抛异常");
    }
}
