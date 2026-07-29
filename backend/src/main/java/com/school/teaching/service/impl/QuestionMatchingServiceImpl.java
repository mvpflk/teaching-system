package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.service.QuestionMatchingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG 风格题库匹配引擎 — 五级 fallback
 *
 * L1: 精准匹配 — categoryId = nodeId, status=1
 * L2: 兄弟节点 — 同 parentId 的其他知识点
 * L3: 考纲扩散 — exam_syllabus_node_relation 同考纲节点
 * L4: 文本标签 — knowledgePoints JSON 含节点名称
 * L5: AI草稿 — status=0（待审核但可做练习素材）
 */
@Slf4j
@Service
public class QuestionMatchingServiceImpl implements QuestionMatchingService {

    @Autowired private QuestionBankMapper questionMapper;
    @Autowired private KnowledgeNodeMapper nodeMapper;
    @Autowired private ExamSyllabusNodeRelationMapper syllabusRelationMapper;

    @Override
    public Map<String, Object> matchQuestions(Set<Long> weakNodeIds, String subject,
                                               int targetCount, Set<Long> usedIds) {
        List<QuestionBank> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>(usedIds);
        Map<String, Integer> matchDetail = new LinkedHashMap<>();

        // L1: 精准匹配 — 直接按 categoryId
        List<QuestionBank> l1 = queryByCategoryIds(new HashSet<>(weakNodeIds), 1, targetCount * 2, seen);
        addUnique(result, l1, seen);
        matchDetail.put("L1_precise", l1.size());
        if (result.size() >= targetCount) return buildResult(result, matchDetail);

        // L2: 兄弟节点扩散
        Set<Long> siblingIds = expandToSiblings(weakNodeIds);
        if (!siblingIds.isEmpty()) {
            List<QuestionBank> l2 = queryByCategoryIds(siblingIds, 1, targetCount * 2, seen);
            addUnique(result, l2, seen);
            matchDetail.put("L2_sibling", l2.size());
            if (result.size() >= targetCount) return buildResult(result, matchDetail);
        }

        // L3: 考纲扩散
        Set<Long> syllabusIds = expandToSyllabus(weakNodeIds);
        if (!syllabusIds.isEmpty()) {
            List<QuestionBank> l3 = queryByCategoryIds(syllabusIds, 1, targetCount * 2, seen);
            addUnique(result, l3, seen);
            matchDetail.put("L3_syllabus", l3.size());
            if (result.size() >= targetCount) return buildResult(result, matchDetail);
        }

        // L4: 知识节点名称文本匹配
        Set<String> nodeNames = resolveNodeNames(weakNodeIds);
        if (!nodeNames.isEmpty()) {
            List<QuestionBank> l4 = queryByKnowledgePointName(nodeNames, 1, targetCount, seen);
            addUnique(result, l4, seen);
            matchDetail.put("L4_textTag", l4.size());
            if (result.size() >= targetCount) return buildResult(result, matchDetail);
        }

        // L5: AI 草稿池（status=0）
        Set<Long> expandedIds = new HashSet<>(weakNodeIds);
        expandedIds.addAll(siblingIds);
        expandedIds.addAll(syllabusIds);
        List<QuestionBank> l5 = queryByCategoryIds(expandedIds, 0, targetCount / 2, seen);
        addUnique(result, l5, seen);
        matchDetail.put("L5_aiDraft", l5.size());

        // 按难度排序（先易后难）
        result.sort(Comparator.comparingInt(q -> q.getDifficultyLevel() != null ? q.getDifficultyLevel() : 2));

        log.info("题库匹配完成: L1={} L2={} L3={} L4={} L5={} total={} target={}",
            matchDetail.getOrDefault("L1_precise", 0),
            matchDetail.getOrDefault("L2_sibling", 0),
            matchDetail.getOrDefault("L3_syllabus", 0),
            matchDetail.getOrDefault("L4_textTag", 0),
            matchDetail.getOrDefault("L5_aiDraft", 0),
            result.size(), targetCount);

        return buildResult(result, matchDetail);
    }

    @Override
    public List<QuestionBank> matchSingleNode(Long nodeId, String subject, int limit) {
        Set<Long> nodeIds = new HashSet<>(Set.of(nodeId));
        Set<Long> seen = new HashSet<>();
        Map<String, Object> r = matchQuestions(nodeIds, subject, limit, seen);
        @SuppressWarnings("unchecked")
        List<QuestionBank> qs = (List<QuestionBank>) r.get("questions");
        return qs != null ? qs : List.of();
    }

    // ═══════════ 内部方法 ═══════════

    private List<QuestionBank> queryByCategoryIds(Set<Long> ids, int status, int limit, Set<Long> excludeIds) {
        if (ids.isEmpty()) return List.of();
        List<QuestionBank> raw = questionMapper.selectList(
            new LambdaQueryWrapper<QuestionBank>()
                .in(QuestionBank::getCategoryId, ids)
                .eq(QuestionBank::getStatus, status)
                .orderByAsc(QuestionBank::getDifficultyLevel)
                .last("LIMIT " + Math.min(limit, 200)));
        return raw.stream().filter(q -> !excludeIds.contains(q.getId())).collect(Collectors.toList());
    }

    private List<QuestionBank> queryByKnowledgePointName(Set<String> names, int status, int limit, Set<Long> excludeIds) {
        if (names.isEmpty()) return List.of();
        List<QuestionBank> result = new ArrayList<>();
        // 对每个名称做 LIKE 搜索
        for (String name : names) {
            if (result.size() >= limit * 2) break;
            List<QuestionBank> raw = questionMapper.selectList(
                new LambdaQueryWrapper<QuestionBank>()
                    .like(QuestionBank::getKnowledgePoints, name)
                    .eq(QuestionBank::getStatus, status)
                    .orderByAsc(QuestionBank::getDifficultyLevel)
                    .last("LIMIT " + (limit / Math.max(1, names.size()) + 5)));
            for (QuestionBank q : raw) {
                if (!excludeIds.contains(q.getId())) {
                    result.add(q);
                    excludeIds.add(q.getId());
                }
            }
        }
        return result;
    }

    private Set<Long> expandToSiblings(Set<Long> nodeIds) {
        Set<Long> result = new HashSet<>();
        if (nodeIds.isEmpty()) return result;
        // 批量查询所有节点的父节点
        List<KnowledgeNode> nodes = nodeMapper.selectBatchIds(nodeIds);
        if (nodes == null) return result;
        Set<Long> parentIds = new HashSet<>();
        for (KnowledgeNode kn : nodes) {
            if (kn.getParentId() != null) parentIds.add(kn.getParentId());
        }
        if (parentIds.isEmpty()) return result;
        // 批量查询所有兄弟节点
        List<KnowledgeNode> siblings = nodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>()
                .in(KnowledgeNode::getParentId, parentIds));
        if (siblings != null) {
            for (KnowledgeNode sib : siblings) {
                if (!nodeIds.contains(sib.getId())) result.add(sib.getId());
            }
        }
        return result;
    }

    private Set<Long> expandToSyllabus(Set<Long> nodeIds) {
        Set<Long> result = new HashSet<>();
        for (Long nid : nodeIds) {
            List<ExamSyllabusNodeRelation> rels = syllabusRelationMapper.selectList(
                new LambdaQueryWrapper<ExamSyllabusNodeRelation>()
                    .eq(ExamSyllabusNodeRelation::getNodeId, nid));
            for (ExamSyllabusNodeRelation rel : rels) {
                if (rel.getSyllabusId() == null) continue;
                // 查同考纲的所有知识点
                List<ExamSyllabusNodeRelation> sameSyllabus = syllabusRelationMapper.selectList(
                    new LambdaQueryWrapper<ExamSyllabusNodeRelation>()
                        .eq(ExamSyllabusNodeRelation::getSyllabusId, rel.getSyllabusId()));
                for (ExamSyllabusNodeRelation sr : sameSyllabus) {
                    if (!nodeIds.contains(sr.getNodeId())) result.add(sr.getNodeId());
                }
            }
        }
        return result;
    }

    private Set<String> resolveNodeNames(Set<Long> nodeIds) {
        Set<String> names = new LinkedHashSet<>();
        for (Long nid : nodeIds) {
            KnowledgeNode kn = nodeMapper.selectById(nid);
            if (kn != null && kn.getName() != null) names.add(kn.getName());
        }
        return names;
    }

    private void addUnique(List<QuestionBank> dest, List<QuestionBank> src, Set<Long> seen) {
        for (QuestionBank q : src) {
            if (!seen.contains(q.getId())) {
                dest.add(q);
                seen.add(q.getId());
            }
        }
    }

    private Map<String, Object> buildResult(List<QuestionBank> questions, Map<String, Integer> detail) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("questions", questions);
        result.put("matchDetail", detail);
        return result;
    }
}
