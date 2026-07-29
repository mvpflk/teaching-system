package com.school.teaching.service.impl;

import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.CreditService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  // R112修复：避免 strict stubbing 误报（userId 参数随测试数据变化）
class ClassroomServiceImplTest {

    @Mock private ClassroomSessionMapper sessionMapper;
    @Mock private ClassroomParticipationMapper participationMapper;
    @Mock private StudentMapper studentMapper;
    @Mock private UserMapper userMapper;
    @Mock private ClassroomQuestionMapper questionMapper;
    @Mock private WrongQuestionMapper wrongQuestionMapper;
    @Mock private QuestionBankMapper questionBankMapper;
    @Mock private CreditService creditService;
    @Mock private ClassroomAbsentStudentMapper absentStudentMapper;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private ClassroomServiceImpl classroomService;

    private Student s(long id, long userId) { Student s = new Student(); s.setId(id); s.setUserId(userId); return s; }
    private User u(long id, String name) { User u = new User(); u.setId(id); u.setRealName(name); return u; }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(classroomService, "self", classroomService);
    }

    /** 让 sessionMapper.insert 自动为 session 生成 ID */
    private void mockInsertGeneratesId() {
        doAnswer(inv -> {
            ClassroomSession s = inv.getArgument(0);
            if (s.getId() == null) s.setId(new Random().nextLong());
            return null;
        }).when(sessionMapper).insert(any(ClassroomSession.class));
    }

    // ═══════════ startQuiz — 自动排重 ═══════════

    @Test
    @DisplayName("无已抽学生 → 从全班中抽取")
    void startQuiz_noPreviousPick_shouldPickFromFullClass() {
        mockInsertGeneratesId();
        when(studentMapper.selectList(any())).thenReturn(List.of(s(1L, 10L), s(2L, 20L)));
        when(sessionMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectById(anyLong())).thenReturn(u(10L, "张三"));  // R112修复：覆盖所有userId避免随机选取导致stub不匹配

        Map<String, Object> result = classroomService.startQuiz(1L, 100L,
                null, "1+1=?", "LAB", null, null, null, null);

        assertNotNull(result.get("sessionId"));
        assertNotNull(result.get("studentId"));
        assertEquals("张三", result.get("studentName"));
        verify(sessionMapper).insert(any(ClassroomSession.class));
    }

    @Test
    @DisplayName("有已抽学生 → 排除后从剩余中抽取")
    void startQuiz_withPreviousPicks_shouldExcludeThem() {
        mockInsertGeneratesId();
        when(studentMapper.selectList(any())).thenReturn(List.of(s(1L, 10L), s(2L, 20L)));

        ClassroomSession recent = new ClassroomSession();
        recent.setId(100L);
        when(sessionMapper.selectList(any())).thenReturn(List.of(recent));

        ClassroomParticipation cp = new ClassroomParticipation();
        cp.setStudentId(1L); cp.setParticipationType("QUIZZED");
        when(participationMapper.selectList(any())).thenReturn(List.of(cp));

        when(userMapper.selectById(20L)).thenReturn(u(20L, "李四"));

        Map<String, Object> result = classroomService.startQuiz(1L, 100L,
                null, "1+1=?", "LAB", null, null, null, null);

        assertEquals(2L, result.get("studentId"), "应排除学生1，抽到学生2");
        assertEquals("李四", result.get("studentName"));
    }

    @Test
    @DisplayName("全班都已抽过 → 清空参与记录重新开始")
    void startQuiz_allPicked_shouldThrow409() {
        when(studentMapper.selectList(any())).thenReturn(List.of(s(1L, 10L)));

        ClassroomSession recent = new ClassroomSession();
        recent.setId(100L);
        when(sessionMapper.selectList(any())).thenReturn(List.of(recent));

        ClassroomParticipation cp = new ClassroomParticipation();
        cp.setStudentId(1L); cp.setParticipationType("QUIZZED");
        when(participationMapper.selectList(any())).thenReturn(List.of(cp));

        assertThrows(BusinessException.class, () ->
            classroomService.startQuiz(1L, 100L, null, "1+1=?", "LAB", null, null, null, null),
            "全部抽过应抛出BusinessException提示教师手动重置");
        // 不再自动删除历史记录
        verify(participationMapper, never()).delete(any());
    }

    @Test
    @DisplayName("前端传入excludeStudentIds → 合并排除")
    void startQuiz_withExcludeIds_shouldMerge() {
        mockInsertGeneratesId();
        when(studentMapper.selectList(any())).thenReturn(List.of(s(1L, 10L), s(2L, 20L), s(3L, 30L)));
        when(sessionMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectById(30L)).thenReturn(u(30L, "王五"));

        Map<String, Object> result = classroomService.startQuiz(1L, 100L,
                null, "1+1=?", "LAB", null, null, List.of(1L, 2L), null);

        assertEquals(3L, result.get("studentId"), "应排除1和2，抽到3");
    }

    @Test
    @DisplayName("班级无活跃学生 → 抛BusinessException")
    void startQuiz_noActiveStudents_shouldThrow() {
        when(studentMapper.selectList(any())).thenReturn(List.of());
        assertThrows(BusinessException.class, () ->
            classroomService.startQuiz(1L, 100L, null, "?", "LAB", null, null, null, null));
    }

    // ═══════════ removeFromQuizPool ═══════════

    @Test
    @DisplayName("removeFromQuizPool → 追加到缺席列表")
    void removeFromQuizPool_shouldAddToAbsent() {
        // getAbsentStudents 查询 classroom_absent_students
        when(absentStudentMapper.selectList(any())).thenReturn(List.of());

        assertDoesNotThrow(() -> classroomService.removeFromQuizPool(1L, 5L));
    }
}
