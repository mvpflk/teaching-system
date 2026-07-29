package com.school.teaching.service;

import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.metrics.AiMetricsService;
import com.school.teaching.service.impl.QuestionReviewService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiQuestionGeneratorServiceTest {

    @Mock private AiServiceGateway aiGateway;
    @Mock private AiCallLogMapper aiCallLogMapper;
    @Mock private AiMetricsService aiMetrics;
    @Mock private KnowledgeNodeMapper nodeMapper;
    @Mock private AiTaskStore taskStore;
    @Mock private QuestionReviewService questionReviewService;

    private AiQuestionGeneratorService service;

    @BeforeEach
    void setUp() {
        service = new AiQuestionGeneratorService(aiGateway, aiCallLogMapper, aiMetrics, nodeMapper, taskStore);
        ReflectionTestUtils.setField(service, "self", service);
        ReflectionTestUtils.setField(service, "questionReviewService", questionReviewService);
    }

    @Test @DisplayName("approve: 委托到 questionReviewService")
    void approve_shouldDelegate() {
        service.approve(100L);
        verify(questionReviewService).approve(100L);
    }

    @Test @DisplayName("approve: 题目不存在→questionReviewService 抛异常")
    void approve_shouldThrow404() {
        doThrow(new BusinessException(404, "题目不存在")).when(questionReviewService).approve(999L);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.approve(999L));
        assertEquals(404, ex.getCode());
    }

    @Test @DisplayName("reject: 委托到 questionReviewService")
    void reject_shouldDelegate() {
        service.reject(200L);
        verify(questionReviewService).reject(200L);
    }

    @Test @DisplayName("listDrafts: 委托到 questionReviewService")
    void listDrafts_shouldDelegate() {
        QuestionBank q = new QuestionBank(); q.setId(1L); q.setStatus(0);
        when(questionReviewService.listDrafts(1L, 1L)).thenReturn(List.of(q));
        List<QuestionBank> r = service.listDrafts(1L, 1L);
        assertEquals(1, r.size());
        assertEquals(0, r.get(0).getStatus());
    }

    @Test @DisplayName("listDrafts: 无草稿返回空列表")
    void listDrafts_shouldReturnEmptyWhenNone() {
        when(questionReviewService.listDrafts(1L, 1L)).thenReturn(List.of());
        assertTrue(service.listDrafts(1L, 1L).isEmpty());
    }
}
