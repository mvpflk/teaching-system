package com.school.teaching.common.handler;

import com.school.teaching.common.QuestionTypeEnum;
import com.school.teaching.common.QuestionTypeHandler;
import com.school.teaching.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 题型处理器注册中心 — Spring 自动注入所有 QuestionTypeHandler Bean。
 * 支持一个 Handler 覆盖多个题型（如 SubjectiveHandler 覆盖 5 种主观题）。
 */
@Component
public class QuestionTypeHandlerRegistry {

    private final Map<QuestionTypeEnum, QuestionTypeHandler> map = new HashMap<>();

    public QuestionTypeHandlerRegistry(List<QuestionTypeHandler> handlers) {
        // 优先注册声明了多类型支持的 Handler
        for (QuestionTypeHandler h : handlers) {
            if (h instanceof SubjectiveHandler) {
                for (QuestionTypeEnum t : SubjectiveHandler.supportedTypes()) map.put(t, h);
            } else {
                map.put(h.getType(), h);
            }
        }
    }

    /** 根据题型获取处理器，未找到抛异常 */
    public QuestionTypeHandler get(QuestionTypeEnum type) {
        QuestionTypeHandler h = map.get(type);
        if (h == null) throw new BusinessException(400, "未找到题型处理器: " + type);
        return h;
    }

    /** 安全获取，用于可选处理 */
    public QuestionTypeHandler getOrNull(QuestionTypeEnum type) {
        return map.get(type);
    }
}
