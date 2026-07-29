package com.school.teaching.common.score;

import com.school.teaching.common.ScoreCalculator;
import com.school.teaching.common.ScoreType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Grade5Calculator implements ScoreCalculator {

    @Override
    public ScoreType getType() { return ScoreType.GRADE_5; }

    @Override
    public boolean isPassed(Map<String, Object> submission, Map<String, Object> config) {
        String grade = toGrade(submission.get("gradeLevel"));
        return !"F".equals(grade) && !"E".equals(grade) && !grade.isEmpty();
    }

    @Override
    public int toCreditValue(Map<String, Object> submission) {
        return switch (toGrade(submission.get("gradeLevel"))) {
            case "A" -> 10;
            case "B" -> 7;
            case "C" -> 5;
            case "D" -> 2;
            default -> 0;
        };
    }

    private String toGrade(Object v) {
        if (v == null) return "";
        String s = v.toString().trim().toUpperCase();
        if (s.length() > 1) s = s.substring(0, 1);
        return s;
    }
}
