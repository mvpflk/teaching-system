package com.school.teaching.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskComparisonService {

    private final StudentAnswerMapper answerMapper;
    private final TaskQuestionMapper taskQuestionMapper;
    private final QuestionBankMapper questionBankMapper;
    private final TaskSubmissionMapper submissionMapper;
    private final StudentMapper studentMapper;
    private final KnowledgeNodeMapper nodeMapper;
    private final ClassesMapper classMapper;
    private final TaskMapper taskMapper;
    private final UserMapper userMapper;
    private final DictSubjectMapper dictSubjectMapper;
    private final AiOutputMapper aiOutputMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 单任务对比（原始入口） */
    public Map<String, Object> compareClasses(Long taskId) {
        return compareMultiTasks(List.of(taskId));
    }

    /**
     * 多任务聚合对比 — 同一试卷拆成多个任务布置给不同班级时使用。
     * 核心逻辑：按 question_id（question_bank）合并，按 class_id 分班统计。
     */
    public Map<String, Object> compareMultiTasks(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) return Map.of("message", "请选择任务");

        // 任务元数据
        List<Task> tasks = taskMapper.selectBatchIds(taskIds);
        if (tasks == null || tasks.isEmpty()) return Map.of("message", "任务不存在");
        String taskTitle = tasks.get(0).getTitle() != null ? tasks.get(0).getTitle() : "任务组";
        String taskSubject = tasks.get(0).getSubject() != null ? tasks.get(0).getSubject() : "";
        String idsStr = taskIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        // 跨任务加载题目关联（按 question_id 去重）
        List<TaskQuestion> allTq = taskQuestionMapper.selectList(
            new LambdaQueryWrapper<TaskQuestion>().in(TaskQuestion::getTaskId, taskIds));
        if (allTq.isEmpty()) return Map.of("taskIds", idsStr, "taskTitle", taskTitle, "subject", taskSubject,
            "message", "没有题目", "classes", List.of(), "perQuestion", List.of(), "perKp", List.of(), "highlightedKps", List.of(),
            "_kpDataQuality", Map.of("totalQuestions", 0, "directMapped", 0, "estimatedMapped", 0, "l3NodesWithRoundRobin", 0));

        // 题目去重：同一 questionId 只保留第一条（不同任务可能引用相同题目）
        Map<Long, TaskQuestion> uniqueTq = new LinkedHashMap<>();
        for (TaskQuestion tq : allTq) {
            uniqueTq.putIfAbsent(tq.getQuestionId(), tq);
        }
        List<Long> allQIds = new ArrayList<>(uniqueTq.keySet());
        Map<Long, QuestionBank> qMap = new HashMap<>();
        List<QuestionBank> qList = questionBankMapper.selectBatchIds(allQIds);
        if (qList != null) for (QuestionBank q : qList) qMap.put(q.getId(), q);

        // 跨任务加载提交
        List<TaskSubmission> submissions = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>().in(TaskSubmission::getTaskId, taskIds)
                .in(TaskSubmission::getStatus, "SUBMITTED", "GRADED"));
        if (submissions.isEmpty()) return Map.of("taskIds", idsStr, "taskTitle", taskTitle, "subject", taskSubject,
            "message", "暂无提交记录", "classes", List.of(), "perQuestion", List.of(), "perKp", List.of(), "highlightedKps", List.of(),
            "_kpDataQuality", Map.of("totalQuestions", 0, "directMapped", 0, "estimatedMapped", 0, "l3NodesWithRoundRobin", 0));

        // 学生→班级映射
        List<Long> allSids = submissions.stream().map(TaskSubmission::getStudentId).distinct().toList();
        Map<Long, Long> studentClassMap = new HashMap<>();
        List<Student> students = studentMapper.selectBatchIds(allSids);
        if (students != null) {
            for (Student s : students) {
                if (s.getClassId() != null) studentClassMap.put(s.getId(), s.getClassId());
            }
        }

        // 按班级分组学生（去重：同一学生多次提交只保留一个）
        Map<Long, Set<Long>> classStudents = new LinkedHashMap<>();
        Map<Long, Long> sToSid = new HashMap<>();  // studentId → submission.studentId (for identity)
        for (TaskSubmission sub : submissions) {
            Long cid = studentClassMap.get(sub.getStudentId());
            if (cid != null) {
                classStudents.computeIfAbsent(cid, k -> new LinkedHashSet<>()).add(sub.getStudentId());
                sToSid.put(sub.getStudentId(), sub.getStudentId());
            }
        }

        // submissionId → studentId
        Map<Long, Long> subStudentMap = new HashMap<>();
        for (TaskSubmission sub : submissions) subStudentMap.put(sub.getId(), sub.getStudentId());

        // 跨任务加载答题
        List<StudentAnswer> answers = answerMapper.selectList(
            new LambdaQueryWrapper<StudentAnswer>().in(StudentAnswer::getTaskId, taskIds));

        // === 课题组别加载（E7: 实验班vs对照班） ===
        Map<Long, String> classResearchGroups = new LinkedHashMap<>();
        for (Long cid : classStudents.keySet()) {
            Classes cls = classMapper.selectById(cid);
            if (cls != null && cls.getResearchGroup() != null && !cls.getResearchGroup().isEmpty()) {
                classResearchGroups.put(cid, cls.getResearchGroup());
            }
        }

        // === 班级概览 ===
        // 使用任务 passRate（如果启用），回退 60
        double passThreshold = !tasks.isEmpty() && tasks.get(0).getPassRate() != null && tasks.get(0).getPassRate() > 0
            ? (tasks.get(0).getTotalScore() != null ? tasks.get(0).getTotalScore().doubleValue() * tasks.get(0).getPassRate() / 100.0 : 60)
            : 60;
        List<Map<String, Object>> classInfos = new ArrayList<>();
        for (Map.Entry<Long, Set<Long>> entry : classStudents.entrySet()) {
            Long cid = entry.getKey();
            Set<Long> clsUniqueSids = new HashSet<>(entry.getValue());
            double totalScore = 0;
            int passCount = 0;
            for (Long sid : clsUniqueSids) {
                TaskSubmission bestSub = null;
                for (TaskSubmission sub : submissions) {
                    if (sub.getStudentId().equals(sid) && (bestSub == null
                        || (sub.getScore() != null && (bestSub.getScore() == null
                        || sub.getScore().doubleValue() > bestSub.getScore().doubleValue())))) {
                        bestSub = sub;
                    }
                }
                if (bestSub != null && bestSub.getScore() != null) {
                    totalScore += bestSub.getScore().doubleValue();
                    if (bestSub.getScore().doubleValue() >= passThreshold) passCount++;
                }
            }
            int cnt = clsUniqueSids.size();
            double avg = cnt == 0 ? 0 : totalScore / cnt;
            double pass = cnt == 0 ? 0 : (double) passCount / cnt * 100;
            Map<String, Object> ci = new LinkedHashMap<>();
            ci.put("classId", cid);
            ci.put("className", resolveClassName(cid));
            ci.put("avgScore", Math.round(avg * 10) / 10.0);
            ci.put("passRate", Math.round(pass * 10) / 10.0);
            ci.put("studentCount", cnt);
            ci.put("researchGroup", classResearchGroups.getOrDefault(cid, ""));
            classInfos.add(ci);
        }

        // === 逐题正确率（按 questionId 合并，按班级统计） ===
        List<Map<String, Object>> perQuestion = new ArrayList<>();
        int qIdx = 0;
        for (Map.Entry<Long, TaskQuestion> tqEntry : uniqueTq.entrySet()) {
            Long qbId = tqEntry.getKey();
            QuestionBank q = qMap.get(qbId);
            if (q == null) continue;
            qIdx++;
            Map<String, Object> qi = new LinkedHashMap<>();
            qi.put("qId", qbId);
            qi.put("questionText", q.getQuestionText() != null ? q.getQuestionText() : "");
            qi.put("questionType", q.getQuestionType());
            qi.put("qIndex", qIdx);
            // 选项列表
            List<String> opts = parseOptionsJson(q.getOptions());
            qi.put("options", opts);
            // 正确答案
            qi.put("correctAnswer", q.getCorrectAnswer() != null ? q.getCorrectAnswer() : "");
            // 高频错答 TOP3
            qi.put("topWrongAnswers", computeTopWrongAnswers(qbId, q.getCorrectAnswer(), opts, answers, subStudentMap));
            List<Map<String, Object>> qClasses = new ArrayList<>();
            for (Map.Entry<Long, Set<Long>> entry : classStudents.entrySet()) {
                Long cid = entry.getKey();
                Set<Long> clsSids = new HashSet<>(entry.getValue());
                int correct = 0, total = 0;
                for (StudentAnswer sa : answers) {
                    Long saSid = subStudentMap.get(sa.getSubmissionId());
                    if (saSid == null || !clsSids.contains(saSid)) continue;
                    if (qbId.equals(sa.getQuestionId())) {
                        total++;
                        if (sa.getIsCorrect() != null && sa.getIsCorrect() == 1) correct++;
                    }
                }
                Map<String, Object> cr = new LinkedHashMap<>();
                cr.put("classId", cid);
                cr.put("correctRate", total == 0 ? 0 : Math.round((double) correct / total * 1000) / 10.0);
                qClasses.add(cr);
            }
            qi.put("classes", qClasses);
            // 诊断日志：输出前3题的选项和答案数据
            if (qIdx <= 3) {
                log.info("perQuestion[{}] qId={} type={} optsCount={} correctAnswer='{}' topWrongCount={}",
                    qIdx, qbId, q.getQuestionType(), opts.size(),
                    qi.get("correctAnswer"),
                    ((List<?>)qi.get("topWrongAnswers")).size());
            }
            perQuestion.add(qi);
        }

        // === 知识点正确率（展开到L4叶子节点） ===
        Map<Long, Long> qKpMap;
        Map<Long, KnowledgeNode> nodeMap = new HashMap<>();  // 移出块作用域，供后续数据质量判断使用
        {
            Set<Long> rawCids = new HashSet<>();
            for (TaskQuestion tq : uniqueTq.values()) {
                QuestionBank qb = qMap.get(tq.getQuestionId());
                if (qb != null && qb.getCategoryId() != null) rawCids.add(qb.getCategoryId());
            }
            Long subjectId = null;
            if (taskSubject != null && !taskSubject.isEmpty()) {
                DictSubject ds = dictSubjectMapper.selectOne(
                    new LambdaQueryWrapper<DictSubject>().eq(DictSubject::getSubjectName, taskSubject));
                if (ds != null) subjectId = ds.getId();
            }
            LambdaQueryWrapper<KnowledgeNode> treeW = new LambdaQueryWrapper<KnowledgeNode>();
            if (subjectId != null) treeW.eq(KnowledgeNode::getSubjectId, subjectId);
            List<KnowledgeNode> allTreeNodes = nodeMapper.selectList(treeW);
            Map<Long, List<KnowledgeNode>> childrenMap = new HashMap<>();
            if (allTreeNodes != null) {
                for (KnowledgeNode n : allTreeNodes) {
                    nodeMap.put(n.getId(), n);
                    if (n.getParentId() != null) childrenMap.computeIfAbsent(n.getParentId(), k -> new ArrayList<>()).add(n);
                }
            }
            qKpMap = expandToL4(qMap, uniqueTq.keySet(), nodeMap, childrenMap);
        }

        Map<Long, Map<Long, int[]>> kpStats = new LinkedHashMap<>();
        for (Map.Entry<Long, TaskQuestion> tqEntry : uniqueTq.entrySet()) {
            Long qbId = tqEntry.getKey();
            Long kpId = qKpMap.get(qbId);
            if (kpId == null) continue;
            kpStats.computeIfAbsent(kpId, k -> new LinkedHashMap<>());
            for (Map.Entry<Long, Set<Long>> entry : classStudents.entrySet()) {
                Long cid = entry.getKey();
                int[] stats = kpStats.get(kpId).computeIfAbsent(cid, k -> new int[]{0, 0});
                for (StudentAnswer sa : answers) {
                    Long saSid = subStudentMap.get(sa.getSubmissionId());
                    if (saSid == null || !entry.getValue().contains(saSid)) continue;
                    if (qbId.equals(sa.getQuestionId())) {
                        stats[1]++;
                        if (sa.getIsCorrect() != null && sa.getIsCorrect() == 1) stats[0]++;
                        break;
                    }
                }
            }
        }

        // 构建反向映射：kpId → 题目ID列表（用于统计题目数量 + 判断映射质量）
        Map<Long, List<Long>> kpToQids = new LinkedHashMap<>();
        for (Map.Entry<Long, Long> e : qKpMap.entrySet()) {
            kpToQids.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }

        List<Map<String, Object>> perKp = new ArrayList<>();
        List<Map<String, Object>> highlighted = new ArrayList<>();
        // 批量预加载知识点名称，避免N+1查询
        Map<Long, String> kpNameMap = new HashMap<>();
        if (!kpStats.isEmpty()) {
            List<KnowledgeNode> nodes = nodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>().in(KnowledgeNode::getId, kpStats.keySet()));
            if (nodes != null) nodes.forEach(n -> kpNameMap.put(n.getId(), n.getName()));
        }

        // F1: 回填逐题分析中的知识点归属（双向可追溯）
        for (Map<String, Object> qi : perQuestion) {
            Long qbId = (Long) qi.get("qId");
            Long kpId = qKpMap.get(qbId);
            if (kpId != null) {
                qi.put("kpId", kpId);
                qi.put("kpName", kpNameMap.getOrDefault(kpId, "知识点" + kpId));
                QuestionBank qb = qMap.get(qbId);
                if (qb != null && kpId.equals(qb.getCategoryId())) {
                    qi.put("_mappingQuality", "direct");
                } else {
                    qi.put("_mappingQuality", "estimated");
                }
            } else {
                qi.put("kpId", null);
                qi.put("kpName", "未知");
                qi.put("_mappingQuality", "unknown");
            }
        }

        // F3: 统计数据质量（直接标注 vs 算法推测）
        int directCount = 0, estimatedCount = 0;
        Set<Long> l3RRNodes = new LinkedHashSet<>();
        for (Map.Entry<Long, TaskQuestion> tqEntry : uniqueTq.entrySet()) {
            Long qbId = tqEntry.getKey();
            QuestionBank qb = qMap.get(qbId);
            if (qb == null || qb.getCategoryId() == null) continue;
            Long kpId = qKpMap.get(qbId);
            if (kpId == null) continue;
            if (kpId.equals(qb.getCategoryId())) {
                directCount++;
            } else {
                estimatedCount++;
                Long rawCid = qb.getCategoryId();
                KnowledgeNode rawNode = nodeMap.get(rawCid);
                if (rawNode != null && rawNode.getLevel() != null && rawNode.getLevel() == 3) {
                    l3RRNodes.add(rawCid);
                }
            }
        }

        for (Map.Entry<Long, Map<Long, int[]>> entry : kpStats.entrySet()) {
            Long kpId = entry.getKey();
            Map<String, Object> kpi = new LinkedHashMap<>();
            kpi.put("kpId", kpId);
            kpi.put("kpName", kpNameMap.getOrDefault(kpId, "知识点" + kpId));
            List<Map<String, Object>> kpClasses = new ArrayList<>();
            for (Map.Entry<Long, int[]> ce : entry.getValue().entrySet()) {
                double rate = ce.getValue()[1] == 0 ? 0 : (double) ce.getValue()[0] / ce.getValue()[1] * 100;
                rate = Math.round(rate * 10) / 10.0;
                Map<String, Object> cr = new LinkedHashMap<>();
                cr.put("classId", ce.getKey());
                cr.put("correctRate", rate);
                cr.put("correct", ce.getValue()[0]);
                cr.put("total", ce.getValue()[1]);
                kpClasses.add(cr);
            }
            kpi.put("classes", kpClasses);
            // F2: 数据质量元数据
            List<Long> qids = kpToQids.getOrDefault(kpId, List.of());
            kpi.put("questionCount", qids.size());
            int totalAns = entry.getValue().values().stream().mapToInt(a -> a[1]).sum();
            kpi.put("totalAnswers", totalAns);
            // 判断映射质量：统计直接标注 vs 算法推测的题目比例
            long directQids = qids.stream().filter(qid -> {
                QuestionBank qb = qMap.get(qid);
                return qb != null && kpId.equals(qb.getCategoryId());
            }).count();
            if (qids.isEmpty()) {
                kpi.put("mappingQuality", "unknown");
            } else if (directQids == qids.size()) {
                kpi.put("mappingQuality", "direct");
            } else if (directQids == 0) {
                kpi.put("mappingQuality", "estimated");
            } else {
                kpi.put("mappingQuality", "mixed");
            }
            perKp.add(kpi);
            if (kpClasses.size() >= 2) {
                double maxRate = kpClasses.stream().mapToDouble(c -> (double) c.get("correctRate")).max().orElse(0);
                double minRate = kpClasses.stream().mapToDouble(c -> (double) c.get("correctRate")).min().orElse(0);
                double delta = maxRate - minRate;
                if (delta > 20) {
                    Map<String, Object> h = new LinkedHashMap<>();
                    h.put("kpId", kpId);
                    h.put("kpName", kpNameMap.getOrDefault(kpId, "知识点" + kpId));
                    h.put("delta", Math.round(delta * 10) / 10.0);
                    h.put("classes", kpClasses);
                    highlighted.add(h);
                }
            }
        }

        // === 课题组别对比（E7: 实验班vs对照班） ===
        boolean hasBothGroups = classResearchGroups.containsValue("EXPERIMENT")
            && classResearchGroups.containsValue("CONTROL");
        Map<String, Object> groupComparison = hasBothGroups
            ? buildGroupComparison(classStudents, submissions, answers, uniqueTq, qMap,
                qKpMap, kpNameMap, classResearchGroups, perKp)
            : null;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskIds", idsStr);
        result.put("taskTitle", taskTitle);
        result.put("subject", taskSubject);
        result.put("classes", classInfos);
        result.put("perQuestion", perQuestion);
        result.put("perKp", perKp);
        result.put("highlightedKps", highlighted);
        // F3: 全局数据质量摘要
        Map<String, Object> kpDataQuality = new LinkedHashMap<>();
        kpDataQuality.put("totalQuestions", allQIds.size());
        kpDataQuality.put("directMapped", directCount);
        kpDataQuality.put("estimatedMapped", estimatedCount);
        kpDataQuality.put("l3NodesWithRoundRobin", l3RRNodes.size());
        result.put("_kpDataQuality", kpDataQuality);
        if (groupComparison != null) result.put("researchGroupComparison", groupComparison);
        return result;
    }

    /**
     * 完整诊断数据 — 对比数据 + 学生明细 + 分数分布 + AI分析文本（可选）
     * 所有定量数据由后端计算（无AI依赖），AI只提供定性分析文本。
     */
    public Map<String, Object> getDiagnosisData(List<Long> taskIds) {
        // 复用多任务对比数据作为基础。
        // 注意：compareMultiTasks 在“无提交/无题目”分支会返回 Map.of()（不可变），
        // 此处用 HashMap 包装，避免后续 base.put(...) 抛 UnsupportedOperationException。
        Map<String, Object> base = new LinkedHashMap<>(compareMultiTasks(taskIds));
        base.put("_type", "diagnosis");

        // 8. 学生明细（含准确的知识点薄弱分析）
        // 注：submissions/answers 与 compareMultiTasks 内部重复查询一次。
        // 复用需改造 compareMultiTasks 的签名（公共方法，被多处调用），超出单次改动范围，暂保留。
        List<TaskSubmission> submissions = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>().in(TaskSubmission::getTaskId, taskIds)
                .in(TaskSubmission::getStatus, "SUBMITTED", "GRADED"));
        List<StudentAnswer> answers = answerMapper.selectList(
            new LambdaQueryWrapper<StudentAnswer>().in(StudentAnswer::getTaskId, taskIds));

        // 学生→提交映射
        Map<Long, TaskSubmission> subByStudent = new LinkedHashMap<>();
        for (TaskSubmission sub : submissions) {
            TaskSubmission existing = subByStudent.get(sub.getStudentId());
            if (existing == null || (sub.getScore() != null && (existing.getScore() == null
                || sub.getScore().doubleValue() > existing.getScore().doubleValue()))) {
                subByStudent.put(sub.getStudentId(), sub);
            }
        }

        // 批量加载学生→班级→姓名
        List<Long> sids = new ArrayList<>(subByStudent.keySet());
        Map<Long, String> studentNames = new HashMap<>();
        Map<Long, Long> studentClassMap = new HashMap<>();
        if (!sids.isEmpty()) {
            List<Student> stus = studentMapper.selectBatchIds(sids);
            if (stus != null) {
                Set<Long> uids = stus.stream().map(Student::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
                Map<Long, String> unames = new HashMap<>();
                if (!uids.isEmpty()) {
                    List<User> users = userMapper.selectBatchIds(uids);
                    if (users != null) for (User u : users) unames.put(u.getId(), u.getRealName());
                }
                for (Student s : stus) {
                    studentNames.put(s.getId(), unames.getOrDefault(s.getUserId(), "学生" + s.getId()));
                    if (s.getClassId() != null) studentClassMap.put(s.getId(), s.getClassId());
                }
            }
        }

        // 题目→知识点映射（解析 category_id → L4 叶子节点，轮询分配）
        Map<Long, Long> qKpMap = new HashMap<>();
        Map<Long, String> kpNameMap = new HashMap<>();
        Set<Long> allQIds = answers.stream().map(StudentAnswer::getQuestionId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (!allQIds.isEmpty()) {
            List<QuestionBank> qbs = questionBankMapper.selectBatchIds(allQIds);
            if (qbs != null) {
                Map<Long, QuestionBank> qMapLocal = qbs.stream().collect(Collectors.toMap(QuestionBank::getId, q -> q));
                String taskSubject = (String) base.get("subject");
                Long subjectId = null;
                if (taskSubject != null && !taskSubject.isEmpty()) {
                    DictSubject ds = dictSubjectMapper.selectOne(
                        new LambdaQueryWrapper<DictSubject>().eq(DictSubject::getSubjectName, taskSubject));
                    if (ds != null) subjectId = ds.getId();
                }
                LambdaQueryWrapper<KnowledgeNode> treeWrapper = new LambdaQueryWrapper<KnowledgeNode>();
                if (subjectId != null) treeWrapper.eq(KnowledgeNode::getSubjectId, subjectId);
                List<KnowledgeNode> allTreeNodes = nodeMapper.selectList(treeWrapper);
                Map<Long, KnowledgeNode> nodeMapLocal = new HashMap<>();
                Map<Long, List<KnowledgeNode>> childrenMapLocal = new HashMap<>();
                if (allTreeNodes != null) {
                    for (KnowledgeNode n : allTreeNodes) {
                        nodeMapLocal.put(n.getId(), n);
                        if (n.getParentId() != null) childrenMapLocal.computeIfAbsent(n.getParentId(), k -> new ArrayList<>()).add(n);
                    }
                }
                qKpMap = expandToL4(qMapLocal, allQIds, nodeMapLocal, childrenMapLocal);

                // 查知识点名称
                Set<Long> finalKpIds = new HashSet<>(qKpMap.values());
                if (!finalKpIds.isEmpty()) {
                    List<KnowledgeNode> kps = nodeMapper.selectBatchIds(finalKpIds);
                    if (kps != null) for (KnowledgeNode kp : kps) kpNameMap.put(kp.getId(), kp.getName());
                }
            }
        }

        // 预建 submissionId → studentId 映射，供逐学生循环 O(1) 查找。
        // 此前用 getStudentIdFromSubmission() 在 O(K×N) 外层循环内做 O(M) 线性查找，
        // 总复杂度 O(K×N×M)（40生×400答×40提交≈64万次），数据翻倍即明显卡顿。
        Map<Long, Long> subToStudent = new HashMap<>();
        for (TaskSubmission sub : submissions) {
            subToStudent.put(sub.getId(), sub.getStudentId());
        }

        // 逐学生统计薄弱知识点
        List<Map<String, Object>> students = new ArrayList<>();
        List<Double> allScores = new ArrayList<>();
        for (Map.Entry<Long, TaskSubmission> entry : subByStudent.entrySet()) {
            Long sid = entry.getKey();
            TaskSubmission sub = entry.getValue();
            String name = studentNames.getOrDefault(sid, "学生" + sid);

            // 该生的错题→知识点（内层 O(1) 查找学生归属）
            Map<Long, int[]> kpWrong = new LinkedHashMap<>(); // kpId → [wrong, total]
            for (StudentAnswer sa : answers) {
                if (!sid.equals(subToStudent.get(sa.getSubmissionId()))) continue;
                Long kpId = qKpMap.get(sa.getQuestionId());
                if (kpId == null) continue;
                int[] st = kpWrong.computeIfAbsent(kpId, k -> new int[]{0, 0});
                st[1]++;
                if (sa.getIsCorrect() == null || sa.getIsCorrect() != 1) st[0]++;
            }

            List<Map<String, Object>> weakKps = new ArrayList<>();
            for (Map.Entry<Long, int[]> e : kpWrong.entrySet()) {
                if (e.getValue()[1] == 0) continue;
                double errRate = (double) e.getValue()[0] / e.getValue()[1] * 100;
                Map<String, Object> wk = new LinkedHashMap<>();
                wk.put("kpId", e.getKey());
                wk.put("kpName", kpNameMap.getOrDefault(e.getKey(), "知识点" + e.getKey()));
                wk.put("errorRate", Math.round(errRate * 10) / 10.0);
                wk.put("wrong", e.getValue()[0]);
                wk.put("total", e.getValue()[1]);
                weakKps.add(wk);
            }
            weakKps.sort((a, b) -> Double.compare((double) b.get("errorRate"), (double) a.get("errorRate")));
            if (weakKps.size() > 3) weakKps = weakKps.subList(0, 3);

            Double score = sub.getScore() != null ? sub.getScore().doubleValue() : null;
            if (score != null) allScores.add(score);

            Map<String, Object> si = new LinkedHashMap<>();
            si.put("studentId", sid);
            si.put("name", name);
            si.put("score", score);
            si.put("classId", studentClassMap.get(sid));
            si.put("className", resolveClassName(studentClassMap.get(sid)));
            si.put("weakPoints", weakKps);
            // 本地阈值标签
            si.put("label", getGradeLabel(score));
            students.add(si);
        }
        // 学生按得分降序
        students.sort((a, b) -> {
            Double sa = (Double) a.getOrDefault("score", -1.0);
            Double sb = (Double) b.getOrDefault("score", -1.0);
            if (sa == null) sa = -1.0; if (sb == null) sb = -1.0;
            return sb.compareTo(sa);
        });
        base.put("students", students);

        // 9. 分数分布（后端计算，不用 AI）
        // 按班级分别计算分布，同时计算全班汇总
        Map<Long, Map<String, Integer>> perClassDistribution = new LinkedHashMap<>();
        if (!allScores.isEmpty()) {
            int cnt90 = 0, cnt75 = 0, cnt60 = 0, cntLow = 0;
            double sum = 0;
            for (double s : allScores) {
                sum += s;
                if (s >= 90) cnt90++;
                else if (s >= 75) cnt75++;
                else if (s >= 60) cnt60++;
                else cntLow++;
            }
            Map<String, Object> dist = new LinkedHashMap<>();
            dist.put("avgScore", Math.round(sum / allScores.size() * 10) / 10.0);
            dist.put("passRate", Math.round((allScores.size() - cntLow) * 1000.0 / allScores.size()) / 10.0);
            dist.put("totalStudents", allScores.size());
            dist.put("distribution", Map.of(
                "90-100", cnt90, "75-89", cnt75, "60-74", cnt60, "<60", cntLow));
            dist.put("labelCounts", Map.of(
                "已达标", cnt90, "成长中", cnt75, "发展中", cnt60, "起步期", cntLow));
            base.put("scoreOverview", dist);

            // 按班级分布：从 studentClassMap 重建 class→students 映射
            Map<Long, List<Long>> clsStudents = new LinkedHashMap<>();
            for (Map.Entry<Long, Long> e : studentClassMap.entrySet()) {
                clsStudents.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
            }
            for (Map.Entry<Long, List<Long>> entry : clsStudents.entrySet()) {
                Long cid = entry.getKey();
                Set<Long> clsSids = new HashSet<>(entry.getValue());
                int c90 = 0, c75 = 0, c60 = 0, cLow = 0;
                for (Long sid : clsSids) {
                    TaskSubmission bestSub = null;
                    for (TaskSubmission sub : submissions) {
                        if (sub.getStudentId().equals(sid) && (bestSub == null
                            || (sub.getScore() != null && (bestSub.getScore() == null
                            || sub.getScore().doubleValue() > bestSub.getScore().doubleValue())))) {
                            bestSub = sub;
                        }
                    }
                    double s = bestSub != null && bestSub.getScore() != null ? bestSub.getScore().doubleValue() : -1;
                    if (s < 0) continue;
                    if (s >= 90) c90++;
                    else if (s >= 75) c75++;
                    else if (s >= 60) c60++;
                    else cLow++;
                }
                perClassDistribution.put(cid, Map.of(
                    "90-100", c90, "75-89", c75, "60-74", c60, "<60", cLow));
            }
            base.put("perClassDistribution", perClassDistribution);
        }

        // 10. 各标签数量
        long labelExcellent = students.stream().filter(s -> "已达标".equals(s.get("label"))).count();
        long labelGrowing = students.stream().filter(s -> "成长中".equals(s.get("label"))).count();
        long labelDeveloping = students.stream().filter(s -> "发展中".equals(s.get("label"))).count();
        long labelStarting = students.stream().filter(s -> "起步期".equals(s.get("label"))).count();
        base.put("labelCounts", Map.of(
            "已达标", labelExcellent, "成长中", labelGrowing,
            "发展中", labelDeveloping, "起步期", labelStarting));

        // 附带 AI 分析文本（如果已有）。此前在 Controller 用 aiOutputMapper 查询，
        // 违反“Controller 禁止注入 Mapper”铁律，下沉到 Service。
        // 取诊断任务组的第一个 taskId 对应的最新 DIAGNOSIS 产出（与 Controller 原逻辑一致）。
        attachDiagnosisAiText(base, taskIds.isEmpty() ? null : taskIds.get(0));

        return base;
    }

    /** 查最新诊断 AI 分析文本并写入 base（失败不影响主数据，保持与原 Controller 行为一致） */
    private void attachDiagnosisAiText(Map<String, Object> base, Long taskId) {
        if (taskId == null) return;
        try {
            AiOutput aiOutput = aiOutputMapper.selectOne(
                new LambdaQueryWrapper<AiOutput>()
                    .eq(AiOutput::getOutputType, "DIAGNOSIS")
                    .eq(AiOutput::getNodeId, taskId)
                    .orderByDesc(AiOutput::getCreatedAt)
                    .last("LIMIT 1"));
            if (aiOutput != null && aiOutput.getContent() != null) {
                base.put("aiAnalysis", aiOutput.getContent());
                base.put("aiAnalysisCreatedAt", aiOutput.getCreatedAt());
            }
        } catch (Exception ignored) { /* AI 分析获取失败不影响主数据 */ }
    }

    /**
     * 批量摘要 — 为多个任务组（每组可能包含多任务）计算每班均分/及格率。
     * 用于列表预览卡片，不计算逐题/知识点等重数据，远轻于 compareMultiTasks。
     */
    public List<Map<String, Object>> getBatchSummary(List<List<Long>> groups) {
        if (groups == null || groups.isEmpty()) return List.of();

        List<Map<String, Object>> result = new ArrayList<>();

        for (List<Long> taskIds : groups) {
            if (taskIds == null || taskIds.isEmpty()) {
                result.add(Map.of("taskIds", "", "classes", List.of(), "message", "空任务组"));
                continue;
            }

            Map<String, Object> groupResult = new LinkedHashMap<>();
            String idsStr = taskIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            groupResult.put("taskIds", idsStr);

            // 验证任务存在
            List<Task> tasks = taskMapper.selectBatchIds(taskIds);
            if (tasks == null || tasks.isEmpty()) {
                groupResult.put("classes", List.of());
                groupResult.put("message", "任务不存在");
                result.add(groupResult);
                continue;
            }
            groupResult.put("taskTitle", tasks.get(0).getTitle() != null ? tasks.get(0).getTitle() : "");
            groupResult.put("subject", tasks.get(0).getSubject() != null ? tasks.get(0).getSubject() : "");

            // 提交记录
            List<TaskSubmission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<TaskSubmission>().in(TaskSubmission::getTaskId, taskIds)
                    .in(TaskSubmission::getStatus, "SUBMITTED", "GRADED"));

            if (submissions.isEmpty()) {
                groupResult.put("classes", List.of());
                groupResult.put("message", "暂无提交记录");
                result.add(groupResult);
                continue;
            }

            // 学生→班级映射
            List<Long> allSids = submissions.stream().map(TaskSubmission::getStudentId).distinct().toList();
            Map<Long, Long> studentClassMap = new HashMap<>();
            List<Student> students = studentMapper.selectBatchIds(allSids);
            if (students != null) {
                for (Student s : students) {
                    if (s.getClassId() != null) studentClassMap.put(s.getId(), s.getClassId());
                }
            }

            // 按班级分组
            Map<Long, List<Long>> classStudents = new LinkedHashMap<>();
            for (TaskSubmission sub : submissions) {
                Long cid = studentClassMap.get(sub.getStudentId());
                if (cid != null) {
                    classStudents.computeIfAbsent(cid, k -> new ArrayList<>()).add(sub.getStudentId());
                }
            }

            // 班级概览（轻量 — 仅均分+及格率）
            // 使用任务 passRate（如果启用），回退 60
            double passThreshold = !tasks.isEmpty() && tasks.get(0).getPassRate() != null && tasks.get(0).getPassRate() > 0
                ? (tasks.get(0).getTotalScore() != null ? tasks.get(0).getTotalScore().doubleValue() * tasks.get(0).getPassRate() / 100.0 : 60)
                : 60;
            List<Map<String, Object>> classInfos = new ArrayList<>();
            for (Map.Entry<Long, List<Long>> entry : classStudents.entrySet()) {
                Long cid = entry.getKey();
                Set<Long> clsUniqueSids = new HashSet<>(entry.getValue());
                double totalScore = 0;
                int passCount = 0;
                for (Long sid : clsUniqueSids) {
                    TaskSubmission bestSub = null;
                    for (TaskSubmission sub : submissions) {
                        if (sub.getStudentId().equals(sid) && (bestSub == null
                            || (sub.getScore() != null && (bestSub.getScore() == null
                            || sub.getScore().doubleValue() > bestSub.getScore().doubleValue())))) {
                            bestSub = sub;
                        }
                    }
                    if (bestSub != null && bestSub.getScore() != null) {
                        totalScore += bestSub.getScore().doubleValue();
                        if (bestSub.getScore().doubleValue() >= passThreshold) passCount++;
                    }
                }
                int cnt = clsUniqueSids.size();
                double avg = cnt == 0 ? 0 : totalScore / cnt;
                double pass = cnt == 0 ? 0 : (double) passCount / cnt * 100;
                Map<String, Object> ci = new LinkedHashMap<>();
                ci.put("classId", cid);
                ci.put("className", resolveClassName(cid));
                ci.put("avgScore", Math.round(avg * 10) / 10.0);
                ci.put("passRate", Math.round(pass * 10) / 10.0);
                ci.put("studentCount", cnt);
                classInfos.add(ci);
            }

            groupResult.put("classes", classInfos);
            result.add(groupResult);
        }

        return result;
    }

    /**
     * 质量预警 — 扫描最近N天关闭的任务，返回班级间差异显著的预警列表。
     * 用于教师首页主动推送。
     */
    public List<Map<String, Object>> getQualityAlerts(Long teacherId, int scanDays,
                                                       double scoreThreshold, double kpThreshold, int maxVisible) {
        if (teacherId == null) return List.of();

        // 1. 该教师创建的已关闭任务（最近 N 天）
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusDays(scanDays);
        List<Task> closedTasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
            .eq(Task::getTeacherId, teacherId)
            .eq(Task::getStatus, "CLOSED")
            .ge(Task::getUpdatedAt, cutoff)
            .orderByDesc(Task::getUpdatedAt));
        if (closedTasks.isEmpty()) return List.of();

        // 2. 按 title+subject 分组（同一试卷多班）
        Map<String, List<Long>> groups = new LinkedHashMap<>();
        Map<String, String> groupTitle = new LinkedHashMap<>();
        Map<String, String> groupSubject = new LinkedHashMap<>();
        for (Task t : closedTasks) {
            String key = (t.getTitle() != null ? t.getTitle() : "") + "|" + (t.getSubject() != null ? t.getSubject() : "");
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(t.getId());
            groupTitle.putIfAbsent(key, t.getTitle());
            groupSubject.putIfAbsent(key, t.getSubject());
        }

        // 3. 批量获取摘要
        List<List<Long>> groupList = new ArrayList<>(groups.values());
        List<Map<String, Object>> summaries = getBatchSummary(groupList);

        // 4. 筛选差异显著的组
        List<Map<String, Object>> alerts = new ArrayList<>();
        int idx = 0;
        for (Map.Entry<String, List<Long>> entry : groups.entrySet()) {
            if (alerts.size() >= maxVisible) break;
            if (idx >= summaries.size()) break;
            Map<String, Object> summary = summaries.get(idx++);
            List<Map<String, Object>> classes = (List<Map<String, Object>>) summary.getOrDefault("classes", List.of());
            if (classes.size() < 2) continue;

            // 计算班级间最大均分差
            double maxScore = 0, minScore = Double.MAX_VALUE;
            for (Map<String, Object> c : classes) {
                Object avg = c.get("avgScore");
                if (avg instanceof Number n) {
                    maxScore = Math.max(maxScore, n.doubleValue());
                    minScore = Math.min(minScore, n.doubleValue());
                }
            }
            double scoreDelta = Math.round((maxScore - minScore) * 10) / 10.0;
            if (scoreDelta < scoreThreshold) continue;

            // 查知识点差异
            String title = groupTitle.getOrDefault(entry.getKey(), "");
            String subject = groupSubject.getOrDefault(entry.getKey(), "");
            try {
                Map<String, Object> full = compareMultiTasks(entry.getValue());
                List<Map<String, Object>> hKps = (List<Map<String, Object>>) full.getOrDefault("highlightedKps", List.of());
                int kpCount = 0;
                for (Map<String, Object> hk : hKps) {
                    Object delta = hk.get("delta");
                    if (delta instanceof Number n && n.doubleValue() > kpThreshold) kpCount++;
                }

                Map<String, Object> alert = new LinkedHashMap<>();
                alert.put("taskIds", entry.getValue().stream().map(Object::toString).collect(Collectors.joining(",")));
                alert.put("firstTaskId", entry.getValue().get(0));
                alert.put("title", title);
                alert.put("subject", subject);
                alert.put("scoreDelta", scoreDelta);
                alert.put("kpCount", kpCount);
                alert.put("classes", classes);
                alerts.add(alert);
            } catch (Exception ignored) { /* 单组失败跳过 */ }
        }

        return alerts;
    }

    private String resolveClassName(Long classId) {
        try {
            var c = classMapper.selectById(classId);
            return c != null && c.getClassName() != null ? c.getClassName() : "班级" + classId;
        } catch (Exception e) { return "班级" + classId; }
    }

    private String resolveKpName(Long kpId) {
        KnowledgeNode node = nodeMapper.selectById(kpId);
        return node != null && node.getName() != null ? node.getName() : "知识点" + kpId;
    }

    /**
     * 纵向对比 — 同一班级同一学科历次考试的均分趋势。
     * GET /teacher/comparison/trend?classId=...&subject=...
     */
    public Map<String, Object> getTrend(Long classId, String subject) {
        if (classId == null) return Map.of("classId", 0, "className", "", "subject", subject, "points", List.of());

        String className = resolveClassName(classId);

        // 查该班级下所有已关闭且包含提交记录的任务
        List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
            .eq(Task::getTargetType, "CLASS").eq(Task::getTargetId, classId)
            .in(Task::getStatus, List.of("PUBLISHED", "ONGOING", "CLOSED")));
        if (subject != null && !subject.isEmpty()) {
            tasks = tasks.stream().filter(t -> subject.equals(t.getSubject())).collect(Collectors.toList());
        }
        if (tasks.isEmpty()) return Map.of("classId", classId, "className", className,
            "subject", subject, "points", List.of(), "message", "暂无考试记录");

        List<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toList());

        // 获取所有提交
        List<TaskSubmission> submissions = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>().in(TaskSubmission::getTaskId, taskIds)
                .in(TaskSubmission::getStatus, "SUBMITTED", "GRADED"));
        Map<Long, List<TaskSubmission>> subByTask = new HashMap<>();
        for (TaskSubmission s : submissions) {
            subByTask.computeIfAbsent(s.getTaskId(), k -> new ArrayList<>()).add(s);
        }

        List<Map<String, Object>> points = new ArrayList<>();
        for (Task task : tasks) {
            List<TaskSubmission> taskSubs = subByTask.getOrDefault(task.getId(), List.of());
            if (taskSubs.isEmpty()) continue;

            // 每个学生取最高分
            Map<Long, Double> bestScores = new LinkedHashMap<>();
            for (TaskSubmission s : taskSubs) {
                if (s.getScore() == null) continue;
                bestScores.merge(s.getStudentId(), s.getScore().doubleValue(), Math::max);
            }
            if (bestScores.isEmpty()) continue;

            double taskPassRateVal = task.getPassRate() != null && task.getPassRate() > 0 ? task.getPassRate() : 60;
            double tsVal = task.getTotalScore() != null ? task.getTotalScore().doubleValue() : 100.0;
            double passThreshold = tsVal * taskPassRateVal / 100.0;
            double sum = 0; int count = 0; int pass = 0;
            for (double score : bestScores.values()) {
                sum += score; count++;
                if (score >= passThreshold) pass++;
            }
            double avg = count == 0 ? 0 : Math.round(sum / count * 10) / 10.0;
            double passRate = count == 0 ? 0 : Math.round((double) pass / count * 1000) / 10.0;

            // 难度推断：全班均分>85简单，60-85中等，<60较难
            String difficulty = avg >= 85 ? "简单" : avg >= 60 ? "中等" : "较难";

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("taskId", task.getId());
            point.put("title", task.getTitle());
            point.put("date", task.getDeadline() != null ? task.getDeadline().toLocalDate().toString()
                : (task.getCreatedAt() != null ? task.getCreatedAt().toLocalDate().toString() : ""));
            point.put("avgScore", avg);
            point.put("passRate", passRate);
            point.put("studentCount", count);
            point.put("difficulty", difficulty);
            point.put("totalScore", task.getTotalScore() != null ? task.getTotalScore().doubleValue() : 100);
            points.add(point);
        }
        points.sort(Comparator.comparing(p -> (String) p.getOrDefault("date", "")));

        // 学科列表（用于筛选下拉）
        Set<String> allSubjects = new LinkedHashSet<>();
        for (Task t : tasks) {
            if (t.getSubject() != null && !t.getSubject().isEmpty()) allSubjects.add(t.getSubject());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("classId", classId);
        result.put("className", className);
        result.put("subject", subject);
        result.put("subjects", new ArrayList<>(allSubjects));
        result.put("points", points);
        return result;
    }

    /**
     * 将一批题目按 category_id 展开到 L4 叶子节点。
     * 当 L3 节点有多个 L4 子节点时，按轮询分配题目到各 L4 子节点。
     * 返回 questionId → L4 kpId 的映射。
     */
    private Map<Long, Long> expandToL4(Map<Long, QuestionBank> qMap,
            Collection<Long> questionIds,
            Map<Long, KnowledgeNode> nodeMap,
            Map<Long, List<KnowledgeNode>> childrenMap) {
        Map<Long, Long> result = new HashMap<>();
        // 按 rawCid 分组
        Map<Long, List<Long>> rawCidToQids = new LinkedHashMap<>();
        for (Long qid : questionIds) {
            QuestionBank qb = qMap.get(qid);
            if (qb == null || qb.getCategoryId() == null) continue;
            rawCidToQids.computeIfAbsent(qb.getCategoryId(), k -> new ArrayList<>()).add(qid);
        }
        for (Map.Entry<Long, List<Long>> e : rawCidToQids.entrySet()) {
            Long rawCid = e.getKey();
            List<Long> qids = e.getValue();
            KnowledgeNode rawNode = nodeMap.get(rawCid);
            if (rawNode == null) { for (Long qid : qids) result.put(qid, rawCid); continue; }
            if (rawNode.getLevel() != null && rawNode.getLevel() == 4) {
                for (Long qid : qids) result.put(qid, rawCid);
            } else {
                List<KnowledgeNode> leaves = collectL4Leaves(rawCid, childrenMap);
                if (leaves.isEmpty()) {
                    for (Long qid : qids) result.put(qid, rawCid);
                } else if (leaves.size() == 1) {
                    for (Long qid : qids) result.put(qid, leaves.get(0).getId());
                } else {
                    for (int i = 0; i < qids.size(); i++) {
                        result.put(qids.get(i), leaves.get(i % leaves.size()).getId());
                    }
                }
            }
        }
        return result;
    }

    /** 递归收集某节点下的所有 L4 叶子子孙（maxDepth=10 防环形引用栈溢出） */
    private List<KnowledgeNode> collectL4Leaves(Long nodeId, Map<Long, List<KnowledgeNode>> childrenMap) {
        return collectL4Leaves(nodeId, childrenMap, 0);
    }
    private List<KnowledgeNode> collectL4Leaves(Long nodeId, Map<Long, List<KnowledgeNode>> childrenMap, int depth) {
        if (depth > 10) return List.of();
        List<KnowledgeNode> result = new ArrayList<>();
        List<KnowledgeNode> children = childrenMap.getOrDefault(nodeId, List.of());
        for (KnowledgeNode child : children) {
            if (child.getLevel() != null && child.getLevel() == 4) {
                result.add(child);
            } else if (child.getLevel() != null && child.getLevel() < 4) {
                result.addAll(collectL4Leaves(child.getId(), childrenMap, depth + 1));
            }
        }
        return result;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    /** 解析选项 JSON 字符串为字符串列表，兼容多种格式 */
    private List<String> parseOptionsJson(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            log.debug("parseOptionsJson: null or blank input");
            return List.of();
        }
        try {
            List<?> raw = objectMapper.readValue(optionsJson, new TypeReference<List<?>>() {});
            List<String> result = new ArrayList<>();
            for (Object o : raw) {
                if (o == null) { result.add(""); continue; }
                if (o instanceof Map) {
                    Map<?,?> m = (Map<?,?>) o;
                    Object text = m.get("text");
                    if (text == null) text = m.get("content");
                    if (text == null) text = m.get("label");
                    if (text == null) text = String.valueOf(o);
                    result.add(String.valueOf(text));
                } else {
                    result.add(String.valueOf(o));
                }
            }
            log.debug("parseOptionsJson: parsed {} options from JSON", result.size());
            return result;
        } catch (Exception e) {
            log.warn("parseOptionsJson: JSON parse failed for '{}', trying fallback",
                optionsJson.length() > 80 ? optionsJson.substring(0, 80) + "..." : optionsJson);
            if (optionsJson.contains("\n")) {
                List<String> fallback = Arrays.stream(optionsJson.split("\\n"))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList();
                log.info("parseOptionsJson: fallback newline split produced {} options", fallback.size());
                return fallback;
            }
            log.warn("parseOptionsJson: all parse strategies failed, returning empty");
            return List.of();
        }
    }

    /** 统计某题的高频错答 TOP3（排除正确答案后按出现次数降序） */
    private List<String> computeTopWrongAnswers(Long questionId, String correctAnswer,
                                                  List<String> options, List<StudentAnswer> answers,
                                                  Map<Long, Long> subStudentMap) {
        if (correctAnswer == null || correctAnswer.isBlank()) return List.of();
        String normalizedCorrect = correctAnswer.trim().toUpperCase();
        // 统计每个错误答案的出现次数
        Map<String, Integer> wrongCount = new HashMap<>();
        for (StudentAnswer sa : answers) {
            if (!questionId.equals(sa.getQuestionId())) continue;
            if (sa.getIsCorrect() != null && sa.getIsCorrect() == 1) continue;
            String ans = sa.getStudentAnswer();
            if (ans == null || ans.isBlank()) continue;
            String normalized = ans.trim().toUpperCase();
            if (normalized.equals(normalizedCorrect)) continue;
            // 对选择题，规范化为字母
            String key = normalizeAnswerToKey(normalized, options);
            wrongCount.merge(key, 1, Integer::sum);
        }
        // 按次数降序取 TOP3
        return wrongCount.entrySet().stream()
            .sorted((a, b) -> b.getValue() - a.getValue())
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /** 将学生答案规范化为选项字母（选择题）或原文（填空题） */
    private String normalizeAnswerToKey(String answer, List<String> options) {
        if (answer == null) return "";
        // 选择题：提取字母
        String stripped = answer.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (!stripped.isEmpty() && stripped.length() <= 4) return stripped;
        // 填空/简答：返回原文（截取前20字）
        return answer.length() > 20 ? answer.substring(0, 20) + "..." : answer;
    }

    // ── E7: 实验班vs对照班对比 ─────────────────────────
    private Map<String, Object> buildGroupComparison(
            Map<Long, Set<Long>> classStudents,
            List<TaskSubmission> submissions,
            List<StudentAnswer> answers,
            Map<Long, TaskQuestion> uniqueTq,
            Map<Long, QuestionBank> qMap,
            Map<Long, Long> qKpMap,
            Map<Long, String> kpNameMap,
            Map<Long, String> classResearchGroups,
            List<Map<String, Object>> existingPerKp) {

        Map<String, Object> gc = new LinkedHashMap<>();

        // 按课题组别聚合学生
        Set<Long> expStudents = new HashSet<>();
        Set<Long> ctrlStudents = new HashSet<>();
        for (var e : classStudents.entrySet()) {
            String rg = classResearchGroups.getOrDefault(e.getKey(), "");
            if ("EXPERIMENT".equals(rg)) expStudents.addAll(e.getValue());
            else if ("CONTROL".equals(rg)) ctrlStudents.addAll(e.getValue());
        }
        if (expStudents.isEmpty() || ctrlStudents.isEmpty()) return gc;

        // 组平均分和通过率
        GroupStats expStats = calcGroupStats(expStudents, submissions);
        GroupStats ctrlStats = calcGroupStats(ctrlStudents, submissions);
        gc.put("experiment", Map.of("avgScore", expStats.avgScore, "passRate", expStats.passRate, "studentCount", expStats.count));
        gc.put("control", Map.of("avgScore", ctrlStats.avgScore, "passRate", ctrlStats.passRate, "studentCount", ctrlStats.count));

        // 每题对比
        Map<Long, Long> subSidMap = new HashMap<>(); // submissionId → studentId
        for (TaskSubmission sub : submissions) subSidMap.put(sub.getId(), sub.getStudentId());
        List<Map<String, Object>> qCompare = new ArrayList<>();
        for (var e : uniqueTq.entrySet()) {
            Long qId = e.getKey();
            QuestionBank q = qMap.get(qId);
            double expRate = calcQuestionCorrectRate(expStudents, answers, qId, subSidMap);
            double ctrlRate = calcQuestionCorrectRate(ctrlStudents, answers, qId, subSidMap);
            double diff = Math.round((expRate - ctrlRate) * 10) / 10.0;
            Map<String, Object> qc = new LinkedHashMap<>();
            qc.put("questionId", qId);
            qc.put("questionText", q != null ? truncate(q.getQuestionText(), 60) : "题目" + qId);
            qc.put("experimentRate", expRate);
            qc.put("controlRate", ctrlRate);
            qc.put("diff", diff);
            if (Math.abs(diff) > 15) qc.put("significant", true);
            qCompare.add(qc);
        }
        gc.put("perQuestion", qCompare);

        // 知识点对比（通过 qKpMap: questionId → kpId）
        List<Map<String, Object>> kpCompare = new ArrayList<>();
        for (Map<String, Object> kpi : existingPerKp) {
            Long kpId = kpi.get("kpId") instanceof Number n ? n.longValue() : null;
            if (kpId == null) continue;
            double expKpRate = calcKpCorrectRate(expStudents, answers, kpId, qKpMap, subSidMap);
            double ctrlKpRate = calcKpCorrectRate(ctrlStudents, answers, kpId, qKpMap, subSidMap);
            double diff = Math.round((expKpRate - ctrlKpRate) * 10) / 10.0;
            Map<String, Object> kc = new LinkedHashMap<>();
            kc.put("kpId", kpId);
            kc.put("kpName", kpNameMap.getOrDefault(kpId, "知识点" + kpId));
            kc.put("experimentRate", expKpRate);
            kc.put("controlRate", ctrlKpRate);
            kc.put("diff", diff);
            if (Math.abs(diff) > 15) kc.put("significant", true);
            kpCompare.add(kc);
        }
        gc.put("perKp", kpCompare);

        // 显著差异知识点
        List<Map<String, Object>> sigKps = kpCompare.stream()
            .filter(k -> k.containsKey("significant")).toList();
        gc.put("highlightedKps", sigKps);

        return gc;
    }

    private GroupStats calcGroupStats(Set<Long> studentIds, List<TaskSubmission> submissions) {
        double totalScore = 0;
        int passCount = 0;
        int cnt = 0;
        for (Long sid : studentIds) {
            TaskSubmission best = null;
            for (TaskSubmission sub : submissions) {
                if (sub.getStudentId().equals(sid) && (best == null
                    || (sub.getScore() != null && (best.getScore() == null
                    || sub.getScore().doubleValue() > best.getScore().doubleValue())))) {
                    best = sub;
                }
            }
            if (best != null && best.getScore() != null) {
                totalScore += best.getScore().doubleValue();
                if (best.getScore().doubleValue() >= 60) passCount++; // 组对比无单一任务上下文，使用默认 60
                cnt++;
            }
        }
        double avg = cnt == 0 ? 0 : Math.round(totalScore / cnt * 10) / 10.0;
        double pass = cnt == 0 ? 0 : Math.round((double) passCount / cnt * 100 * 10) / 10.0;
        return new GroupStats(avg, pass, cnt);
    }

    private double calcQuestionCorrectRate(Set<Long> studentIds, List<StudentAnswer> answers, Long questionId,
            Map<Long, Long> subSidMap) {
        int total = 0;
        int correct = 0;
        for (StudentAnswer a : answers) {
            if (a.getQuestionId() != null && a.getQuestionId().equals(questionId)) {
                Long sid = subSidMap.get(a.getSubmissionId());
                if (sid != null && studentIds.contains(sid)) {
                    total++;
                    if (a.getIsCorrect() != null && a.getIsCorrect() == 1) correct++;
                }
            }
        }
        return total == 0 ? 0 : Math.round((double) correct / total * 100 * 10) / 10.0;
    }

    private double calcKpCorrectRate(Set<Long> studentIds, List<StudentAnswer> answers, Long kpId,
            Map<Long, Long> qKpMap, Map<Long, Long> subSidMap) {
        int total = 0;
        int correct = 0;
        for (StudentAnswer a : answers) {
            Long mappedKpId = qKpMap.get(a.getQuestionId());
            if (mappedKpId != null && mappedKpId.equals(kpId)) {
                Long sid = subSidMap.get(a.getSubmissionId());
                if (sid != null && studentIds.contains(sid)) {
                    total++;
                    if (a.getIsCorrect() != null && a.getIsCorrect() == 1) correct++;
                }
            }
        }
        return total == 0 ? 0 : Math.round((double) correct / total * 100 * 10) / 10.0;
    }

    // Internal POJO for group stats
    private static class GroupStats {
        final double avgScore;
        final double passRate;
        final int count;
        GroupStats(double avgScore, double passRate, int count) {
            this.avgScore = avgScore; this.passRate = passRate; this.count = count;
        }
    }

    /** 发展性等级标签 — 前端 constants/grading.js 保持同步 */
    public static String getGradeLabel(Double score) {
        if (score == null) return "起步期";
        if (score >= 85) return "已达标";
        if (score >= 70) return "成长中";
        if (score >= 60) return "发展中";
        return "起步期";
    }
}
