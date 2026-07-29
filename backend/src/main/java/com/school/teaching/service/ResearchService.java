package com.school.teaching.service;

import java.time.LocalDateTime;
import java.util.Map;

public interface ResearchService {
    /** 拍摄学生基线快照 */
    Map<String, Object> captureBaseline(String snapshotLabel);

    /** 导出基线快照为CSV（researchGroup可选过滤） */
    byte[] exportBaselineCsv(String snapshotLabel, String researchGroup);

    /** 获取最新快照摘要（researchGroup可选过滤） */
    Map<String, Object> getBaselineSummary(String snapshotLabel, String researchGroup);

    /**
     * masteryPercent效度验证 — 计算标准化考试分数与知识点掌握度的Pearson相关系数
     * @param taskId 标准化考试任务ID
     * @param subject 学科过滤(可选, null=不限)
     * @return {r, rSquared, n, scatter, thresholdMet(r>0.4), interpretation}
     */
    Map<String, Object> validateMastery(Long taskId, String subject);
}
