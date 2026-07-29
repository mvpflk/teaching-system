package com.school.teaching.service;
import com.school.teaching.dto.RubricDetailDTO;
import com.school.teaching.dto.RubricScoreDTO;

public interface RubricScoringService {
    RubricDetailDTO getTaskRubric(Long taskId);
    RubricScoreDTO saveRubricScores(Long submissionId, RubricScoreDTO scores);
}
