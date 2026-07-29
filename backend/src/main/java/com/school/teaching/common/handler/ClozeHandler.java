package com.school.teaching.common.handler;

import com.school.teaching.common.QuestionTypeEnum;
import com.school.teaching.common.QuestionTypeHandler;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class ClozeHandler implements QuestionTypeHandler {

    @Override public QuestionTypeEnum getType() { return QuestionTypeEnum.CLOZE; }

    @Override
    public Map<String, Object> renderQuestion(QuestionBank q) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("type", "cloze");
        r.put("text", q.getQuestionText());
        // 从 answer_schema 提取空白数量
        r.put("blanks", extractBlankCount(q));
        return r;
    }

    @Override
    public void validateAnswer(QuestionBank q, Object answer) {
        if (answer == null || answer.toString().trim().isEmpty())
            throw new BusinessException(400, "请填写完形填空");
    }

    @Override
    public BigDecimal scoreAnswer(QuestionBank q, Object answer) {
        if (q.getCorrectAnswer() == null || answer == null) return BigDecimal.ZERO;
        // 答案格式: "词1,词2,词3" — 按顺序比较
        String[] corrects = q.getCorrectAnswer().split(",");
        String[] students = answer.toString().split(",");
        if (corrects.length == 0) return BigDecimal.ZERO;
        long matched = 0;
        for (int i = 0; i < Math.min(corrects.length, students.length); i++) {
            if (corrects[i].trim().equalsIgnoreCase(students[i].trim())) matched++;
        }
        return BigDecimal.valueOf(matched).divide(BigDecimal.valueOf(corrects.length), 2, RoundingMode.HALF_UP);
    }

    private int extractBlankCount(QuestionBank q) {
        if (q.getCorrectAnswer() == null) return 1;
        return q.getCorrectAnswer().split(",").length;
    }
}
