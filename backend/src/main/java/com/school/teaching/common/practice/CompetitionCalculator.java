package com.school.teaching.common.practice;

import com.school.teaching.entity.PracticeRubric;
import com.school.teaching.entity.PracticeStepGrade;
import com.school.teaching.entity.PracticeSubmission;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CompetitionCalculator implements PracticeScoringModel {

    @Override
    public ScoringModelType getType() {
        return ScoringModelType.COMPETITION;
    }

    @Override
    public Map<String, Object> compute(List<PracticeStepGrade> stepGrades, List<PracticeRubric> rubrics,
        PracticeSubmission submission) {
        if (rubrics == null || rubrics.isEmpty()) {
            double avg = stepGrades.stream()
                .filter(g -> g.getStepScore() != null)
                .mapToDouble(g -> Math.min(10, g.getStepScore().doubleValue() / 2))
                .average().orElse(0);
            return buildResult(avg * 10, 0, 0, 0, 0, 0);
        }

        double skillScore = avgObs("skill_", rubrics, stepGrades);
        double profScore = avgObs("prof_", rubrics, stepGrades);
        double valueScore = avgObs("value_", rubrics, stepGrades);
        double innovScore = avgObs("innov_", rubrics, stepGrades);
        double teamScore = avgObs("team_", rubrics, stepGrades);

        boolean hasTeam = rubrics.stream().anyMatch(
            r -> r.getDimension() != null && r.getDimension().startsWith("team_"));
        double skillWeight = hasTeam ? 0.60 : 0.70;
        double teamWeight = hasTeam ? 0.10 : 0;
        if (!hasTeam) teamScore = 0;

        double overall = (skillScore * skillWeight + profScore * 0.10
            + valueScore * 0.10 + innovScore * 0.10 + teamScore * teamWeight) * 10;
        overall = Math.min(100, Math.max(0, overall));

        return buildResult(overall, skillScore, profScore, valueScore, innovScore, teamScore);
    }

    private double avgObs(String prefix, List<PracticeRubric> rubrics, List<PracticeStepGrade> grades) {
        if (rubrics == null || rubrics.isEmpty()) {
            return grades.stream()
                .filter(g -> g.getStepScore() != null)
                .mapToDouble(g -> Math.min(10, g.getStepScore().doubleValue() / 2))
                .average().orElse(0);
        }
        List<PracticeRubric> dims = rubrics.stream()
            .filter(r -> r.getDimension() != null && r.getDimension().startsWith(prefix))
            .toList();
        if (dims.isEmpty()) return 0;
        return grades.stream()
            .filter(g -> g.getStepScore() != null)
            .mapToDouble(g -> Math.min(10, g.getStepScore().doubleValue() / 2))
            .average().orElse(0);
    }

    private Map<String, Object> buildResult(double overall, double skill, double prof,
        double value, double innov, double team) {
        String level = overall >= 90 ? "优秀" : overall >= 75 ? "良好" : overall >= 60 ? "合格" : "不合格";
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overallScore", Math.round(overall * 10) / 10.0);
        result.put("gradeLevel", level);
        result.put("skillScore", Math.round(skill * 10) / 10.0);
        result.put("profScore", Math.round(prof * 10) / 10.0);
        result.put("valueScore", Math.round(value * 10) / 10.0);
        result.put("innovScore", Math.round(innov * 10) / 10.0);
        result.put("teamScore", Math.round(team * 10) / 10.0);
        return result;
    }
}
