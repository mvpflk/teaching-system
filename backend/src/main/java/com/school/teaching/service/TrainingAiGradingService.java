package com.school.teaching.service;

import java.util.Map;

/**
 * 实训 AI 评分服务 — 对非检查点类型步骤（text/file/sim/web）进行 AI 辅助评分
 */
public interface TrainingAiGradingService {

    /**
     * 对单个步骤进行 AI 评分
     * @param stepType     步骤类型（text/file/sim/web）
     * @param stepDesc     步骤要求描述
     * @param maxScore     该步骤满分
     * @param studentData  学生提交的数据
     * @return {score, reason, confidence} 或 null（AI不可用时返回null，回退手动评分）
     */
    Map<String, Object> gradeStep(String stepType, String stepDesc, int maxScore, Map<String, Object> studentData);
}
