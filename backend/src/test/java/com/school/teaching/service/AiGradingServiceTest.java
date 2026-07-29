package com.school.teaching.service;

import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.metrics.AiMetricsService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AiGradingService 批改结果组装回归测试。
 * 复现审计发现:aiResult.getOrDefault(key, default) 遇 AI 返回的 null 值不 fallback,
 * 叠加 Map.of 禁 null → 整次批改抛 NPE 失败。
 */
@ExtendWith(MockitoExtension.class)
class AiGradingServiceTest {

    @Mock private AiServiceGateway aiGateway;
    @Mock private QuestionBankMapper questionBankMapper;
    @Mock private AiCallLogMapper aiCallLogMapper;
    @Mock private TaskSubmissionMapper submissionMapper;
    @Mock private AiTaskStore taskStore;
    @Mock private AiMetricsService aiMetrics;

    @InjectMocks private AiGradingService service;

    @Test
    @DisplayName("AI返回含null字段值 → 批改完成(降级默认值)而非NPE失败")
    void nullFields_doesNotCrash() {
        ReflectionTestUtils.setField(service, "aiGradingEnabled", true);
        when(submissionMapper.selectById(1L)).thenReturn(new TaskSubmission());
        QuestionBank q = new QuestionBank();
        q.setQuestionText("题干");
        q.setCorrectAnswer("参考答案");
        when(questionBankMapper.selectById(2L)).thenReturn(q);

        Map<String, Object> aiRet = new HashMap<>();
        aiRet.put("score", null);
        aiRet.put("comment", null);
        aiRet.put("confidence", null);
        aiRet.put("explanation", null);
        when(aiGateway.scoreTextAnswer(any())).thenReturn(aiRet);
        when(aiGateway.getProvider()).thenReturn("deepseek");

        service.executeAsync("t1", 1L, 2L, 100L);

        // 关键:批改完成而非失败(旧代码 Map.of(null) 会抛 NPE → fail)
        verify(taskStore).complete(eq("t1"), any());
        verify(taskStore, never()).fail(anyString(), anyString());
    }
}
