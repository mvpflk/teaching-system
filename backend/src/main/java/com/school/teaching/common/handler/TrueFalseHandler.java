package com.school.teaching.common.handler;

import com.school.teaching.common.QuestionTypeEnum;
import com.school.teaching.common.QuestionTypeHandler;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class TrueFalseHandler implements QuestionTypeHandler {

    private static final Set<String> VALID = Set.of("T", "F", "TRUE", "FALSE", "正确", "错误", "对", "错");

    @Override public QuestionTypeEnum getType() { return QuestionTypeEnum.TRUE_FALSE; }

    @Override
    public Map<String, Object> renderQuestion(QuestionBank q) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("type", "truefalse");
        r.put("text", q.getQuestionText());
        return r;
    }

    @Override
    public void validateAnswer(QuestionBank q, Object answer) {
        if (answer == null || !VALID.contains(answer.toString().trim().toUpperCase()))
            throw new BusinessException(400, "答案须为正确/错误");
    }

    @Override
    public BigDecimal scoreAnswer(QuestionBank q, Object answer) {
        if (q.getCorrectAnswer() == null || answer == null) return BigDecimal.ZERO;
        String correct = normalize(q.getCorrectAnswer());
        String student = normalize(answer.toString());
        return correct.equals(student) ? BigDecimal.ONE : BigDecimal.ZERO;
    }

    private String normalize(String s) {
        s = s.trim().toUpperCase();
        if (s.equals("正确") || s.equals("对") || s.equals("TRUE") || s.equals("A")) return "T";
        if (s.equals("错误") || s.equals("错") || s.equals("FALSE") || s.equals("B")) return "F";
        return s;
    }
}
