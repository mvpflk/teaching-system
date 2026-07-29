package com.school.teaching.service;

import java.math.BigDecimal;
import java.util.Map;

public interface TaskGradingService {

    int autoGradeObjective(Long submissionId);

    void manualGradeSubjective(Long submissionId, Map<Long, BigDecimal> questionScores, Long teacherId);

    /** 将作答错误的题目同步到错题本 */
    void syncWrongQuestions(Long submissionId);

    BigDecimal calculateTotalScore(Long submissionId);

    boolean isPassed(Long submissionId);

    /** 逐题评分 → 更新autoScore + 错题本 + 重算总分 → 返回总分 */
    BigDecimal gradeItems(Long submissionId, Map<Long, BigDecimal> scores, Long teacherId);
}
