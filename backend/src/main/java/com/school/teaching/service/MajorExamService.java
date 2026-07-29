package com.school.teaching.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 专业大类综合卷 — 跨学科组卷服务
 * 按考纲分值比例，将大类下多门专业课合并生成一份综合试卷。
 * 支持 100+ 题的高质量分批生成：每批 25 题，跨批去重 + 防重指令。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MajorExamService {

    private final DictMajorMapper majorMapper;
    private final DictMajorSubjectMapper majorSubjectMapper;
    private final DictSubjectMapper subjectMapper;
    private final KnowledgeNodeMapper nodeMapper;
    private final AiQuestionGeneratorService aiQuestionService;
    private final AiTaskStore taskStore;
    private final com.school.teaching.service.ExamSyllabusService syllabusService;
    private final com.school.teaching.service.SystemService systemService;

    private static final Set<String> CULTURE_NAMES = Set.of("语文", "数学", "英语");
    private static final int BATCH_SIZE = 25;

    /** 获取所有专业大类 */
    public List<DictMajor> getMajors() {
        return majorMapper.selectList(
            new LambdaQueryWrapper<DictMajor>().eq(DictMajor::getStatus, 1).orderByAsc(DictMajor::getSortOrder));
    }

    /** 获取某大类下的专业课学科（自动排除语数英） */
    public List<Map<String, Object>> getMajorProfessionalSubjects(Long majorId) {
        List<DictMajorSubject> mappings = majorSubjectMapper.selectList(
            new LambdaQueryWrapper<DictMajorSubject>().eq(DictMajorSubject::getMajorId, majorId));
        if (mappings.isEmpty()) return List.of();

        List<Long> subjectIds = mappings.stream().map(DictMajorSubject::getSubjectId).toList();
        List<DictSubject> allSubjects = subjectMapper.selectBatchIds(subjectIds);

        List<Map<String, Object>> result = new ArrayList<>();
        for (DictSubject s : allSubjects) {
            String name = s.getSubjectName();
            if (name == null) continue;
            String bare = name.replaceAll("\\[.*?\\]", "").trim();
            if (CULTURE_NAMES.contains(bare)) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("name", name);
            long nodeCount = nodeMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeNode>().eq(KnowledgeNode::getSubjectId, s.getId()));
            m.put("nodeCount", nodeCount);
            result.add(m);
        }
        return result;
    }

    /** 异步执行专业大类综合卷生成（支持 100+ 题分批） */
    @Async("aiExecutor")
    public void executeAsync(String taskId, Long teacherId, Long majorId,
            Map<String, Integer> typeCounts, Map<String, Integer> difficultyRatio) {
        taskStore.markRunning(taskId);
        try {
            DictMajor major = majorMapper.selectById(majorId);
            if (major == null) { taskStore.fail(taskId, "专业大类不存在"); return; }

            List<Map<String, Object>> subjects = getMajorProfessionalSubjects(majorId);
            if (subjects.isEmpty()) { taskStore.fail(taskId, "该大类下无专业课学科"); return; }

            // — 合并多学科知识点 + RAG + 考纲 —
            List<Map<String, Object>> allKps = new ArrayList<>();
            StringBuilder ragBuilder = new StringBuilder();
            StringBuilder syllabusBuilder = new StringBuilder();
            Map<String, Integer> subjectWeightMap = new LinkedHashMap<>();

            for (Map<String, Object> subj : subjects) {
                String name = (String) subj.get("name");
                Long subjectId = (Long) subj.get("id");
                List<KnowledgeNode> roots = nodeMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getSubjectId, subjectId)
                        .eq(KnowledgeNode::getLevel, 1));
                if (roots.isEmpty()) continue;

                int kpCount = 0;
                for (KnowledgeNode root : roots) {
                    List<KnowledgeNode> descendants = collectAllDescendants(root.getId(), new HashSet<>());
                    for (KnowledgeNode d : descendants) {
                        if (d.getLevel() != null && d.getLevel() >= 3) {
                            // 过滤内容过薄的知识点（<100字无法出高质量题）
                            String content = d.getContent();
                            if (content == null || content.length() < 100) continue;
                            allKps.add(Map.of("id", d.getId(), "name",
                                d.getName() != null ? d.getName() : "", "subject", name));
                            kpCount++;
                        }
                    }
                    ragBuilder.append("【").append(name).append("】\n");
                    ragBuilder.append(root.getContent() != null ? root.getContent() : name + "学科知识体系").append("\n\n");
                }
                subjectWeightMap.put(name, Math.max(kpCount, 1));

                try {
                    String syllabus = syllabusService.getSyllabusPromptContext(subjectId);
                    if (syllabus != null && !syllabus.isBlank()) {
                        syllabusBuilder.append("【").append(name).append("考纲】\n").append(syllabus).append("\n\n");
                    }
                } catch (Exception e) {
                    log.warn("加载考纲上下文失败: subjectId={}", subjectId);
                }
            }

            int totalWeight = subjectWeightMap.values().stream().mapToInt(Integer::intValue).sum();

            if (typeCounts == null || typeCounts.isEmpty()) {
                typeCounts = new LinkedHashMap<>(Map.of("SINGLE_CHOICE", 10, "MULTI_CHOICE", 5,
                    "TRUE_FALSE", 5, "FILL_IN", 5, "SHORT_ANSWER", 3));
            }
            if (difficultyRatio == null || difficultyRatio.isEmpty()) {
                difficultyRatio = Map.of("EASY", 30, "MEDIUM", 50, "HARD", 20);
            }

        int totalExpected = typeCounts.values().stream().mapToInt(Integer::intValue).sum();
        String syllabusFull = syllabusBuilder.toString();
        String ragFull = ragBuilder.toString();

        List<Map<String, Object>> allQuestions;
        if (totalExpected <= BATCH_SIZE) {
            allQuestions = generateSingleBatch(teacherId, major, allKps, typeCounts,
                difficultyRatio, syllabusFull, ragFull, null);
        } else {
            allQuestions = generateBatched(teacherId, major, allKps, typeCounts,
                difficultyRatio, syllabusFull, ragFull, totalExpected);
        }

        int actualCount = allQuestions != null ? allQuestions.size() : 0;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "questions");
        result.put("questions", allQuestions != null ? allQuestions : List.of());
        result.put("count", actualCount);
        result.put("subject", major.getMajorName() + "专业综合");

        // 难度分布校验
        if (allQuestions != null && !allQuestions.isEmpty()) {
            result.put("_difficultyAnomaly", checkDifficultyAnomaly(allQuestions, difficultyRatio));
            result.put("_difficultyAudit", buildDifficultyAudit(allQuestions, difficultyRatio));
        }
        if (totalExpected > actualCount) {
            result.put("_filteredGap", totalExpected - actualCount);
        }

        log.info("跨学科组卷完成: major={}, 请求{}题, 实得{}题, difficultyAnomaly={}",
            major.getMajorName(), totalExpected, actualCount,
            result.get("_difficultyAnomaly"));
        taskStore.complete(taskId, result);
        } catch (Exception e) {
            log.error("专业大类综合卷生成失败: taskId={}", taskId, e);
            taskStore.fail(taskId, e.getMessage());
        }
    }

    /** 单批生成（≤25 题，原有逻辑 + 修复 RAG key） */
    private List<Map<String, Object>> generateSingleBatch(Long teacherId, DictMajor major,
            List<Map<String, Object>> kps, Map<String, Integer> typeCounts,
            Map<String, Integer> difficultyRatio, String syllabusContext, String ragContext,
            String alreadyCovered) {
        Map<String, Object> params = buildBatchParams(major, kps, typeCounts, difficultyRatio,
            syllabusContext, ragContext, alreadyCovered);
        return aiQuestionService.generateFromAiAssistant(teacherId, params);
    }

    /** 分批生成（>25 题，多轮调用 + 合并 + 去重） */
    private List<Map<String, Object>> generateBatched(Long teacherId, DictMajor major,
            List<Map<String, Object>> allKps, Map<String, Integer> typeCounts,
            Map<String, Integer> difficultyRatio, String syllabusContext, String ragContext,
            int totalExpected) {
        int numBatches = (totalExpected + BATCH_SIZE - 1) / BATCH_SIZE;
        log.info("跨学科组卷分批: 共{}题, {}批", totalExpected, numBatches);

        List<Map<String, Integer>> batchTypeCounts = distributeTypeCounts(typeCounts, numBatches);
        List<List<Map<String, Object>>> batchKps = distributeKps(allKps, numBatches);

        List<Map<String, Object>> merged = new ArrayList<>();
        for (int i = 0; i < numBatches; i++) {
            log.info("批次 {}/{}: 配置={}, 知识点={}", i + 1, numBatches,
                batchTypeCounts.get(i), batchKps.get(i).size());
            String batchCovered = buildAlreadyCoveredStr(merged);
            Map<String, Object> params = buildBatchParams(major, batchKps.get(i),
                batchTypeCounts.get(i), difficultyRatio, syllabusContext, ragContext, batchCovered);
            try {
                List<Map<String, Object>> batchResult = aiQuestionService.generateFromAiAssistant(teacherId, params);
                if (batchResult != null && !batchResult.isEmpty()) {
                    merged.addAll(batchResult);
                    log.info("批次 {}/{} 完成: 实得{}题", i + 1, numBatches, batchResult.size());
                } else {
                    log.warn("批次 {}/{} 返回空", i + 1, numBatches);
                }
            } catch (Exception e) {
                log.error("批次 {}/{} 失败: {}", i + 1, numBatches, e.getMessage());
            }
        }

        List<Map<String, Object>> deduped = deduplicate(merged);
        if (deduped.size() < merged.size()) {
            log.info("去重移除{}题: {}→{}", merged.size() - deduped.size(), merged.size(), deduped.size());
        }
        return deduped;
    }

    /** 按题型数量等比分批 */
    private List<Map<String, Integer>> distributeTypeCounts(Map<String, Integer> typeCounts, int numBatches) {
        List<Map<String, Integer>> batches = new ArrayList<>();
        for (int i = 0; i < numBatches; i++) batches.add(new LinkedHashMap<>());

        for (var entry : typeCounts.entrySet()) {
            String type = entry.getKey();
            int total = entry.getValue();
            int base = total / numBatches;
            int remainder = total - base * numBatches;
            for (int i = 0; i < numBatches; i++) {
                int count = base + (i < remainder ? 1 : 0);
                if (count > 0) batches.get(i).put(type, count);
            }
        }
        return batches;
    }

    /** 知识点跨学科均匀分布（按学科 round-robin） */
    private List<List<Map<String, Object>>> distributeKps(List<Map<String, Object>> allKps, int numBatches) {
        // 按学科分组
        Map<String, List<Map<String, Object>>> bySubject = allKps.stream()
            .collect(Collectors.groupingBy(kp -> (String) kp.get("subject"), LinkedHashMap::new, Collectors.toList()));

        List<List<Map<String, Object>>> batches = new ArrayList<>();
        for (int i = 0; i < numBatches; i++) batches.add(new ArrayList<>());

        // 每学科内 round-robin 分配到各批
        for (var subjectKps : bySubject.values()) {
            int idx = 0;
            for (Map<String, Object> kp : subjectKps) {
                batches.get(idx % numBatches).add(kp);
                idx++;
            }
        }
        return batches;
    }

    /** 构建单批的生成参数 */
    private Map<String, Object> buildBatchParams(DictMajor major, List<Map<String, Object>> kps,
            Map<String, Integer> typeCounts, Map<String, Integer> difficultyRatio,
            String syllabusContext, String ragContext, String alreadyCovered) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("subject", major.getMajorName() + "专业综合");
        params.put("typeCounts", typeCounts);
        params.put("difficulty", difficultyRatio != null ? difficultyRatio.getOrDefault("MEDIUM", 3) : 3);
        params.put("comprehensive", true);

        // categoryPaths → 告诉 AI 每个题须标注 knowledgeNodeId
        List<Map<String, Object>> categoryPaths = new ArrayList<>();
        for (Map<String, Object> kp : kps) {
            categoryPaths.add(Map.of("nodeId", kp.get("id"),
                "path", kp.getOrDefault("subject", "") + " > " + kp.get("name")));
        }
        params.put("categoryPaths", categoryPaths);

        // 统计本批知识点覆盖的学科
        Set<String> coveredSubjects = kps.stream()
            .map(kp -> (String) kp.get("subject"))
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        StringBuilder weightHint = new StringBuilder("本批出题范围覆盖以下学科：");
        for (String s : coveredSubjects) weightHint.append(" ").append(s).append(",");
        if (weightHint.charAt(weightHint.length() - 1) == ',') weightHint.setLength(weightHint.length() - 1);
        weightHint.append("。各学科知识点分布均匀。");
        params.put("_weightHint", weightHint.toString());

        // syllabusContext → system & user prompt
        if (syllabusContext != null && !syllabusContext.isEmpty()) {
            params.put("syllabusContext", syllabusContext);
        }

        // 修复 RAG key 断路：ragContext → referenceMaterial
        if (ragContext != null && !ragContext.isEmpty()) {
            // RAG 太长则截取与本批知识点相关的部分
            String relevantRag = filterRagBySubjects(ragContext, coveredSubjects);
            params.put("referenceMaterial", relevantRag);
        }

        // 跨批次防重
        params.put("count", typeCounts.values().stream().mapToInt(Integer::intValue).sum());
        if (alreadyCovered != null && !alreadyCovered.isEmpty()) {
            params.put("_alreadyCovered", alreadyCovered);
        }

        // token 预算：本批需要多少 token（对齐 DeepSeekGateway.resolveMaxTokens v2：500/题）
        int essayCount = typeCounts.getOrDefault("ESSAY", 0) + typeCounts.getOrDefault("SHORT_ANSWER", 0);
        int nonEssay = typeCounts.values().stream().mapToInt(Integer::intValue).sum() - essayCount;
        int maxTokens = Math.max(8000, Math.min(600 + nonEssay * 500 + essayCount * 1200 + 800, 32768));
        params.put("_maxTokens", maxTokens);
        // 禁止重试时膨胀 typeCounts（分批已精确分配）
        params.put("_batchMode", true);

        return params;
    }

    /** 从完整 RAG 中提取与本批学科相关的内容 */
    private String filterRagBySubjects(String ragContext, Set<String> subjects) {
        if (subjects == null || subjects.isEmpty() || ragContext == null || ragContext.isEmpty()) {
            return ragContext;
        }
        StringBuilder filtered = new StringBuilder();
        String[] sections = ragContext.split("(?=【)");  // 按【分割RAG段落
        for (String section : sections) {
            for (String subject : subjects) {
                if (section.contains("【" + subject + "】") || section.contains(subject)) {
                    filtered.append(section);
                    break;
                }
            }
        }
        if (filtered.isEmpty()) {
            return ragContext.length() > 20000 ? ragContext.substring(0, 20000) + "\n...(截断)" : ragContext;
        }
        return filtered.length() > 20000
            ? filtered.substring(0, 20000) + "\n...(RAG内容已截断)"
            : filtered.toString();
    }

    /** 构建已有题目的汇总文本，注入下一批的防重指令 */
    private String buildAlreadyCoveredStr(List<Map<String, Object>> generated) {
        if (generated == null || generated.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("以下题目已在之前批次生成，请避免重复出题：\n");
        int count = 0;
        for (Map<String, Object> q : generated) {
            String text = q.containsKey("questionText") ? String.valueOf(q.get("questionText")) : null;
            String type = q.containsKey("questionType") ? String.valueOf(q.get("questionType")) : null;
            if (text != null) {
                String display = text.length() > 40 ? text.substring(0, 40) + "..." : text;
                sb.append("- [").append(type != null ? type : "?").append("] ").append(display).append("\n");
                count++;
                if (count >= 50) { sb.append("- ...(共").append(generated.size()).append("题，仅展示前50)\n"); break; }
            }
        }
        return sb.toString();
    }

    /** 基于题目文本归一化去重 */
    private List<Map<String, Object>> deduplicate(List<Map<String, Object>> questions) {
        if (questions == null || questions.size() <= 1) return questions;
        Set<String> seen = new HashSet<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> q : questions) {
            String text = q.containsKey("questionText") ? String.valueOf(q.get("questionText")) : "";
            String key = normalize(text);
            if (!key.isEmpty() && seen.add(key)) {
                result.add(q);
            }
        }
        return result;
    }

    private String normalize(String s) {
        if (s == null) return "";
        // 去空白和标点，保留数字（防数学题误判为重复）
        return s.replaceAll("[\\s,，。、；;：:！!？?（）()'\"\\[\\]【】]", "")
                .toLowerCase()
                .trim();
    }

    /** 递归收集节点下所有子孙 */
    private List<KnowledgeNode> collectAllDescendants(Long parentId, Set<Long> visited) {
        if (!visited.add(parentId)) return List.of();
        List<KnowledgeNode> children = nodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>().eq(KnowledgeNode::getParentId, parentId));
        if (children.isEmpty()) return List.of();
        List<KnowledgeNode> result = new ArrayList<>(children);
        for (KnowledgeNode child : children) {
            result.addAll(collectAllDescendants(child.getId(), visited));
        }
        return result;
    }

    // ── 质量管道：难度分布校验 ──

    /** 将题目难度级别(1-5)映射到难度类别 */
    private String mapDifficultyLevel(Object dl) {
        if (dl instanceof Number n) {
            int v = n.intValue();
            if (v <= 2) return "EASY";
            if (v == 3) return "MEDIUM";
            return "HARD";
        }
        return "MEDIUM";
    }

    /** 检查难度分布是否与教师配置偏差 >15% */
    boolean checkDifficultyAnomaly(List<Map<String, Object>> questions, Map<String, Integer> difficultyRatio) {
        Map<String, Integer> actualCounts = new LinkedHashMap<>();
        actualCounts.put("EASY", 0);
        actualCounts.put("MEDIUM", 0);
        actualCounts.put("HARD", 0);
        for (Map<String, Object> q : questions) {
            String cat = mapDifficultyLevel(q.get("difficultyLevel"));
            actualCounts.put(cat, actualCounts.get(cat) + 1);
        }
        int total = questions.size();
        for (var entry : difficultyRatio.entrySet()) {
            String cat = entry.getKey();
            int expectedPct = entry.getValue();
            int actualCount = actualCounts.getOrDefault(cat, 0);
            int actualPct = total > 0 ? actualCount * 100 / total : 0;
            if (Math.abs(actualPct - expectedPct) > 15) return true;
        }
        return false;
    }

    /** 构建难度分布审计详情 */
    private String buildDifficultyAudit(List<Map<String, Object>> questions, Map<String, Integer> difficultyRatio) {
        Map<String, Integer> actualCounts = new LinkedHashMap<>();
        actualCounts.put("EASY", 0);
        actualCounts.put("MEDIUM", 0);
        actualCounts.put("HARD", 0);
        for (Map<String, Object> q : questions) {
            String cat = mapDifficultyLevel(q.get("difficultyLevel"));
            actualCounts.put(cat, actualCounts.get(cat) + 1);
        }
        int total = questions.size();
        StringBuilder sb = new StringBuilder("难度分布审计：");
        for (String cat : List.of("EASY", "MEDIUM", "HARD")) {
            int actualCount = actualCounts.getOrDefault(cat, 0);
            int actualPct = total > 0 ? actualCount * 100 / total : 0;
            int expectedPct = difficultyRatio.getOrDefault(cat, 0);
            sb.append(cat).append("=").append(actualPct).append("%(预期").append(expectedPct).append("%) ");
        }
        sb.append("题数=").append(total);
        return sb.toString();
    }
}
