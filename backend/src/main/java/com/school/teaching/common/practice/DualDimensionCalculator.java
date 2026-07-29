package com.school.teaching.common.practice;

import com.school.teaching.entity.PracticeRubric;
import com.school.teaching.entity.PracticeStepGrade;
import com.school.teaching.entity.PracticeSubmission;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class DualDimensionCalculator implements PracticeScoringModel {

    @Override
    public ScoringModelType getType() {
        return ScoringModelType.DUAL_DIMENSION;
    }

    @Override
    public Map<String, Object> compute(List<PracticeStepGrade> stepGrades, List<PracticeRubric> rubrics,
        PracticeSubmission submission) {
        if (rubrics == null || rubrics.isEmpty()) {
            double avg = stepGrades.stream()
                .filter(g -> g.getStepScore() != null)
                .mapToDouble(g -> g.getStepScore().doubleValue())
                .average().orElse(0);
            double overall = avg * 10;
            return buildResult(overall);
        }

        List<PracticeRubric> processDims = rubrics.stream()
            .filter(r -> r.getDimension() != null && r.getDimension().startsWith("process_"))
            .toList();
        List<PracticeRubric> productDims = rubrics.stream()
            .filter(r -> r.getDimension() != null && r.getDimension().startsWith("product_"))
            .toList();

        double processScore = weightedScore(processDims, stepGrades);
        double productScore = weightedScore(productDims, stepGrades);
        double overall = (processScore * 0.5 + productScore * 0.5) * 20;
        overall = Math.min(100, Math.max(0, overall));

        Map<String, Object> result = buildResult(overall);
        result.put("processScore", Math.round(processScore * 10) / 10.0);
        result.put("productScore", Math.round(productScore * 10) / 10.0);
        return result;
    }

    private double weightedScore(List<PracticeRubric> dims, List<PracticeStepGrade> stepGrades) {
        if (dims.isEmpty()) return 0;
        double totalWeight = dims.stream()
            .filter(r -> r.getWeight() != null)
            .mapToDouble(r -> r.getWeight().doubleValue()).sum();
        if (totalWeight == 0) return 0;

        double sum = 0;
        for (PracticeRubric r : dims) {
            double w = r.getWeight() != null ? r.getWeight().doubleValue() : 0;
            double score = stepGrades.stream()
                .filter(g -> g.getStepScore() != null)
                .mapToDouble(g -> g.getStepScore().doubleValue())
                .average().orElse(0);
            sum += w * score;
        }
        return sum / totalWeight;
    }

    private Map<String, Object> buildResult(double overall) {
        String level;
        if (overall >= 90) level = "优秀";
        else if (overall >= 75) level = "良好";
        else if (overall >= 60) level = "合格";
        else level = "不合格";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overallScore", Math.round(overall * 10) / 10.0);
        result.put("gradeLevel", level);
        return result;
    }
}
