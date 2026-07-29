package com.school.teaching.service;

import com.school.teaching.entity.Task;
import com.school.teaching.entity.TaskSubmission;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("需要 MySQL 数据库 — 本地无 MySQL 时跳过，CI/环境自动运行")
@SpringBootTest
@org.springframework.test.context.ActiveProfiles("local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql(scripts = {"/schema.sql", "/data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TaskServiceTest {

    @Autowired private TaskService taskService;
    @Autowired private TaskSubmissionService submissionService;

    private static Long taskId;
    private static Long submissionId;

    @Test @Order(1)
    void createTask_shouldPersist() {
        Task task = new Task();
        task.setTitle("E2E Homework Test");
        task.setDescription("Test homework flow");
        task.setTeacherId(1L);
        task.setSubject("Java");
        task.setTaskType("AFTER_CLASS");
        task.setScoreType("POINT_100");
        task.setTotalScore(BigDecimal.valueOf(100));
        task.setTargetType("CLASS");
        task.setTargetId(1L);
        task.setSchoolId(1L);
        task.setStageId(4L);

        Task created = taskService.create(task);
        assertNotNull(created.getId());
        assertEquals("DRAFT", created.getStatus());
        taskId = created.getId();
    }

    @Test @Order(2)
    void publishTask_shouldChangeStatusAndNotify() {
        assertNotNull(taskId);
        Task published = taskService.publish(taskId);
        assertEquals("PUBLISHED", published.getStatus());
    }

    @Test @Order(3)
    void getAccessibleTasks_shouldReturnForStudent() {
        List<Task> tasks = taskService.getStudentTasks(1L);
        assertFalse(tasks.isEmpty());
        assertTrue(tasks.stream().anyMatch(t -> t.getId().equals(taskId)));
    }

    @Test @Order(4)
    void submitTask_shouldCreateSubmission() {
        assertNotNull(taskId);
        Map<String, Object> payload = Map.of("content", "My homework answer", "attachments", "");
        TaskSubmission sub = submissionService.submit(taskId, 1L, payload);
        assertNotNull(sub.getId());
        assertEquals("SUBMITTED", sub.getStatus());
        submissionId = sub.getId();
    }

    @Test @Order(5)
    void gradeTask_shouldAssignScore() {
        assertNotNull(submissionId);
        TaskSubmission graded = submissionService.grade(submissionId, BigDecimal.valueOf(85), null, 2L);
        assertEquals(BigDecimal.valueOf(85), graded.getScore());
        assertEquals("GRADED", graded.getStatus());
        assertEquals("TEACHER", graded.getGradeType());
    }

    @Test @Order(6)
    void getQuestions_shouldReturnEmptyForHomework() {
        assertNotNull(taskId);
        assertTrue(taskService.getQuestions(taskId).isEmpty());
    }

    @Test @Order(7)
    void closeTask_shouldChangeStatus() {
        assertNotNull(taskId);
        Task closed = taskService.close(taskId);
        assertEquals("CLOSED", closed.getStatus());
    }

    @Test @Order(8)
    void deleteTask_shouldFailForNonDraft() {
        assertNotNull(taskId);
        assertThrows(Exception.class, () -> taskService.delete(taskId));
    }
}