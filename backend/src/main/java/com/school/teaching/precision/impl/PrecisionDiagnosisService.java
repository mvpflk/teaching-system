package com.school.teaching.precision.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.common.ExamTaskHandler;
import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.entity.PrecisionProgress;
import com.school.teaching.entity.PrecisionVocabulary;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.entity.Student;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.DictSubjectMapper;
import com.school.teaching.mapper.KnowledgeNodeMapper;
import com.school.teaching.mapper.PrecisionProgressMapper;
import com.school.teaching.mapper.PrecisionVocabularyMapper;
import com.school.teaching.mapper.QuestionBankMapper;
import com.school.teaching.mapper.StudentMapper;
import com.school.teaching.mapper.WrongQuestionMapper;
import com.school.teaching.precision.PrecisionEnglishService;
import com.school.teaching.precision.PrecisionMathService;
import com.school.teaching.service.ExamSyllabusService;
import com.school.teaching.service.SystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PrecisionDiagnosisService {

    @Autowired
    private PrecisionProgressMapper progressMapper;

    @Autowired
    private PrecisionVocabularyMapper vocabMapper;

    @Autowired
    private KnowledgeNodeMapper nodeMapper;

    @Autowired
    private QuestionBankMapper questionMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private DictSubjectMapper subjectMapper;

    @Autowired
    private WrongQuestionMapper wrongMapper;

    @Autowired(required = false)
    private PrecisionEnglishService englishService;

    @Autowired(required = false)
    private PrecisionMathService mathService;

    @Autowired
    private ExamSyllabusService examSyllabusService;

    @Autowired
    private SystemService systemService;

    @Autowired
    private AutoGroupService autoGroupService;

    @Autowired
    private PrecisionHelper helper;

    @Autowired(required = false)
    private CalculationGradingService calcGradingService;

    @Autowired(required = false)
    private com.school.teaching.mapper.AiOutputMapper aiOutputMapper;

    // profileLocks/loadProfileForWrite/saveProfile 已提升到 PrecisionHelper 共享，避免并发覆盖
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> getDashboard(Long studentId) {
        Map<String, Object> result = new LinkedHashMap<>();

        Student st = studentMapper.selectById(studentId);
        Map<String, Object> profileMap = new LinkedHashMap<>();
        if (st != null && st.getPrecisionProfile() != null) {
            try {
                profileMap = objectMapper.readValue(st.getPrecisionProfile(),
                    new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("解析precision_profile失败，使用空画像: {}", e.getMessage());
            }
        }

        long engVocab = vocabMapper.selectCount(
            new LambdaQueryWrapper<PrecisionVocabulary>().eq(PrecisionVocabulary::getStudentId, studentId));
        long engMastered = vocabMapper.selectCount(
            new LambdaQueryWrapper<PrecisionVocabulary>()
                .eq(PrecisionVocabulary::getStudentId, studentId)
                .ge(PrecisionVocabulary::getMasterLevel, 3));
        int engStreak = 0;
        @SuppressWarnings("unchecked")
        Map<String, Object> engProfile = (Map<String, Object>) profileMap.get("英语[职高]");
        if (engProfile != null && engProfile.get("streakWeeks") instanceof Number n) engStreak = n.intValue();
        result.put("english", Map.of("vocabTotal", engVocab, "vocabMastered", engMastered,
            "streakWeeks", engStreak));

        if (englishService != null) {
            try {
                Map<String, Object> engDash = englishService.getDashboard(studentId);
                Map<String, Object> mergedEng = new LinkedHashMap<>();
                mergedEng.put("vocabTotal", engVocab);
                mergedEng.put("vocabMastered", engMastered);
                mergedEng.put("streakWeeks", engStreak);
                mergedEng.putAll(engDash);
                result.put("english", mergedEng);
            } catch (Exception e) {
                log.warn("获取英语完整仪表盘失败 sid={}", studentId, e);
            }
        }

        List<PrecisionProgress> mathProg = progressMapper.selectList(
            new LambdaQueryWrapper<PrecisionProgress>()
                .eq(PrecisionProgress::getStudentId, studentId)
                .eq(PrecisionProgress::getSubject, "数学[职高]"));
        long mathTotal = mathProg.size();
        long mathMastered = mathProg.stream().filter(p -> "mastered".equals(p.getStatus())).count();
        double mathAvg = mathProg.stream().mapToDouble(p ->
            p.getMasteryPercent() != null ? p.getMasteryPercent().doubleValue() : 0).average().orElse(0);
        result.put("math", Map.of("nodesTotal", mathTotal, "nodesMastered", mathMastered,
            "avgMastery", Math.round(mathAvg)));

        result.put("profile", profileMap);

        return result;
    }

    private Map<String, Object> checkDiagnosisCooldown(Long studentId, String subject) {
        Student st = studentMapper.selectById(studentId);
        if (st == null || st.getPrecisionProfile() == null) return null;
        try {
            Map<String, Object> profile = objectMapper.readValue(st.getPrecisionProfile(),
                new TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked")
            Map<String, Object> subjProfile = (Map<String, Object>) profile.get(subject);
            if (subjProfile != null && subjProfile.get("lastDiagnoseAt") instanceof String s
                && !s.isEmpty() && subjProfile.get("diagnoseScore") instanceof Number ns) {
                LocalDate lastDate = LocalDate.parse(s);
                long daysBetween = ChronoUnit.DAYS.between(lastDate, LocalDate.now());
                if (daysBetween < 7) {
                    return Map.of("cooldown", true, "lastDiagnoseAt", s,
                        "lastScore", ns.intValue(), "remainingDays", 7 - daysBetween,
                        "message", "距上次诊断仅" + daysBetween + "天，需满7天后方可重新诊断。已完成诊断分数：" + ns.intValue() + "分");
                }
            }
        } catch (Exception e) {
            log.warn("读取诊断冷冻期数据失败: {}", e.getMessage());
        }
        return null;
    }

    public Map<String, Object> getDiagnosis(Long studentId, String subject) {
        Map<String, Object> cooldown = checkDiagnosisCooldown(studentId, subject);
        if (cooldown != null) return cooldown;

        if (subject.contains("英语") && englishService != null) {
            return englishService.diagnose(studentId);
        }
        if (subject.contains("数学") && mathService != null) {
            Map<String, Object> mathResult = mathService.diagnose(studentId);
            // mathService.diagnose() might return Map.of() (immutable) — wrap in modifiable map
            mathResult = new LinkedHashMap<>(mathResult);
            try {
                Long sid = helper.getSubjectId(subject);
                if (sid != null) {
                    var syllabi = examSyllabusService.getSyllabiByNodeId(sid);
                    if (syllabi != null && !syllabi.isEmpty()) {
                        mathResult.put("syllabusTitle", syllabi.get(0).getTitle());
                    }
                }
            } catch (Exception ignored) {
                log.warn("注入考纲标题失败 subject={}", subject);
            }
            return mathResult;
        }

        List<QuestionBank> pool = questionMapper.selectList(
            new LambdaQueryWrapper<QuestionBank>().eq(QuestionBank::getSubject, subject)
                .eq(QuestionBank::getStatus, 1));

        if (pool.size() < 15) {
            String broadSubject = subject.replaceAll("\\[.*\\]", "").trim();
            Set<Long> existingIds = pool.stream().map(QuestionBank::getId).collect(Collectors.toSet());
            pool.addAll(questionMapper.selectList(
                new LambdaQueryWrapper<QuestionBank>()
                    .like(QuestionBank::getSubject, broadSubject)
                    .eq(QuestionBank::getStatus, 1)
                    .notIn(!existingIds.isEmpty(), QuestionBank::getId, existingIds)
                    .last("LIMIT 100")));
        }

        String stageTag = subject.contains("[") ? subject.substring(subject.indexOf("[")) : "";
        if (!stageTag.isEmpty()) {
            List<QuestionBank> exactMatch = pool.stream()
                .filter(q -> q.getSubject().contains(stageTag))
                .collect(Collectors.toList());
            if (exactMatch.size() >= 5) pool = exactMatch;
        }

        if (pool.size() < 5) {
            return Map.of("subject", subject, "totalQuestions", 0, "questions", List.of(),
                "warning", subject + "题库题目不足（仅" + pool.size() + "题），请联系管理员导入种子数据");
        }

        Collections.shuffle(pool);
        int count = Math.min(pool.size(), 15);
        List<Map<String, Object>> questions = pool.stream().limit(count).map(q -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("questionId", q.getId());
            m.put("questionType", q.getQuestionType());
            m.put("questionText", PrecisionHelper.fixEncoding(q.getQuestionText()));
            String optsJson = q.getOptions();
            String qType = q.getQuestionType();
            if ("TRUE_FALSE".equals(qType) && (optsJson == null || optsJson.isBlank() || "[]".equals(optsJson.trim()))) {
                optsJson = "[\"A. √\",\"B. ×\"]";
            }
            m.put("options", PrecisionHelper.parseJson(optsJson));
            m.put("difficultyLevel", q.getDifficultyLevel());
            m.put("tier", q.getTier());
            m.put("knowledgePoints", q.getKnowledgePoints());
            return m;
        }).collect(Collectors.toList());

        String syllabusTitle = null;
        try {
            Long sid = helper.getSubjectId(subject);
            if (sid != null) {
                var syllabi = examSyllabusService.getSyllabiByNodeId(sid);
                if (syllabi != null && !syllabi.isEmpty()) {
                    syllabusTitle = syllabi.get(0).getTitle();
                }
            }
        } catch (Exception ignored) { log.warn("获取考纲标题失败: {}", ignored.getMessage(), ignored); }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("subject", subject);
        result.put("totalQuestions", questions.size());
        result.put("questions", questions);
        if (syllabusTitle != null) result.put("syllabusTitle", syllabusTitle);
        return result;
    }

    @Transactional
    public Map<String, Object> submitDiagnosis(Long studentId, String subject, List<Map<String, Object>> answers) {
        int correct = 0, total = answers.size();
        int choiceCorrect = 0, choiceTotal = 0;
        int fillInCorrect = 0, fillInTotal = 0;
        int essayPending = 0, essayTotal = 0;
        List<Map<String, Object>> itemResults = new ArrayList<>();

        Set<Long> qids = answers.stream()
            .map(a -> PrecisionHelper.toLong(a.get("questionId")))
            .filter(id -> id != null && id > 0)
            .collect(Collectors.toSet());
        Map<Long, QuestionBank> qMap = qids.isEmpty() ? Map.of()
            : questionMapper.selectBatchIds(qids).stream()
                .collect(Collectors.toMap(QuestionBank::getId, q -> q, (a, b) -> a));

        for (Map<String, Object> ans : answers) {
            Long qid = PrecisionHelper.toLong(ans.get("questionId"));
            String studentAnswer = String.valueOf(ans.getOrDefault("answer", "")).trim();
            String questionType = String.valueOf(ans.getOrDefault("questionType", "FILL_IN"));

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("questionId", qid);
            item.put("studentAnswer", studentAnswer);
            item.put("questionType", questionType);

            boolean isCorrect = false;
            String correctAnswer = "";
            String explanation = "";
            String matchMode = "exact";

            if (studentAnswer.isEmpty()) {
                matchMode = "unanswered";
            } else if (qid != null && qid > 0) {
                QuestionBank q = qMap.get(qid);
                if (q != null) {
                    correctAnswer = q.getCorrectAnswer() != null ? q.getCorrectAnswer().trim() : "";
                    explanation = q.getExplanation() != null ? q.getExplanation().trim() : "";
                    String qType = q.getQuestionType() != null ? q.getQuestionType() : questionType;

                    if ("CALCULATION".equals(qType) || "CALCULATION".equals(questionType)
                        || "PROOF".equals(qType) || "PROOF".equals(questionType)) {
                        // 解答题/证明题：AI 评分 + 教师审核（三级置信度）
                        essayTotal++;
                        if (studentAnswer.length() >= 5) {
                            if (calcGradingService != null) {
                                Map<String, Object> aiGrade = calcGradingService.grade(q, studentAnswer, 100);
                                String aiMode = String.valueOf(aiGrade.getOrDefault("matchMode", "pending_review"));
                                int aiScore = aiGrade.get("score") instanceof Number n ? n.intValue() : 0;
                                if ("ai_graded".equals(aiMode)) {
                                    isCorrect = Boolean.TRUE.equals(aiGrade.get("isCorrect"));
                                    matchMode = "ai_graded";
                                    explanation = String.valueOf(aiGrade.getOrDefault("feedback", ""));
                                    if (isCorrect) { correct++; fillInCorrect++; }
                                    else fillInTotal++;
                                } else if ("ai_suggested".equals(aiMode)) {
                                    matchMode = "ai_suggested";
                                    essayPending++;
                                    explanation = String.valueOf(aiGrade.getOrDefault("feedback", ""));
                                    item.put("aiScore", aiScore);
                                    item.put("aiConfidence", aiGrade.get("confidence"));
                                    saveCalcReview(studentId, qid, q.getQuestionText(), studentAnswer,
                                        q.getCorrectAnswer(), aiScore, (Double) aiGrade.get("confidence"), explanation);
                                } else {
                                    matchMode = "pending_review";
                                    essayPending++;
                                    explanation = String.valueOf(aiGrade.getOrDefault("feedback", ""));
                                    saveCalcReview(studentId, qid, q.getQuestionText(), studentAnswer,
                                        q.getCorrectAnswer(), 0, 0.0, explanation);
                                }
                            } else {
                                matchMode = "pending_review";
                                essayPending++;
                            }
                        } else {
                            matchMode = "unanswered";
                        }
                    } else if ("ESSAY".equals(qType) || "ESSAY".equals(questionType)) {
                        essayTotal++;
                        if (studentAnswer.length() >= 5) {
                            matchMode = "pending_review";
                            essayPending++;
                        } else {
                            matchMode = "unanswered";
                        }
                    } else if ("FILL_IN".equals(qType) || "FILL_IN".equals(questionType)) {
                        fillInTotal++;
                        isCorrect = PrecisionHelper.matchFillInAnswer(studentAnswer, correctAnswer);
                        matchMode = isCorrect ? PrecisionHelper.determineMatchMode(studentAnswer, correctAnswer) : "incorrect";
                        if (isCorrect) fillInCorrect++;
                    } else {
                        choiceTotal++;
                        isCorrect = ExamTaskHandler.answersMatch(
                            qType != null ? qType : questionType,
                            correctAnswer,
                            studentAnswer);
                        if (isCorrect) choiceCorrect++;
                    }
                } else {
                    String expected = String.valueOf(ans.getOrDefault("expected", ""));
                    correctAnswer = expected;
                    if ("FILL_IN".equals(questionType)) {
                        fillInTotal++;
                        isCorrect = PrecisionHelper.matchFillInAnswer(studentAnswer, expected);
                        if (isCorrect) fillInCorrect++;
                    } else {
                        choiceTotal++;
                        isCorrect = studentAnswer.equalsIgnoreCase(expected.trim());
                        if (isCorrect) choiceCorrect++;
                    }
                }
            } else {
                String engExpected = "";
                if (englishService instanceof PrecisionEnglishServiceImpl engSvc) {
                    if (qid != null && qid < 0) {
                        if (qid > -100) {
                            engExpected = engSvc.getVocabExpected(qid, String.valueOf(ans.getOrDefault("prompt", "")));
                        } else {
                            String[] expArr = engSvc.getExpectedAndExplanation(qid);
                            if (expArr != null) {
                                engExpected = expArr[0];
                                explanation = expArr.length > 1 ? expArr[1] : "";
                            }
                        }
                    }
                }
                correctAnswer = engExpected;
                if ("SINGLE_CHOICE".equals(questionType) || "TRUE_FALSE".equals(questionType)) {
                    choiceTotal++;
                    isCorrect = ExamTaskHandler.answersMatch(questionType, engExpected, studentAnswer);
                    if (isCorrect) choiceCorrect++;
                } else {
                    fillInTotal++;
                    isCorrect = PrecisionHelper.matchVocabularyAnswer(studentAnswer, engExpected);
                    matchMode = isCorrect ? "fuzzy" : "fuzzy_mismatch";
                    if (isCorrect) fillInCorrect++;
                }
            }

            if (isCorrect) correct++;
            item.put("correctAnswer", PrecisionHelper.fixEncoding(correctAnswer));
            item.put("explanation", PrecisionHelper.fixEncoding(explanation));
            item.put("isCorrect", isCorrect);
            item.put("matchMode", matchMode);
            String qText = String.valueOf(ans.getOrDefault("questionText", ""));
            if ((qText.isEmpty() || "null".equals(qText)) && qid != null && qMap.containsKey(qid)) {
                QuestionBank qb = qMap.get(qid);
                if (qb != null && qb.getQuestionText() != null) qText = qb.getQuestionText();
            }
            item.put("questionText", PrecisionHelper.fixEncoding(qText));
            String modName = String.valueOf(ans.getOrDefault("moduleName", ""));
            if (!modName.isEmpty() && !"null".equals(modName)) item.put("moduleName", modName);
            itemResults.add(item);
        }
        double score = total > 0 ? Math.round((double) correct / total * 100) : 0;

        synchronized (helper.getProfileLock(studentId)) {
            Map<String, Object> profile = helper.loadProfileForWrite(studentId);
            Map<String, Object> subjProfile = new LinkedHashMap<>();
            subjProfile.put("diagnoseScore", (int) score);
            subjProfile.put("estimatedScore", PrecisionHelper.estimateScore(subject, (int) score));
            subjProfile.put("lastDiagnoseAt", LocalDate.now().toString());
            profile.put(subject, subjProfile);
            helper.saveProfile(studentId, profile);
        }

        boolean shouldAutoGroup = true;
        String autoGroupReason = null;
        if (essayPending >= 3) {
            shouldAutoGroup = false;
            autoGroupReason = "问答题待评阅≥3道，跳过自动入组，等待教师人工评阅";
            log.info("submitDiagnosis: 跳过自动入组 sid={} essayPending={}", studentId, essayPending);
        } else if (total > 0) {
            int boundaryMin = systemService.getIntConfig("remedial.boundary_min", 45);
            int boundaryMax = systemService.getIntConfig("remedial.boundary_max", 55);
            if (score >= boundaryMin && score <= boundaryMax) {
                long answeredCount = itemResults.stream().filter(it -> !"unanswered".equals(it.get("matchMode"))).count();
                if (answeredCount == total) {
                    shouldAutoGroup = false;
                    autoGroupReason = "诊断分数" + (int) score + "处于边界(" + boundaryMin + "-" + boundaryMax + ")且完成率100%，标记待教师人工复核";
                    log.warn("submitDiagnosis: 边界分数不入组 sid={} score={} boundaryMin={} boundaryMax={} total={}", studentId, (int) score, boundaryMin, boundaryMax, total);
                }
            }
        }
        if (shouldAutoGroup) {
            try {
                int threshold = systemService.getIntConfig("remedial.auto_group_threshold", 50);
                if (score < threshold) {
                    autoGroupService.addSingleStudent(studentId, subject, (int) score);
                }
            } catch (Exception e) {
                log.warn("submitDiagnosis: auto-group failed sid={}", studentId, e);
            }
        }

        if (subject.contains("英语") && englishService != null) {
            try {
                englishService.determineStage(studentId, answers);
            } catch (Exception e) {
                log.warn("submitDiagnosis: determineStage failed sid={}", studentId, e);
            }
        }

        Map<String, Object> analysis = buildDiagnosisAnalysis(subject, (int) score);

        Map<String, Object> typeBreakdown = new LinkedHashMap<>();
        typeBreakdown.put("choice", Map.of("correct", choiceCorrect, "total", choiceTotal));
        typeBreakdown.put("fillIn", Map.of("correct", fillInCorrect, "total", fillInTotal));
        typeBreakdown.put("essay", Map.of("pending", essayPending, "total", essayTotal));

        String scoringRule = "选择题/判断题：选项字母精确匹配；填空题：去标点+多答案拆分+包含匹配（模糊判分）；问答题：仅检查完成度，标记待教师评阅";

        Set<Long> categoryIds = itemResults.stream()
            .filter(it -> !Boolean.TRUE.equals(it.get("isCorrect")))
            .map(it -> it.get("questionId") instanceof Number n ? n.longValue() : null)
            .filter(Objects::nonNull)
            .filter(qMap::containsKey)
            .map(qid_ -> qMap.get(qid_).getCategoryId())
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, String> nodeNameMap = categoryIds.isEmpty() ? Map.of()
            : nodeMapper.selectBatchIds(categoryIds).stream()
                .filter(n -> n.getName() != null)
                .collect(Collectors.toMap(
                    KnowledgeNode::getId,
                    KnowledgeNode::getName,
                    (a, b) -> a));

        List<Map<String, Object>> weakItems = itemResults.stream()
            .filter(it -> !Boolean.TRUE.equals(it.get("isCorrect")))
            .map(it -> {
                Map<String, Object> w = new LinkedHashMap<>();
                w.put("questionId", it.get("questionId"));
                w.put("questionText", it.get("questionText"));
                w.put("studentAnswer", it.get("studentAnswer"));
                w.put("correctAnswer", it.get("correctAnswer"));
                w.put("matchMode", it.get("matchMode"));
                w.put("questionType", it.get("questionType"));
                Long qidInner = it.get("questionId") instanceof Number n ? n.longValue() : null;
                if (qidInner != null && qMap.containsKey(qidInner)) {
                    QuestionBank qb = qMap.get(qidInner);
                    if (qb.getCategoryId() != null) {
                        w.put("knowledgeNodeId", qb.getCategoryId());
                        String nodeName = nodeNameMap.get(qb.getCategoryId());
                        if (nodeName != null) w.put("knowledgeNodeName", nodeName);
                    }
                }
                return w;
            }).collect(Collectors.toList());

        Map<String, Map<String, Object>> moduleScores = new LinkedHashMap<>();
        for (Map<String, Object> it : itemResults) {
            String mod = String.valueOf(it.getOrDefault("moduleName", "其他"));
            Map<String, Object> ms = moduleScores.computeIfAbsent(mod, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("moduleName", mod);
                m.put("correct", 0);
                m.put("total", 0);
                return m;
            });
            ms.put("total", (Integer) ms.get("total") + 1);
            if (Boolean.TRUE.equals(it.get("isCorrect"))) ms.put("correct", (Integer) ms.get("correct") + 1);
        }
        List<Map<String, Object>> moduleList = moduleScores.values().stream()
            .map(ms -> {
                int c = (Integer) ms.get("correct"), t = (Integer) ms.get("total");
                ms.put("accuracy", t > 0 ? Math.round(c * 100.0 / t) : 0);
                return ms;
            })
            .sorted((a, b) -> Integer.compare((Integer) a.get("accuracy"), (Integer) b.get("accuracy")))
            .collect(Collectors.toList());

        for (Map<String, Object> it : itemResults) {
            Long qidInner = it.get("questionId") instanceof Number n ? n.longValue() : null;
            boolean isQCorrect = Boolean.TRUE.equals(it.get("isCorrect"));
            String mm = String.valueOf(it.getOrDefault("matchMode", ""));
            String qt = String.valueOf(it.getOrDefault("questionText", ""));
            helper.updateProgressForAnswer(studentId, qidInner, subject, isQCorrect);
            helper.saveWrongAnswer(studentId, qidInner, qt, isQCorrect, mm);
        }

        Map<String, Object> diagnosisReport = new LinkedHashMap<>();
        diagnosisReport.put("correctCount", correct);
        diagnosisReport.put("totalQuestions", total);
        diagnosisReport.put("score", (int) score);
        diagnosisReport.put("subject", subject);
        diagnosisReport.put("itemResults", itemResults);
        diagnosisReport.put("typeBreakdown", typeBreakdown);
        diagnosisReport.put("scoringRule", scoringRule);
        diagnosisReport.put("weakItems", weakItems);
        diagnosisReport.put("moduleScores", moduleList);
        if (autoGroupReason != null) diagnosisReport.put("autoGroupNote", autoGroupReason);
        diagnosisReport.putAll(analysis);

        try {
            Student diagnosticStudent = studentMapper.selectById(studentId);
            if (diagnosticStudent != null && diagnosticStudent.getPrecisionProfile() != null) {
                Map<String, Object> profile = objectMapper.readValue(diagnosticStudent.getPrecisionProfile(),
                    new TypeReference<Map<String, Object>>() {});
                @SuppressWarnings("unchecked")
                Map<String, Object> sp = (Map<String, Object>) profile.get(subject);
                if (sp != null && sp.get("diagnoseScore") instanceof Number prev) {
                    int previousScore = prev.intValue();
                    int currentScore = (int) score;
                    diagnosisReport.put("previousScore", previousScore);
                    diagnosisReport.put("scoreChange", currentScore - previousScore);
                    diagnosisReport.put("isFirstDiagnosis", false);
                } else {
                    diagnosisReport.put("isFirstDiagnosis", true);
                }
            } else {
                diagnosisReport.put("isFirstDiagnosis", true);
            }
        } catch (Exception e) {
            diagnosisReport.put("isFirstDiagnosis", true);
        }

        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("diagnosisReport", diagnosisReport);
        resultMap.putAll(analysis);
        return resultMap;
    }

    public Map<String, Object> gradeOneAnswer(Long studentId, Long questionId, String answer,
                                               String subject, String questionType) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("questionId", questionId);

        QuestionBank qb = questionMapper.selectById(questionId);
        if (qb == null) {
            result.put("correct", false);
            result.put("matchMode", "not_found");
            result.put("correctAnswer", "");
            result.put("explanation", "题目不存在");
            return result;
        }

        String correctAnswer = qb.getCorrectAnswer();
        String explanation = qb.getExplanation();
        String qType = questionType != null && !questionType.isEmpty() ? questionType : qb.getQuestionType();

        if ("ESSAY".equals(qType) || "SHORT_ANSWER".equals(qType)) {
            boolean seriousAttempt = answer != null && answer.trim().replaceAll("\\s+", "").length() >= 5;
            result.put("correct", null);
            result.put("matchMode", "pending_review");
            result.put("correctAnswer", seriousAttempt ? "已收到，待教师评阅" : "");
            result.put("explanation", seriousAttempt ? "你的回答已提交，等待教师评阅。" : "请认真作答（至少5个字）。");
            return result;
        }

        if ("FILL_IN".equals(qType) || "CLOZE".equals(qType)) {
            boolean correct = PrecisionHelper.matchFillInAnswer(answer, correctAnswer);
            String actualMode = PrecisionHelper.determineMatchMode(answer, correctAnswer);
            result.put("correct", correct);
            result.put("matchMode", correct ? actualMode : "incorrect");
            result.put("correctAnswer", PrecisionHelper.fixEncoding(correctAnswer));
            result.put("explanation", explanation != null ? PrecisionHelper.fixEncoding(explanation) : "");
            return result;
        }

        boolean correct = ExamTaskHandler.answersMatch(qType, correctAnswer, answer);
        result.put("correct", correct);
        result.put("matchMode", correct ? "exact" : "incorrect");
        result.put("correctAnswer", PrecisionHelper.fixEncoding(correctAnswer));
        result.put("explanation", explanation != null ? PrecisionHelper.fixEncoding(explanation) : "");
        return result;
    }

    private Map<String, Object> buildDiagnosisAnalysis(String subject, int score) {
        Map<String, Object> analysis = new LinkedHashMap<>();
        String level;
        String advice;
        int estimatedScore = PrecisionHelper.estimateScore(subject, score);
        analysis.put("estimatedScore", estimatedScore);

        if (subject.contains("数学")) {
            analysis.put("coverage", "覆盖11模块：集合/不等式/函数/三角/数列/向量/立体几何/解析几何/概率统计/导数/初中补漏");
            if (score >= 85) {
                level = "优秀";
                advice = "11模块基础扎实，建议加强综合大题训练，冲击本科线(90分+)。";
            } else if (score >= 65) {
                level = "良好";
                advice = "多数模块掌握较好，重点关注薄弱模块，每天3-5题持续提升。";
            } else if (score >= 40) {
                level = "发展中";
                advice = "已掌握 " + score + "% 模块基础，建议从集合、数列等基础模块开始，配合分步提示每天练习。";
            } else {
                level = "起步期";
                advice = "目前已掌握 " + score + "% 内容，建议从初中补漏（方程/勾股/幂运算）开始，打好基础再进阶。";
            }
        } else if (subject.contains("英语")) {
            analysis.put("coverage", "覆盖50个高频词汇（英译中+中译英）+15道语法选择（时态/语态/从句/情态等8类考点）");
            if (score >= 85) {
                level = "优秀";
                advice = "词汇和语法基础扎实，建议加强阅读理解和写作训练，目标本科线80分+。";
            } else if (score >= 60) {
                level = "良好";
                advice = "词汇掌握较好，语法有提升空间，建议针对薄弱语法点（时态/被动语态/从句）专项练习。";
            } else if (score >= 35) {
                level = "发展中";
                advice = "已掌握 " + score + "% 词汇和语法内容，建议从高频300词开始，每天翻词卡15分钟+1个语法点练习。";
            } else {
                level = "起步期";
                advice = "已迈出 " + score + "% 的第一步，建议从最常用100词+一般现在时/过去时入手，培养每日学英语的习惯。";
            }
        } else {
            analysis.put("coverage", subject + "核心知识点");
            if (score >= 80) {
                level = "优秀";
                advice = subject + "基础扎实，建议加强综合应用训练。";
            } else if (score >= 60) {
                level = "良好";
                advice = "多数知识点掌握较好，重点关注薄弱环节，持续提升。";
            } else if (score >= 40) {
                level = "发展中";
                advice = "已掌握 " + score + "% 基础内容，建议从核心知识点开始，配合练习巩固。";
            } else {
                level = "起步期";
                advice = "目前已掌握 " + score + "% 内容，建议从最基础的知识点开始，打好基础再进阶。";
            }
        }
        analysis.put("level", level);
        analysis.put("advice", advice);
        return analysis;
    }

    /** 持久化待审解答题到 ai_outputs，供教师端审核 */
    private void saveCalcReview(Long studentId, Long questionId, String questionText,
            String studentAnswer, String correctAnswer, int aiScore, double aiConfidence, String feedback) {
        if (aiOutputMapper == null || questionId == null) return;
        try {
            com.school.teaching.entity.AiOutput review = new com.school.teaching.entity.AiOutput();
            review.setNodeId(questionId); // 复用 nodeId 存 questionId
            review.setOutputType("CALC_REVIEW");
            review.setSubject("数学[职高]");
            review.setTitle("解答题待审 — 学生" + studentId + " qid=" + questionId);
            review.setStatus(0); // 0=待审
            review.setTokensUsed(0);
            review.setLatencyMs(0);
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("studentId", studentId);
            content.put("questionId", questionId);
            content.put("questionText", questionText);
            content.put("studentAnswer", studentAnswer);
            content.put("correctAnswer", correctAnswer);
            content.put("aiScore", aiScore);
            content.put("aiConfidence", aiConfidence);
            content.put("feedback", feedback);
            review.setContent(objectMapper.writeValueAsString(content));
            aiOutputMapper.insert(review);
        } catch (Exception e) {
            log.warn("保存解答题待审记录失败 sid={} qid={}: {}", studentId, questionId, e.getMessage());
        }
    }
}
