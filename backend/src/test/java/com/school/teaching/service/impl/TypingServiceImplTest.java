package com.school.teaching.service.impl;

import com.school.teaching.entity.TypingRecord;
import com.school.teaching.mapper.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TypingServiceImplTest {

    @Mock private TypingRecordMapper recordMapper;
    @Mock private TypingTextMapper textMapper;
    @Mock private TypingCompetitionMapper competitionMapper;
    @Mock private TypingCompetitionResultMapper resultMapper;
    @Mock private TypingLevelMapper levelMapper;
    @Mock private StudentMapper studentMapper;
    @Mock private ClassesMapper classesMapper;

    @InjectMocks
    private TypingServiceImpl typingService;

    private TypingRecord r(LocalDateTime t, int speed, double accuracy) {
        TypingRecord r = new TypingRecord();
        r.setCreatedAt(t);
        r.setSpeedWpm(BigDecimal.valueOf(speed));
        r.setAccuracy(BigDecimal.valueOf(accuracy));
        r.setTotalChars(100);
        r.setDurationSeconds(120);
        return r;
    }

    // ═══════════ getStudentSpeedTrend ═══════════

    @Test
    @DisplayName("DB按时间倒序返回 → 方法内反转后按日期升序")
    void getStudentSpeedTrend_shouldReturnAscending() {
        // 模拟DB返回结果：ORDER BY created_at DESC → 最新的在前
        List<TypingRecord> dbResult = new ArrayList<>(List.of(
            r(LocalDateTime.of(2026, 5, 30, 10, 0), 48, 91.0),
            r(LocalDateTime.of(2026, 5, 29, 10, 0), 52, 94.5),
            r(LocalDateTime.of(2026, 5, 28, 10, 0), 45, 92.0)
        ));
        when(recordMapper.selectList(any())).thenReturn(dbResult);

        List<Map<String, Object>> result = typingService.getStudentSpeedTrend(1L, 10);

        assertEquals(3, result.size());
        // 方法内 Collections.reverse → 最终输出升序
        assertEquals("2026-05-28", result.get(0).get("date"));
        assertEquals("2026-05-29", result.get(1).get("date"));
        assertEquals("2026-05-30", result.get(2).get("date"));
        // 验证字段值
        assertEquals(45, result.get(0).get("speedWpm"));
        assertEquals(48, result.get(2).get("speedWpm"));
    }

    @Test
    @DisplayName("无记录 → 返回空列表")
    void getStudentSpeedTrend_noRecords_shouldReturnEmpty() {
        when(recordMapper.selectList(any())).thenReturn(List.of());
        assertTrue(typingService.getStudentSpeedTrend(1L, 10).isEmpty());
    }

    @Test
    @DisplayName("返回的记录含全部必需字段")
    void getStudentSpeedTrend_shouldContainRequiredFields() {
        when(recordMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(
            r(LocalDateTime.now(), 60, 95.0)
        )));

        List<Map<String, Object>> result = typingService.getStudentSpeedTrend(1L, 10);
        assertEquals(1, result.size());
        Map<String, Object> item = result.get(0);
        assertNotNull(item.get("date"), "date不能为null");
        assertNotNull(item.get("speedWpm"), "speedWpm不能为null");
        assertNotNull(item.get("accuracy"), "accuracy不能为null");
        assertNotNull(item.get("totalChars"), "totalChars不能为null");
        assertNotNull(item.get("durationSeconds"), "durationSeconds不能为null");
    }
}
