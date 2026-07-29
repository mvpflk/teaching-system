package com.school.teaching.common;

import com.school.teaching.entity.QuestionBank;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 题型处理策略接口 — 每种题型独立实现渲染和自动评分逻辑。
 *
 * 扩展方式：实现此接口 + @Component 注册为 Bean → Spring 自动注入 Map。
 * 约束：纯逻辑，不得依赖 Web 层；评分使用 BigDecimal。
 */
public interface QuestionTypeHandler {

    /** 返回此 Handler 处理的题型 */
    QuestionTypeEnum getType();

    /** 渲染为前端结构化数据 */
    Map<String, Object> renderQuestion(QuestionBank question);

    /** 校验答案格式。不合法抛 BusinessException */
    void validateAnswer(QuestionBank question, Object studentAnswer);

    /** 自动评分。客观题返回得分，主观题返回 null */
    BigDecimal scoreAnswer(QuestionBank question, Object studentAnswer);
}
