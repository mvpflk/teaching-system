package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.entity.WrongQuestion;
import com.school.teaching.mapper.KnowledgeNodeMapper;
import com.school.teaching.mapper.QuestionBankMapper;
import com.school.teaching.mapper.WrongQuestionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 题库混搭服务 — 在 AI 出题时从现有题库中抽取高质量题目混入结果。
 *
 * <p>策略：按 mixRatio 比例从 question_bank 抽取同知识点下高质量题，
 * 随机替换 AI 生成的题目，使最终题目组合兼具 AI 灵活性与题库稳定性。
 *
 * <p>高质量标准：选项完整 + 答案非空 + 题干长度 20-200 字 + status=1。
 *
 * @since V055
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionBankMixer {

    private final QuestionBankMapper questionBankMapper;
    private final KnowledgeNodeMapper nodeMapper;
    private final WrongQuestionMapper wrongQuestionMapper;

    /** 题库混搭比例（0.0~1.0），默认 0.3 即 30% 来自题库 */
    @Value("${ai.question.mix-ratio:0.3}")
    private double mixRatio;

    /** 单次混搭最多从题库抽取的数量上限 */
    @Value("${ai.question.mix-max-bank:15}")
    private int maxBankQuestions;

    /**
     * 将 AI 生成的题目与题库高质量题混搭。
     *
     * @param nodeId      主知识节点 ID（可 null）
     * @param categoryIds 多节点 ID 列表（可 null）
     * @param aiQuestions AI 生成的题目列表
     * @param studentId   学生 ID（用于去重，可 null 则不去重）
     * @return 混搭后的题目列表
     */
    public List<Map<String, Object>> mix(Long nodeId, List<Long> categoryIds,
                                          List<Map<String, Object>> aiQuestions,
                                          Long studentId) {
        if (aiQuestions == null || aiQuestions.isEmpty()) {
            return aiQuestions;
        }

        // 收集目标节点 ID
        Set<Long> targetNodeIds = new LinkedHashSet<>();
        if (nodeId != null && nodeId > 0) {
            targetNodeIds.add(nodeId);
            // 递归收集该节点的所有 L4 子节点
            collectL4Descendants(nodeId, targetNodeIds, new HashSet<>());
        }
        if (categoryIds != null) {
            for (Long cid : categoryIds) {
                if (cid != null && cid > 0) {
                    targetNodeIds.add(cid);
                    collectL4Descendants(cid, targetNodeIds, new HashSet<>());
                }
            }
        }

        if (targetNodeIds.isEmpty()) {
            log.debug("混搭跳过: 无有效节点ID, nodeId={}", nodeId);
            return aiQuestions;
        }

        // 计算需要从题库抽取的数量
        int totalQuestions = aiQuestions.size();
        int bankCount = (int) Math.min(totalQuestions * mixRatio, maxBankQuestions);
        if (bankCount < 1) {
            bankCount = 1; // 至少尝试抽 1 道
        }

        // 查询题库
        List<QuestionBank> bankPool = queryQualityQuestions(targetNodeIds, studentId, bankCount * 3);
        if (bankPool.isEmpty()) {
            log.debug("混搭跳过: 目标节点无可用题库题, nodeIds={}", targetNodeIds);
            return aiQuestions;
        }

        // 去重：排除已在 AI 结果中的相同题干
        Set<String> aiStems = aiQuestions.stream()
            .map(q -> normalizeStem((String) q.getOrDefault("questionText", "")))
            .collect(Collectors.toSet());
        List<QuestionBank> filtered = bankPool.stream()
            .filter(q -> !aiStems.contains(normalizeStem(q.getQuestionText())))
            .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            log.debug("混搭跳过: 题库题与AI题全重复");
            return aiQuestions;
        }

        // V055: 知识点均匀抽样 — 每个知识点至少1题，按比例分配剩余
        int actualBank = Math.min(bankCount, filtered.size());
        List<QuestionBank> picked = distributeAcrossKps(filtered, actualBank);
        log.info("题库混搭: AI {} 题 + 题库 {} 题 (pool={}, groups={}, nodeIds={})",
            aiQuestions.size() - picked.size() + picked.size(), picked.size(),
            bankPool.size(), groupByKp(filtered).size(), targetNodeIds.size());

        // V055-fix: 按题型匹配替换，保持用户设定的题型配比
        List<Map<String, Object>> result = new ArrayList<>(aiQuestions);

        // 1. 将 picked 按题型分组
        Map<String, List<QuestionBank>> pickedByType = new LinkedHashMap<>();
        for (QuestionBank q : picked) {
            pickedByType.computeIfAbsent(q.getQuestionType(), k -> new ArrayList<>()).add(q);
        }

        // 2. 按题型分组计算替换数并执行替换
        Set<Integer> usedIndices = new LinkedHashSet<>();
        int totalReplaced = 0;
        List<QuestionBank> overflowBank = new ArrayList<>(); // 题型不匹配时的溢出题库题

        for (Map.Entry<String, List<QuestionBank>> entry : pickedByType.entrySet()) {
            String type = entry.getKey();
            List<QuestionBank> bankOfType = entry.getValue();

            // 找该题型的 AI 题位置
            List<Integer> aiIndicesOfType = new ArrayList<>();
            for (int i = 0; i < result.size(); i++) {
                if (!usedIndices.contains(i) && type.equals(result.get(i).get("questionType"))) {
                    aiIndicesOfType.add(i);
                }
            }

            // 该题型替换数 = min(题库该题型数, AI该题型数 × mixRatio)
            int typeReplace = Math.min(bankOfType.size(),
                (int) Math.ceil(aiIndicesOfType.size() * mixRatio));
            typeReplace = Math.min(typeReplace, aiIndicesOfType.size());

            if (typeReplace > 0) {
                List<Integer> toReplace = randomSample(aiIndicesOfType, typeReplace);
                int bankIdx = 0;
                for (int idx : toReplace) {
                    if (bankIdx < bankOfType.size()) {
                        usedIndices.add(idx);
                        result.set(idx, convertToMap(bankOfType.get(bankIdx++)));
                        totalReplaced++;
                    }
                }
                // 剩余未使用的该题型题库题（超出AI同题型数量的部分）
                while (bankIdx < bankOfType.size()) {
                    overflowBank.add(bankOfType.get(bankIdx++));
                }
            } else {
                overflowBank.addAll(bankOfType);
            }
        }

        // 3. 溢出处理：题库有但AI中无对应题型的，替换剩余AI题
        if (!overflowBank.isEmpty()) {
            List<Integer> remainingAi = new ArrayList<>();
            for (int i = 0; i < result.size(); i++) {
                if (!usedIndices.contains(i)) remainingAi.add(i);
            }
            int overflowReplace = Math.min(overflowBank.size(), remainingAi.size());
            if (overflowReplace > 0) {
                List<Integer> overflowIndices = randomSample(remainingAi, overflowReplace);
                for (int j = 0; j < overflowIndices.size(); j++) {
                    usedIndices.add(overflowIndices.get(j));
                    result.set(overflowIndices.get(j), convertToMap(overflowBank.get(j)));
                    totalReplaced++;
                }
            }
        }

        log.info("题型混搭: 替换{}题, 各题型替换数={}", totalReplaced,
            pickedByType.entrySet().stream()
                .map(e -> e.getKey() + ":" + Math.min(e.getValue().size(),
                    (int) Math.ceil(result.stream()
                        .filter(q -> e.getKey().equals(q.get("questionType")))
                        .count() * mixRatio)))
                .collect(Collectors.joining(",")));

        // 打乱顺序，避免题库题集中在末尾
        Collections.shuffle(result, ThreadLocalRandom.current());

        return result;
    }

    /**
     * 从题库补充题目填补 AI 生成缺口。只做加法，不替换已有题目。
     *
     * @param nodeId      主知识节点 ID
     * @param categoryIds 多节点 ID 列表
     * @param existing    已有的题目列表
     * @param targetCount 目标总数
     * @param relaxed     是否放宽质量阈值（Level 2 降级）
     * @return 补充后的完整列表（existing + 新抽取的题库题）
     */
    public List<Map<String, Object>> supplement(Long nodeId, List<Long> categoryIds,
                                                  List<Map<String, Object>> existing,
                                                  int targetCount, boolean relaxed) {
        if (existing == null) existing = Collections.emptyList();
        int gap = targetCount - existing.size();
        if (gap <= 0) return new ArrayList<>(existing);

        // 收集目标节点 ID
        Set<Long> targetNodeIds = new LinkedHashSet<>();
        if (nodeId != null && nodeId > 0) {
            targetNodeIds.add(nodeId);
            collectL4Descendants(nodeId, targetNodeIds, new HashSet<>());
        }
        if (categoryIds != null) {
            for (Long cid : categoryIds) {
                if (cid != null && cid > 0) {
                    targetNodeIds.add(cid);
                    collectL4Descendants(cid, targetNodeIds, new HashSet<>());
                }
            }
        }
        if (targetNodeIds.isEmpty()) {
            log.info("补充模式跳过: 无有效节点ID");
            return new ArrayList<>(existing);
        }

        // 查询题库（多拉一些，给去重留余量）
        int fetchLimit = gap * 5;
        List<QuestionBank> bankPool = queryQualityQuestions(targetNodeIds, null, fetchLimit);
        if (relaxed && bankPool.size() < gap) {
            // Level 2: 放宽阈值再查
            bankPool = queryRelaxedQuestions(targetNodeIds, fetchLimit);
            log.info("补充模式(relaxed): 严格匹配{}题, 放宽后{}题",
                queryQualityQuestions(targetNodeIds, null, fetchLimit).size(), bankPool.size());
        }
        if (bankPool.isEmpty()) {
            log.info("补充模式: 题库无可用题, nodeIds={}", targetNodeIds.size());
            return new ArrayList<>(existing);
        }

        // 排除已在 existing 中的题目
        Set<String> existingStems = existing.stream()
            .map(q -> normalizeStem((String) q.getOrDefault("questionText", "")))
            .collect(Collectors.toSet());
        List<QuestionBank> filtered = bankPool.stream()
            .filter(q -> !existingStems.contains(normalizeStem(q.getQuestionText())))
            .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            log.info("补充模式: 题库题全与已有题重复");
            return new ArrayList<>(existing);
        }

        // 均匀分布抽取
        int actualGap = Math.min(gap, filtered.size());
        List<QuestionBank> picked = distributeAcrossKps(filtered, actualGap);

        // 转换为 Map 格式并合并
        List<Map<String, Object>> result = new ArrayList<>(existing);
        for (QuestionBank q : picked) {
            result.add(convertToMap(q));
        }
        log.info("题库补充: 已有{}题 + 补充{}题 = 共{}题 (缺口{}题, relaxed={})",
            existing.size(), picked.size(), result.size(), gap, relaxed);
        return result;
    }

    /**
     * 放宽质量阈值的题库查询（用于三级补齐的 Level 2）。
     * 允许题干 10-500 字，允许 status=0 的草稿题。
     */
    private List<QuestionBank> queryRelaxedQuestions(Set<Long> nodeIds, int limit) {
        LambdaQueryWrapper<QuestionBank> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(QuestionBank::getCategoryId, nodeIds)
            .ne(QuestionBank::getStatus, 2)  // 排除删除的
            .isNotNull(QuestionBank::getCorrectAnswer)
            .ne(QuestionBank::getCorrectAnswer, "");
        List<QuestionBank> all = questionBankMapper.selectList(wrapper);
        if (all.isEmpty()) return Collections.emptyList();

        List<QuestionBank> filtered = all.stream()
            .filter(q -> {
                String text = q.getQuestionText();
                return text != null && text.length() >= 10 && text.length() <= 500;
            })
            .collect(Collectors.toList());

        // 去重
        Set<String> seen = new HashSet<>();
        List<QuestionBank> deduped = new ArrayList<>();
        for (QuestionBank q : filtered) {
            if (seen.add(normalizeStem(q.getQuestionText()))) {
                deduped.add(q);
            }
        }
        if (deduped.size() > limit) {
            deduped = randomSample(deduped, limit);
        }
        return deduped;
    }

    // ── 内部方法 ──

    /**
     * 查询高质量题库题目。
     * 条件：status=1、选项非空、答案非空、题干长度合理。
     */
    private List<QuestionBank> queryQualityQuestions(Set<Long> nodeIds, Long studentId, int limit) {
        // 构建基本条件
        LambdaQueryWrapper<QuestionBank> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(QuestionBank::getCategoryId, nodeIds)
            .eq(QuestionBank::getStatus, 1)
            .isNotNull(QuestionBank::getOptions)
            .ne(QuestionBank::getOptions, "[]")
            .ne(QuestionBank::getOptions, "")
            .isNotNull(QuestionBank::getCorrectAnswer)
            .ne(QuestionBank::getCorrectAnswer, "");

        List<QuestionBank> all = questionBankMapper.selectList(wrapper);
        if (all.isEmpty()) {
            return Collections.emptyList();
        }

        // 过滤题干长度
        List<QuestionBank> filtered = all.stream()
            .filter(q -> {
                String text = q.getQuestionText();
                return text != null && text.length() >= 20 && text.length() <= 200;
            })
            .collect(Collectors.toList());

        // 去重（按题干归一化）
        Set<String> seen = new HashSet<>();
        List<QuestionBank> deduped = new ArrayList<>();
        for (QuestionBank q : filtered) {
            String norm = normalizeStem(q.getQuestionText());
            if (seen.add(norm)) {
                deduped.add(q);
            }
        }

        // 学生去重：排除已做对的题
        if (studentId != null && !deduped.isEmpty()) {
            deduped = excludeMasteredByStudent(deduped, studentId);
        }

        // 限制返回数量
        if (deduped.size() > limit) {
            deduped = randomSample(deduped, limit);
        }

        return deduped;
    }

    /** 题干归一化：去空格、去标点差异，用于去重比较 */
    private String normalizeStem(String text) {
        if (text == null) return "";
        return text.replaceAll("\\s+", "")
            .replaceAll("[，,。．.；;：:！!？?、（）()【】\\[\\]《》\"\"'']", "")
            .toLowerCase()
            .trim();
    }

    /** 排除学生已掌握的题目（wrong_questions 中 is_mastered=1） */
    private List<QuestionBank> excludeMasteredByStudent(List<QuestionBank> questions, Long studentId) {
        List<Long> qids = questions.stream().map(QuestionBank::getId).collect(Collectors.toList());
        try {
            LambdaQueryWrapper<WrongQuestion> wqWrapper = new LambdaQueryWrapper<>();
            wqWrapper.eq(WrongQuestion::getStudentId, studentId)
                .in(WrongQuestion::getQuestionId, qids)
                .eq(WrongQuestion::getIsMastered, 1);
            List<WrongQuestion> mastered = wrongQuestionMapper.selectList(wqWrapper);
            if (mastered.isEmpty()) return questions;
            Set<Long> excludedIds = mastered.stream()
                .map(WrongQuestion::getQuestionId).collect(Collectors.toSet());
            return questions.stream()
                .filter(q -> !excludedIds.contains(q.getId()))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("学生去重查询失败(studentId={}), 降级为不去重: {}", studentId, e.getMessage());
            return questions;
        }
    }

    /** 随机抽样（Fisher-Yates 部分洗牌） */
    private <T> List<T> randomSample(List<T> list, int n) {
        if (n >= list.size()) return new ArrayList<>(list);
        List<T> copy = new ArrayList<>(list);
        for (int i = 0; i < n; i++) {
            int j = i + ThreadLocalRandom.current().nextInt(copy.size() - i);
            T tmp = copy.get(i);
            copy.set(i, copy.get(j));
            copy.set(j, tmp);
        }
        return copy.subList(0, n);
    }

    /** 按知识点分组 */
    private Map<Long, List<QuestionBank>> groupByKp(List<QuestionBank> questions) {
        Map<Long, List<QuestionBank>> groups = new LinkedHashMap<>();
        for (QuestionBank q : questions) {
            Long kpId = q.getCategoryId();
            if (kpId != null) {
                groups.computeIfAbsent(kpId, k -> new ArrayList<>()).add(q);
            }
        }
        return groups;
    }

    /**
     * 知识点均匀抽样：每个知识点至少抽1题，剩余按比例分配。
     * 确保混搭的题库题覆盖所有子知识点，不会集中在某一两个知识点上。
     */
    private List<QuestionBank> distributeAcrossKps(List<QuestionBank> pool, int total) {
        Map<Long, List<QuestionBank>> groups = groupByKp(pool);
        int groupCount = groups.size();
        if (groupCount == 0) return Collections.emptyList();

        List<QuestionBank> result = new ArrayList<>();
        List<List<QuestionBank>> groupLists = new ArrayList<>(groups.values());

        // 每个知识点至少1题
        int perGroup = Math.max(1, total / groupCount);
        int assigned = 0;
        for (List<QuestionBank> g : groupLists) {
            int take = Math.min(perGroup, g.size());
            result.addAll(randomSample(g, take));
            assigned += take;
        }

        // 余量：从题目多的知识点中补足
        int remaining = total - assigned;
        if (remaining > 0) {
            // 按可用余量排序，优先从题目多的组补
            List<Map.Entry<Long, List<QuestionBank>>> sorted = new ArrayList<>(groups.entrySet());
            sorted.sort((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()));
            for (Map.Entry<Long, List<QuestionBank>> e : sorted) {
                if (remaining <= 0) break;
                // 该组已抽走的题目ID
                Set<Long> takenIds = result.stream()
                    .map(QuestionBank::getId).collect(Collectors.toSet());
                List<QuestionBank> available = e.getValue().stream()
                    .filter(q -> !takenIds.contains(q.getId()))
                    .collect(Collectors.toList());
                if (!available.isEmpty()) {
                    int take = Math.min(remaining, available.size());
                    result.addAll(randomSample(available, take));
                    remaining -= take;
                }
            }
        }

        // 若仍不足（极少数情况），从全池随机补
        if (result.size() < total) {
            Set<Long> takenIds = result.stream().map(QuestionBank::getId).collect(Collectors.toSet());
            List<QuestionBank> remaining_pool = pool.stream()
                .filter(q -> !takenIds.contains(q.getId()))
                .collect(Collectors.toList());
            if (!remaining_pool.isEmpty()) {
                result.addAll(randomSample(remaining_pool,
                    Math.min(total - result.size(), remaining_pool.size())));
            }
        }

        Collections.shuffle(result, ThreadLocalRandom.current());
        log.debug("均匀分布抽样: {}组知识点, 目标{}题, 实抽{}题, 覆盖率{}/{}",
            groupCount, total, result.size(),
            result.stream().map(QuestionBank::getCategoryId).distinct().count(), groupCount);
        return result;
    }

    /** 递归收集节点下的所有 L4 子节点 */
    private void collectL4Descendants(Long nodeId, Set<Long> result, Set<Long> visited) {
        if (!visited.add(nodeId)) return; // 防环
        KnowledgeNode node = nodeMapper.selectById(nodeId);
        if (node == null) return;
        if (node.getLevel() != null && node.getLevel() == 4) {
            result.add(nodeId);
            return; // L4 无子节点
        }
        // L2/L3: 递归收集子节点
        LambdaQueryWrapper<KnowledgeNode> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(KnowledgeNode::getParentId, nodeId);
        List<KnowledgeNode> children = nodeMapper.selectList(childWrapper);
        for (KnowledgeNode child : children) {
            collectL4Descendants(child.getId(), result, visited);
        }
    }

    /** 将 QuestionBank 实体转为前端期望的 Map 格式 */
    private Map<String, Object> convertToMap(QuestionBank q) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", q.getId());
        map.put("questionText", q.getQuestionText());
        map.put("questionType", q.getQuestionType());
        map.put("correctAnswer", q.getCorrectAnswer());
        map.put("explanation", q.getExplanation() != null ? q.getExplanation() : "");
        map.put("difficultyLevel", q.getDifficultyLevel() != null ? q.getDifficultyLevel() : 2);
        map.put("categoryId", q.getCategoryId());
        map.put("knowledgeNodeId", q.getCategoryId());
        map.put("source", "BANK"); // 标记来源为题库

        // 解析 options JSON
        if (q.getOptions() != null && !q.getOptions().isEmpty() && !"[]".equals(q.getOptions())) {
            try {
                map.put("options", new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(q.getOptions(), List.class));
            } catch (Exception e) {
                map.put("options", Collections.emptyList());
            }
        } else {
            map.put("options", Collections.emptyList());
        }

        return map;
    }
}
