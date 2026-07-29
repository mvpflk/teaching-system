package com.school.teaching.service.impl;

import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.security.StudentResolver;
import com.school.teaching.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock private TaskMapper taskMapper;
    @Mock private TaskSubmissionMapper submissionMapper;
    @Mock private StudentMapper studentMapper;
    @Mock private ClassesMapper classesMapper;
    @Mock private TeacherMapper teacherMapper;
    @Mock private StudentResolver studentResolver;
    @Mock private SystemSettingMapper systemSettingMapper;
    @Mock private CheatEventLogMapper cheatEventLogMapper;
    @Mock private TaskCrudService taskCrudService;
    @Mock private TaskPublishService taskPublishService;
    @Mock private TaskQueryService taskQueryService;
    @Mock private TaskQuestionService taskQuestionService;
    @Mock private TaskReviewService taskReviewService;

    @InjectMocks private TaskServiceImpl taskService;
    private MockedStatic<SecurityUtils> securityMock;

    @BeforeEach
    void setup() {
        securityMock = mockStatic(SecurityUtils.class);
        securityMock.when(SecurityUtils::isAdmin).thenReturn(true);
        securityMock.when(SecurityUtils::getCurrentSchoolId).thenReturn(1L);
        securityMock.when(SecurityUtils::getCurrentStageId).thenReturn(4L);
        securityMock.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
    }

    @AfterEach
    void teardown() {
        securityMock.close();
    }

    @Test @DisplayName("getById: 委托到TaskCrudService")
    void getById_shouldDelegateToCrudService() {
        Task t = new Task(); t.setId(1L); t.setTitle("测试任务");
        when(taskCrudService.getById(1L)).thenReturn(t);
        assertEquals("测试任务", taskService.getById(1L).getTitle());
    }

    @Test @DisplayName("getById: TaskCrudService抛异常时传播")
    void getById_shouldPropagateException() {
        when(taskCrudService.getById(999L)).thenThrow(new BusinessException(404, "任务不存在"));
        BusinessException ex = assertThrows(BusinessException.class, () -> taskService.getById(999L));
        assertEquals(404, ex.getCode());
    }

    @Test @DisplayName("create: 委托到TaskCrudService")
    void create_shouldDelegateToCrudService() {
        Task input = new Task(); input.setTitle("新建任务");
        Task created = new Task(); created.setId(100L); created.setStatus("DRAFT");
        when(taskCrudService.create(any(Task.class))).thenReturn(created);
        Task result = taskService.create(input);
        assertNotNull(result.getId());
        assertEquals("DRAFT", result.getStatus());
    }

    @Test @DisplayName("delete: 委托到TaskCrudService")
    void delete_shouldDelegateToCrudService() {
        doNothing().when(taskCrudService).delete(1L);
        assertDoesNotThrow(() -> taskService.delete(1L));
        verify(taskCrudService).delete(1L);
    }

    @Test @DisplayName("getQuestions: 委托到TaskQuestionService")
    void getQuestions_shouldDelegateToQuestionService() {
        when(taskQuestionService.getQuestions(1L)).thenReturn(List.of());
        assertTrue(taskService.getQuestions(1L).isEmpty());
    }

    @Test @DisplayName("getPendingCount: 委托到TaskQueryService")
    void getPendingCount_shouldDelegateToQueryService() {
        when(taskQueryService.getPendingCount(1L)).thenReturn(Map.of("count", 0L, "urgent", 0L, "warning", 0L));
        Map<String, Object> r = taskService.getPendingCount(1L);
        assertEquals(0L, r.get("count"));
        assertEquals(0L, r.get("urgent"));
        assertEquals(0L, r.get("warning"));
    }

    @Test @DisplayName("resendToPending: 委托到TaskPublishService")
    void resendToPending_shouldDelegateToPublishService() {
        when(taskPublishService.resendToPending(1L)).thenReturn(Map.of("count", 0));
        assertEquals(0, taskService.resendToPending(1L).get("count"));
    }

    @Test @DisplayName("publish: 委托到TaskPublishService")
    void publish_shouldDelegateToPublishService() {
        Task t = new Task(); t.setId(1L); t.setStatus("PUBLISHED");
        when(taskPublishService.publish(1L)).thenReturn(t);
        assertEquals("PUBLISHED", taskService.publish(1L).getStatus());
    }

    @Test @DisplayName("exportScores: 委托到TaskReviewService")
    void exportScores_shouldDelegateToReviewService() {
        byte[] data = new byte[]{1, 2, 3};
        when(taskReviewService.exportScores(1L)).thenReturn(data);
        assertArrayEquals(data, taskService.exportScores(1L));
    }

    @Test @DisplayName("getSubmissionAnswers: 委托到TaskReviewService")
    void getSubmissionAnswers_shouldDelegateToReviewService() {
        when(taskReviewService.getSubmissionAnswers(1L, 2L)).thenReturn(List.of());
        assertTrue(taskService.getSubmissionAnswers(1L, 2L).isEmpty());
    }
}