package com.school.teaching.service;

import com.school.teaching.service.impl.KnowledgeBaseServiceImpl;
import com.school.teaching.service.impl.KnowledgeBaseAdminService;
import com.school.teaching.service.impl.KnowledgeBaseStudentService;
import com.school.teaching.service.impl.KnowledgeBaseReviewService;
import com.school.teaching.service.impl.KnowledgeBaseRecommendService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * KnowledgeBaseServiceImpl 委托层测试 — 验证方法正确转发到子服务。
 * 业务逻辑的完整测试应在对应子服务的测试中。
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceTest {

    @Mock private KnowledgeBaseAdminService adminService;
    @Mock private KnowledgeBaseStudentService studentService;
    @Mock private KnowledgeBaseReviewService reviewService;
    @Mock private KnowledgeBaseRecommendService recommendService;

    @InjectMocks
    private KnowledgeBaseServiceImpl knowledgeBaseService;

    @Test
    void getSubjectsGrouped_计算机专业返回分组结果() {
        List<Map<String, Object>> publicSubjects = List.of(Map.of("id", 20L, "name", "语文[职高]"));
        List<Map<String, Object>> majorSubjects = List.of(Map.of("id", 4L, "name", "信息技术应用基础"));
        Map<String, Object> expected = Map.of(
            "majorName", "计算机",
            "publicSubjects", publicSubjects,
            "majorSubjects", majorSubjects
        );

        when(studentService.getSubjectsGrouped(1L)).thenReturn(expected);

        Map<String, Object> result = knowledgeBaseService.getSubjectsGrouped(1L);

        assertNotNull(result);
        assertEquals("计算机", result.get("majorName"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pubSubjects = (List<Map<String, Object>>) result.get("publicSubjects");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> majSubjects = (List<Map<String, Object>>) result.get("majorSubjects");

        // 语文被2个专业映射，应归为公共课
        assertEquals(1, pubSubjects.size());
        assertEquals(20L, pubSubjects.get(0).get("id"));

        // 信息技术仅被计算机映射，应归为专业课
        assertEquals(1, majSubjects.size());
        assertEquals(4L, majSubjects.get(0).get("id"));

        verify(studentService).getSubjectsGrouped(1L);
    }

    @Test
    void getSubjectsGrouped_未分配专业返回空专业课() {
        Map<String, Object> nullMajorResult = new HashMap<>();
        nullMajorResult.put("majorName", null);
        when(studentService.getSubjectsGrouped(2L)).thenReturn(nullMajorResult);

        Map<String, Object> result = knowledgeBaseService.getSubjectsGrouped(2L);

        assertNotNull(result);
        assertNull(result.get("majorName"));
        verify(studentService).getSubjectsGrouped(2L);
    }
}
