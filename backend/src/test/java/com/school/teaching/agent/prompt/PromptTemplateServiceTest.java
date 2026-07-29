package com.school.teaching.agent.prompt;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.PromptTemplate;
import com.school.teaching.mapper.PromptTemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromptTemplateServiceTest {

    @Mock private PromptTemplateMapper mapper;
    @Mock private PromptTemplateCache cache;
    @InjectMocks private PromptTemplateService service;

    private PromptTemplate template;

    @BeforeEach
    void setUp() {
        template = new PromptTemplate();
        template.setType("TEMPLATE");
        template.setName("lesson_prep");
        template.setContent("你是一个备课助手...");
    }

    @Test
    void createVersion_shouldIncrementVersion() {
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        service.createVersion(template);
        assertEquals(1, template.getVersion());
        verify(mapper).insert(template);
    }

    @Test
    void activateVersion_shouldDeactivateOldThenActivateNew() {
        PromptTemplate newVer = new PromptTemplate();
        newVer.setId(2L);
        newVer.setIsActive(false);
        when(mapper.selectById(2L)).thenReturn(newVer);

        service.activateVersion("lesson_prep", 2L);

        verify(mapper, times(1)).update(any(PromptTemplate.class), any(LambdaQueryWrapper.class));
        assertTrue(newVer.getIsActive());
        verify(cache).refresh();
    }

    @Test
    void upsertFinal_shouldUpdateExisting() {
        PromptTemplate existing = new PromptTemplate();
        existing.setId(1L);
        existing.setContent("旧内容");
        template.setSubject("math");
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        PromptTemplate result = service.upsertFinal(template);

        assertEquals("你是一个备课助手...", result.getContent());
        verify(mapper).updateById(existing);
        verify(cache).refresh();
    }
}
