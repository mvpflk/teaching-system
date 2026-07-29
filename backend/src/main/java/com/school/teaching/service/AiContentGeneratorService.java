package com.school.teaching.service;

import com.school.teaching.entity.AiOutput;

import java.util.List;
import java.util.Map;

public interface AiContentGeneratorService {

    String submitGeneration(Long teacherId, Map<String, Object> params);

    AiTaskStore.TaskEntry getResult(String taskId);

    Map<String, Object> generateSync(Long teacherId, Map<String, Object> params);

    void executeAsync(String taskId, Long teacherId, Map<String, Object> params);

    List<AiOutput> listOutputs(Long teacherId, String outputType, String keyword, Integer page, Integer pageSize);

    AiOutput getById(Long id);

    void updateOutput(Long id, AiOutput update);

    void publish(Long id);

    void archive(Long id);

    void rate(Long id, Integer rating, String feedback);

    /** 将AI实训方案发布为实训任务 */
    Map<String, Object> publishAsTask(Long id, Map<String, Object> config);

    /** 将AI生成的题目一键组卷发布为考试任务 */
    Map<String, Object> publishQuestionsAsExam(Long teacherId, List<Long> questionIds, Map<String, Object> config);

    String exportMarkdown(Long id);

    String buildRagContext(Long nodeId);

    /** AI 生成实训方案步骤+评分标准（默认返回 null，表示尚未实现） */
    default String generatePracticePlan(Map<String, Object> params) {
        return null;
    }

    /** 获取诊断结果 — 查 ai_outputs 表最新 DIAGNOSIS 产出（原 Controller 直接注入 AiOutputMapper） */
    java.util.Map<String, Object> getDiagnosisResult(Long taskId);

    /** 列出题目批次 — 从 question_bank 查教师自建的题目并按 batchId 分组（原 Controller list() 的 QUESTION_TYPES 分支） */
    java.util.List<java.util.Map<String, Object>> listQuestionBatches(Long teacherId, String outputType,
                                                                      String keyword, int page, int pageSize);
}
