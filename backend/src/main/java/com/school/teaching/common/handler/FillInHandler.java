package com.school.teaching.common.handler;

import com.school.teaching.common.QuestionTypeEnum;
import com.school.teaching.common.QuestionTypeHandler;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class FillInHandler implements QuestionTypeHandler {

    @Override public QuestionTypeEnum getType() { return QuestionTypeEnum.FILL_IN; }

    @Override
    public Map<String, Object> renderQuestion(QuestionBank q) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("type", "fillin");
        r.put("text", q.getQuestionText());
        return r;
    }

    @Override
    public void validateAnswer(QuestionBank q, Object answer) {
        if (answer == null || answer.toString().trim().isEmpty())
            throw new BusinessException(400, "请填写答案");
        if (answer.toString().length() > 5000)
            throw new BusinessException(400, "答案过长");
    }

    @Override
    public BigDecimal scoreAnswer(QuestionBank q, Object answer) {
        if (q.getCorrectAnswer() == null || answer == null) return BigDecimal.ZERO;
        String correct = q.getCorrectAnswer().trim().toLowerCase();
        String student = answer.toString().trim().toLowerCase();
        // 支持多答案（用 ,/，/| 分隔，匹配任一即正确）
        for (String c : correct.split("[,，|]")) {
            String t = c.trim(); if (!t.isEmpty() && t.equals(student)) return BigDecimal.ONE;
        }
        return BigDecimal.ZERO;
    }
}
