package com.school.teaching.common.practice;

import com.school.teaching.entity.PracticeRubric;
import com.school.teaching.entity.PracticeStepGrade;
import com.school.teaching.entity.PracticeSubmission;

import java.util.List;
import java.util.Map;

public interface PracticeScoringModel {
    ScoringModelType getType();
    Map<String, Object> compute(List<PracticeStepGrade> stepGrades, List<PracticeRubric> rubrics,
        PracticeSubmission submission);
}
