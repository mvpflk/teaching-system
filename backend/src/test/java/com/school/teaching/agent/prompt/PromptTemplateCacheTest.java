package com.school.teaching.agent.prompt;

import com.school.teaching.entity.PromptTemplate;
import com.school.teaching.mapper.PromptTemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptTemplateCacheTest {

    @Mock
    private PromptTemplateMapper mapper;

    @InjectMocks
    private PromptTemplateCache cache;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getTemplate_shouldReturnByExactSubject() {
        PromptTemplate math = new PromptTemplate();
        math.setType("TEMPLATE");
        math.setName("lesson_prep");
        math.setSubject("math");
        math.setContent("数学备课助手内容");
        math.setVersion(1);
        math.setIsActive(true);

        PromptTemplate generic = new PromptTemplate();
        generic.setType("TEMPLATE");
        generic.setName("lesson_prep");
        generic.setSubject(null);
        generic.setContent("通用备课助手内容");
        generic.setVersion(2);
        generic.setIsActive(true);

        when(mapper.selectList(any())).thenReturn(List.of(math, generic));
        cache.refresh();

        assertEquals("数学备课助手内容", cache.getTemplate("lesson_prep", "math"));
        assertEquals(1, cache.getTemplateVersion("lesson_prep", "math"));
        assertEquals("通用备课助手内容", cache.getTemplate("lesson_prep", null));
        assertEquals(2, cache.getTemplateVersion("lesson_prep", null));
    }

    @Test
    void getTemplate_shouldFallbackToGeneric() {
        PromptTemplate generic = new PromptTemplate();
        generic.setType("TEMPLATE");
        generic.setName("lesson_prep");
        generic.setSubject(null);
        generic.setContent("通用内容");
        generic.setVersion(1);
        generic.setIsActive(true);
        when(mapper.selectList(any())).thenReturn(List.of(generic));
        cache.refresh();

        assertEquals("通用内容", cache.getTemplate("lesson_prep", "physics"));
        assertEquals(1, cache.getTemplateVersion("lesson_prep", "physics"));
    }

    @Test
    void getFinal_shouldReturnExactMatch() {
        PromptTemplate pt = new PromptTemplate();
        pt.setType("FINAL");
        pt.setName("teaching_design");
        pt.setSubject("math");
        pt.setContent("数学覆盖");
        pt.setIsActive(true);
        when(mapper.selectList(any())).thenReturn(List.of(pt));
        cache.refresh();

        assertEquals("数学覆盖", cache.getFinal("teaching_design", "math"));
    }

    @Test
    void getFinal_shouldReturnGlobalWhenNoSubjectMatch() {
        PromptTemplate pt = new PromptTemplate();
        pt.setType("FINAL");
        pt.setName("teaching_design");
        pt.setSubject(null);
        pt.setContent("全局覆盖");
        pt.setIsActive(true);
        when(mapper.selectList(any())).thenReturn(List.of(pt));
        cache.refresh();

        assertEquals("全局覆盖", cache.getFinal("teaching_design", "english"));
        assertEquals("全局覆盖", cache.getFinal("teaching_design", "math"));
        assertNull(cache.getFinal("nonexistent", "math"));
    }

    @Test
    void refresh_shouldLoadOnlyActive() {
        PromptTemplate active = new PromptTemplate();
        active.setType("TEMPLATE");
        active.setName("lesson_prep");
        active.setContent("active");
        active.setVersion(2);
        active.setIsActive(true);

        PromptTemplate inactive = new PromptTemplate();
        inactive.setType("TEMPLATE");
        inactive.setName("lesson_prep");
        inactive.setContent("inactive");
        inactive.setVersion(1);
        inactive.setIsActive(false);

        when(mapper.selectList(any())).thenReturn(List.of(active, inactive));
        cache.refresh();

        assertEquals("active", cache.getTemplate("lesson_prep", null));
        assertEquals(2, cache.getTemplateVersion("lesson_prep", null));
    }

    @Test
    void refresh_shouldHandleEmptyDb() {
        when(mapper.selectList(any())).thenReturn(List.of());
        cache.refresh();
        assertNull(cache.getTemplate("anything", null));
        assertEquals(0, cache.getTemplateVersion("anything", null));
        assertNull(cache.getFinal("anything", null));
    }
}
