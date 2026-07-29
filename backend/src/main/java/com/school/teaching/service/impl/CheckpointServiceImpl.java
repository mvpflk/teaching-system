package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import static com.school.teaching.service.impl.CheckpointContentHelper.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.*;
import com.school.teaching.entity.CreditTransaction;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.CheckpointService;
import com.school.teaching.service.CreditService;
import com.school.teaching.service.NotificationService;
import com.school.teaching.service.PrecisionProgressService;
import com.school.teaching.service.QuestionMatchingService;
import com.school.teaching.service.SystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CheckpointServiceImpl implements CheckpointService {

    @Autowired private CheckpointConfigMapper configMapper;
    @Autowired private CheckpointProgressMapper progressMapper;
    @Autowired private CheckpointKeywordLogMapper keywordLogMapper;
    @Autowired private CheckpointMemoryCardMapper memoryCardMapper;
    @Autowired private DictMajorSubjectMapper majorSubjectMapper;
    @Autowired private DictMajorMapper majorMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private ClassesMapper classesMapper;
    @Autowired private QuestionBankMapper questionBankMapper;
    @Autowired private WrongQuestionMapper wrongQuestionMapper;
    @Autowired private DictSubjectMapper dictSubjectMapper;
    @Autowired private CreditTransactionMapper creditTransactionMapper;
    @Autowired private QuestionMatchingService questionMatchingService;
    @Autowired private SystemService systemService;
    @Autowired private CreditService creditService;
    @Autowired private NotificationService notificationService;
    @Autowired private com.school.teaching.mapper.UserMapper userMapper;
    @Autowired private com.school.teaching.mapper.KnowledgeNodeMapper knowledgeNodeMapper;
    @Autowired private com.school.teaching.mapper.TeacherClassMapper teacherClassMapper;
    @Autowired private com.school.teaching.mapper.TeacherMapper teacherMapper;
    @Autowired private PrecisionProgressService precisionProgressService;
    @Autowired private KnowledgeBaseReviewService knowledgeBaseReviewService;

    private final Map<Long, Map<Integer, Long>> keywordIndexToNodeIdCache = new ConcurrentHashMap<>();

    // ═══════════════════ 学生端API ═══════════════════

    @Override
    public List<Map<String, Object>> listSubjects(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null || student.getClassId() == null) return List.of();
        Classes clazz = classesMapper.selectById(student.getClassId());
        if (clazz == null || clazz.getMajor() == null) return List.of();

        Set<Long> subjectIds = new LinkedHashSet<>();

        // 1. 专业映射的学科
        DictMajor major = majorMapper.selectOne(
            new LambdaQueryWrapper<DictMajor>().eq(DictMajor::getMajorName, clazz.getMajor()));
        if (major != null) {
            List<DictMajorSubject> mappings = majorSubjectMapper.selectList(
                new LambdaQueryWrapper<DictMajorSubject>().eq(DictMajorSubject::getMajorId, major.getId()));
            for (DictMajorSubject m : mappings) {
                subjectIds.add(m.getSubjectId());
            }
        }

        // 2. 公共学科（所有专业学生可见）
        List<DictSubject> publicSubjects = dictSubjectMapper.selectList(
            new LambdaQueryWrapper<DictSubject>().eq(DictSubject::getIsPublic, 1));
        for (DictSubject s : publicSubjects) {
            subjectIds.add(s.getId());
        }

        if (subjectIds.isEmpty()) return List.of();

        List<CheckpointConfig> configs = configMapper.selectList(
            new LambdaQueryWrapper<CheckpointConfig>()
                .in(CheckpointConfig::getSubjectId, subjectIds)
                .eq(CheckpointConfig::getReviewStatus, "REVIEWED")
                .eq(CheckpointConfig::getStatus, 1));

        // 按学科分组
        Map<Long, List<CheckpointConfig>> grouped = new LinkedHashMap<>();
        for (CheckpointConfig c : configs) {
            grouped.computeIfAbsent(c.getSubjectId(), k -> new ArrayList<>()).add(c);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        // 需要查subject_name——可以通过knowledge_nodes level=1 或直接查 dict_subject
        // 这里使用 dict_subject 查询（通过subject_id可查到）
        for (Long sid : subjectIds) {
            List<CheckpointConfig> subjConfigs = grouped.getOrDefault(sid, List.of());
            if (subjConfigs.isEmpty()) continue;
            int passed = 0;
            for (CheckpointConfig c : subjConfigs) {
                CheckpointProgress p = getProgress(studentId, c.getId());
                if (p != null && p.getCheckpointPassed() == 1) passed++;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("subjectId", sid);
            item.put("subjectName", getSubjectName(sid));
            item.put("totalCheckpoints", subjConfigs.size());
            item.put("passedCheckpoints", passed);
            item.put("progress", subjConfigs.isEmpty() ? 0 : Math.round(passed * 100.0 / subjConfigs.size()));
            result.add(item);
        }
        return result;
    }

    private String getSubjectName(Long subjectId) {
        if (subjectId == null) return "未知学科";
        if (dictSubjectMapper == null) return "学科-" + subjectId;
        DictSubject ds = dictSubjectMapper.selectById(subjectId);
        return ds != null ? ds.getSubjectName() : "学科-" + subjectId;
    }

    @Override
    public Map<String, Object> getOverview(Long studentId, Long subjectId) {
        List<CheckpointConfig> configs = configMapper.selectList(
            new LambdaQueryWrapper<CheckpointConfig>()
                .eq(CheckpointConfig::getSubjectId, subjectId)
                .eq(CheckpointConfig::getReviewStatus, "REVIEWED")
                .eq(CheckpointConfig::getStatus, 1)
                .orderByAsc(CheckpointConfig::getSeq));

        List<Map<String, Object>> checkpoints = new ArrayList<>();
        int passedCount = 0;

        for (CheckpointConfig c : configs) {
            CheckpointProgress p = getProgress(studentId, c.getId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("configId", c.getId());
            item.put("seq", c.getSeq());
            item.put("taskName", c.getTaskName());
            item.put("chapterName", c.getChapterName());
            item.put("checkpointType", c.getCheckpointType());
            item.put("passed", p != null && p.getCheckpointPassed() == 1);
            item.put("isLocked", isLocked(configs, c, studentId));
            item.put("keywordsPassed", p != null && p.getKeywordsPassed() == 1);
            item.put("attempts", p != null ? p.getAttempts() : 0);
            checkpoints.add(item);

            if (p != null && p.getCheckpointPassed() == 1) passedCount++;
        }

        // 掌握度面板：从keyword_log统计
        Map<String, Object> masteryPanel = buildMasteryPanel(studentId, subjectId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("subjectId", subjectId);
        result.put("subjectName", getSubjectName(subjectId));
        result.put("checkpoints", checkpoints);
        result.put("totalCheckpoints", configs.size());
        result.put("passedCheckpoints", passedCount);
        result.put("progress", configs.isEmpty() ? 0 : Math.round(passedCount * 100.0 / configs.size()));
        result.put("masteryPanel", masteryPanel);
        return result;
    }

    private Map<String, Object> buildMasteryPanel(Long studentId, Long subjectId) {
        Map<String, Object> panel = new LinkedHashMap<>();
        List<CheckpointKeywordLog> logs = keywordLogMapper.selectList(
            new LambdaQueryWrapper<CheckpointKeywordLog>()
                .eq(CheckpointKeywordLog::getStudentId, studentId));

        List<Long> configIds = configMapper.selectList(
            new LambdaQueryWrapper<CheckpointConfig>()
                .eq(CheckpointConfig::getSubjectId, subjectId)
                .eq(CheckpointConfig::getReviewStatus, "REVIEWED")
                .eq(CheckpointConfig::getStatus, 1))
            .stream().map(CheckpointConfig::getId).toList();

        logs = logs.stream().filter(l -> configIds.contains(l.getConfigId())).collect(Collectors.toList());

        // 按创建时间排序（确保趋势计算按时间顺序）
        if (logs != null) {
            logs.sort(Comparator.comparing(CheckpointKeywordLog::getCreatedAt));
        }

        // 计算每个关键词的正答率
        Map<String, int[]> keywordStats = new LinkedHashMap<>(); // keyword -> [correct, total]
        for (CheckpointKeywordLog log : logs) {
            String key = log.getConfigId() + ":" + log.getKeywordIndex();
            int[] stats = keywordStats.computeIfAbsent(key, k -> new int[2]);
            stats[1]++;
            if (log.getIsCorrect() == 1) stats[0]++;
        }

        int mastered = 0, total = keywordStats.size();
        List<Double> recentAccList = new ArrayList<>();
        List<Map<String, Object>> weakList = new ArrayList<>();
        for (Map.Entry<String, int[]> e : keywordStats.entrySet()) {
            double acc = e.getValue()[1] > 0 ? (double) e.getValue()[0] / e.getValue()[1] : 0;
            recentAccList.add(acc);
            if (acc >= 0.75) mastered++;
            else if (e.getValue()[1] >= 2) {
                String[] parts = e.getKey().split(":");
                Map<String, Object> weak = new LinkedHashMap<>();
                weak.put("configId", Long.parseLong(parts[0]));
                weak.put("keywordIndex", Integer.parseInt(parts[1]));
                weak.put("accuracy", Math.round(acc * 100.0) / 100.0);
                weak.put("attempts", e.getValue()[1]);
                weakList.add(weak);
            }
        }

        // 趋势：最近3次正答率（按时间顺序，不排序）
        List<Double> trendData = new ArrayList<>();
        if (recentAccList.size() >= 3) {
            trendData = recentAccList.subList(recentAccList.size() - 3, recentAccList.size());
        } else {
            trendData = new ArrayList<>(recentAccList);
        }

        String trendDirection = "STABLE";
        if (trendData.size() >= 2) {
            double first = trendData.get(0);
            double last = trendData.get(trendData.size() - 1);
            if (last > first + 0.05) trendDirection = "UP";
            else if (last < first - 0.05) trendDirection = "DOWN";
        }

        panel.put("masteredKeywords", mastered);
        panel.put("totalKeywords", total);
        panel.put("recentAccuracy", trendData);
        panel.put("trendDirection", trendDirection);
        panel.put("weakConcepts", weakList);
        return panel;
    }

    private boolean isLocked(List<CheckpointConfig> configs, CheckpointConfig current, Long studentId) {
        String type = current.getCheckpointType();
        if ("NORMAL".equals(type)) {
            int idx = configs.indexOf(current);
            if (idx > 0) {
                CheckpointConfig prev = configs.get(idx - 1);
                CheckpointProgress pp = getProgress(studentId, prev.getId());
                return pp == null || pp.getCheckpointPassed() != 1;
            }
        } else if ("BOSS".equals(type) || "MIXED".equals(type)) {
            List<CheckpointConfig> normalConfigs = configs.stream()
                .filter(c -> "NORMAL".equals(c.getCheckpointType()))
                .collect(Collectors.toList());
            int passedCount = 0;
            for (CheckpointConfig nc : normalConfigs) {
                CheckpointProgress p = getProgress(studentId, nc.getId());
                if (p != null && p.getCheckpointPassed() == 1) passedCount++;
            }
            int required = (int) Math.ceil(normalConfigs.size() * 0.5);
            return passedCount < Math.max(required, 1);
        }
        return false;
    }

    /**
     * 检查关卡是否已解锁，未解锁则抛异常
     */
    private void checkCheckpointNotLocked(Long studentId, CheckpointConfig config) {
        if (!"BOSS".equals(config.getCheckpointType()) && !"MIXED".equals(config.getCheckpointType())) return;
        List<CheckpointConfig> configs = configMapper.selectList(
            new LambdaQueryWrapper<CheckpointConfig>()
                .eq(CheckpointConfig::getSubjectId, config.getSubjectId())
                .eq(CheckpointConfig::getReviewStatus, "REVIEWED")
                .eq(CheckpointConfig::getStatus, 1)
                .orderByAsc(CheckpointConfig::getSeq));
        if (isLocked(configs, config, studentId)) {
            throw new BusinessException(403, "该关卡暂未解锁，请先通过足够的普通关");
        }
    }
    @Override
    public Map<String, Object> startCheckpoint(Long studentId, Long configId) {
        CheckpointConfig config = getConfig(configId);
        if (!"REVIEWED".equals(config.getReviewStatus()) || config.getStatus() != 1) {
            throw new BusinessException(403, "该关卡暂未开放");
        }

        CheckpointProgress progress = getOrCreateProgress(studentId, configId, config.getSubjectId());

        // ★★★ 核心改写：动态读取 task_node_id 下所有 level=4 子知识点 ★★★
        List<KnowledgeNode> childNodes = loadChildKnowledgeNodes(config.getTaskNodeId());

        // 为每个子知识点构建 key_point
        List<Map<String, Object>> keyPoints = new ArrayList<>();
        List<Map<String, Object>> allKeywords = new ArrayList<>(); // 扁平化，全局 index
        Map<Integer, Long> globalIndexToNodeId = new LinkedHashMap<>(); // 全局index → nodeId

        for (KnowledgeNode node : childNodes) {
            Map<String, Object> kp = new LinkedHashMap<>();
            kp.put("nodeId", node.getId());
            kp.put("title", cleanNodeTitle(node.getName()));

            // ★ 新管线：解析备课模板 → 分层学生可读内容
            Map<String, String> parsed = parseContentTemplate(node.getContent());
            kp.put("detailHtml", parsed.get("detailHtml"));       // 定义+例子+考法
            kp.put("quickRead", parsed.get("quickRead"));          // 一句速览

            // 只从定义段提取核心概念（2-4个），不再是全量粗体扫描
            String[] coreTerms = parsed.getOrDefault("coreTermsCsv", "").split(",");
            List<Map<String, Object>> kwds = buildCoreKeywords(coreTerms, node.getContent());
            kp.put("keywords", kwds);

            // 记录全局映射
            for (Map<String, Object> kw : kwds) {
                int globalIdx = allKeywords.size();
                kw.put("globalIndex", globalIdx);
                globalIndexToNodeId.put(globalIdx, node.getId());
                allKeywords.add(kw);
            }

            // 此题节点下题库中的应会题
            List<Map<String, Object>> pq = loadNodePracticeQuestions(node.getId());
            kp.put("practiceQuestions", pq);

            keyPoints.add(kp);
        }

        keywordIndexToNodeIdCache.put(configId, new LinkedHashMap<>(globalIndexToNodeId));

        // 从所有子知识点的题库中抽取验证题（按难度配比）
        List<Map<String, Object>> checkpointQuestions = loadVerificationQuestions(
            childNodes, progress != null ? progress.getQuestionIds() : null, config);

        // 从弱项知识点中抽取交错练习（回顾强化）
        List<Map<String, Object>> interleavingQuestions = loadInterleavingNodes(studentId);

        // 查已正确确认的关键词（globalIndex → true）
        Map<Integer, Boolean> previouslyCorrect = loadPreviouslyCorrectKeywords(
            studentId, configId, allKeywords.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("configId", config.getId());
        result.put("seq", config.getSeq());
        result.put("taskName", config.getTaskName());
        result.put("chapterName", config.getChapterName());
        result.put("keyPoints", keyPoints);
        result.put("keywordCounts", globalIndexToNodeId.entrySet().stream()
            .collect(java.util.stream.Collectors.groupingBy(
                e -> e.getValue(), LinkedHashMap::new,
                java.util.stream.Collectors.counting())));
        result.put("previouslyCorrect", previouslyCorrect);
        result.put("checkpointQuestions", checkpointQuestions);
        result.put("interleavingQuestions", interleavingQuestions);
        result.put("lastNodeIndex", progress != null ? progress.getLastNodeIndex() : null);
        // 仅当全部子节点的全部关键词都已确认 才认为"已通过"
        boolean trulyPassed = progress.getKeywordsPassed() != null
            && progress.getKeywordsPassed() == 1
            && allKeywords.size() > 0
            && previouslyCorrect.size() >= allKeywords.size();
        result.put("keywordsPreviouslyPassed", trulyPassed);
        result.put("attempts", progress.getAttempts() != null ? progress.getAttempts() : 0);
        result.put("totalKnowledgePoints", childNodes.size());
        result.put("totalKeywords", allKeywords.size());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> verifyKeywords(Long studentId, Long configId, List<Map<String, Object>> answers) {
        CheckpointConfig config = getConfig(configId);
        CheckpointProgress progress = getOrCreateProgress(studentId, configId, config.getSubjectId());

        int attemptNo = progress.getKeywordsAttempts() + 1;

        List<Map<String, Object>> results = new ArrayList<>();
        boolean allCorrect = true;

        // 循环前批量加载本次尝试的已有关键词日志，避免逐条 selectOne（N+1）
        List<Integer> answerIndices = answers.stream()
            .map(a -> ((Number) a.get("index")).intValue())
            .collect(Collectors.toList());
        Map<Integer, CheckpointKeywordLog> existingLogMap = answerIndices.isEmpty() ? new HashMap<>()
            : keywordLogMapper.selectList(new LambdaQueryWrapper<CheckpointKeywordLog>()
                    .eq(CheckpointKeywordLog::getStudentId, studentId)
                    .eq(CheckpointKeywordLog::getConfigId, configId)
                    .eq(CheckpointKeywordLog::getAttemptNo, attemptNo)
                    .in(CheckpointKeywordLog::getKeywordIndex, answerIndices))
                .stream().collect(Collectors.toMap(CheckpointKeywordLog::getKeywordIndex, l -> l, (a, b) -> a));

        for (Map<String, Object> ans : answers) {
            int idx = ((Number) ans.get("index")).intValue();
            String value = (String) ans.getOrDefault("value", "");
            String term = (String) ans.getOrDefault("term", "");
            @SuppressWarnings("unchecked")
            List<String> aliases = (List<String>) ans.getOrDefault("acceptAliases", List.of());

            Map<String, Object> r = new LinkedHashMap<>();
            r.put("index", idx);
            boolean correct = matchKeywordValue(value, term, aliases);
            r.put("correct", correct);
            if (!correct) allCorrect = false;

            // 记录日志：如果已有记录且本次正确，则更新为正确（允许纠正）
            CheckpointKeywordLog existingLog = existingLogMap.get(idx);
            if (existingLog != null) {
                // 已有记录：仅当本次正确时覆盖（允许学生纠正错误答案）
                if (correct && existingLog.getIsCorrect() == 0) {
                    existingLog.setStudentInput(value);
                    existingLog.setIsCorrect(1);
                    keywordLogMapper.updateById(existingLog);
                }
            } else {
                CheckpointKeywordLog kwLog = new CheckpointKeywordLog();
                kwLog.setStudentId(studentId);
                kwLog.setConfigId(configId);
                kwLog.setKeywordIndex(idx);
                kwLog.setAttemptNo(attemptNo);
                kwLog.setStudentInput(value);
                kwLog.setIsCorrect(correct ? 1 : 0);
                keywordLogMapper.insert(kwLog);
            }

            // 错 3 次显示提示
            int totalErrors = countKeywordErrors(studentId, configId, idx);
            if (totalErrors >= 3) {
                r.put("hint", term);
                r.put("totalErrors", totalErrors);
                r.put("showAnswer", true);
            }

            if (correct) {
                int quality = totalErrors == 0 ? 4 : totalErrors == 1 ? 3 : 2;
                Long nodeId = getNodeIdByKeywordIndex(configId, idx);
                if (nodeId != null) {
                    knowledgeBaseReviewService.rateNodeKnowledge(studentId, nodeId, quality);
                }
            }

            results.add(r);
        }

        progress.setKeywordsAttempts(attemptNo);
        progressMapper.updateById(progress);

        // 检查是否所有关键词都已确认（正确或跳过）
        boolean allKeywordsCovered = allKeywordsResolved(studentId, configId, config);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("allCorrect", allCorrect);
        result.put("results", results);
        result.put("allKeywordsCovered", allKeywordsCovered);
        result.put("keywordsPassed", allKeywordsCovered);
        result.put("attemptNo", attemptNo);
        result.put("canSkip", progress.getKeywordsSkipped() < 3);
        result.put("skippedCount", progress.getKeywordsSkipped());

        if (allKeywordsCovered && progress.getKeywordsPassed() != 1) {
            progress.setKeywordsPassed(1);
            progressMapper.updateById(progress);
        }

        return result;
    }

    private boolean checkAllKeywordsResolved(Long studentId, Long configId) {
        // 从 keyword_log 中统计已确认正确的关键词索引数
        // 对比 checkpoint_config 中 task_node 下所有子节点的关键词总数
        CheckpointConfig config = configMapper.selectById(configId);
        if (config == null) return false;

        List<KnowledgeNode> childNodes = loadChildKnowledgeNodes(config.getTaskNodeId());
        int totalKw = 0;
        for (KnowledgeNode node : childNodes) {
            Map<String, String> parsed = parseContentTemplate(node.getContent());
            String[] terms = parsed.getOrDefault("coreTermsCsv", "").split(",");
            totalKw += (terms.length == 1 && terms[0].isBlank()) ? 0 : terms.length;
        }

        // 统计已正确 + 已跳过的数量
        CheckpointProgress progress = getProgress(studentId, configId);
        int skipped = progress != null ? progress.getKeywordsSkipped() : 0;

        Long correctCount = keywordLogMapper.selectCount(
            new LambdaQueryWrapper<CheckpointKeywordLog>()
                .eq(CheckpointKeywordLog::getStudentId, studentId)
                .eq(CheckpointKeywordLog::getConfigId, configId)
                .eq(CheckpointKeywordLog::getIsCorrect, 1));

        return (correctCount != null ? correctCount.intValue() : 0) + skipped >= totalKw;
    }

    private int countKeywordErrors(Long studentId, Long configId, int keywordIndex) {
        Long count = keywordLogMapper.selectCount(
            new LambdaQueryWrapper<CheckpointKeywordLog>()
                .eq(CheckpointKeywordLog::getStudentId, studentId)
                .eq(CheckpointKeywordLog::getConfigId, configId)
                .eq(CheckpointKeywordLog::getKeywordIndex, keywordIndex)
                .eq(CheckpointKeywordLog::getIsCorrect, 0));
        return count == null ? 0 : count.intValue();
    }

    @Override
    @Transactional
    public Map<String, Object> skipKeyword(Long studentId, Long configId, int keywordIndex) {
        CheckpointConfig config = getConfig(configId);
        CheckpointProgress progress = getOrCreateProgress(studentId, configId, config.getSubjectId());

        if (progress.getKeywordsSkipped() >= 3) {
            throw new BusinessException(400, "本关跳过次数已达上限（3次）");
        }

        boolean ok = consumeCredits(studentId, 1,
            "checkpoint_keyword_skip:" + studentId + ":" + configId + ":" + keywordIndex);
        if (!ok) {
            throw new BusinessException(400, "积分不足，跳过关键词需要消耗1积分");
        }

        progress.setKeywordsSkipped(progress.getKeywordsSkipped() + 1);
        progressMapper.updateById(progress);

        // 记录日志：跳过视为未正确
        CheckpointKeywordLog kwLog = new CheckpointKeywordLog();
        kwLog.setStudentId(studentId);
        kwLog.setConfigId(configId);
        kwLog.setKeywordIndex(keywordIndex);
        kwLog.setAttemptNo(progress.getKeywordsAttempts() + 1);
        kwLog.setStudentInput("[SKIPPED]");
        kwLog.setIsCorrect(0);
        keywordLogMapper.insert(kwLog);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("skipped", true);
        result.put("keywordIndex", keywordIndex);
        result.put("skippedCount", progress.getKeywordsSkipped());
        result.put("canSkipMore", progress.getKeywordsSkipped() < 3);
        result.put("keywordsPassed", allKeywordsResolved(studentId, configId, config));
        return result;
    }

    private boolean allKeywordsResolved(Long studentId, Long configId, CheckpointConfig config) {
        CheckpointProgress progress = getProgress(studentId, configId);
        if (progress != null && progress.getKeywordsPassed() == 1) return true;

        // 统一使用动态解析（与 checkAllKeywordsResolved 一致）
        List<KnowledgeNode> childNodes = loadChildKnowledgeNodes(config.getTaskNodeId());
        int totalKw = 0;
        if (childNodes.isEmpty()) {
            // 兜底：知识树为空时回退到 keyPointsJson 静态配置
            List<Map<String, Object>> keyPoints = parseKeyPointsJson(config.getKeyPointsJson());
            for (Map<String, Object> kp : keyPoints) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> kwds = (List<Map<String, Object>>) kp.get("keywords");
                if (kwds != null) totalKw += kwds.size();
            }
        } else {
            for (KnowledgeNode node : childNodes) {
                Map<String, String> parsed = parseContentTemplate(node.getContent());
                String[] terms = parsed.getOrDefault("coreTermsCsv", "").split(",");
                totalKw += (terms.length == 1 && terms[0].isBlank()) ? 0 : terms.length;
            }
        }

        Long correctCount = keywordLogMapper.selectCount(
            new LambdaQueryWrapper<CheckpointKeywordLog>()
                .eq(CheckpointKeywordLog::getStudentId, studentId)
                .eq(CheckpointKeywordLog::getConfigId, configId)
                .eq(CheckpointKeywordLog::getIsCorrect, 1));
        int correct = correctCount != null ? correctCount.intValue() : 0;
        int skipped = progress != null ? progress.getKeywordsSkipped() : 0;

        int minRequiredCorrect = (int) Math.ceil(totalKw * 0.5);
        if (correct >= minRequiredCorrect && (correct + skipped) >= totalKw) {
            if (progress != null) {
                progress.setKeywordsPassed(1);
                progressMapper.updateById(progress);
            }
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public Map<String, Object> submitCheckpoint(Long studentId, Long configId, Map<String, Object> answer) {
        // 兼容单题和多题两种格式
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = answer.containsKey("answers")
            ? (List<Map<String, Object>>) answer.get("answers")
            : List.of(answer);

        CheckpointConfig config = getConfig(configId);
        CheckpointProgress progress = getOrCreateProgress(studentId, configId, config.getSubjectId());

        int correctCount = 0;
        int totalCount = answers.size();
        List<Map<String, Object>> details = new ArrayList<>();
        Set<Long> wrongNodeIds = new HashSet<>();

        for (Map<String, Object> ans : answers) {
            Long questionId = Long.valueOf(ans.get("questionId").toString());
            String studentAnswer = (String) ans.getOrDefault("answer", "");

            QuestionBank question = questionBankMapper.selectById(questionId);
            if (question == null) continue;

            boolean correct = checkAnswer(studentAnswer, question);
            if (correct) {
                correctCount++;
                recordWrongQuestionManager(studentId, questionId, configId, true);
            } else {
                recordWrongQuestion(studentId, questionId, configId);
                if (question.getCategoryId() != null) wrongNodeIds.add(question.getCategoryId());
            }

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("questionId", questionId);
            detail.put("correct", correct);
            detail.put("correctAnswer", question.getCorrectAnswer());
            detail.put("explanation", question.getExplanation());
            details.add(detail);
        }

        progress.setAttempts(progress.getAttempts() + 1);
        boolean allCorrect = totalCount > 0 && correctCount == totalCount;
        if (allCorrect && progress.getCorrectCount() < correctCount) {
            progress.setCorrectCount(correctCount);
        }

        Map<String, Object> result = new LinkedHashMap<>();

        if (allCorrect) {
            progress.setCheckpointPassed(1);
            progress.setPassedAt(LocalDateTime.now());
            progressMapper.updateById(progress);

            grantCheckpointCredit(studentId, config);
            generateMemoryCard(studentId, config);

            result.put("passed", true);
            result.put("correctCount", correctCount);
            result.put("totalCount", totalCount);
            result.put("creditsEarned", progress.getCreditAmount());
        } else {
            progressMapper.updateById(progress);

            for (Long nodeId : wrongNodeIds) {
                precisionProgressService.markWeakIfNeeded(studentId, nodeId, configId);
            }

            int failCount = progress.getAttempts() != null ? progress.getAttempts() : 1;

            Map<String, Object> intervention = new LinkedHashMap<>();
            if (failCount >= 3) {
                intervention.put("level", 2);
                intervention.put("message", "这一关遇到困难了？试试先复习一下错题再挑战吧 💪");
                intervention.put("action", "showHint");
            } else if (failCount >= 2) {
                intervention.put("level", 1);
                intervention.put("message", "别灰心，看一下错题的解析再试试！");
                intervention.put("action", "showReview");
            } else {
                intervention.put("level", 0);
                intervention.put("message", "差一点就过关了，再试一次吧！");
                intervention.put("action", "retry");
            }

            result.put("passed", false);
            result.put("correctCount", correctCount);
            result.put("totalCount", totalCount);
            result.put("accuracy", Math.round(correctCount * 10000.0 / Math.max(totalCount, 1)) / 100.0);
            result.put("retryable", true);
            result.put("intervention", intervention);
        }

        result.put("details", details);
        result.put("attempts", progress.getAttempts());
        return result;
    }

    private void grantCheckpointCredit(Long studentId, CheckpointConfig config) {
        String today = LocalDate.now().toString();
        String bizKey = "checkpoint_" + config.getCheckpointType().toLowerCase() + ":" + studentId + ":" + config.getId();

        int dailyCap = systemService.getIntConfig("feature.checkpoint_daily_credit_cap", 20);
        Long dailyCount = creditTransactionMapper.selectCount(
            new LambdaQueryWrapper<CreditTransaction>()
                .eq(CreditTransaction::getStudentId, studentId)
                .likeRight(CreditTransaction::getDescription, "checkpoint_")
                .ge(CreditTransaction::getCreateTime, LocalDate.now().atStartOfDay()));
        if (dailyCount != null && dailyCount >= dailyCap) return;

        int amount = "NORMAL".equals(config.getCheckpointType()) ? 1 : 0;
        try {
            creditService.awardMoralCredit(studentId, amount, bizKey);
        } catch (Exception e) {
            log.warn("积分发放失败 studentId={} bizKey={}", studentId, bizKey, e);
        }
    }

    private void generateMemoryCard(Long studentId, CheckpointConfig config) {
        if (memoryCardMapper.selectCount(
            new LambdaQueryWrapper<CheckpointMemoryCard>()
                .eq(CheckpointMemoryCard::getStudentId, studentId)
                .eq(CheckpointMemoryCard::getConfigId, config.getId())) > 0) return;

        try {
            // 使用动态解析（与 startCheckpoint 一致）
            List<KnowledgeNode> childNodes = loadChildKnowledgeNodes(config.getTaskNodeId());
            List<Map<String, Object>> cardKeywords = new ArrayList<>();
            StringBuilder quickSummary = new StringBuilder();

            for (KnowledgeNode node : childNodes) {
                Map<String, String> parsed = parseContentTemplate(node.getContent());
                String quickRead = parsed.getOrDefault("quickRead", "");
                if (!quickRead.isEmpty() && quickSummary.length() == 0) {
                    quickSummary.append(quickRead);
                }

                String definitionText = parsed.getOrDefault("definitionText", "");
                String[] coreTerms = parsed.getOrDefault("coreTermsCsv", "").split(",");
                for (String term : coreTerms) {
                    term = term.trim();
                    if (term.isEmpty()) continue;
                    Map<String, Object> cardKw = new LinkedHashMap<>();
                    cardKw.put("front", term);
                    cardKw.put("back", definitionText.isEmpty() ? term : definitionText);
                    cardKw.put("type", "concept");
                    cardKeywords.add(cardKw);
                }
            }

            Map<String, Object> cardJson = new LinkedHashMap<>();
            cardJson.put("configId", config.getId());
            cardJson.put("taskName", config.getTaskName());
            cardJson.put("chapterName", config.getChapterName());
            cardJson.put("earnedAt", LocalDateTime.now().toString());
            cardJson.put("keywords", cardKeywords);
            cardJson.put("quickSummary", quickSummary.toString());

            CheckpointMemoryCard card = new CheckpointMemoryCard();
            card.setStudentId(studentId);
            card.setConfigId(config.getId());
            card.setCardJson(JSON.writeValueAsString(cardJson));
            memoryCardMapper.insert(card);
        } catch (Exception e) {
            log.error("记忆卡生成失败 studentId={} configId={}", studentId, config.getId(), e);
        }
    }

    private void recordWrongQuestion(Long studentId, Long questionId, Long configId) {
        WrongQuestion existing = wrongQuestionMapper.selectOne(
            new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getStudentId, studentId)
                .eq(WrongQuestion::getQuestionId, questionId)
                .last("LIMIT 1"));
        if (existing != null) {
            existing.setWrongCount(existing.getWrongCount() + 1);
            existing.setLastWrongTime(LocalDateTime.now());
            wrongQuestionMapper.updateById(existing);
        } else {
            WrongQuestion wq = new WrongQuestion();
            wq.setStudentId(studentId);
            wq.setQuestionId(questionId);
            wq.setWrongCount(1);
            wq.setLastWrongTime(LocalDateTime.now());
            wq.setIsMastered(0);
            wq.setConsecutiveCorrect(0);
            wq.setMasteredStreak(0);
            wq.setSourceType("checkpoint");
            wq.setSourceTaskId(configId);
            wrongQuestionMapper.insert(wq);
        }
    }

    private boolean checkAnswer(String studentAnswer, QuestionBank question) {
        if (question == null || question.getCorrectAnswer() == null) return false;
        return com.school.teaching.common.ExamTaskHandler.answersMatch(
            question.getQuestionType(), question.getCorrectAnswer(), studentAnswer);
    }

    // ═══════════════════ Boss战 ═══════════════════

    @Override
    public Map<String, Object> startBoss(Long studentId, Long configId) {
        CheckpointConfig config = getConfig(configId);
        if (!"BOSS".equals(config.getCheckpointType())) {
            throw new BusinessException(400, "该关卡不是Boss战");
        }
        checkCheckpointNotLocked(studentId, config);
        CheckpointProgress progress = getOrCreateProgress(studentId, configId, config.getSubjectId());

        List<Map<String, Object>> questions = drawInterleavedQuestions(
            config, studentId, getPreviousQuestionIds(progress), config.getSubjectId());
        Collections.shuffle(questions);

        // 记录本场题集
        List<Long> qids = questions.stream().map(q -> Long.valueOf(q.get("id").toString())).toList();
        try { progress.setQuestionIds(JSON.writeValueAsString(qids)); } catch (Exception ignored) { log.warn("闯关题目ID序列化失败: {}", ignored.getMessage()); }
        progressMapper.updateById(progress);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("configId", configId);
        result.put("taskName", config.getTaskName());
        result.put("questions", questions);
        result.put("attempts", progress.getAttempts());
        result.put("retryable", progress.getAttempts() > 0);
        result.put("retryCost", 2);
        if (progress.getCheckpointPassed() == 1) {
            result.put("bestScore", progress.getCorrectCount());
            result.put("creditGranted", progress.getCreditGranted());
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> submitBoss(Long studentId, Long configId, List<Map<String, Object>> answers) {
        CheckpointConfig config = getConfig(configId);
        CheckpointProgress progress = getOrCreateProgress(studentId, configId, config.getSubjectId());

        int correct = 0, total = answers.size();
        for (Map<String, Object> ans : answers) {
            Long qid = Long.valueOf(ans.get("questionId").toString());
            String a = (String) ans.getOrDefault("answer", "");
            QuestionBank qb = questionBankMapper.selectById(qid);
            if (qb != null && checkAnswer(a, qb)) {
                correct++;
                recordWrongQuestionManager(studentId, qid, configId, true);
            } else {
                if (qb != null) recordWrongQuestion(studentId, qid, configId);
            }
        }

        progress.setAttempts(progress.getAttempts() + 1);
        boolean isBest = correct > progress.getCorrectCount();
        if (isBest) progress.setCorrectCount(correct);

        double rate = total > 0 ? (double) correct / total : 0;
        boolean pass80 = rate >= 0.8;

        if (pass80) {
            progress.setCheckpointPassed(1);
            if (progress.getPassedAt() == null) progress.setPassedAt(LocalDateTime.now());
        }
        progressMapper.updateById(progress);

        int credits = 0;
        if (pass80 && progress.getCreditGranted() == 0) {
            credits = grantBossCredit(studentId, config, rate);
            progress.setCreditGranted(1);
            progress.setCreditAmount(credits);
            progress.setCreditGrantedAt(LocalDateTime.now());
            progressMapper.updateById(progress);
            generateMemoryCard(studentId, config);
        } else if (pass80 && progress.getCreditGranted() == 1 && isBest) {
            int prevLevel = rateToCreditLevel((double) progress.getCorrectCount() / total);
            int newLevel = rateToCreditLevel(rate);
            if (newLevel > prevLevel) {
                credits = (newLevel - prevLevel) * 2;
                try { creditService.awardMoralCredit(studentId, credits, "checkpoint_boss_diff:" + studentId + ":" + configId); } catch (Exception ignored) { log.warn("闯关积分发放失败: {}", ignored.getMessage()); }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("correctCount", correct);
        result.put("totalCount", total);
        result.put("accuracy", Math.round(rate * 10000.0) / 100.0);
        result.put("passed", pass80);
        result.put("bestScore", progress.getCorrectCount());
        result.put("creditsEarned", credits);
        result.put("retryable", !pass80);
        result.put("retryCost", 2);
        result.put("isNewBest", isBest);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> retryBoss(Long studentId, Long configId) {
        CheckpointProgress progress = getProgress(studentId, configId);
        if (progress == null) throw new BusinessException(404, "未找到闯关进度");

        String today = LocalDate.now().toString();
        Long todayRetries = creditTransactionMapper.selectCount(
            new LambdaQueryWrapper<CreditTransaction>()
                .eq(CreditTransaction::getStudentId, studentId)
                .likeRight(CreditTransaction::getBizKey, "checkpoint_boss_retry:" + studentId + ":" + configId + ":")
                .ge(CreditTransaction::getCreateTime, LocalDate.now().atStartOfDay()));
        if (todayRetries != null && todayRetries >= 3) {
            throw new BusinessException(429, "今日重考次数已达上限（3次），请明天再来");
        }

        if (!consumeCredits(studentId, 2, "checkpoint_boss_retry:" + studentId + ":" + configId + ":" + today + ":" + todayRetries)) {
            throw new BusinessException(400, "积分不足，重考需要消耗2积分");
        }
        return startBoss(studentId, configId);
    }

    // ═══════════════════ 混合战 ═══════════════════

    @Override
    public Map<String, Object> startMixed(Long studentId, Long configId) {
        CheckpointConfig config = getConfig(configId);
        if (!"MIXED".equals(config.getCheckpointType())) {
            throw new BusinessException(400, "该关卡不是混合战");
        }
        checkCheckpointNotLocked(studentId, config);
        CheckpointProgress progress = getOrCreateProgress(studentId, configId, config.getSubjectId());

        List<Map<String, Object>> questions = drawInterleavedQuestions(
            config, studentId, getPreviousQuestionIds(progress), config.getSubjectId());
        Collections.shuffle(questions);

        List<Long> qids = questions.stream().map(q -> Long.valueOf(q.get("id").toString())).toList();
        try { progress.setQuestionIds(JSON.writeValueAsString(qids)); } catch (Exception ignored) { log.warn("闯关题目ID序列化失败: {}", ignored.getMessage()); }
        progressMapper.updateById(progress);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("configId", configId);
        result.put("taskName", config.getTaskName());
        result.put("questions", questions);
        result.put("attempts", progress.getAttempts());
        result.put("retryable", progress.getAttempts() > 0);
        result.put("retryCost", 2);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> submitMixed(Long studentId, Long configId, List<Map<String, Object>> answers) {
        // 与submitBoss逻辑一致，复用
        return submitBoss(studentId, configId, answers);
    }

    @Override
    @Transactional
    public Map<String, Object> retryMixed(Long studentId, Long configId) {
        CheckpointProgress progress = getProgress(studentId, configId);
        if (progress == null) throw new BusinessException(404, "未找到闯关进度");

        String today = LocalDate.now().toString();
        Long todayRetries = creditTransactionMapper.selectCount(
            new LambdaQueryWrapper<CreditTransaction>()
                .eq(CreditTransaction::getStudentId, studentId)
                .likeRight(CreditTransaction::getBizKey, "checkpoint_mixed_retry:" + studentId + ":" + configId + ":")
                .ge(CreditTransaction::getCreateTime, LocalDate.now().atStartOfDay()));
        if (todayRetries != null && todayRetries >= 3) {
            throw new BusinessException(429, "今日重考次数已达上限（3次），请明天再来");
        }

        if (!consumeCredits(studentId, 2, "checkpoint_mixed_retry:" + studentId + ":" + configId + ":" + today + ":" + todayRetries)) {
            throw new BusinessException(400, "积分不足，重考需要消耗2积分");
        }
        return startMixed(studentId, configId);
    }

    // ═══════════════════ 记忆卡 ═══════════════════

    @Override
    public List<Map<String, Object>> listMemoryCards(Long studentId, Long subjectId) {
        if (subjectId == null || subjectId == 0) {
            return memoryCardMapper.selectList(
                new LambdaQueryWrapper<CheckpointMemoryCard>()
                    .eq(CheckpointMemoryCard::getStudentId, studentId)
                    .orderByDesc(CheckpointMemoryCard::getCreatedAt))
                .stream().map(this::toMemoryCardMap).toList();
        }

        List<Long> configIds = configMapper.selectList(
            new LambdaQueryWrapper<CheckpointConfig>().eq(CheckpointConfig::getSubjectId, subjectId))
            .stream().map(CheckpointConfig::getId).toList();
        if (configIds.isEmpty()) return List.of();

        return memoryCardMapper.selectList(
            new LambdaQueryWrapper<CheckpointMemoryCard>()
                .eq(CheckpointMemoryCard::getStudentId, studentId)
                .in(CheckpointMemoryCard::getConfigId, configIds)
                .orderByDesc(CheckpointMemoryCard::getCreatedAt))
            .stream().map(this::toMemoryCardMap).toList();
    }

    @Override
    public Map<String, Object> getMemoryCard(Long cardId) {
        CheckpointMemoryCard card = memoryCardMapper.selectById(cardId);
        if (card == null) throw new BusinessException(404, "记忆卡不存在");
        return toMemoryCardMap(card);
    }

    @Override
    public void reviewMemoryCard(Long cardId) {
        CheckpointMemoryCard card = memoryCardMapper.selectById(cardId);
        if (card == null) throw new BusinessException(404, "记忆卡不存在");
        card.setLastReviewedAt(LocalDateTime.now());
        card.setReviewCount(card.getReviewCount() + 1);
        memoryCardMapper.updateById(card);
    }

    @Override
    public int getUnreviewedCount(Long studentId) {
        int pushDays = systemService.getIntConfig("feature.checkpoint_memory_push_days", 3);
        if (pushDays <= 0) return 0;
        LocalDateTime threshold = LocalDateTime.now().minusDays(pushDays);
        Long count = memoryCardMapper.selectCount(
            new LambdaQueryWrapper<CheckpointMemoryCard>()
                .eq(CheckpointMemoryCard::getStudentId, studentId)
                .and(w -> w.isNull(CheckpointMemoryCard::getLastReviewedAt)
                    .or().lt(CheckpointMemoryCard::getLastReviewedAt, threshold)));
        return count == null ? 0 : count.intValue();
    }

    private Map<String, Object> toMemoryCardMap(CheckpointMemoryCard card) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", card.getId());
        m.put("studentId", card.getStudentId());
        m.put("configId", card.getConfigId());
        m.put("lastReviewedAt", card.getLastReviewedAt());
        m.put("reviewCount", card.getReviewCount());

        try {
            m.put("card", JSON.readValue(card.getCardJson(), Map.class));
        } catch (Exception e) {
            m.put("card", Map.of());
        }

        CheckpointConfig config = configMapper.selectById(card.getConfigId());
        if (config != null) {
            m.put("taskName", config.getTaskName());
            m.put("chapterName", config.getChapterName());
            m.put("subjectId", config.getSubjectId());
        }
        return m;
    }

    // ═══════════════════ 教师端 ═══════════════════

    @Override
    public Map<String, Object> adminList(Long subjectId, String reviewStatus, int page, int size) {
        LambdaQueryWrapper<CheckpointConfig> w = new LambdaQueryWrapper<>();
        if (subjectId != null) w.eq(CheckpointConfig::getSubjectId, subjectId);
        if (reviewStatus != null && !reviewStatus.isEmpty()) w.eq(CheckpointConfig::getReviewStatus, reviewStatus);
        w.orderByAsc(CheckpointConfig::getSubjectId, CheckpointConfig::getSeq);

        Page<CheckpointConfig> pg = configMapper.selectPage(new Page<>(page, size), w);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", pg.getRecords());
        result.put("total", pg.getTotal());
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    @Transactional
    public void adminUpdate(Long configId, CheckpointConfig config) {
        CheckpointConfig existing = getConfig(configId);
        if (config.getKeyPointsJson() != null) existing.setKeyPointsJson(config.getKeyPointsJson());
        if (config.getTaskName() != null) existing.setTaskName(config.getTaskName());
        if (config.getChapterName() != null) existing.setChapterName(config.getChapterName());
        if (config.getQuestionSource() != null) existing.setQuestionSource(config.getQuestionSource());
        if (config.getQuestionCount() != null) existing.setQuestionCount(config.getQuestionCount());
        if (config.getPracticeCount() != null) existing.setPracticeCount(config.getPracticeCount());
        if (config.getSeq() != null) existing.setSeq(config.getSeq());
        if (config.getDifficultyLevel() != null) existing.setDifficultyLevel(config.getDifficultyLevel());
        configMapper.updateById(existing);
    }

    @Override
    @Transactional
    public void adminReview(Long configId, boolean approved, String comment) {
        CheckpointConfig config = getConfig(configId);
        config.setReviewStatus(approved ? "REVIEWED" : "REJECTED");
        config.setReviewedBy(SecurityUtils.getCurrentUserId());
        config.setReviewedAt(LocalDateTime.now());
        configMapper.updateById(config);
    }

    @Override
    @Transactional
    public int adminBatchApprove(Long subjectId) {
        List<CheckpointConfig> drafts = configMapper.selectList(
            new LambdaQueryWrapper<CheckpointConfig>()
                .eq(CheckpointConfig::getSubjectId, subjectId)
                .eq(CheckpointConfig::getReviewStatus, "DRAFT"));
        if (drafts.isEmpty()) return 0;

        Long userId = SecurityUtils.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        for (CheckpointConfig c : drafts) {
            c.setReviewStatus("REVIEWED");
            c.setReviewedBy(userId);
            c.setReviewedAt(now);
            configMapper.updateById(c);
        }
        return drafts.size();
    }

    // ═══════════════════ 白名单 ═══════════════════

    @Override
    public boolean isStudentInWhitelist(Long userId) {
        Map<String, String> settings = systemService.getAllSettings();
        String ids = settings.getOrDefault("feature.checkpoint_class_ids", "");
        if (ids == null || ids.isBlank()) return true;

        Student student = studentMapper.selectOne(
            new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId));
        if (student == null || student.getClassId() == null) return false;

        Set<Long> allowed = new HashSet<>();
        for (String part : ids.split(",")) {
            try { allowed.add(Long.parseLong(part.trim())); } catch (NumberFormatException ignored) { log.debug("班级ID解析失败: {}", part); }
        }
        return allowed.contains(student.getClassId());
    }

    // ═══════════════════ 积分消耗（乐观锁扣减）══════════════════

    @Override
    public boolean consumeCredits(Long studentId, int amount, String reason) {
        if (studentId == null || amount <= 0) return false;
        int updated = studentMapper.update(null,
            new LambdaUpdateWrapper<Student>()
                .eq(Student::getId, studentId)
                .ge(Student::getTotalCredits, amount)
                .setSql("total_credits = total_credits - " + amount));
        if (updated > 0) {
            Student refreshed = studentMapper.selectById(studentId);
            CreditTransaction ct = new CreditTransaction();
            ct.setStudentId(studentId);
            ct.setTransactionType("spend");
            ct.setCreditAmount(amount);
            ct.setBalanceAfter(refreshed != null && refreshed.getTotalCredits() != null ? refreshed.getTotalCredits() : 0);
            ct.setSourceType("BEHAVIOR");
            ct.setDescription(reason);
            ct.setBizKey(reason);
            ct.setCreateTime(LocalDateTime.now());
            CreditTransaction existing = creditTransactionMapper.selectOne(
                new LambdaQueryWrapper<CreditTransaction>()
                    .eq(CreditTransaction::getBizKey, reason)
                    .last("LIMIT 1"));
            if (existing == null) {
                creditTransactionMapper.insert(ct);
            }
            log.info("积分消耗成功 studentId={} amount={} reason={}", studentId, amount, reason);
            return true;
        }
        log.warn("积分消耗失败(余额不足) studentId={} amount={}", studentId, amount);
        return false;
    }

    // ═══════════════════ SOS & 追问 ═══════════════════

    @Override
    public void sendSOS(Long studentId, Long configId) {
        CheckpointConfig config = getConfig(configId);
        Student student = studentMapper.selectById(studentId);
        String studentName = "学生#" + studentId;
        if (student != null && student.getUserId() != null) {
            com.school.teaching.entity.User user = userMapper.selectById(student.getUserId());
            if (user != null && user.getRealName() != null) studentName = user.getRealName();
        }

        String title = "学生卡关求助";
        String content = "「" + studentName + "」在关卡「" + config.getTaskName()
            + "」(" + config.getChapterName() + ")遇到困难，请关注";

        Classes clazz = student != null && student.getClassId() != null
            ? classesMapper.selectById(student.getClassId()) : null;
        if (clazz != null && clazz.getHeadTeacherId() != null) {
            try {
                notificationService.notify(clazz.getHeadTeacherId(), "CHECKPOINT_SOS", title, content, configId);
                log.info("SOS通知已发送 studentId={} teacherId={}", studentId, clazz.getHeadTeacherId());
            } catch (Exception e) {
                log.warn("SOS通知失败 studentId={}", studentId, e);
            }
        }

        if (student != null && student.getClassId() != null && config.getSubjectId() != null) {
            try {
                List<TeacherClass> tcs = teacherClassMapper.selectList(
                    new LambdaQueryWrapper<TeacherClass>()
                        .eq(TeacherClass::getClassId, student.getClassId()));
                for (TeacherClass tc : tcs) {
                    if (tc.getSubject() != null
                        && ("," + tc.getSubject() + ",").contains("," + config.getSubjectId() + ",")) {
                        Teacher teacher = teacherMapper.selectById(tc.getTeacherId());
                        if (teacher != null && teacher.getUserId() != null
                            && !teacher.getUserId().equals(clazz != null ? clazz.getHeadTeacherId() : null)) {
                            notificationService.notify(teacher.getUserId(), "CHECKPOINT_SOS",
                                title + "（学科教师）", content, configId);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("SOS通知学科教师失败 studentId={}", studentId, e);
            }
        }
    }

    @Override
    public void recordFollowup(Long studentId, Long configId, int keywordIndex, boolean correct) {
        CheckpointKeywordLog log = keywordLogMapper.selectOne(
            new LambdaQueryWrapper<CheckpointKeywordLog>()
                .eq(CheckpointKeywordLog::getStudentId, studentId)
                .eq(CheckpointKeywordLog::getConfigId, configId)
                .eq(CheckpointKeywordLog::getKeywordIndex, keywordIndex)
                .orderByDesc(CheckpointKeywordLog::getId)
                .last("LIMIT 1"));
        if (log != null) {
            log.setFollowupCorrect(correct ? 1 : 0);
            keywordLogMapper.updateById(log);
        }
    }

    // ═══════════════════ 内部辅助 ═══════════════════

    private CheckpointConfig getConfig(Long configId) {
        CheckpointConfig config = configMapper.selectById(configId);
        if (config == null) throw new BusinessException(404, "闯关配置不存在");
        return config;
    }

    private CheckpointProgress getProgress(Long studentId, Long configId) {
        return progressMapper.selectOne(
            new LambdaQueryWrapper<CheckpointProgress>()
                .eq(CheckpointProgress::getStudentId, studentId)
                .eq(CheckpointProgress::getConfigId, configId)
                .last("LIMIT 1"));
    }

    private CheckpointProgress getOrCreateProgress(Long studentId, Long configId, Long subjectId) {
        CheckpointProgress p = getProgress(studentId, configId);
        if (p == null) {
            p = new CheckpointProgress();
            p.setStudentId(studentId);
            p.setConfigId(configId);
            p.setSubjectId(subjectId);
            progressMapper.insert(p);
            // 重新查询获取数据库默认值（keywordsPassed=0 等）
            p = progressMapper.selectById(p.getId());
        }
        return p;
    }

    private List<KnowledgeNode> loadChildKnowledgeNodes(Long parentNodeId) {
        if (parentNodeId == null) return List.of();
        return knowledgeNodeMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getParentId, parentNodeId)
                .eq(KnowledgeNode::getLevel, 4)
                .orderByAsc(KnowledgeNode::getSortOrder));
    }

    private List<Map<String, Object>> loadNodePracticeQuestions(Long nodeId) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (nodeId == null) return result;
        List<QuestionBank> qs = questionBankMapper.selectList(
            new LambdaQueryWrapper<QuestionBank>()
                .eq(QuestionBank::getCategoryId, nodeId)
                .eq(QuestionBank::getStatus, 1)
                .last("LIMIT 2"));
        for (QuestionBank qb : qs) {
            result.add(questionToMap(qb));
        }
        return result;
    }

    /**
     * 从所有子知识点题库抽取验证题，确保每个知识点至少一道：
     * 第1轮：每个 node 取 1 道（easy）
     * 第2轮：每个 node 再取 1 道（补齐到 node 数 × 2）
     * 最终 shuffle 打乱题序
     */
    private List<Map<String, Object>> loadVerificationQuestions(
            List<KnowledgeNode> childNodes, String previousQuestionIdsJson, CheckpointConfig config) {
        List<Map<String, Object>> all = new ArrayList<>();
        Set<Long> previousIds = previousQuestionIdsJson != null
            ? parseQuestionIdSet(previousQuestionIdsJson) : Collections.emptySet();
        Set<Long> addedIds = new HashSet<>(previousIds);

        Integer dl = config != null ? config.getDifficultyLevel() : 3;
        int[] targets = calcDifficultyRatio(dl != null ? dl : 3, childNodes.size() * 2);
        int minTotal = Math.max(childNodes.size(), 5);

        Map<Integer, int[]> diffMap = Map.of(1, new int[]{1}, 2, new int[]{2, 3}, 3, new int[]{4, 5});
        for (int level = 1; level <= 3; level++) {
            int[] diffs = diffMap.get(level);
            int target = targets[level - 1];
            int picked = 0;

            for (KnowledgeNode node : childNodes) {
                if (picked >= target) break;
                boolean found = false;
                List<QuestionBank> qs = questionBankMapper.selectList(
                    new LambdaQueryWrapper<QuestionBank>()
                        .eq(QuestionBank::getCategoryId, node.getId())
                        .eq(QuestionBank::getStatus, 1)
                        .in(QuestionBank::getDifficultyLevel, diffs)
                        .last("LIMIT 3"));
                for (QuestionBank qb : qs) {
                    if (!addedIds.contains(qb.getId())) {
                        addedIds.add(qb.getId());
                        all.add(questionToMap(qb));
                        picked++;
                        found = true;
                        break;
                    }
                }
                if (!found && node.getParentId() != null) {
                    List<QuestionBank> pqs = questionBankMapper.selectList(
                        new LambdaQueryWrapper<QuestionBank>()
                            .eq(QuestionBank::getCategoryId, node.getParentId())
                            .eq(QuestionBank::getStatus, 1)
                            .in(QuestionBank::getDifficultyLevel, diffs)
                            .last("LIMIT 3"));
                    for (QuestionBank qb : pqs) {
                        if (!addedIds.contains(qb.getId())) {
                            addedIds.add(qb.getId());
                            all.add(questionToMap(qb));
                            picked++;
                            break;
                        }
                    }
                }
            }
        }

        if (all.size() < minTotal && !childNodes.isEmpty()) {
            Long parentId = childNodes.get(0).getParentId();
            if (parentId != null) {
                List<QuestionBank> fallback = questionMatchingService.matchSingleNode(
                    parentId, null, minTotal - all.size());
                for (QuestionBank qb : fallback) {
                    if (!addedIds.contains(qb.getId())) {
                        addedIds.add(qb.getId());
                        all.add(questionToMap(qb));
                        if (all.size() >= minTotal) break;
                    }
                }
            }
        }

        return all;
    }

    private List<Map<String, Object>> loadInterleavingNodes(Long studentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Long> weakNodeIds = precisionProgressService.findWeakNodeIds(
            studentId, BigDecimal.valueOf(0.60));
        if (weakNodeIds.isEmpty()) return result;

        for (Long nodeId : weakNodeIds) {
            List<QuestionBank> qs = questionBankMapper.selectList(
                new LambdaQueryWrapper<QuestionBank>()
                    .eq(QuestionBank::getCategoryId, nodeId)
                    .eq(QuestionBank::getStatus, 1)
                    .last("LIMIT 1"));
            for (QuestionBank qb : qs) {
                result.add(questionToMap(qb));
            }
        }
        Collections.shuffle(result);
        return result;
    }

    private Long getNodeIdByKeywordIndex(Long configId, int keywordIndex) {
        Map<Integer, Long> mapping = keywordIndexToNodeIdCache.get(configId);
        return mapping != null ? mapping.get(keywordIndex) : null;
    }

    private Map<Integer, Boolean> loadPreviouslyCorrectKeywords(Long studentId, Long configId, int totalKw) {
        Map<Integer, Boolean> result = new LinkedHashMap<>();
        List<CheckpointKeywordLog> logs = keywordLogMapper.selectList(
            new LambdaQueryWrapper<CheckpointKeywordLog>()
                .eq(CheckpointKeywordLog::getStudentId, studentId)
                .eq(CheckpointKeywordLog::getConfigId, configId)
                .eq(CheckpointKeywordLog::getIsCorrect, 1));
        for (CheckpointKeywordLog log : logs) {
            result.put(log.getKeywordIndex(), true);
        }
        return result;
    }

    private Set<Long> getTargetNodeIds(CheckpointConfig config) {
        Set<Long> nodeIds = new LinkedHashSet<>();
        if ("MIXED".equals(config.getCheckpointType()) && config.getParentConfigId() != null) {
            try {
                List<Long> childConfigIds = JSON.readValue(config.getParentConfigId(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<Long>>() {});
                for (Long cid : childConfigIds) {
                    CheckpointConfig child = configMapper.selectById(cid);
                    if (child != null && child.getTaskNodeId() != null) {
                        nodeIds.add(child.getTaskNodeId());
                    }
                }
            } catch (Exception e) {
                log.warn("解析 parentConfigId 失败 configId={}", config.getId(), e);
            }
        }
        if (nodeIds.isEmpty() && config.getTaskNodeId() != null) {
            nodeIds.add(config.getTaskNodeId());
        }
        return nodeIds;
    }

    private List<Map<String, Object>> drawQuestions(CheckpointConfig config, Set<Long> excludeIds) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (config.getTaskNodeId() == null) return result;

        try {
            List<QuestionBank> matched;
            if ("MIXED".equals(config.getCheckpointType())) {
                matched = new ArrayList<>();
                Set<Long> targetNodeIds = getTargetNodeIds(config);
                int totalCount = config.getQuestionCount() != null ? config.getQuestionCount() : 10;
                int perNode = Math.max(1, totalCount / Math.max(targetNodeIds.size(), 1));
                for (Long nid : targetNodeIds) {
                    matched.addAll(questionBankMapper.selectList(
                        new LambdaQueryWrapper<QuestionBank>()
                            .eq(QuestionBank::getCategoryId, nid)
                            .eq(QuestionBank::getStatus, 1)
                            .last("LIMIT " + perNode)));
                }
            } else {
                matched = questionMatchingService.matchSingleNode(
                    config.getTaskNodeId(), null, Math.max(config.getQuestionCount() + excludeIds.size(), 10));
            }
            for (QuestionBank qb : matched) {
                if (excludeIds.contains(qb.getId())) continue;
                Map<String, Object> q = questionToMap(qb);
                result.add(q);
                if (result.size() >= config.getQuestionCount()) break;
            }
        } catch (Exception e) {
            log.warn("题库匹配失败 configId={}", config.getId(), e);
        }
        return result;
    }

    private List<Map<String, Object>> drawQuestionsWithFallback(CheckpointConfig config, Set<Long> excludeIds) {
        // 当 drawQuestions 不足时，尝试扩大匹配范围（不加 limit 限制）
        List<Map<String, Object>> result = drawQuestions(config, excludeIds);
        if (result.size() >= config.getQuestionCount()) return result;

        // 最后一次尝试：忽略 excludeIds，能拿到多少是多少
        log.warn("题库严重不足 configId={} nodeId={} need={} got={}, 建议配置AI兜底",
            config.getId(), config.getTaskNodeId(), config.getQuestionCount(), result.size());
        return result;
    }

    // ═══════════════════ 交错练习辅助 ═══════════════════

    private List<Map<String, Object>> drawInterleavedQuestions(CheckpointConfig config,
            Long studentId, Set<Long> excludeIds, Long subjectId) {
        int totalCount = config.getQuestionCount() != null ? config.getQuestionCount() : 10;
        List<Map<String, Object>> result = new ArrayList<>();
        Set<Long> allExclude = new HashSet<>(excludeIds);

        Set<Long> currentNodeIds = new HashSet<>();
        if ("MIXED".equals(config.getCheckpointType())) {
            currentNodeIds = getTargetNodeIds(config);
        } else if (config.getTaskNodeId() != null) {
            currentNodeIds.add(config.getTaskNodeId());
        }

        int currentCount = Math.max(1, totalCount * 50 / 100);
        List<Map<String, Object>> currentQs = drawQuestionsFromNodes(
            currentNodeIds, currentCount, allExclude, config.getDifficultyLevel());
        result.addAll(currentQs);

        List<Long> prevNodeIds = getPreviousCheckpointNodeIds(subjectId, config.getSeq(), 3);
        if (!prevNodeIds.isEmpty()) {
            int prevCount = Math.max(1, totalCount * 30 / 100);
            Set<Long> prevSet = new HashSet<>(prevNodeIds);
            prevSet.removeAll(currentNodeIds);
            if (!prevSet.isEmpty()) {
                List<Map<String, Object>> prevQs = drawQuestionsFromNodes(
                    prevSet, prevCount, allExclude, 3);
                result.addAll(prevQs);
            }
        }

        int randomCount = totalCount - result.size();
        if (randomCount > 0) {
            List<Long> randomNodeIds = getRandomNodeIds(subjectId, currentNodeIds, new HashSet<>(prevNodeIds));
            if (!randomNodeIds.isEmpty()) {
                Set<Long> randomSet = new HashSet<>(randomNodeIds);
                randomSet.removeAll(currentNodeIds);
                randomSet.removeAll(prevNodeIds);
                if (!randomSet.isEmpty()) {
                    List<Map<String, Object>> randomQs = drawQuestionsFromNodes(
                        randomSet, randomCount, allExclude, 2);
                    result.addAll(randomQs);
                }
            }
        }

        if (result.size() < totalCount) {
            int need = totalCount - result.size();
            List<Map<String, Object>> fillQs = drawQuestionsFromNodes(
                currentNodeIds, need, allExclude, config.getDifficultyLevel());
            result.addAll(fillQs);
        }

        Collections.shuffle(result);
        return result;
    }

    private int[] calcDifficultyRatio(int difficultyLevel, int total) {
        int easyPct, mediumPct, hardPct;
        switch (difficultyLevel) {
            case 1: easyPct = 60; mediumPct = 30; hardPct = 10; break;
            case 2: easyPct = 40; mediumPct = 40; hardPct = 20; break;
            case 3: easyPct = 25; mediumPct = 50; hardPct = 25; break;
            case 4: easyPct = 15; mediumPct = 45; hardPct = 40; break;
            case 5: easyPct = 10; mediumPct = 30; hardPct = 60; break;
            default: easyPct = 25; mediumPct = 50; hardPct = 25;
        }
        return new int[]{
            Math.max(1, total * easyPct / 100),
            Math.max(1, total * mediumPct / 100),
            Math.max(1, total * hardPct / 100)
        };
    }

    private List<Map<String, Object>> drawQuestionsFromNodes(
            Set<Long> nodeIds, int targetCount, Set<Long> excludeIds, Integer difficultyLevel) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (nodeIds.isEmpty() || targetCount <= 0) return result;

        int[] targets = calcDifficultyRatio(difficultyLevel != null ? difficultyLevel : 3, targetCount);
        int perNode = Math.max(1, targetCount / nodeIds.size());

        for (int diff = 1; diff <= 3 && result.size() < targetCount; diff++) {
            int remaining = targets[diff - 1];
            if (remaining <= 0) continue;

            for (Long nodeId : nodeIds) {
                if (result.size() >= targetCount || remaining <= 0) break;
                List<QuestionBank> qs = questionBankMapper.selectList(
                    new LambdaQueryWrapper<QuestionBank>()
                        .eq(QuestionBank::getCategoryId, nodeId)
                        .eq(QuestionBank::getStatus, 1)
                        .eq(QuestionBank::getDifficultyLevel, diff)
                        .last("LIMIT 3"));
                for (QuestionBank qb : qs) {
                    if (excludeIds.contains(qb.getId())) continue;
                    excludeIds.add(qb.getId());
                    result.add(questionToMap(qb));
                    remaining--;
                    break;
                }
            }

            if (remaining > 0) {
                for (Long nodeId : nodeIds) {
                    if (result.size() >= targetCount || remaining <= 0) break;
                    List<QuestionBank> qs = questionBankMapper.selectList(
                        new LambdaQueryWrapper<QuestionBank>()
                            .eq(QuestionBank::getCategoryId, nodeId)
                            .eq(QuestionBank::getStatus, 1)
                            .eq(QuestionBank::getDifficultyLevel, diff)
                            .last("LIMIT 5"));
                    for (QuestionBank qb : qs) {
                        if (excludeIds.contains(qb.getId())) continue;
                        excludeIds.add(qb.getId());
                        result.add(questionToMap(qb));
                        remaining--;
                        if (remaining <= 0) break;
                    }
                }
            }
        }

        return result;
    }

    private List<Long> getPreviousCheckpointNodeIds(Long subjectId, int currentSeq, int count) {
        List<CheckpointConfig> prevConfigs = configMapper.selectList(
            new LambdaQueryWrapper<CheckpointConfig>()
                .eq(CheckpointConfig::getSubjectId, subjectId)
                .eq(CheckpointConfig::getStatus, 1)
                .lt(CheckpointConfig::getSeq, currentSeq)
                .orderByDesc(CheckpointConfig::getSeq)
                .last("LIMIT " + count));
        return prevConfigs.stream()
            .map(CheckpointConfig::getTaskNodeId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private List<Long> getRandomNodeIds(Long subjectId, Set<Long> excludeNodeIds, Set<Long> extraExclude) {
        Set<Long> allExclude = new HashSet<>(excludeNodeIds);
        allExclude.addAll(extraExclude);
        List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getSubjectId, subjectId)
                .eq(KnowledgeNode::getLevel, 3)
                .notIn(!allExclude.isEmpty(), KnowledgeNode::getId, allExclude)
                .orderByAsc(KnowledgeNode::getId)
                .last("LIMIT 5"));
        return nodes.stream().map(KnowledgeNode::getId).collect(Collectors.toList());
    }

    private int countByDifficulty(List<Map<String, Object>> questions, int difficultyLevel) {
        return (int) questions.stream()
            .filter(q -> {
                Object dl = q.get("difficultyLevel");
                return dl != null && dl.toString().equals(String.valueOf(difficultyLevel));
            })
            .count();
    }

    private Map<String, Object> questionToMap(QuestionBank qb) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", qb.getId());
        map.put("questionType", qb.getQuestionType());
        map.put("questionText", qb.getQuestionText());
        map.put("correctAnswer", qb.getCorrectAnswer() != null ? qb.getCorrectAnswer() : "");
        map.put("explanation", qb.getExplanation() != null ? qb.getExplanation() : "");
        try {
            String optsJson = qb.getOptions();
            // TRUE_FALSE 无选项时注入默认选项（与 PrecisionServiceImpl 注入逻辑一致）
            if ("TRUE_FALSE".equals(qb.getQuestionType()) && (optsJson == null || optsJson.isBlank() || "[]".equals(optsJson.trim()))) {
                map.put("options", List.of("A. √", "B. ×"));
            } else {
                map.put("options", optsJson != null ? new ObjectMapper().readValue(optsJson, List.class) : List.of());
            }
        } catch (Exception e) {
            map.put("options", List.of());
        }
        map.put("difficultyLevel", qb.getDifficultyLevel());
        return map;
    }

    private Set<Long> getPreviousQuestionIds(CheckpointProgress progress) {
        if (progress == null || progress.getQuestionIds() == null) return Collections.emptySet();
        try {
            List<Integer> list = JSON.readValue(progress.getQuestionIds(), new TypeReference<List<Integer>>() {});
            Set<Long> set = new HashSet<>();
            for (Integer i : list) set.add(i.longValue());
            return set;
        } catch (Exception ignored) {
            return Collections.emptySet();
        }
    }

    private int grantBossCredit(Long studentId, CheckpointConfig config, double rate) {
        String bizKey = "checkpoint_boss:" + studentId + ":" + config.getId();
        int amount = rateToCreditLevel(rate);
        try {
            creditService.awardMoralCredit(studentId, amount, bizKey);
            return amount;
        } catch (Exception e) {
            return 0;
        }
    }

    private void recordWrongQuestionManager(Long studentId, Long questionId, Long configId, boolean isCorrect) {
        if (isCorrect) {
            WrongQuestion existing = wrongQuestionMapper.selectOne(
                new LambdaQueryWrapper<WrongQuestion>()
                    .eq(WrongQuestion::getStudentId, studentId)
                    .eq(WrongQuestion::getQuestionId, questionId));
            if (existing != null) {
                existing.setIsMastered(1);
                existing.setMasteredAt(LocalDateTime.now());
                existing.setMasteredSource("checkpoint");
                wrongQuestionMapper.updateById(existing);
            }
        } else {
            recordWrongQuestion(studentId, questionId, configId);
        }
    }
}
