package com.school.teaching.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentGrowthService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StudentGrowthService.class);

    private final TaskSubmissionMapper submissionMapper;
    private final TaskMapper taskMapper;
    private final StudentAnswerMapper answerMapper;
    private final TaskQuestionMapper taskQuestionMapper;
    private final QuestionBankMapper questionBankMapper;
    private final KnowledgeNodeMapper nodeMapper;
    private final StudentMapper studentMapper;
    private final UserMapper userMapper;

    public Map<String, Object> getGrowthCurve(Long studentId, String subject) {
        // 双ID解析：前端可能传 users.id, 需映射到 students.id
        Long realSid = resolveStudentId(studentId);

        // 先解析学生姓名（全路径都需要）
        String studentName = "学生" + studentId;
        Student student = studentMapper.selectById(realSid);
        if (student != null && student.getUserId() != null) {
            var user = userMapper.selectById(student.getUserId());
            if (user != null && user.getRealName() != null) studentName = user.getRealName();
        }

        List<TaskSubmission> submissions = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getStudentId, realSid)
                .in(TaskSubmission::getStatus, "SUBMITTED", "GRADED"));
        if (submissions.isEmpty()) return Map.of("studentId", realSid, "studentName", studentName,
            "points", List.of(), "weakPoints", List.of());

        List<Long> taskIds = submissions.stream().map(TaskSubmission::getTaskId).distinct().collect(Collectors.toList());
        List<Task> tasks = taskMapper.selectBatchIds(taskIds);
        if (tasks == null) tasks = List.of();
        Map<Long, Task> taskMap = tasks.stream().collect(Collectors.toMap(Task::getId, t -> t));
        Map<Long, Long> subToSid = submissions.stream().collect(Collectors.toMap(TaskSubmission::getId, TaskSubmission::getStudentId));

        List<Map<String, Object>> points = new ArrayList<>();
        // 计算班级均分参考线：该生同班同学在相同任务上的均分
        Map<Long, Double> classAvgMap = Map.of();
        Long classId = student != null ? student.getClassId() : null;
        log.info("[GrowthCurve] studentId={} realSid={} classId={} taskCount={} subCount={}",
            studentId, realSid, classId, taskIds.size(), submissions.size());
        if (classId != null && !taskIds.isEmpty()) {
            List<Student> classmates = studentMapper.selectList(
                new LambdaQueryWrapper<Student>().eq(Student::getClassId, classId));
            Set<Long> classSids = classmates.stream().map(Student::getId).collect(Collectors.toSet());
            classSids.remove(realSid); // 排除学生自己
            if (!classSids.isEmpty()) {
                log.info("[GrowthCurve] classId={} classmates={} classSids={}", classId, classmates.size(), classSids.size());
                // 查同班同学全部提交（不过滤 taskId —— 否则同任务数据稀疏时回报空导致无参考线）
                List<TaskSubmission> classSubs = submissionMapper.selectList(
                    new LambdaQueryWrapper<TaskSubmission>()
                        .in(TaskSubmission::getStudentId, classSids)
                        .in(TaskSubmission::getStatus, "SUBMITTED", "GRADED"));
                log.info("[GrowthCurve] classSubs={} for {} tasks", classSubs.size(), taskIds.size());
                // 按 taskId 分组计算均分
                Map<Long, List<TaskSubmission>> byTask = new HashMap<>();
                for (TaskSubmission cs : classSubs) {
                    byTask.computeIfAbsent(cs.getTaskId(), k -> new ArrayList<>()).add(cs);
                }
                Map<Long, Double> avgMap = new HashMap<>();
                for (Map.Entry<Long, List<TaskSubmission>> e : byTask.entrySet()) {
                    double sum = 0; int cnt = 0;
                    for (TaskSubmission cs : e.getValue()) {
                        if (cs.getScore() != null) { sum += cs.getScore().doubleValue(); cnt++; }
                    }
                    if (cnt > 0) avgMap.put(e.getKey(), Math.round(sum / cnt * 10) / 10.0);
                }
                classAvgMap = avgMap;
                log.info("[GrowthCurve] classAvgMap built: {} task averages", avgMap.size());
            } else {
                log.warn("[GrowthCurve] no classmates found for classId={} (student={})", classId, realSid);
            }
        } else {
            log.warn("[GrowthCurve] skipped class avg: classId={} taskIds={}", classId, taskIds.isEmpty());
        }

        for (TaskSubmission sub : submissions) {
            Task task = taskMap.get(sub.getTaskId());
            if (task == null) continue;
            if (subject != null && !subject.isEmpty() && !subject.equals(task.getSubject())) continue;
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("taskId", task.getId());
            point.put("title", task.getTitle());
            point.put("date", sub.getSubmittedAt() != null ? sub.getSubmittedAt().toLocalDate().toString() : "");
            point.put("score", sub.getScore());
            point.put("totalScore", task.getTotalScore());
            point.put("rate", task.getTotalScore() != null && sub.getScore() != null && task.getTotalScore().doubleValue() > 0
                ? Math.round(sub.getScore().doubleValue() / task.getTotalScore().doubleValue() * 1000) / 10.0 : 0);
            // 班级均分参考线 — 始终计算百分比形式
            Double classAvg = classAvgMap.get(task.getId());
            if (classAvg != null) {
                double totalScore = task.getTotalScore() != null && task.getTotalScore().doubleValue() > 0
                    ? task.getTotalScore().doubleValue() : 100.0;
                point.put("classAvgRate", Math.round(classAvg / totalScore * 1000) / 10.0);
                point.put("classAvgScore", classAvg);
            }
            points.add(point);
        }
        log.info("[GrowthCurve] points built: {} total, {} have classAvg", points.size(),
            points.stream().filter(p -> p.containsKey("classAvgRate")).count());
        points.sort(Comparator.comparing(p -> (String) p.getOrDefault("date", "")));

        List<Map<String, Object>> weakPoints = getWeakPoints(realSid, taskIds, subToSid);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", realSid);
        result.put("studentName", studentName);
        result.put("subject", subject);
        result.put("points", points);
        result.put("weakPoints", weakPoints);
        return result;
    }

        /** 双解析：先按 students.id 查，再按 students.user_id 查 */
    private Long resolveStudentId(Long rawId) {
        if (rawId == null) return null;
        Student s = studentMapper.selectById(rawId);
        if (s == null) {
            s = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>().eq(Student::getUserId, rawId));
        }
        return s != null ? s.getId() : rawId;
    }

    private List<Map<String, Object>> getWeakPoints(Long studentId, List<Long> taskIds, Map<Long, Long> subToSid) {
        if (taskIds.isEmpty()) return List.of();

        // 批量加载：一次查所有题目ID → 一次查所有答题 → 一次查所有题目 → 内存聚合（消除N+1）
        List<TaskQuestion> allTq = taskQuestionMapper.selectList(
            new LambdaQueryWrapper<TaskQuestion>().in(TaskQuestion::getTaskId, taskIds));
        if (allTq.isEmpty()) return List.of();

        Set<Long> allQIds = allTq.stream().map(TaskQuestion::getQuestionId).collect(Collectors.toSet());
        List<StudentAnswer> allAnswers = answerMapper.selectList(
            new LambdaQueryWrapper<StudentAnswer>().in(StudentAnswer::getTaskId, taskIds));
        List<QuestionBank> allQuestions = questionBankMapper.selectBatchIds(allQIds);

        // 收集所有 categoryId → 批量加载 knowledge_nodes
        Set<Long> rawCids = new HashSet<>();
        if (allQuestions != null) {
            for (QuestionBank q : allQuestions) {
                if (q.getCategoryId() != null) rawCids.add(q.getCategoryId());
            }
        }
        // 加载相关学科的知识树 → nodeMap + childrenMap（仅加载涉及的学科，非全量）
        Map<Long, KnowledgeNode> nodeMap = new HashMap<>();
        Map<Long, List<KnowledgeNode>> childrenMap = new HashMap<>();
        if (!rawCids.isEmpty()) {
            // 先查 rawCids 对应的 subjectId，再按学科过滤加载
            List<KnowledgeNode> rawNodes = nodeMapper.selectBatchIds(new ArrayList<>(rawCids));
            Set<Long> relevantSubjectIds = rawNodes.stream()
                .map(KnowledgeNode::getSubjectId).filter(Objects::nonNull).collect(Collectors.toSet());
            LambdaQueryWrapper<KnowledgeNode> nodeQuery = new LambdaQueryWrapper<KnowledgeNode>()
                .isNotNull(KnowledgeNode::getId);
            if (!relevantSubjectIds.isEmpty()) {
                nodeQuery.in(KnowledgeNode::getSubjectId, relevantSubjectIds);
            }
            List<KnowledgeNode> allTreeNodes = nodeMapper.selectList(nodeQuery);
            if (allTreeNodes != null) {
                for (KnowledgeNode n : allTreeNodes) {
                    nodeMap.put(n.getId(), n);
                    if (n.getParentId() != null) {
                        childrenMap.computeIfAbsent(n.getParentId(), k -> new ArrayList<>()).add(n);
                    }
                }
            }
        }

        // 构建 questionId → [L4 kpIds]（category_id 非 L4 时递归找叶子）
        Map<Long, List<Long>> qLeafKps = new HashMap<>();
        if (allQuestions != null) {
            // 按 rawCid 分组，轮询分配到 L4 子节点
            Map<Long, List<Long>> rawCidToQids = new LinkedHashMap<>();
            for (QuestionBank q : allQuestions) {
                if (q.getCategoryId() != null) rawCidToQids.computeIfAbsent(q.getCategoryId(), k -> new ArrayList<>()).add(q.getId());
            }
            for (Map.Entry<Long, List<Long>> e : rawCidToQids.entrySet()) {
                Long rawCid = e.getKey();
                List<Long> qids = e.getValue();
                KnowledgeNode rawNode = nodeMap.get(rawCid);
                if (rawNode == null) continue;
                List<Long> leafIds = new ArrayList<>();
                if (rawNode.getLevel() != null && rawNode.getLevel() == 4) {
                    for (Long qid : qids) qLeafKps.put(qid, List.of(rawCid));
                } else {
                    List<KnowledgeNode> leaves = collectL4Leaves(rawCid, childrenMap);
                    if (leaves.isEmpty()) {
                        for (Long qid : qids) qLeafKps.put(qid, List.of(rawCid));
                    } else if (leaves.size() == 1) {
                        for (Long qid : qids) qLeafKps.put(qid, List.of(leaves.get(0).getId()));
                    } else {
                        for (int i = 0; i < qids.size(); i++) {
                            qLeafKps.put(qids.get(i), List.of(leaves.get(i % leaves.size()).getId()));
                        }
                    }
                }
            }
        }
        // 答案预索引：taskId::questionId → [studentId → answer]
        Map<String, Map<Long, StudentAnswer>> answerIndex = new HashMap<>();
        for (StudentAnswer sa : allAnswers) {
            Long sid = subToSid.get(sa.getSubmissionId());
            if (sid == null) continue;
            String key = sa.getTaskId() + "::" + sa.getQuestionId();
            answerIndex.computeIfAbsent(key, k -> new HashMap<>()).put(sid, sa);
        }

        Map<Long, int[]> kpErrors = new LinkedHashMap<>();
        for (TaskQuestion tq : allTq) {
            String key = tq.getTaskId() + "::" + tq.getQuestionId();
            Map<Long, StudentAnswer> saMap = answerIndex.get(key);
            if (saMap == null) continue;
            StudentAnswer sa = saMap.get(studentId);
            if (sa == null) continue;
            int wrong = (sa.getIsCorrect() != null && sa.getIsCorrect() == 0) ? 1 : 0;
            // 使用解析后的 L4 知识点列表
            List<Long> kpIds = qLeafKps.get(tq.getQuestionId());
            if (kpIds == null || kpIds.isEmpty()) continue;
            for (Long kpId : kpIds) {
                int[] stats = kpErrors.computeIfAbsent(kpId, k -> new int[]{0, 0});
                stats[0] += wrong;
                stats[1] += 1;
            }
        }

        // 批量查知识点名称（只查 L4 叶子节点）
        Map<Long, String> kpNameMap = new HashMap<>();
        if (!kpErrors.isEmpty()) {
            List<KnowledgeNode> nodes = nodeMapper.selectBatchIds(kpErrors.keySet());
            if (nodes != null) {
                for (KnowledgeNode n : nodes) {
                    kpNameMap.put(n.getId(), n.getName() != null ? n.getName() : "知识点" + n.getId());
                }
            }
        }

        List<Map<String, Object>> weakList = new ArrayList<>();
        for (Map.Entry<Long, int[]> e : kpErrors.entrySet()) {
            if (e.getValue()[1] == 0) continue;
            Map<String, Object> wp = new LinkedHashMap<>();
            wp.put("kpId", e.getKey());
            wp.put("kpName", kpNameMap.getOrDefault(e.getKey(), "知识点" + e.getKey()));
            wp.put("wrongCount", e.getValue()[0]);
            double rate = (double) e.getValue()[0] / e.getValue()[1] * 100;
            wp.put("errorRate", Math.round(rate * 10) / 10.0);
            weakList.add(wp);
        }
        weakList.sort((a, b) -> Double.compare((double) b.get("errorRate"), (double) a.get("errorRate")));
        if (weakList.size() > 5) return weakList.subList(0, 5);
        return weakList;
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

}
