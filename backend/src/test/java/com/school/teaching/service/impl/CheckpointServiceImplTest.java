package com.school.teaching.service.impl;

import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.CreditService;
import com.school.teaching.service.QuestionMatchingService;
import com.school.teaching.service.SystemService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("闯关系统核心服务测试")
class CheckpointServiceImplTest {

    @Mock private CheckpointConfigMapper configMapper;
    @Mock private CheckpointProgressMapper progressMapper;
    @Mock private CheckpointKeywordLogMapper keywordLogMapper;
    @Mock private CheckpointMemoryCardMapper memoryCardMapper;
    @Mock private DictMajorSubjectMapper majorSubjectMapper;
    @Mock private DictMajorMapper majorMapper;
    @Mock private StudentMapper studentMapper;
    @Mock private ClassesMapper classesMapper;
    @Mock private QuestionBankMapper questionBankMapper;
    @Mock private WrongQuestionMapper wrongQuestionMapper;
    @Mock private DictSubjectMapper dictSubjectMapper;
    @Mock private CreditTransactionMapper creditTransactionMapper;
    @Mock private QuestionMatchingService questionMatchingService;
    @Mock private SystemService systemService;
    @Mock private CreditService creditService;

    @InjectMocks private CheckpointServiceImpl checkpointService;

    // ══════════════════════ rateToCreditLevel ══════════════════════

    @Test @DisplayName("rateToCreditLevel: 100% → 10分")
    void rate100_shouldReturn10() {
        assertEquals(10, invokeRateToCreditLevel(1.0));
    }

    @Test @DisplayName("rateToCreditLevel: 90% → 5分")
    void rate90_shouldReturn5() {
        assertEquals(5, invokeRateToCreditLevel(0.90));
        assertEquals(5, invokeRateToCreditLevel(0.95));
    }

    @Test @DisplayName("rateToCreditLevel: 80% → 3分")
    void rate80_shouldReturn3() {
        assertEquals(3, invokeRateToCreditLevel(0.80));
        assertEquals(3, invokeRateToCreditLevel(0.85));
    }

    @Test @DisplayName("rateToCreditLevel: 75% → 0分（不通过）")
    void rateUnder80_shouldReturn0() {
        assertEquals(0, invokeRateToCreditLevel(0.75));
        assertEquals(0, invokeRateToCreditLevel(0.0));
        assertEquals(0, invokeRateToCreditLevel(0.50));
    }

    // ══════════════════════ consumeCredits ══════════════════════

    @Test @DisplayName("consumeCredits: 余额充足成功扣减")
    void consumeCredits_sufficientBalance() {
        Student stu = new Student(); stu.setId(1L); stu.setTotalCredits(100);
        when(studentMapper.update(any(), any())).thenReturn(1);
        when(studentMapper.selectById(1L)).thenReturn(stu);
        when(creditTransactionMapper.selectOne(any())).thenReturn(null);

        boolean ok = checkpointService.consumeCredits(1L, 5, "test_consume");
        assertTrue(ok);
    }

    @Test @DisplayName("consumeCredits: 余额不足返回false")
    void consumeCredits_insufficientBalance() {
        lenient().when(studentMapper.update(any(), any())).thenReturn(0);

        boolean ok = checkpointService.consumeCredits(1L, 999, "test_consume");
        assertFalse(ok);
    }

    @Test @DisplayName("consumeCredits: null或0金额返回false")
    void consumeCredits_nullOrZero() {
        assertFalse(checkpointService.consumeCredits(null, 5, "test"));
        assertFalse(checkpointService.consumeCredits(1L, 0, "test"));
    }

    // ══════════════════════ isStudentInWhitelist ══════════════════════

    @Test @DisplayName("白名单: 空名单→开放")
    void whitelist_emptyMeansAll() {
        Map<String, String> settings = Map.of("feature.checkpoint_class_ids", "");
        when(systemService.getAllSettings()).thenReturn(settings);

        assertTrue(checkpointService.isStudentInWhitelist(1L));
    }

    @Test @DisplayName("白名单: 学生在名单内")
    void whitelist_studentInList() {
        Map<String, String> settings = Map.of("feature.checkpoint_class_ids", "1,3,5");
        when(systemService.getAllSettings()).thenReturn(settings);

        Student stu = new Student(); stu.setClassId(3L);
        when(studentMapper.selectOne(any())).thenReturn(stu);

        assertTrue(checkpointService.isStudentInWhitelist(1L));
    }

    @Test @DisplayName("白名单: 学生不在名单内")
    void whitelist_studentNotInList() {
        Map<String, String> settings = Map.of("feature.checkpoint_class_ids", "1,3,5");
        when(systemService.getAllSettings()).thenReturn(settings);

        Student stu = new Student(); stu.setClassId(99L);
        when(studentMapper.selectOne(any())).thenReturn(stu);

        assertFalse(checkpointService.isStudentInWhitelist(1L));
    }

    // ══════════════════════ getUnreviewedCount ══════════════════════

    @Test @DisplayName("待复习: 推送关闭返回0")
    void unreviewedCount_pushDisabled() {
        when(systemService.getIntConfig("feature.checkpoint_memory_push_days", 3)).thenReturn(0);
        assertEquals(0, checkpointService.getUnreviewedCount(1L));
    }

    @Test @DisplayName("待复习: 正常计算")
    void unreviewedCount_normal() {
        when(systemService.getIntConfig("feature.checkpoint_memory_push_days", 3)).thenReturn(3);
        when(memoryCardMapper.selectCount(any())).thenReturn(5L);
        assertEquals(5, checkpointService.getUnreviewedCount(1L));
    }

    // ═══════════════════════ 辅助 ═══════════════════════

    @SuppressWarnings("SameParameterValue")
    private int invokeRateToCreditLevel(double rate) {
        return CheckpointContentHelper.rateToCreditLevel(rate);
    }
}
