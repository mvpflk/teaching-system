package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.mapper.QuestionBankMapper;
import com.school.teaching.mapper.UserMapper;
import com.school.teaching.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionBankServiceImplTest {

    @Mock QuestionBankMapper bankMapper;
    @Mock UserMapper userMapper;
    @InjectMocks QuestionBankServiceImpl service;

    @Test
    void pageQuestions_passesNewParamsToService() {
        Page<QuestionBank> empty = new Page<>(1, 20);
        when(bankMapper.selectPage(any(Page.class), any())).thenReturn(empty);

        IPage<QuestionBank> result = service.pageQuestions(null, null, "SINGLE_CHOICE", 2, null, 1,
            "BASIC", "THEORY", "MANUAL", "latest", 1, 20);

        assertNotNull(result);
        verify(bankMapper, times(1)).selectPage(any(Page.class), any());
    }

    @Test
    void pageQuestions_nullNewParams_noException() {
        Page<QuestionBank> empty = new Page<>(1, 20);
        when(bankMapper.selectPage(any(Page.class), any())).thenReturn(empty);

        IPage<QuestionBank> result = service.pageQuestions(null, null, null, null, null, 1,
            null, null, null, null, 1, 20);

        assertNotNull(result);
        verify(bankMapper, times(1)).selectPage(any(Page.class), any());
    }

    @Test
    void pageQuestions_mostUsed_callsSelectPage() {
        Page<QuestionBank> empty = new Page<>(1, 20);
        when(bankMapper.selectPage(any(Page.class), any())).thenReturn(empty);

        IPage<QuestionBank> result = service.pageQuestions(null, null, null, null, null, 1,
            null, null, null, "mostUsed", 1, 20);

        assertNotNull(result);
        verify(bankMapper, times(1)).selectPage(any(Page.class), any());
    }

    @Test
    void listByIds_capsAt200() {
        java.util.List<Long> ids = java.util.stream.LongStream.rangeClosed(1, 300).boxed().toList();
        assertThrows(BusinessException.class, () -> service.listByIds(ids));
    }
}
