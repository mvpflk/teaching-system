package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.entity.StudentAnswer;
import com.school.teaching.entity.TaskQuestion;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.mapper.KnowledgeNodeMapper;
import com.school.teaching.mapper.StudentAnswerMapper;
import com.school.teaching.mapper.TaskQuestionMapper;
import com.school.teaching.mapper.QuestionBankMapper;
import com.school.teaching.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsolidationMaterialService {

    private final AiContentGeneratorService aiContentService;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final StudentAnswerMapper studentAnswerMapper;
    private final TaskQuestionMapper taskQuestionMapper;
    private final QuestionBankMapper questionBankMapper;

    public String generateMaterial(Long teacherId, Long taskId, List<Long> knowledgeNodeIds,
            String subject, String commonMistakes, String ragContext) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("contentType", "CONSOLIDATION_MATERIAL");
        params.put("taskId", taskId);
        params.put("subject", subject != null ? subject : "通用");
        params.put("knowledgePoint", "巩固材料");
        params.put("stageHint", "中职");

        // 从数据库查询真实知识点名称
        List<String> kpNames = new ArrayList<>();
        List<Double> errorRates = new ArrayList<>();

        if (knowledgeNodeIds != null && !knowledgeNodeIds.isEmpty()) {
            List<KnowledgeNode> nodes = knowledgeNodeMapper.selectBatchIds(knowledgeNodeIds);
            Map<Long, String> nameMap = new LinkedHashMap<>();
            if (nodes != null) {
                for (KnowledgeNode n : nodes) {
                    nameMap.put(n.getId(), n.getName() != null ? n.getName() : "知识点" + n.getId());
                }
            }

            // 构建 questionId → knowledgeNodeId 映射（通过 task_questions → question_bank.categoryId）
            Map<Long, Long> qIdToKpId = new HashMap<>();
            if (taskId != null) {
                try {
                    List<TaskQuestion> tqList = taskQuestionMapper.selectList(
                        new LambdaQueryWrapper<TaskQuestion>().eq(TaskQuestion::getTaskId, taskId));
                    if (tqList != null && !tqList.isEmpty()) {
                        List<Long> qbIds = tqList.stream().map(TaskQuestion::getQuestionId)
                            .filter(Objects::nonNull).distinct().toList();
                        if (!qbIds.isEmpty()) {
                            List<QuestionBank> qbList = questionBankMapper.selectBatchIds(qbIds);
                            if (qbList != null) {
                                Map<Long, Long> qbToKp = qbList.stream()
                                    .filter(q -> q.getCategoryId() != null)
                                    .collect(Collectors.toMap(QuestionBank::getId, QuestionBank::getCategoryId, (a, b) -> a));
                                for (TaskQuestion tq : tqList) {
                                    Long kpId = qbToKp.get(tq.getQuestionId());
                                    if (kpId != null) qIdToKpId.put(tq.getQuestionId(), kpId);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("构建题目-知识点映射失败: {}", e.getMessage());
                }
            }

            // 从 student_answers 计算每个知识点的实际错误率
            for (Long kpId : knowledgeNodeIds) {
                String name = nameMap.getOrDefault(kpId, "知识点" + kpId);
                kpNames.add(name);

                double errorRate = 50.0; // 默认值
                if (taskId != null && !qIdToKpId.isEmpty()) {
                    try {
                        // 找到属于该知识点的所有 questionId
                        Set<Long> kpQuestionIds = qIdToKpId.entrySet().stream()
                            .filter(e -> kpId.equals(e.getValue()))
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toSet());
                        if (!kpQuestionIds.isEmpty()) {
                            List<StudentAnswer> answers = studentAnswerMapper.selectList(
                                new LambdaQueryWrapper<StudentAnswer>()
                                    .eq(StudentAnswer::getTaskId, taskId)
                                    .in(StudentAnswer::getQuestionId, kpQuestionIds));
                            if (answers != null && !answers.isEmpty()) {
                                long wrongCount = answers.stream()
                                    .filter(a -> a.getIsCorrect() == null || a.getIsCorrect() == 0)
                                    .count();
                                errorRate = Math.round((double) wrongCount / answers.size() * 1000.0) / 10.0;
                            }
                        }
                    } catch (Exception e) {
                        log.warn("查询知识点 {} 答题记录失败: {}", kpId, e.getMessage());
                    }
                }
                errorRates.add(errorRate);
            }
        }

        // 如果前端未传 commonMistakes，尝试从错误答案中聚合
        if ((commonMistakes == null || commonMistakes.isEmpty()) && taskId != null && knowledgeNodeIds != null) {
            try {
                // 通过 task_questions 获取所有题目ID
                List<TaskQuestion> tqList = taskQuestionMapper.selectList(
                    new LambdaQueryWrapper<TaskQuestion>().eq(TaskQuestion::getTaskId, taskId));
                if (tqList != null && !tqList.isEmpty()) {
                    Set<Long> qIds = tqList.stream().map(TaskQuestion::getQuestionId)
                        .filter(Objects::nonNull).collect(Collectors.toSet());
                    List<StudentAnswer> wrongAnswers = studentAnswerMapper.selectList(
                        new LambdaQueryWrapper<StudentAnswer>()
                            .eq(StudentAnswer::getTaskId, taskId)
                            .eq(StudentAnswer::getIsCorrect, 0)
                            .in(StudentAnswer::getQuestionId, qIds));
                    if (wrongAnswers != null && !wrongAnswers.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        // 最多取前20个典型错误，避免 Prompt 过长
                        int count = 0;
                        for (StudentAnswer a : wrongAnswers) {
                            if (count++ >= 20) break;
                            String ans = a.getStudentAnswer();
                            if (ans != null && !ans.isBlank()) {
                                String shortAns = ans.length() > 60 ? ans.substring(0, 60) + "..." : ans;
                                sb.append(shortAns).append("；");
                            }
                        }
                        if (sb.length() > 0) commonMistakes = sb.toString();
                    }
                }
            } catch (Exception e) {
                log.warn("聚合常见错误失败: {}", e.getMessage());
            }
        }

        // 构建 RAG 上下文（如果前端未提供）
        if ((ragContext == null || ragContext.isEmpty()) && knowledgeNodeIds != null && !knowledgeNodeIds.isEmpty()) {
            try {
                StringBuilder rag = new StringBuilder();
                List<KnowledgeNode> nodes = knowledgeNodeMapper.selectBatchIds(knowledgeNodeIds);
                if (nodes != null) {
                    for (KnowledgeNode n : nodes) {
                        if (n.getContent() != null && !n.getContent().isEmpty()) {
                            rag.append("【").append(n.getName()).append("】\n")
                                .append(n.getContent().length() > 2000
                                    ? n.getContent().substring(0, 2000) + "..."
                                    : n.getContent())
                                .append("\n\n");
                        }
                    }
                }
                if (rag.length() > 0) ragContext = rag.toString();
            } catch (Exception e) {
                log.warn("构建 RAG 上下文失败: {}", e.getMessage());
            }
        }

        TeachingContentPromptBuilder.PromptResult pr =
            TeachingContentPromptBuilder.buildConsolidationPrompt(
                subject, kpNames, errorRates, commonMistakes, ragContext);
        params.putAll(pr.extraParams());

        return aiContentService.submitGeneration(teacherId, params);
    }
}
