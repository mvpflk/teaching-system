package com.school.teaching.service;

import com.school.teaching.entity.QuestionBank;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 题库匹配服务 — RAG 风格三级匹配引擎
 * 错题 → 知识点 → 考纲 → 题库精准匹配，仅在库不足时走 AI
 */
public interface QuestionMatchingService {

    /**
     * 多节点衍生练习匹配（错题本场景）
     * @param weakNodeIds 薄弱知识点ID集合
     * @param subject 学科（可选）
     * @param targetCount 目标题数
     * @param usedIds 已做过/已排除的题目ID
     * @return 匹配结果：questions(题目列表) + matchDetail(各级匹配数日志)
     */
    Map<String, Object> matchQuestions(Set<Long> weakNodeIds, String subject, int targetCount, Set<Long> usedIds);

    /**
     * 单节点精准匹配（考点地图场景）
     * @param nodeId 知识点ID
     * @param subject 学科
     * @param limit 最大返回数
     * @return 匹配的题目列表
     */
    List<QuestionBank> matchSingleNode(Long nodeId, String subject, int limit);
}
