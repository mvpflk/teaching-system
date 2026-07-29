package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.entity.WrongQuestion;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.QuestionBankMapper;
import com.school.teaching.mapper.WrongQuestionMapper;
import com.school.teaching.service.SystemService;
import com.school.teaching.service.WrongQuestionService;
import com.school.teaching.agent.prompt.PromptTemplateCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Slf4j
@Service
@Transactional
public class WrongQuestionServiceImpl implements WrongQuestionService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WrongQuestionServiceImpl.class);

    @Autowired private WrongQuestionMapper wrongMapper;
    @Autowired private QuestionBankMapper questionMapper;
    @Autowired private com.school.teaching.mapper.StudentAnswerMapper studentAnswerMapper;
    @Autowired private com.school.teaching.mapper.TaskSubmissionMapper taskSubmissionMapper;
    @Autowired private com.school.teaching.mapper.TaskMapper taskMapper;
    @Autowired private com.school.teaching.mapper.PracticeSessionMapper sessionMapper;
    @Autowired private com.school.teaching.mapper.PracticeSessionItemMapper itemMapper;
    @Autowired private com.school.teaching.mapper.KnowledgeNodeMapper nodeMapper;
    @Autowired private com.school.teaching.mapper.NotificationMapper notificationMapper;
    @Autowired private SystemService systemService;
    @Autowired private com.school.teaching.mapper.StudentMapper studentMapper;
    @Autowired private com.school.teaching.service.AiQuestionGeneratorService aiService;
    @org.springframework.context.annotation.Lazy
    @Autowired private WrongQuestionServiceImpl self;
    @Autowired private com.school.teaching.mapper.ClassesMapper classesMapper;
    @Autowired private com.school.teaching.service.ExamSyllabusService examSyllabusService;
    @Autowired private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Autowired private com.school.teaching.mapper.UserMapper userMapper;
    @Autowired(required = false) private com.school.teaching.service.TeacherService teacherService;
    @Autowired(required = false) private PromptTemplateCache promptTemplateCache;
    @Autowired private com.school.teaching.service.QuestionMatchingService questionMatchingService;

    @Override
    public Map<String, Object> listWrongQuestions(Long studentId, int mastered, int page, int pageSize, String sourceType) {
        LambdaQueryWrapper<WrongQuestion> countW = new LambdaQueryWrapper<>();
        countW.eq(WrongQuestion::getStudentId, studentId);
        if (sourceType != null && !sourceType.isEmpty()) {
            countW.eq(WrongQuestion::getSourceType, sourceType);
        }
        long total = wrongMapper.selectCount(countW);
        LambdaQueryWrapper<WrongQuestion> masterCountW = new LambdaQueryWrapper<>();
        masterCountW.eq(WrongQuestion::getStudentId, studentId);
        if (sourceType != null && !sourceType.isEmpty()) {
            masterCountW.eq(WrongQuestion::getSourceType, sourceType);
        }
        masterCountW.eq(WrongQuestion::getIsMastered, 1);
        long masteredCount = wrongMapper.selectCount(masterCountW);

        LambdaQueryWrapper<WrongQuestion> w = new LambdaQueryWrapper<>();
        w.eq(WrongQuestion::getStudentId, studentId);
        if (sourceType != null && !sourceType.isEmpty()) {
            w.eq(WrongQuestion::getSourceType, sourceType);
        }
        if (mastered == 1) w.eq(WrongQuestion::getIsMastered, 1);        // 仅已掌握
        else if (mastered == 2) w.eq(WrongQuestion::getIsMastered, 0);   // 仅未掌握
        // mastered == 0 或其它 → 不过滤，显示全部
        w.orderByDesc(WrongQuestion::getLastWrongTime);
        int safeLimit = Math.min(Math.max(pageSize, 1), 1000);
        w.last("LIMIT " + ((page - 1) * safeLimit) + "," + safeLimit);
        List<WrongQuestion> list = wrongMapper.selectList(w);

        Set<Long> qIds = new HashSet<>();
        for (WrongQuestion wq : list) qIds.add(wq.getQuestionId());
        Map<Long, QuestionBank> qMap = new HashMap<>();
        if (!qIds.isEmpty()) questionMapper.selectBatchIds(qIds).forEach(q -> qMap.put(q.getId(), q));

        // 通过 提交→任务 查找题目所属任务的学科 + 学生答案
        Map<Long, String> qSubjectMap = new HashMap<>();
        Map<Long, String> qAnswerMap = new HashMap<>();
        if (!qIds.isEmpty()) {
            List<com.school.teaching.entity.TaskSubmission> allSubs = taskSubmissionMapper.selectList(
                new LambdaQueryWrapper<com.school.teaching.entity.TaskSubmission>()
                    .eq(com.school.teaching.entity.TaskSubmission::getStudentId, studentId));
            if (!allSubs.isEmpty()) {
                Set<Long> subIds = allSubs.stream().map(com.school.teaching.entity.TaskSubmission::getId).collect(java.util.stream.Collectors.toSet());
                List<com.school.teaching.entity.StudentAnswer> answers = studentAnswerMapper.selectList(
                    new LambdaQueryWrapper<com.school.teaching.entity.StudentAnswer>()
                        .in(com.school.teaching.entity.StudentAnswer::getSubmissionId, subIds)
                        .in(com.school.teaching.entity.StudentAnswer::getQuestionId, qIds));
                Map<Long, Long> subTaskMap = allSubs.stream().collect(java.util.stream.Collectors.toMap(
                    com.school.teaching.entity.TaskSubmission::getId, com.school.teaching.entity.TaskSubmission::getTaskId));
                Set<Long> taskIds = allSubs.stream().map(com.school.teaching.entity.TaskSubmission::getTaskId).collect(java.util.stream.Collectors.toSet());
                Map<Long, String> taskSubjectMap = new HashMap<>();
                taskMapper.selectBatchIds(taskIds).forEach(t -> taskSubjectMap.put(t.getId(), t.getSubject()));
                for (com.school.teaching.entity.StudentAnswer a : answers) {
                    Long tId = subTaskMap.get(a.getSubmissionId());
                    if (tId != null && taskSubjectMap.containsKey(tId)) {
                        qSubjectMap.putIfAbsent(a.getQuestionId(), taskSubjectMap.get(tId));
                    }
                    qAnswerMap.putIfAbsent(a.getQuestionId(), a.getStudentAnswer());
                }
            }
        }

        List<Map<String, Object>> enriched = new ArrayList<>();
        for (WrongQuestion wq : list) {
            QuestionBank q = qMap.get(wq.getQuestionId());
            Map<String, Object> item = new HashMap<>();
            item.put("id", wq.getId()); item.put("questionId", wq.getQuestionId());
            item.put("wrongCount", wq.getWrongCount()); item.put("lastWrongTime", wq.getLastWrongTime());
            item.put("isMastered", wq.getIsMastered());
            item.put("practiceCount", wq.getPracticeCount());
            item.put("lastPracticeTime", wq.getLastPracticeTime());
            item.put("masteredAt", wq.getMasteredAt());
            item.put("masteredSource", wq.getMasteredSource());
            if (q != null) {
                item.put("questionText", q.getQuestionText()); item.put("questionType", q.getQuestionType());
                item.put("options", ensureTrueFalseOptions(q.getQuestionType(), q.getOptions())); item.put("correctAnswer", q.getCorrectAnswer());
                item.put("explanation", q.getExplanation());
            }
            item.put("subject", qSubjectMap.getOrDefault(wq.getQuestionId(), ""));
            item.put("myAnswer", qAnswerMap.getOrDefault(wq.getQuestionId(), ""));
            item.put("sourceType", wq.getSourceType());
            item.put("sourceTaskId", wq.getSourceTaskId());
            enriched.add(item);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("records", enriched); data.put("total", total);
        data.put("masteredCount", masteredCount); data.put("unmasteredCount", total - masteredCount);
        data.put("page", page); data.put("pageSize", pageSize);
        return data;
    }

    @Override
    public void markMastered(Long id, Long studentId) {
        markMasteredWithSource(id, studentId, "manual");
    }

    @Override
    public void markMasteredWithSource(Long id, Long studentId, String source) {
        WrongQuestion wq = wrongMapper.selectById(id);
        if (wq == null) throw new BusinessException(404, "错题记录不存在");
        if (!wq.getStudentId().equals(studentId)) throw new BusinessException(403, "无权操作");
        wq.setIsMastered(1);
        wq.setMasteredAt(LocalDateTime.now());
        wq.setMasteredSource(source);
        wq.setUpdateTime(LocalDateTime.now());
        wrongMapper.updateById(wq);
    }

    @Override
    public void markUnmastered(Long id, Long studentId) {
        WrongQuestion wq = wrongMapper.selectById(id);
        if (wq == null) throw new BusinessException(404, "不存在");
        if (!wq.getStudentId().equals(studentId)) throw new BusinessException(403, "无权操作");
        wq.setIsMastered(0);
        wq.setMasteredAt(null);
        wq.setMasteredSource(null);
        wq.setUpdateTime(LocalDateTime.now());
        wrongMapper.updateById(wq);
    }

    @Override
    public List<Map<String, Object>> getPracticeList(Long studentId) {
        // 未掌握错题 + 已掌握但到期复习的错题
        List<WrongQuestion> wqs = wrongMapper.selectList(
            new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getStudentId, studentId)
                .and(w -> w.eq(WrongQuestion::getIsMastered, 0)
                    .or(w2 -> w2.eq(WrongQuestion::getIsMastered, 1)
                        .le(WrongQuestion::getNextReviewAt, LocalDateTime.now())))
                .orderByDesc(WrongQuestion::getWrongCount));
        Set<Long> qIds = new HashSet<>();
        for (WrongQuestion wq : wqs) qIds.add(wq.getQuestionId());
        Map<Long, QuestionBank> qMap = new HashMap<>();
        if (!qIds.isEmpty()) questionMapper.selectBatchIds(qIds).forEach(q -> qMap.put(q.getId(), q));

        // 智能排序：错题次数×10 + 距上次练习天数，优先推送高频+久未复习
        LocalDateTime now = LocalDateTime.now();
        wqs.sort(Comparator.comparingLong((WrongQuestion w) -> {
            long daysSincePractice = w.getLastPracticeTime() != null
                ? java.time.Duration.between(w.getLastPracticeTime(), now).toDays() : 30;
            long wrongWeight = (w.getWrongCount() != null ? w.getWrongCount() : 1) * 10L;
            return wrongWeight + daysSincePractice; // 高分优先
        }).reversed());

        List<Map<String, Object>> result = new ArrayList<>();
        // 最多返回20题
        for (WrongQuestion wq : wqs.stream().limit(20).toList()) {
            QuestionBank q = qMap.get(wq.getQuestionId());
            if (q == null) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("wrongId", wq.getId()); item.put("questionId", q.getId());
            item.put("questionType", q.getQuestionType()); item.put("questionText", q.getQuestionText());
            item.put("options", ensureTrueFalseOptions(q.getQuestionType(), q.getOptions()));
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> submitPractice(Long wrongId, String answer, Long studentId) {
        WrongQuestion wq = wrongMapper.selectById(wrongId);
        if (wq == null) throw new BusinessException(404, "错题记录不存在");
        if (!wq.getStudentId().equals(studentId)) throw new BusinessException(403, "无权操作");

        QuestionBank question = questionMapper.selectById(wq.getQuestionId());
        if (question == null) throw new BusinessException(404, "题目不存在");

        boolean isCorrect = checkAnswerStr(answer, question.getQuestionType(), question.getCorrectAnswer());
        // 记录练习行为（直接修改 wq 避免覆盖问题）
        wq.setPracticeCount((wq.getPracticeCount() != null ? wq.getPracticeCount() : 0) + 1);
        wq.setLastPracticeTime(LocalDateTime.now());

        applySpacedRepetition(wq, isCorrect, "single_practice");
        if (!isCorrect) {
            // 额外记录错误时间（applySpacedRepetition 已处理重置，此处补充原有字段）
            wq.setLastWrongTime(LocalDateTime.now());
        }
        wq.setUpdateTime(LocalDateTime.now());
        wrongMapper.updateById(wq);
        return Map.of("isCorrect", isCorrect, "correctAnswer", question.getCorrectAnswer() != null ? question.getCorrectAnswer() : "", "explanation", question.getExplanation() != null ? question.getExplanation() : "");
    }

    @Override
    public void recordPractice(Long wrongId, Long studentId) {
        recordPractice(wrongId, studentId, null);
    }

    public void recordPractice(Long wrongId, Long studentId, Boolean correct) {
        WrongQuestion wq = wrongMapper.selectById(wrongId);
        if (wq == null || !wq.getStudentId().equals(studentId)) return;
        if (correct != null) {
            // 答对/答错触发遗忘曲线调度（统一间隔复习逻辑）
            applySpacedRepetition(wq, correct, "redo");
        } else {
            // 兼容旧调用（无 correct 参数）：仅更新练习时间计数
            wq.setPracticeCount((wq.getPracticeCount() != null ? wq.getPracticeCount() : 0) + 1);
            wq.setLastPracticeTime(LocalDateTime.now());
        }
        wrongMapper.updateById(wq);
    }

    // ═══════════ 衍生练习：错题诊断→薄弱知识点→混合检索→生成会话 ═══════════

    @Override
    public Map<String, Object> generateDerivedPractice(Long studentId) {
        // 0. 防刷：检查是否有生成中+未完成的会话
        List<com.school.teaching.entity.PracticeSession> activeSessions = sessionMapper.selectList(
            new LambdaQueryWrapper<com.school.teaching.entity.PracticeSession>()
                .eq(com.school.teaching.entity.PracticeSession::getStudentId, studentId)
                .in(com.school.teaching.entity.PracticeSession::getStatus, "generating", "ongoing")
                .orderByDesc(com.school.teaching.entity.PracticeSession::getStartedAt));
        if (!activeSessions.isEmpty()) {
            return Map.of("sessionId", activeSessions.get(0).getId(),
                "status", activeSessions.get(0).getStatus());
        }

        // 0.1 冷却检查（failed 状态不计入冷却）
        List<com.school.teaching.entity.PracticeSession> recentSessions = sessionMapper.selectList(
            new LambdaQueryWrapper<com.school.teaching.entity.PracticeSession>()
                .eq(com.school.teaching.entity.PracticeSession::getStudentId, studentId)
                .ne(com.school.teaching.entity.PracticeSession::getStatus, "failed")
                .orderByDesc(com.school.teaching.entity.PracticeSession::getStartedAt)
                .last("LIMIT 1"));
        if (!recentSessions.isEmpty() && recentSessions.get(0).getStartedAt() != null) {
            long secondsSinceLast = java.time.Duration.between(
                recentSessions.get(0).getStartedAt(), LocalDateTime.now()).getSeconds();
            if (secondsSinceLast < 60) {
                throw new BusinessException(429, "请等待 " + (60 - secondsSinceLast) + " 秒后再生成");
            }
        }

        // 0.2 每日上限
        long todayCount = sessionMapper.selectCount(
            new LambdaQueryWrapper<com.school.teaching.entity.PracticeSession>()
                .eq(com.school.teaching.entity.PracticeSession::getStudentId, studentId)
                .ge(com.school.teaching.entity.PracticeSession::getStartedAt, LocalDate.now().atStartOfDay()));
        int quota = systemService.getIntConfig("ai.student.daily_quota", 6);
        if (quota > 0 && todayCount >= quota) throw new BusinessException(429, "今日衍生练习次数已用完（" + quota + "次/天）");

        // ★ 防并发：提前创建 session（status=generating），后续请求立即被 Step 0 拦截
        com.school.teaching.entity.PracticeSession session = new com.school.teaching.entity.PracticeSession();
        session.setStudentId(studentId);
        session.setSourceType("wrong_book");
        session.setTotalQuestions(0);
        session.setStatus("generating");
        session.setStartedAt(LocalDateTime.now());
        sessionMapper.insert(session);
        Long earlySessionId = session.getId();

        try {
            // 1-4. 同步阶段：错题收集 + 薄弱知识点 + 题库匹配（与之前相同）
            List<WrongQuestion> wqs = wrongMapper.selectList(
            new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getStudentId, studentId)
                .eq(WrongQuestion::getIsMastered, 0));
        if (wqs.isEmpty()) throw new BusinessException(400, "没有未掌握的错题，无需衍生练习");

        Set<Long> wrongQids = new HashSet<>();
        for (WrongQuestion wq : wqs) wrongQids.add(wq.getQuestionId());
        Map<Long, QuestionBank> qMap = new HashMap<>();
        questionMapper.selectBatchIds(wrongQids).forEach(q -> qMap.put(q.getId(), q));

        Map<String, Integer> pointFreq = new LinkedHashMap<>();
        Set<Long> weakCategoryIds = new HashSet<>();
        for (WrongQuestion wq : wqs) {
            QuestionBank q = qMap.get(wq.getQuestionId());
            if (q == null) continue;
            if (q.getCategoryId() != null) weakCategoryIds.add(q.getCategoryId());
            List<String> kps = parseKnowledgePoints(q.getKnowledgePoints());
            if (kps.isEmpty() && q.getQuestionText() != null) {
                kps = List.of(q.getQuestionText().length() > 30
                    ? q.getQuestionText().substring(0, 30) : q.getQuestionText());
            }
            for (String kp : kps) {
                pointFreq.merge(kp, wq.getWrongCount() != null ? wq.getWrongCount() : 1, Integer::sum);
            }
        }
        List<Map.Entry<String, Integer>> sortedPoints = pointFreq.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).toList();
        List<Map<String, Object>> weakPoints = sortedPoints.stream()
            .map(e -> Map.<String, Object>of("name", e.getKey(), "frequency", e.getValue()))
            .toList();

        int totalDiff = 0, diffCount = 0;
        for (WrongQuestion wq : wqs) {
            QuestionBank q = qMap.get(wq.getQuestionId());
            if (q != null && q.getDifficultyLevel() != null) { totalDiff += q.getDifficultyLevel(); diffCount++; }
        }
        int avgDifficulty = diffCount > 0 ? Math.max(1, Math.round((float) totalDiff / diffCount)) : 2;

        Set<Long> usedIds = new HashSet<>(wrongQids);
        // 两步查询防SQL注入：先查sessionId列表，再in查询item
        List<Long> historySessionIds = sessionMapper.selectList(
            new LambdaQueryWrapper<com.school.teaching.entity.PracticeSession>()
                .eq(com.school.teaching.entity.PracticeSession::getStudentId, studentId)
                .select(com.school.teaching.entity.PracticeSession::getId))
            .stream().map(com.school.teaching.entity.PracticeSession::getId).toList();
        if (!historySessionIds.isEmpty()) {
            List<com.school.teaching.entity.PracticeSessionItem> historyItems = itemMapper.selectList(
                new LambdaQueryWrapper<com.school.teaching.entity.PracticeSessionItem>()
                    .in(com.school.teaching.entity.PracticeSessionItem::getSessionId, historySessionIds));
            for (com.school.teaching.entity.PracticeSessionItem hi : historyItems) {
                if (hi.getQuestionId() != null) usedIds.add(hi.getQuestionId());
            }
        }
        List<QuestionBank> derivatives = new ArrayList<>();
        // ★ RAG 匹配引擎：五级 fallback（L1精准→L2兄弟→L3考纲→L4文本→L5 AI草稿）
        Map<String, Object> matchResult = questionMatchingService.matchQuestions(
            weakCategoryIds, null, 15, usedIds);
        @SuppressWarnings("unchecked")
        List<QuestionBank> matched = (List<QuestionBank>) matchResult.get("questions");
        if (matched != null) derivatives.addAll(matched);
        @SuppressWarnings("unchecked")
        Map<String, Integer> matchDetail = (Map<String, Integer>) matchResult.get("matchDetail");
        log.info("衍生练习匹配: L1={} L2={} L3={} L4={} L5={} total={}",
            matchDetail != null ? matchDetail.getOrDefault("L1_precise", 0) : 0,
            matchDetail != null ? matchDetail.getOrDefault("L2_sibling", 0) : 0,
            matchDetail != null ? matchDetail.getOrDefault("L3_syllabus", 0) : 0,
            matchDetail != null ? matchDetail.getOrDefault("L4_textTag", 0) : 0,
            matchDetail != null ? matchDetail.getOrDefault("L5_aiDraft", 0) : 0,
            derivatives.size());

        if (derivatives.isEmpty()) {
            // 所有级别均无题目，走纯 AI 兜底
            session.setStatus("generating");
            sessionMapper.updateById(session);
            if (!sortedPoints.isEmpty()) {
                self.supplementWithAi(session.getId(), studentId, 0,
                    weakCategoryIds, sortedPoints, avgDifficulty, wrongQids, qMap);
            } else {
                session.setStatus("failed");
                sessionMapper.updateById(session);
                throw new BusinessException(400, "未找到可用的衍生题目，请先丰富题库");
            }
            return Map.of("sessionId", session.getId(), "status", session.getStatus());
        }

        // 5. 按难度递进排序：组内随机，组间升序（从易到难）— 由 QuestionMatchingService 已排序
        session.setTotalQuestions(derivatives.size());
        session.setWeakPoints(toJson(weakPoints));
        sessionMapper.updateById(session);

        for (int i = 0; i < derivatives.size(); i++) {
            QuestionBank q = derivatives.get(i);
            com.school.teaching.entity.PracticeSessionItem item = new com.school.teaching.entity.PracticeSessionItem();
            item.setSessionId(session.getId());
            item.setQuestionId(q.getId());
            item.setQuestionType(q.getQuestionType());
            item.setQuestionText(q.getQuestionText());
            item.setOptions(q.getOptions());
            item.setCorrectAnswer(q.getCorrectAnswer());
            item.setExplanation(q.getExplanation());
            item.setSource(q.getStatus() != null && q.getStatus() == 1 ? "bank" : "ai");
            item.setSortOrder(i);
            itemMapper.insert(item);
        }

        // 6. 判断是否需要AI补充（不足5题才触发，避免浪费配额）
        int targetTotal = 5;
        if (derivatives.size() < targetTotal && !sortedPoints.isEmpty()) {
            // 异步AI补充
            self.supplementWithAi(session.getId(), studentId, derivatives.size(),
                weakCategoryIds, sortedPoints, avgDifficulty, wrongQids, qMap);
        } else {
            // 不需要AI，直接标记为ongoing
            session.setStatus("ongoing");
            sessionMapper.updateById(session);
        }

        return Map.of("sessionId", session.getId(), "status", session.getStatus());
        } catch (BusinessException e) {
            session.setStatus("failed");
            sessionMapper.updateById(session);
            throw e;
        } catch (Exception e) {
            session.setStatus("failed");
            sessionMapper.updateById(session);
            throw new BusinessException(500, "生成衍生练习失败: " + e.getMessage());
        }
    }

    /** 异步AI补充衍生题 — 完成后更新会话状态为ongoing */
    @Async
    public void supplementWithAi(Long sessionId, Long studentId, int currentCount,
                                  Set<Long> weakCategoryIds, List<Map.Entry<String, Integer>> sortedPoints,
                                  int avgDifficulty, Set<Long> wrongQids, Map<Long, QuestionBank> qMap) {
        try {
            // 学生上下文
            String studentStage = "中职", studentMajor = "";
            try {
                com.school.teaching.entity.Student st = studentMapper.selectById(studentId);
                if (st != null) {
                    if (st.getCurrentType() != null) studentStage = st.getCurrentType().contains("职高") ? "职高" : st.getCurrentType();
                    if (st.getClassId() != null) {
                        com.school.teaching.entity.Classes cls = classesMapper.selectById(st.getClassId());
                        if (cls != null && cls.getMajor() != null) studentMajor = cls.getMajor();
                    }
                }
            } catch (Exception ignored) { /* 学生上下文查询失败不影响AI出题，使用默认学段 */ }
            Map<Long, String> categoryContentCache = new HashMap<>();
            for (Long cid : weakCategoryIds) {
                try {
                    com.school.teaching.entity.KnowledgeNode cat = nodeMapper.selectById(cid);
                    if (cat != null && cat.getContent() != null && !cat.getContent().isBlank()) {
                        categoryContentCache.put(cid, cat.getContent());
                    }
                } catch (Exception ignored) { /* 知识点内容查询失败不影响AI出题 */ }
            }

            int targetTotal = 10;
            int need = targetTotal - currentCount;
            int randomIdx = new java.util.Random().nextInt(sortedPoints.size());
            String selectedPoint = sortedPoints.get(randomIdx).getKey();

            Map<String, Object> aiParams = new LinkedHashMap<>();
            aiParams.put("knowledgePoint", selectedPoint);
            String[] types = {"SINGLE_CHOICE", "MULTI_CHOICE", "FILL_IN"};
            aiParams.put("questionType", types[new java.util.Random().nextInt(types.length)]);
            aiParams.put("candidateCount", Math.min(need, 5));
            aiParams.put("difficultyLevel", avgDifficulty);
            aiParams.put("stageHint", studentStage);
            if (!studentMajor.isEmpty()) aiParams.put("studentMajor", studentMajor);

            Long matchedCategoryId = findCategoryForPoint(selectedPoint, wrongQids, qMap);
            if (matchedCategoryId != null) aiParams.put("categoryId", matchedCategoryId);
            else if (!weakCategoryIds.isEmpty()) aiParams.put("categoryId", weakCategoryIds.iterator().next());

            StringBuilder reference = new StringBuilder();
            if (!studentMajor.isEmpty()) reference.append("学生专业：").append(studentMajor).append("；");
            reference.append("学段：").append(studentStage).append("；");
            if (matchedCategoryId != null && categoryContentCache.containsKey(matchedCategoryId)) {
                String content = categoryContentCache.get(matchedCategoryId);
                String catName = selectedPoint;
                try {
                    com.school.teaching.entity.KnowledgeNode cat = nodeMapper.selectById(matchedCategoryId);
                    if (cat != null && cat.getName() != null) catName = cat.getName();
                } catch (Exception ignored) { /* 节点名称查询失败，使用知识点名称作为回退 */ }
                if (content.length() > 800) content = content.substring(0, 800) + "...";
                reference.append("【").append(catName).append("】").append(content).append("；");
            }
            if (reference.length() <= 5 + studentMajor.length() + studentStage.length()) {
                reference.append("请根据知识点「").append(selectedPoint).append("」名称生成题目。");
            }
            aiParams.put("referenceMaterial", reference.toString());

            // 注入考纲上下文（Phase 4.3）— 必须在 _instructionPrompt 之前，确保考纲内容被纳入 Prompt
            try {
                Long cid = matchedCategoryId != null ? matchedCategoryId
                    : (!weakCategoryIds.isEmpty() ? weakCategoryIds.iterator().next() : null);
                if (cid != null) {
                    String syllabusCtx = examSyllabusService.getSyllabusPromptContextByNode(cid);
                    if (syllabusCtx != null && !syllabusCtx.isEmpty()) {
                        aiParams.put("syllabusContext", syllabusCtx);
                    }
                }
            } catch (Exception ignored) { /* 考纲上下文注入失败不影响AI出题 */ }

            // FIX-2: 注入 _instructionPrompt，让 DeepSeekGateway 走专用 Prompt 模板而非通用分支
            aiParams.put("_instructionPrompt", buildRemedialInstruction(
                selectedPoint, aiParams, reference.toString()));
            aiParams.put("_skipGenericFormat", Boolean.TRUE);

            List<Map<String, Object>> aiQs = aiService.generateSync(studentId, aiParams);

            // 取当前最大sortOrder
            List<com.school.teaching.entity.PracticeSessionItem> existing = itemMapper.selectList(
                new LambdaQueryWrapper<com.school.teaching.entity.PracticeSessionItem>()
                    .eq(com.school.teaching.entity.PracticeSessionItem::getSessionId, sessionId)
                    .orderByDesc(com.school.teaching.entity.PracticeSessionItem::getSortOrder)
                    .last("LIMIT 1"));
            int nextOrder = existing.isEmpty() ? 0 : (existing.get(0).getSortOrder() != null ? existing.get(0).getSortOrder() + 1 : 0);

            for (Map<String, Object> aq : aiQs) {
                QuestionBank qb = new QuestionBank();
                qb.setQuestionType((String) aq.get("questionType"));
                qb.setQuestionText((String) aq.get("questionText"));
                qb.setOptions(aq.get("options") instanceof List<?> opts ? toJson(opts) : (String) aq.get("options"));
                qb.setCorrectAnswer((String) aq.get("correctAnswer"));
                qb.setStatus(0); qb.setVersion(1); qb.setIsLatest(1);
                qb.setCreatedBy(studentId);
                if (matchedCategoryId != null) qb.setCategoryId(matchedCategoryId);
                else if (!weakCategoryIds.isEmpty()) qb.setCategoryId(weakCategoryIds.iterator().next());
                qb.setKnowledgePoints(toJson(List.of(selectedPoint)));
                // 注入 knowledgeDim 和 tier（考纲维度标签）
                if (aiParams.get("knowledgeDim") instanceof String kd) qb.setKnowledgeDim(kd);
                if (aiParams.get("tier") instanceof String t) qb.setTier(t);
                questionMapper.insert(qb);

                com.school.teaching.entity.PracticeSessionItem item = new com.school.teaching.entity.PracticeSessionItem();
                item.setSessionId(sessionId);
                item.setQuestionId(qb.getId());
                item.setQuestionType(qb.getQuestionType());
                item.setQuestionText(qb.getQuestionText());
                item.setOptions(qb.getOptions());
                item.setCorrectAnswer(qb.getCorrectAnswer());
                item.setExplanation(qb.getExplanation());
                item.setSource("ai");
                item.setSortOrder(nextOrder++);
                itemMapper.insert(item);
            }

            // 更新会话：totalQuestions + status
            com.school.teaching.entity.PracticeSession session = sessionMapper.selectById(sessionId);
            if (session != null) {
                session.setTotalQuestions(session.getTotalQuestions() + aiQs.size());
                session.setStatus("ongoing");
                sessionMapper.updateById(session);
            }
        } catch (Exception e) {
            // AI失败：仍然将会话状态改为ongoing（至少有题库的题可以练习）
            try {
                com.school.teaching.entity.PracticeSession session = sessionMapper.selectById(sessionId);
                if (session != null && "generating".equals(session.getStatus())) {
                    session.setStatus("ongoing");
                    sessionMapper.updateById(session);
                }
            } catch (Exception ignored) { /* AI失败后会话状态恢复失败不影响主流程 */ }
        }
    }

    /** 供 Controller 状态轮询用，不加权限校验 */
    public com.school.teaching.entity.PracticeSession getSessionById(Long sessionId) {
        return sessionMapper.selectById(sessionId);
    }

    @Override
    public Map<String, Object> getPracticeSession(Long sessionId, Long studentId) {
        com.school.teaching.entity.PracticeSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new BusinessException(404, "会话不存在");
        if (!session.getStudentId().equals(studentId)) throw new BusinessException(403, "无权访问");
        return buildSessionResponse(session, false);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> submitPracticeSession(Long sessionId, Long studentId, List<Map<String, Object>> answers) {
        // SELECT FOR UPDATE 防并发重复提交
        com.school.teaching.entity.PracticeSession session = sessionMapper.selectOne(
            new LambdaQueryWrapper<com.school.teaching.entity.PracticeSession>()
                .eq(com.school.teaching.entity.PracticeSession::getId, sessionId)
                .last("FOR UPDATE"));
        if (session == null) throw new BusinessException(404, "会话不存在");
        if (!session.getStudentId().equals(studentId)) throw new BusinessException(403, "无权操作");
        if ("finished".equals(session.getStatus())) throw new BusinessException(400, "该练习已完成");

        List<com.school.teaching.entity.PracticeSessionItem> items = itemMapper.selectList(
            new LambdaQueryWrapper<com.school.teaching.entity.PracticeSessionItem>()
                .eq(com.school.teaching.entity.PracticeSessionItem::getSessionId, sessionId)
                .orderByAsc(com.school.teaching.entity.PracticeSessionItem::getSortOrder));

        int correct = 0, wrong = 0;
        List<Map<String, Object>> results = new ArrayList<>();

        // 批量加载已有的错题记录，避免循环内 N+1 查询
        Set<Long> itemQuestionIds = items.stream().map(com.school.teaching.entity.PracticeSessionItem::getQuestionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, WrongQuestion> existMap = itemQuestionIds.isEmpty() ? Map.of()
            : wrongMapper.selectList(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getStudentId, studentId)
                .in(WrongQuestion::getQuestionId, itemQuestionIds))
                .stream().collect(Collectors.toMap(WrongQuestion::getQuestionId, wq -> wq, (a, b) -> a));

        for (com.school.teaching.entity.PracticeSessionItem item : items) {
            String studentAnswer = "";
            for (Map<String, Object> a : answers) {
                Object aid = a.get("itemId");
                if (aid != null && aid.toString().equals(item.getId().toString())) {
                    studentAnswer = String.valueOf(a.getOrDefault("answer", ""));
                    break;
                }
            }
            item.setStudentAnswer(studentAnswer);
            item.setAnsweredAt(LocalDateTime.now());

            boolean isCorrect = checkAnswerStr(studentAnswer, item.getQuestionType(), item.getCorrectAnswer());
            item.setIsCorrect(isCorrect ? 1 : 0);
            item.setAutoScore(isCorrect ? java.math.BigDecimal.ONE : java.math.BigDecimal.ZERO);
            itemMapper.updateById(item);

            if (isCorrect) {
                correct++;
                if (item.getQuestionId() != null) {
                    WrongQuestion exist = existMap.get(item.getQuestionId());
                    if (exist != null) {
                        applySpacedRepetition(exist, true, "derived");
                        wrongMapper.updateById(exist);
                    }
                }
            } else {
                wrong++;
                if (item.getQuestionId() != null) {
                    WrongQuestion exist = existMap.get(item.getQuestionId());
                    if (exist != null) {
                        applySpacedRepetition(exist, false, null);
                        wrongMapper.updateById(exist);
                    } else {
                        WrongQuestion nw = new WrongQuestion();
                        nw.setStudentId(studentId); nw.setQuestionId(item.getQuestionId());
                        nw.setWrongCount(1); nw.setLastWrongTime(LocalDateTime.now());
                        nw.setLastPracticeTime(LocalDateTime.now());
                        nw.setPracticeCount(1);
                        nw.setSourceType("PRACTICE");
                        nw.setConsecutiveCorrect(0);
                        nw.setMasteredStreak(0);
                        wrongMapper.insert(nw);
                    }
                }
            }

            Map<String, Object> r = new LinkedHashMap<>();
            r.put("itemId", item.getId()); r.put("isCorrect", isCorrect);
            r.put("correctAnswer", item.getCorrectAnswer());
            r.put("explanation", item.getExplanation() != null ? item.getExplanation() : "");
            results.add(r);
        }

        session.setCorrectCount(correct); session.setWrongCount(wrong);
        session.setStatus("finished"); session.setFinishedAt(LocalDateTime.now());
        sessionMapper.updateById(session);

        int total = correct + wrong;
        int currentScore = total > 0 ? Math.round(correct * 100f / total) : 0;

        // 查询历史平均正确率（过去7天同源练习）
        int prevAvgScore = 0;
        prevAvgScore = getPreviousAvgScore(studentId, sessionId);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("sessionId", sessionId); resp.put("totalQuestions", total);
        resp.put("correctCount", correct); resp.put("wrongCount", wrong);
        resp.put("score", currentScore);
        resp.put("previousAvgScore", prevAvgScore);
        resp.put("results", results);
        return resp;
    }

    @Override
    public List<Map<String, Object>> weaknessAnalysis(Long studentId, String subject) {
        // 查询该学生所有未掌握错题
        List<WrongQuestion> wqs = wrongMapper.selectList(
            new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getStudentId, studentId)
                .eq(WrongQuestion::getIsMastered, 0));

        if (wqs.isEmpty()) return List.of();

        Set<Long> qids = new HashSet<>();
        for (WrongQuestion wq : wqs) qids.add(wq.getQuestionId());

        Map<Long, QuestionBank> qMap = new HashMap<>();
        if (!qids.isEmpty()) {
            List<QuestionBank> questions = questionMapper.selectBatchIds(qids);
            if (subject != null && !subject.isEmpty()) {
                questions = questions.stream()
                    .filter(q -> subject.equals(q.getSubject()))
                    .collect(java.util.stream.Collectors.toList());
            }
            for (QuestionBank q : questions) qMap.put(q.getId(), q);
        }

        // 按 knowledgeNodeId / categoryId 聚合
        Map<Long, int[]> nodeStats = new LinkedHashMap<>(); // [errorCount, latestEpoch]
        Map<Long, String> nodeNameMap = new HashMap<>();
        Set<Long> allNodeIds = new HashSet<>();

        for (WrongQuestion wq : wqs) {
            QuestionBank q = qMap.get(wq.getQuestionId());
            if (q == null) continue;
            Long nodeId = q.getCategoryId();
            if (nodeId == null) continue;
            allNodeIds.add(nodeId);
            int[] stats = nodeStats.computeIfAbsent(nodeId, k -> new int[2]);
            stats[0] += (wq.getWrongCount() != null ? wq.getWrongCount() : 1);
            long epoch = wq.getLastWrongTime() != null
                ? wq.getLastWrongTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                : 0;
            if (epoch > stats[1]) stats[1] = (int)(epoch / 1000);
        }

        // 批量查知识节点名称
        if (!allNodeIds.isEmpty()) {
            List<com.school.teaching.entity.KnowledgeNode> nodes = nodeMapper.selectBatchIds(allNodeIds);
            for (com.school.teaching.entity.KnowledgeNode n : nodes) {
                nodeNameMap.put(n.getId(), n.getName());
            }
        }

        // 排序并构建结果
        return nodeStats.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0]))
            .limit(10)
            .map(e -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("knowledgeNodeId", e.getKey());
                item.put("knowledgeNodeName", nodeNameMap.getOrDefault(e.getKey(), "知识点" + e.getKey()));
                item.put("errorCount", e.getValue()[0]);
                item.put("lastErrorTime", e.getValue()[1] > 0
                    ? java.time.Instant.ofEpochSecond(e.getValue()[1]).toString() : null);
                return item;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getWeakPoints(Long studentId) {
        List<WrongQuestion> wqs = wrongMapper.selectList(
            new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getStudentId, studentId)
                .eq(WrongQuestion::getIsMastered, 0));
        Map<String, Integer> freq = new LinkedHashMap<>();
        Set<Long> qids = new HashSet<>();
        for (WrongQuestion wq : wqs) qids.add(wq.getQuestionId());
        Map<Long, QuestionBank> qMap = new HashMap<>();
        if (!qids.isEmpty()) questionMapper.selectBatchIds(qids).forEach(q -> qMap.put(q.getId(), q));
        for (WrongQuestion wq : wqs) {
            QuestionBank q = qMap.get(wq.getQuestionId());
            if (q == null) continue;
            for (String kp : parseKnowledgePoints(q.getKnowledgePoints())) {
                freq.merge(kp, wq.getWrongCount() != null ? wq.getWrongCount() : 1, Integer::sum);
            }
        }
        return freq.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .map(e -> Map.<String, Object>of("name", e.getKey(), "frequency", e.getValue()))
            .toList();
    }

    // ── 辅助方法 ──

    /** 找到某个知识点名称对应的 question_bank.category_id（用于RAG注入路径） */
    private Long findCategoryForPoint(String point, Set<Long> wrongQids, Map<Long, QuestionBank> qMap) {
        for (Long qid : wrongQids) {
            QuestionBank q = qMap.get(qid);
            if (q != null && q.getCategoryId() != null) {
                List<String> kps = parseKnowledgePoints(q.getKnowledgePoints());
                if (kps.contains(point)) return q.getCategoryId();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> parseKnowledgePoints(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, List.class); } catch (Exception e) { return List.of(); }
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (Exception e) { return "[]"; }
    }

    /**
     * TRUE_FALSE 无选项时注入默认选项 {@code ["A. √","B. ×"]}。
     * 与 {@link com.school.teaching.precision.impl.PrecisionServiceImpl#diagnose} 中的注入逻辑保持一致。
     */
    private String ensureTrueFalseOptions(String questionType, String optionsJson) {
        if ("TRUE_FALSE".equals(questionType) && (optionsJson == null || optionsJson.isBlank() || "[]".equals(optionsJson.trim()))) {
            return "[\"A. √\",\"B. ×\"]";
        }
        return optionsJson;
    }

    private Map<String, Object> buildSessionResponse(
            com.school.teaching.entity.PracticeSession session, boolean revealAnswers) {
        List<com.school.teaching.entity.PracticeSessionItem> items = itemMapper.selectList(
            new LambdaQueryWrapper<com.school.teaching.entity.PracticeSessionItem>()
                .eq(com.school.teaching.entity.PracticeSessionItem::getSessionId, session.getId())
                .orderByAsc(com.school.teaching.entity.PracticeSessionItem::getSortOrder));

        List<Map<String, Object>> itemList = new ArrayList<>();
        for (com.school.teaching.entity.PracticeSessionItem it : items) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("itemId", it.getId()); m.put("questionType", it.getQuestionType());
            m.put("questionText", it.getQuestionText()); m.put("source", it.getSource());
            String sessionOptsJson = ensureTrueFalseOptions(it.getQuestionType(), it.getOptions());
            if (sessionOptsJson != null) {
                try { m.put("options", objectMapper.readValue(sessionOptsJson, List.class)); }
                catch (Exception e) { m.put("options", List.of()); }
            } else { m.put("options", List.of()); }
            if (revealAnswers) {
                m.put("correctAnswer", it.getCorrectAnswer());
                m.put("explanation", it.getExplanation());
                m.put("studentAnswer", it.getStudentAnswer());
                m.put("isCorrect", it.getIsCorrect());
            }
            if (it.getStudentAnswer() != null) m.put("studentAnswer", it.getStudentAnswer());
            itemList.add(m);
        }

        List<Map<String, Object>> weakPoints;
        try {
            weakPoints = objectMapper.readValue(session.getWeakPoints() != null ? session.getWeakPoints() : "[]", List.class);
        } catch (Exception e) { weakPoints = List.of(); }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("sessionId", session.getId()); resp.put("status", session.getStatus());
        resp.put("totalQuestions", session.getTotalQuestions());
        resp.put("correctCount", session.getCorrectCount());
        resp.put("wrongCount", session.getWrongCount());
        resp.put("weakPoints", weakPoints);
        resp.put("items", itemList);
        resp.put("startedAt", session.getStartedAt());
        resp.put("finishedAt", session.getFinishedAt());
        return resp;
    }

    /** 答案匹配 — 与 ExamTaskHandler.answersMatch 保持完全一致 */
    private boolean checkAnswerStr(String sa, String questionType, String ca) {
        return com.school.teaching.common.ExamTaskHandler.answersMatch(questionType, ca, sa);
    }

    @Override
    public void deleteWrongQuestion(Long id, Long studentId) {
        WrongQuestion wq = wrongMapper.selectById(id);
        if (wq == null) throw new BusinessException(404, "错题记录不存在");
        if (!wq.getStudentId().equals(studentId)) throw new BusinessException(403, "无权操作");
        wrongMapper.deleteById(id);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void batchDeleteWrongQuestions(List<Long> ids, Long studentId) {
        for (Long id : ids) {
            deleteWrongQuestion(id, studentId);
        }
    }

    /** 查询该学生最近7天同源练习的历史平均正确率 */
    private int getPreviousAvgScore(Long studentId, Long excludeSessionId) {
        try {
            List<com.school.teaching.entity.PracticeSession> prev = sessionMapper.selectList(
                new LambdaQueryWrapper<com.school.teaching.entity.PracticeSession>()
                    .eq(com.school.teaching.entity.PracticeSession::getStudentId, studentId)
                    .eq(com.school.teaching.entity.PracticeSession::getStatus, "finished")
                    .eq(com.school.teaching.entity.PracticeSession::getSourceType, "wrong_book")
                    .ne(excludeSessionId != null, com.school.teaching.entity.PracticeSession::getId, excludeSessionId)
                    .ge(com.school.teaching.entity.PracticeSession::getStartedAt, LocalDateTime.now().minusDays(7))
                    .orderByDesc(com.school.teaching.entity.PracticeSession::getStartedAt)
                    .last("LIMIT 10"));
            if (prev.isEmpty()) return 0;
            int totalScore = 0, count = 0;
            for (var p : prev) {
                if (p.getCorrectCount() != null && p.getWrongCount() != null) {
                    int t = p.getCorrectCount() + p.getWrongCount();
                    if (t > 0) { totalScore += Math.round(p.getCorrectCount() * 100f / t); count++; }
                }
            }
            return count > 0 ? Math.round((float) totalScore / count) : 0;
        } catch (Exception e) {
            // 查询失败不影响主流程
            return 0;
        }
    }

    /**
     * 遗忘曲线复习调度（Ebbinghaus 间隔重复）
     * <p>答对：按 1→3→7→14→30 天推进，完成全部5轮标记掌握。
     * 答错：重置进度，清空复习计划。</p>
     * @param wq     错题记录（会被修改但不会持久化，由调用方 updateById）
     * @param correct 本次是否正确
     * @param source  掌握来源标识（single_practice / derived）
     */
    void applySpacedRepetition(WrongQuestion wq, boolean correct, String source) {
        if (wq == null) throw new IllegalArgumentException("wrongQuestion must not be null");
        int[] intervals = {1, 3, 7, 14, 30}; // 天
        wq.setPracticeCount((wq.getPracticeCount() != null ? wq.getPracticeCount() : 0) + 1);
        wq.setLastPracticeTime(LocalDateTime.now());

        if (correct) {
            int consecutiveCorrect = (wq.getConsecutiveCorrect() != null ? wq.getConsecutiveCorrect() : 0) + 1;
            wq.setConsecutiveCorrect(consecutiveCorrect);
            int masteredStreak = wq.getMasteredStreak() != null ? wq.getMasteredStreak() : 0;

            if (masteredStreak < intervals.length) {
                int nextInterval = intervals[masteredStreak];
                wq.setNextReviewAt(LocalDateTime.now().plusDays(nextInterval));
                wq.setMasteredStreak(masteredStreak + 1);
                log.info("遗忘曲线: wrongId={} stage={}/{} nextReview=+{}day",
                    wq.getId(), masteredStreak + 1, intervals.length, nextInterval);
            }

            // 完成全部5轮 → 永久掌握
            if (wq.getMasteredStreak() >= intervals.length) {
                wq.setMasteredStreak(intervals.length);
                wq.setNextReviewAt(null);
                wq.setIsMastered(1);
                wq.setMasteredAt(LocalDateTime.now());
                wq.setMasteredSource(source);
                log.info("遗忘曲线完成: wrongId={} studentId={} 全部{}轮→永久掌握",
                    wq.getId(), wq.getStudentId(), intervals.length);
            }
        } else {
            // 答错重置
            wq.setConsecutiveCorrect(0);
            wq.setMasteredStreak(0);
            wq.setNextReviewAt(null);
            wq.setWrongCount((wq.getWrongCount() != null ? wq.getWrongCount() : 0) + 1);
            if (wq.getIsMastered() != null && wq.getIsMastered() == 1) {
                wq.setIsMastered(0);
                wq.setMasteredAt(null);
                wq.setMasteredSource(null);
            }
        }
        wq.setUpdateTime(LocalDateTime.now());
    }

    private boolean checkAnswer(String sa, QuestionBank q) {
        if (sa == null || sa.trim().isEmpty()) return false;
        sa = sa.trim();
        String ca = q.getCorrectAnswer() != null ? q.getCorrectAnswer().trim() : "";
        if ("TRUE_FALSE".equals(q.getQuestionType())) {
            String su = sa.toUpperCase().replaceAll("[.。、)）]$", "");
            String cu = ca.toUpperCase().replaceAll("[.。、)）]$", "");
            Predicate<String> isTrue = v -> Set.of("A","T","TRUE","YES","对","正确","√").contains(v);
            Predicate<String> isFalse = v -> Set.of("B","F","FALSE","NO","错","错误","×").contains(v);
            return (isTrue.test(su) && isTrue.test(cu)) || (isFalse.test(su) && isFalse.test(cu));
        }
        if ("SINGLE_CHOICE".equals(q.getQuestionType()) || "FILL_IN".equals(q.getQuestionType())) {
            if (sa.equalsIgnoreCase(ca)) return true;
            var m = Pattern.compile("^([A-Z])[.、．\\s]?").matcher(sa);
            return m.find() && m.group(1).equalsIgnoreCase(ca);
        }
        if ("MULTI_CHOICE".equals(q.getQuestionType())) {
            String[] sp = sa.split(","), cp = ca.split(",");
            Arrays.sort(sp); Arrays.sort(cp);
            return Arrays.equals(sp, cp);
        }
        return false;
    }

    // ═══════════ 学生统计（错题本首页数据卡片 + streak） ═══════════

    @Override
    public Map<String, Object> getStudentStats(Long studentId) {
        try {
            long total = wrongMapper.selectCount(
                new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getStudentId, studentId));
            long mastered = wrongMapper.selectCount(
                new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getStudentId, studentId).eq(WrongQuestion::getIsMastered, 1));
            long unmastered = total - mastered;

            // 本周练习次数（仅统计 practice_sessions，避免查询 exist=false 字段）
            long weekPractice = sessionMapper.selectCount(
                new LambdaQueryWrapper<com.school.teaching.entity.PracticeSession>()
                    .eq(com.school.teaching.entity.PracticeSession::getStudentId, studentId)
                    .eq(com.school.teaching.entity.PracticeSession::getSourceType, "wrong_book")
                    .ge(com.school.teaching.entity.PracticeSession::getStartedAt, LocalDate.now().with(java.time.DayOfWeek.MONDAY).atStartOfDay()));

            // 连续练习天数：从 practice_sessions 回溯
            int streak = 0;
            LocalDate today = LocalDate.now();
            LocalDateTime sixtyDaysAgo = today.minusDays(60).atStartOfDay();
            Set<LocalDate> activeDays = new HashSet<>();
            sessionMapper.selectList(
                new LambdaQueryWrapper<com.school.teaching.entity.PracticeSession>()
                    .eq(com.school.teaching.entity.PracticeSession::getStudentId, studentId)
                    .eq(com.school.teaching.entity.PracticeSession::getSourceType, "wrong_book")
                    .ge(com.school.teaching.entity.PracticeSession::getStartedAt, sixtyDaysAgo)
                    .select(com.school.teaching.entity.PracticeSession::getStartedAt))
                .forEach(s -> { if (s.getStartedAt() != null) activeDays.add(s.getStartedAt().toLocalDate()); });
            LocalDate cursor = today;
            while (activeDays.contains(cursor)) { streak++; cursor = cursor.minusDays(1); }

            return Map.of("total", total, "mastered", mastered, "unmastered", unmastered,
                "weekPractice", weekPractice, "streak", streak);
        } catch (Exception e) {
            log.error("getStudentStats 查询失败(studentId={}), 返回空统计", studentId, e);
            return Map.of("total", 0L, "mastered", 0L, "unmastered", 0L,
                "weekPractice", 0L, "streak", 0);
        }
    }

    // ═══════════ 教师监督 API ═══════════

    @Override
    public Map<String, Object> teacherSummary(Long teacherUserId) {
        Set<Long> classIds = getTeacherClassIds(teacherUserId);
        if (classIds.isEmpty()) return Map.of("classCount", 0, "studentCount", 0, "totalWrong", 0, "unmasteredWrong", 0);

        List<com.school.teaching.entity.Student> students = studentMapper.selectList(
            new LambdaQueryWrapper<com.school.teaching.entity.Student>().in(com.school.teaching.entity.Student::getClassId, classIds));
        Set<Long> studentIds = students.stream().map(com.school.teaching.entity.Student::getId).collect(java.util.stream.Collectors.toSet());
        if (studentIds.isEmpty()) return Map.of("classCount", classIds.size(), "studentCount", 0, "totalWrong", 0, "unmasteredWrong", 0);

        long totalWrong = wrongMapper.selectCount(
            new LambdaQueryWrapper<WrongQuestion>().in(WrongQuestion::getStudentId, studentIds));
        long unmasteredWrong = wrongMapper.selectCount(
            new LambdaQueryWrapper<WrongQuestion>().in(WrongQuestion::getStudentId, studentIds).eq(WrongQuestion::getIsMastered, 0));

        // 最近7天有练习的学生数
        long recentActiveStudents = wrongMapper.selectList(
            new LambdaQueryWrapper<WrongQuestion>()
                .in(WrongQuestion::getStudentId, studentIds)
                .ge(WrongQuestion::getLastPracticeTime, LocalDateTime.now().minusDays(7))
                .select(WrongQuestion::getStudentId))
            .stream().map(WrongQuestion::getStudentId).distinct().count();

        // 超过3天未练习且错题≥5 的需催促学生数（批量查询消除N+1）
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        Set<Long> needNudgeIds = new HashSet<>();
        if (!studentIds.isEmpty()) {
            List<WrongQuestion> allWqsSummary = wrongMapper.selectList(
                new LambdaQueryWrapper<WrongQuestion>().in(WrongQuestion::getStudentId, studentIds));
            Map<Long, Long> unmasteredCountMap = new HashMap<>();
            Map<Long, LocalDateTime> studentLatestMap = new HashMap<>();
            for (WrongQuestion wq : allWqsSummary) {
                if (wq.getIsMastered() == null || wq.getIsMastered() == 0)
                    unmasteredCountMap.merge(wq.getStudentId(), 1L, Long::sum);
                if (wq.getLastPracticeTime() != null)
                    studentLatestMap.merge(wq.getStudentId(), wq.getLastPracticeTime(), (old, nw) -> nw.isAfter(old) ? nw : old);
            }
            for (Long sid : studentIds) {
                if (unmasteredCountMap.getOrDefault(sid, 0L) < 5) continue;
                LocalDateTime latest = studentLatestMap.get(sid);
                if (latest == null || latest.isBefore(threeDaysAgo)) needNudgeIds.add(sid);
            }
        }

        return Map.of("classCount", classIds.size(), "studentCount", studentIds.size(),
            "totalWrong", totalWrong, "unmasteredWrong", unmasteredWrong,
            "recentActiveStudents", recentActiveStudents, "needNudgeStudents", needNudgeIds.size());
    }

    @Override
    public List<Map<String, Object>> teacherStudentList(Long teacherUserId) {
        Set<Long> classIds = getTeacherClassIds(teacherUserId);
        if (classIds.isEmpty()) return List.of();

        List<com.school.teaching.entity.Student> students = studentMapper.selectList(
            new LambdaQueryWrapper<com.school.teaching.entity.Student>().in(com.school.teaching.entity.Student::getClassId, classIds));
        Set<Long> studentIds = students.stream().map(com.school.teaching.entity.Student::getId).collect(java.util.stream.Collectors.toSet());

        // 批量加载用户和班级名（消除 N+1）
        Set<Long> userIds = students.stream().map(com.school.teaching.entity.Student::getUserId).collect(java.util.stream.Collectors.toSet());
        Map<Long, String> userRealNameMap = userIds.isEmpty() ? Map.of()
            : userMapper.selectBatchIds(userIds).stream().collect(java.util.stream.Collectors.toMap(com.school.teaching.entity.User::getId, u -> u.getRealName() != null ? u.getRealName() : "未知"));
        Set<Long> classIdsSet = students.stream().map(com.school.teaching.entity.Student::getClassId).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        Map<Long, String> batchClassMap = classIdsSet.isEmpty() ? Map.of()
            : classesMapper.selectBatchIds(classIdsSet).stream().collect(java.util.stream.Collectors.toMap(com.school.teaching.entity.Classes::getId, com.school.teaching.entity.Classes::getClassName));
        Map<Long, String> nameMap = new HashMap<>();
        Map<Long, String> classNameMap = new HashMap<>();
        for (com.school.teaching.entity.Student s : students) {
            nameMap.put(s.getId(), userRealNameMap.getOrDefault(s.getUserId(), "未知"));
            if (s.getClassId() != null) classNameMap.put(s.getId(), batchClassMap.getOrDefault(s.getClassId(), ""));
        }

        // 批量统计错题（一次查询含所有学生，替代循环内 N 次 selectCount）
        Map<Long, Long> totalMap = new HashMap<>();
        Map<Long, Long> unmasteredMap = new HashMap<>();
        Map<Long, LocalDateTime> latestMap = new HashMap<>();
        if (!studentIds.isEmpty()) {
            List<WrongQuestion> allWqs = wrongMapper.selectList(
                new LambdaQueryWrapper<WrongQuestion>().in(WrongQuestion::getStudentId, studentIds));
            for (WrongQuestion wq : allWqs) {
                totalMap.merge(wq.getStudentId(), 1L, Long::sum);
                if (wq.getIsMastered() == null || wq.getIsMastered() == 0)
                    unmasteredMap.merge(wq.getStudentId(), 1L, Long::sum);
                if (wq.getLastPracticeTime() != null)
                    latestMap.merge(wq.getStudentId(), wq.getLastPracticeTime(), (old, nw) -> nw.isAfter(old) ? nw : old);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Long sid : studentIds) {
            long total = totalMap.getOrDefault(sid, 0L);
            long unmastered = unmasteredMap.getOrDefault(sid, 0L);
            LocalDateTime lastPractice = latestMap.get(sid);
            long daysSincePractice = lastPractice != null
                ? java.time.Duration.between(lastPractice, LocalDateTime.now()).toDays() : 999;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", sid);
            row.put("studentName", nameMap.getOrDefault(sid, "未知"));
            row.put("className", classNameMap.getOrDefault(sid, ""));
            row.put("total", total);
            row.put("unmastered", unmastered);
            row.put("lastPracticeTime", lastPractice != null ? lastPractice.toString() : "");
            row.put("daysSincePractice", daysSincePractice);
            result.add(row);
        }
        result.sort((a, b) -> Long.compare((Long)b.get("unmastered"), (Long)a.get("unmastered")));
        return result;
    }

    @Override
    public List<Map<String, Object>> teacherWeakPoints(Long teacherUserId) {
        Set<Long> classIds = getTeacherClassIds(teacherUserId);
        if (classIds.isEmpty()) return List.of();
        List<com.school.teaching.entity.Student> students = studentMapper.selectList(
            new LambdaQueryWrapper<com.school.teaching.entity.Student>().in(com.school.teaching.entity.Student::getClassId, classIds));
        Set<Long> studentIds = students.stream().map(com.school.teaching.entity.Student::getId).collect(java.util.stream.Collectors.toSet());
        if (studentIds.isEmpty()) return List.of();

        List<WrongQuestion> wqs = wrongMapper.selectList(
            new LambdaQueryWrapper<WrongQuestion>().in(WrongQuestion::getStudentId, studentIds).eq(WrongQuestion::getIsMastered, 0));
        Set<Long> qids = new HashSet<>();
        for (WrongQuestion wq : wqs) qids.add(wq.getQuestionId());
        Map<Long, QuestionBank> qMap = new HashMap<>();
        if (!qids.isEmpty()) questionMapper.selectBatchIds(qids).forEach(q -> qMap.put(q.getId(), q));

        Map<Long, Map<String, Object>> pointStats = new LinkedHashMap<>();
        Set<Long> errorStudents = new HashSet<>();
        for (WrongQuestion wq : wqs) {
            QuestionBank q = qMap.get(wq.getQuestionId());
            if (q == null || q.getCategoryId() == null) continue;
            Long nodeId = q.getCategoryId();
            Map<String, Object> stat = pointStats.computeIfAbsent(nodeId, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("knowledgeNodeId", nodeId);
                m.put("errorCount", 0);
                m.put("studentSet", new HashSet<Long>());
                return m;
            });
            stat.put("errorCount", (Integer)stat.get("errorCount") + (wq.getWrongCount() != null ? wq.getWrongCount() : 1));
            @SuppressWarnings("unchecked")
            Set<Long> sSet = (Set<Long>) stat.get("studentSet");
            sSet.add(wq.getStudentId());
        }

        // 填充名称
        Set<Long> allNodeIds = pointStats.keySet();
        if (!allNodeIds.isEmpty()) {
            nodeMapper.selectBatchIds(allNodeIds).forEach(n -> {
                Map<String, Object> stat = pointStats.get(n.getId());
                if (stat != null) stat.put("knowledgeNodeName", n.getName() != null ? n.getName() : "知识点" + n.getId());
            });
        }

        return pointStats.values().stream()
            .sorted((a, b) -> Integer.compare((Integer)b.get("errorCount"), (Integer)a.get("errorCount")))
            .limit(15)
            .map(stat -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("knowledgeNodeId", stat.get("knowledgeNodeId"));
                m.put("knowledgeNodeName", stat.getOrDefault("knowledgeNodeName", "未知知识点"));
                m.put("errorCount", stat.get("errorCount"));
                @SuppressWarnings("unchecked")
                Set<Long> sSet = (Set<Long>) stat.get("studentSet");
                m.put("studentCount", sSet != null ? sSet.size() : 0);
                return m;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    private Set<Long> getTeacherClassIds(Long teacherUserId) {
        if (teacherUserId == null) return Set.of();
        // 班主任所管班级
        List<com.school.teaching.entity.Classes> headClasses = classesMapper.selectList(
            new LambdaQueryWrapper<com.school.teaching.entity.Classes>()
                .eq(com.school.teaching.entity.Classes::getHeadTeacherId, teacherUserId));
        Set<Long> ids = headClasses.stream().map(com.school.teaching.entity.Classes::getId).collect(java.util.stream.Collectors.toSet());
        // 任课教师班级（通过 teacherService）
        if (teacherService != null) {
            try { ids.addAll(teacherService.getTeachingClassIds(teacherUserId)); } catch (Exception ignored) { /* 教师班级查询失败，跳过该教师 */ }
        }
        return ids;
    }
    // ════════════════════════════════════════════════════════
    // 教师干预（Phase 2）
    // ════════════════════════════════════════════════════════

    @Override
    public List<Map<String, Object>> teacherStudentWrongDetail(Long teacherUserId, Long studentId, int mastered) {
        // 验证教师可访问该学生
        Set<Long> classIds = getTeacherClassIds(teacherUserId);
        if (classIds.isEmpty()) return List.of();
        com.school.teaching.entity.Student student = studentMapper.selectById(studentId);
        if (student == null || !classIds.contains(student.getClassId())) return List.of();

        // 查询该生错题
        List<WrongQuestion> wqList = wrongMapper.selectList(
            new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getStudentId, studentId)
                .eq(mastered >= 0, WrongQuestion::getIsMastered, mastered)
                .orderByDesc(WrongQuestion::getLastWrongTime));

        if (wqList.isEmpty()) return List.of();

        // 批量加载题目信息
        List<Long> qIds = wqList.stream().map(WrongQuestion::getQuestionId).distinct().toList();
        Map<Long, QuestionBank> qMap = questionMapper.selectBatchIds(qIds).stream()
            .collect(java.util.stream.Collectors.toMap(QuestionBank::getId, q -> q, (a, b) -> a));

        return wqList.stream().map(wq -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", wq.getId());
            m.put("wrongCount", wq.getWrongCount());
            m.put("lastWrongTime", wq.getLastWrongTime());
            m.put("isMastered", wq.getIsMastered());
            m.put("sourceType", wq.getSourceType());
            m.put("sourceTaskId", wq.getSourceTaskId());
            QuestionBank qb = qMap.get(wq.getQuestionId());
            if (qb != null) {
                m.put("questionText", qb.getQuestionText());
                m.put("questionType", qb.getQuestionType());
                m.put("options", ensureTrueFalseOptions(qb.getQuestionType(), qb.getOptions()));
                m.put("correctAnswer", qb.getCorrectAnswer());
                m.put("explanation", qb.getExplanation());
                m.put("subject", qb.getSubject());
                m.put("knowledgeDim", qb.getKnowledgeDim());
                m.put("tier", qb.getTier());
            }
            return m;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void notifyStudentReview(Long teacherUserId, Long studentId) {
        // 验证教师权限
        Set<Long> classIds = getTeacherClassIds(teacherUserId);
        if (classIds.isEmpty()) return;
        com.school.teaching.entity.Student student = studentMapper.selectById(studentId);
        if (student == null || !classIds.contains(student.getClassId())) return;

        // 插入通知记录
        try {
            com.school.teaching.entity.Notification notif = new com.school.teaching.entity.Notification();
            notif.setUserId(student.getUserId());
            notif.setTitle("错题复习提醒");

            notif.setType("WRONG_REVIEW");
            notif.setIsRead(0);
            notif.setCreateTime(java.time.LocalDateTime.now());
            notif.setIsRead(0);
            notificationMapper.insert(notif);
        } catch (Exception e) {
            log.warn("发送复习提醒失败: studentId={}", studentId, e);
        }
    }

    @Override
    public Map<String, Object> teacherWeakPointsTrend(Long teacherUserId, int weeks) {
        Map<String, Object> result = new LinkedHashMap<>();
        Set<Long> classIds = getTeacherClassIds(teacherUserId);
        if (classIds.isEmpty()) { result.put("current", List.of()); result.put("previous", List.of()); return result; }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime currentStart = now.minusWeeks(1);
        java.time.LocalDateTime previousStart = now.minusWeeks(weeks);
        java.time.LocalDateTime previousEnd = currentStart;

        // 本周薄弱点
        List<Map<String, Object>> current = computeWeakPointsByTime(classIds, currentStart, now);
        // 前N周薄弱点
        List<Map<String, Object>> previous = computeWeakPointsByTime(classIds, previousStart, previousEnd);

        // 计算趋势变化
        Map<Long, Integer> prevMap = previous.stream()
            .collect(java.util.stream.Collectors.toMap(
                m -> (Long) m.get("knowledgeNodeId"),
                m -> (Integer) m.get("errorCount"),
                (a, b) -> a));
        for (Map<String, Object> cur : current) {
            Long nid = (Long) cur.get("knowledgeNodeId");
            int curCnt = (Integer) cur.get("errorCount");
            Integer prevCnt = prevMap.get(nid);
            if (prevCnt != null && prevCnt > 0) {
                double delta = (double) (curCnt - prevCnt) / prevCnt * 100;
                cur.put("deltaPercent", Math.round(delta * 10) / 10.0);
                cur.put("trend", delta > 10 ? "up" : delta < -10 ? "down" : "stable");
            } else {
                cur.put("deltaPercent", 0);
                cur.put("trend", "new");
            }
        }

        result.put("current", current);
        result.put("previous", previous);
        return result;
    }

    private List<Map<String, Object>> computeWeakPointsByTime(Set<Long> classIds, java.time.LocalDateTime from, java.time.LocalDateTime to) {
        List<Long> studentIds = studentMapper.selectList(
            new LambdaQueryWrapper<com.school.teaching.entity.Student>()
                .in(com.school.teaching.entity.Student::getClassId, classIds))
            .stream().map(com.school.teaching.entity.Student::getId).toList();
        if (studentIds.isEmpty()) return List.of();

        List<WrongQuestion> wqList = wrongMapper.selectList(
            new LambdaQueryWrapper<WrongQuestion>()
                .in(WrongQuestion::getStudentId, studentIds)
                .between(WrongQuestion::getLastWrongTime, from, to));

        Set<Long> qids = new HashSet<>();
        for (WrongQuestion wq : wqList) qids.add(wq.getQuestionId());
        Map<Long, QuestionBank> qMap = qids.isEmpty() ? Map.of()
            : questionMapper.selectBatchIds(qids).stream()
                .collect(java.util.stream.Collectors.toMap(QuestionBank::getId, q -> q, (a, b) -> a));

        Map<Long, Integer> countMap = new java.util.LinkedHashMap<>();
        for (WrongQuestion wq : wqList) {
            QuestionBank qb = qMap.get(wq.getQuestionId());
            if (qb != null && qb.getCategoryId() != null) {
                countMap.merge(qb.getCategoryId(), 1, Integer::sum);
            }
        }
        // 批量查询知识点名称
        Set<Long> nodeIds = countMap.keySet();
        Map<Long, String> nameMap = nodeIds.isEmpty() ? Map.of()
            : nodeMapper.selectBatchIds(nodeIds).stream()
                .collect(Collectors.toMap(com.school.teaching.entity.KnowledgeNode::getId, n -> n.getName() != null ? n.getName() : "未知", (a, b) -> a));

        return countMap.entrySet().stream()
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .limit(10)
            .map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("knowledgeNodeId", e.getKey());
                m.put("knowledgeNodeName", nameMap.getOrDefault(e.getKey(), "未知"));
                m.put("errorCount", e.getValue());
                return m;
            }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getDueReviews(Long studentId) {
        List<WrongQuestion> dueList = wrongMapper.selectList(
            new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getStudentId, studentId)
                .le(WrongQuestion::getNextReviewAt, LocalDateTime.now())
                .eq(WrongQuestion::getIsMastered, 0)
                .orderByAsc(WrongQuestion::getNextReviewAt)
                .last("LIMIT 20"));
        if (dueList.isEmpty()) return List.of();

        Set<Long> qids = dueList.stream().map(WrongQuestion::getQuestionId).collect(Collectors.toSet());
        Map<Long, QuestionBank> qMap = questionMapper.selectBatchIds(qids).stream()
            .collect(Collectors.toMap(QuestionBank::getId, q -> q, (a, b) -> a));

        return dueList.stream().map(wq -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("wrongId", wq.getId());
            m.put("questionId", wq.getQuestionId());
            QuestionBank qb = qMap.get(wq.getQuestionId());
            m.put("questionText", qb != null ? qb.getQuestionText() : "");
            m.put("questionType", qb != null ? qb.getQuestionType() : "");
            m.put("nextReviewAt", wq.getNextReviewAt() != null ? wq.getNextReviewAt().toString() : null);
            m.put("masteredStreak", wq.getMasteredStreak());
            m.put("consecutiveCorrect", wq.getConsecutiveCorrect());
            return m;
        }).collect(Collectors.toList());
    }

    /** FIX-2: 构建衍生练习专用 _instructionPrompt，让 AI 走完整角色约束+质量规则 */
    private String buildRemedialInstruction(String kp, Map<String, Object> params, String reference) {
        String stageHint = String.valueOf(params.getOrDefault("stageHint", "中职"));
        String subject = params.get("subject") instanceof String s ? s : "";
        String studentMajor = String.valueOf(params.getOrDefault("studentMajor", ""));
        String questionType = String.valueOf(params.getOrDefault("questionType", "SINGLE_CHOICE"));
        int difficulty = params.get("difficultyLevel") instanceof Number n ? n.intValue() : 2;
        int count = params.get("candidateCount") instanceof Number n ? n.intValue() : 5;
        String syllabus = String.valueOf(params.getOrDefault("syllabusContext", ""));

        StringBuilder sb = new StringBuilder();
        sb.append("你是").append(stageHint);
        if (!studentMajor.isEmpty() && !"null".equals(studentMajor))
            sb.append(studentMajor).append("专业");
        if (!subject.isEmpty()) sb.append(subject);
        sb.append("教师。\n\n");
        sb.append("学生在该知识点存在薄弱环节，请生成").append(count).append("道");
        sb.append(typeLabelRemedial(questionType)).append("，帮助学生针对性巩固。\n\n");
        sb.append("【薄弱知识点】").append(kp).append("\n");
        sb.append("【难度要求】").append(difficulty).append("/5\n");
        sb.append("【出题要求】\n");
        sb.append("1. 针对该知识点最常见的易错点设计题目\n");
        sb.append("2. 每题必须包含详细的答案解析(explanation字段)\n");
        sb.append("3. 易错选项须基于典型错误认知设计\n");
        sb.append("4. 输出纯JSON数组，每题含questionText/questionType/options/correctAnswer/explanation/difficultyLevel\n");
        if (!syllabus.isEmpty() && !"null".equals(syllabus))
            sb.append("\n【考纲参考】\n").append(syllabus).append("\n");
        if (reference != null && !reference.isEmpty())
            sb.append("\n【学生背景】\n").append(reference).append("\n");
        // 命题质量控制
        sb.append("\n【命题质量控制——必须严格遵守】\n");
        sb.append("1. 选项唯一性：同一道题的所有选项内容必须互不相同。\n");
        sb.append("2. 题干差异性：禁止生成多道题干高度相似的题目。\n");
        sb.append("3. 答案字母精确：correctAnswer的字母必须严格对应选项数组的索引位置。\n");
        sb.append("4. 生成前自检：确认选项无重复、答案字母未越界后再输出。\n");
        if (promptTemplateCache != null) {
            String override = promptTemplateCache.getFinal("wrong_question_remedial", subject);
            if (override != null) return override;
        }
        return sb.toString();
    }

    private static String typeLabelRemedial(String key) {
        return switch (key) {
            case "SINGLE_CHOICE" -> "单选题";
            case "MULTI_CHOICE" -> "多选题";
            case "TRUE_FALSE" -> "判断题";
            case "FILL_IN" -> "填空题";
            case "SHORT_ANSWER", "ESSAY" -> "简答题";
            case "CALCULATION" -> "计算题";
            default -> "题目";
        };
    }
}
