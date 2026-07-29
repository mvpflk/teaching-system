package com.school.teaching.precision.impl;

import com.school.teaching.entity.Student;
import com.school.teaching.entity.StudentGroup;
import com.school.teaching.entity.StudentGroupMember;
import com.school.teaching.mapper.*;
import com.school.teaching.service.NotificationService;
import com.school.teaching.service.SystemService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoGroupServiceTest {

    @Mock private StudentMapper studentMapper;
    @Mock private StudentGroupMapper groupMapper;
    @Mock private StudentGroupMemberMapper memberMapper;
    @Mock private PrecisionProgressMapper progressMapper;
    @Mock private NotificationService notificationService;
    @Mock private SystemService systemService;
    @Mock private DictSubjectMapper dictSubjectMapper;

    @InjectMocks
    private AutoGroupService autoGroupService;

    private static final Long STUDENT_ID = 42L;
    private static final Long CLASS_ID = 10L;
    private static final Long USER_ID = 100L;
    private static final String SUBJECT = "数学[职高]";

    private Student makeStudent() {
        Student st = new Student();
        st.setId(STUDENT_ID);
        st.setClassId(CLASS_ID);
        st.setUserId(USER_ID);
        return st;
    }

    private void stubGroupInsertSetsId() {
        doAnswer(invocation -> {
            StudentGroup g = invocation.getArgument(0);
            g.setId(999L);
            return 1;
        }).when(groupMapper).insert(any(StudentGroup.class));
    }

    @Test
    @DisplayName("addSingleStudent: 分数≥阈值直接返回，不查学生")
    void addSingleStudent_scoreAboveThreshold_doesNothing() {
        when(systemService.getIntConfig("remedial.auto_group_threshold", 50)).thenReturn(50);

        autoGroupService.addSingleStudent(STUDENT_ID, SUBJECT, 60);

        verify(studentMapper, never()).selectById(any());
        verify(groupMapper, never()).insert(any());
        verify(memberMapper, never()).insert(any());
        verify(notificationService, never()).notify(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("addSingleStudent: 学生不存在直接返回")
    void addSingleStudent_studentNotFound_doesNothing() {
        when(systemService.getIntConfig("remedial.auto_group_threshold", 50)).thenReturn(50);
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(null);

        autoGroupService.addSingleStudent(STUDENT_ID, SUBJECT, 40);

        verify(groupMapper, never()).insert(any());
        verify(memberMapper, never()).insert(any());
        verify(notificationService, never()).notify(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("addSingleStudent: 低分且新入组→创建分组+通知")
    void addSingleStudent_belowThreshold_newlyAdded_createsGroupAndNotifies() {
        stubGroupInsertSetsId();
        when(systemService.getIntConfig("remedial.auto_group_threshold", 50)).thenReturn(50);
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(makeStudent());
        when(groupMapper.selectOne(any())).thenReturn(null);
        when(memberMapper.selectCount(any())).thenReturn(0L);

        autoGroupService.addSingleStudent(STUDENT_ID, SUBJECT, 40);

        verify(groupMapper).insert(any(StudentGroup.class));
        ArgumentCaptor<StudentGroupMember> captor = ArgumentCaptor.forClass(StudentGroupMember.class);
        verify(memberMapper).insert(captor.capture());
        assertEquals(999L, captor.getValue().getGroupId());
        assertEquals(STUDENT_ID, captor.getValue().getStudentId());
        verify(notificationService).notify(eq(USER_ID), eq("remedial_group"), eq("偏科提分·加入分组"), anyString(), any());
    }

    @Test
    @DisplayName("addSingleStudent: 已在组内不重复通知")
    void addSingleStudent_belowThreshold_alreadyInGroup_skipsNotification() {
        stubGroupInsertSetsId();
        when(systemService.getIntConfig("remedial.auto_group_threshold", 50)).thenReturn(50);
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(makeStudent());
        when(groupMapper.selectOne(any())).thenReturn(null);
        when(memberMapper.selectCount(any())).thenReturn(1L);

        autoGroupService.addSingleStudent(STUDENT_ID, SUBJECT, 40);

        verify(groupMapper).insert(any(StudentGroup.class));
        verify(memberMapper, never()).insert(any());
        verify(notificationService, never()).notify(any(), any(), any(), any(), any());
    }
}
