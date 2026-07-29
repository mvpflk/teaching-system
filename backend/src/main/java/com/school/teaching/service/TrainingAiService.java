package com.school.teaching.service;

import java.util.List;
import java.util.Map;

public interface TrainingAiService {
    /** 从考纲知识节点 AI 生成实训步骤 */
    List<Map<String, Object>> generateSteps(Long userId, String subject, List<Long> nodeIds, int stepCount);

    /** 从上传文本 AI 解析实训步骤 */
    List<Map<String, Object>> importFromText(Long userId, String text);
}
