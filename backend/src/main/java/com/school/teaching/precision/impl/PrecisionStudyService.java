package com.school.teaching.precision.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.common.ExamTaskHandler;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.entity.Student;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.DictSubjectMapper;
import com.school.teaching.mapper.KnowledgeNodeMapper;
import com.school.teaching.mapper.PrecisionProgressMapper;
import com.school.teaching.mapper.QuestionBankMapper;
import com.school.teaching.mapper.StudentMapper;
import com.school.teaching.mapper.WrongQuestionMapper;
import com.school.teaching.precision.PrecisionEnglishService;
import com.school.teaching.precision.PrecisionMathService;
import com.school.teaching.service.SystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PrecisionStudyService {

    @Autowired private PrecisionProgressMapper progressMapper;
    @Autowired private KnowledgeNodeMapper nodeMapper;
    @Autowired private QuestionBankMapper questionMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private WrongQuestionMapper wrongMapper;
    @Autowired private DictSubjectMapper subjectMapper;
    @Autowired(required = false) private PrecisionEnglishService englishService;
    @Autowired(required = false) private PrecisionMathService mathService;
    @Autowired private PrecisionHelper helper;
    @Autowired private SystemService systemService;

    @Autowired(required = false)
    private CalculationGradingService calcGradingService;

    // profileLocks/loadProfileForWrite/saveProfile 已提升到 PrecisionHelper 共享
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 检查学生是否已完成诊断（前置门控） */
    private void requireDiagnosisDone(Long studentId, String subject) {
        Student st = studentMapper.selectById(studentId);
        if (st == null || st.getPrecisionProfile() == null)
            throw new BusinessException(400, "请先完成「诊断」测试，解锁学习包和线上小测");
        try {
            Map<String, Object> profile = objectMapper.readValue(st.getPrecisionProfile(),
                new TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked")
            Map<String, Object> subjProfile = (Map<String, Object>) profile.get(subject);
            if (subjProfile == null || subjProfile.get("diagnoseScore") == null)
                throw new BusinessException(400, "请先完成「诊断」测试（" + subject + "），解锁学习包和线上小测");
        } catch (BusinessException e) { throw e; }
        catch (Exception e) { throw new BusinessException(400, "读取诊断状态失败，请重新登录后重试"); }
    }

    public String getWeeklyPackHtml(Long studentId, String subject, int weekNo) {
        requireDiagnosisDone(studentId, subject);
        boolean isEarlyMathMode = false;
        if (subject.contains("数学")) {
            int mathStartWeek = systemService.getIntConfig("remedial.math_start_week", 6);
            if (weekNo < mathStartWeek) {
                isEarlyMathMode = true;
            }
        }
        Map<String, Object> packData;
        if (subject.contains("英语")) {
            packData = englishService != null ? englishService.buildEnglishPackData(studentId, weekNo) : Map.of();
        } else {
            packData = mathService != null ? mathService.buildMathPackData(studentId, weekNo) : Map.of();
        }
        @SuppressWarnings("unchecked")
        List<Number> qids = (List<Number>) packData.get("questionIds");
        if (qids != null && !qids.isEmpty()) {
            synchronized (helper.getProfileLock(studentId)) {
                try {
                    Map<String, Object> profile = helper.loadProfileForWrite(studentId);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> subj = (Map<String, Object>) profile.computeIfAbsent(subject, k -> new LinkedHashMap<>());
                    subj.put("lastPackQuestionIds", qids.stream().map(Number::longValue).toList());
                    subj.put("lastPackWeekNo", weekNo);
                    helper.saveProfile(studentId, profile);
                } catch (Exception e) { log.warn("保存学习包题目ID失败 sid={}", studentId, e); }
            }
        }
        return buildPackHtml(subject, weekNo, packData, isEarlyMathMode);
    }

    public Map<String, Object> getOnlineTest(Long studentId, String subject) {
        requireDiagnosisDone(studentId, subject);
        int minDays = systemService.getIntConfig("remedial.test_unlock_days", 4);
        Student st = studentMapper.selectById(studentId);
        if (st != null && st.getPrecisionProfile() != null) {
            try {
                Map<String, Object> profile = objectMapper.readValue(st.getPrecisionProfile(),
                    new TypeReference<Map<String, Object>>() {});
                @SuppressWarnings("unchecked")
                Map<String, Object> subjProfile = (Map<String, Object>) profile.get(subject);
                if (subjProfile != null && subjProfile.get("lastDiagnoseAt") instanceof String s && !s.isEmpty()) {
                    LocalDate lastDiag = LocalDate.parse(s);
                    long daysSince = ChronoUnit.DAYS.between(lastDiag, LocalDate.now());
                    if (daysSince < minDays) {
                        throw new BusinessException(400,
                            "诊断后需学习" + minDays + "天方可参加线上小测（距诊断已" + daysSince + "天）。请先完成「学习包」中的练习。");
                    }
                }
            } catch (BusinessException e) { throw e; }
            catch (Exception e) { log.warn("getOnlineTest: 解析诊断日期失败", e); }
        }
        List<Map<String, Object>> questions;
        if (subject.contains("英语") && englishService != null) {
            questions = englishService.buildOnlineTestQuestions(studentId);
        } else if (mathService != null) {
            List<Long> packIds = null;
            try {
                if (st != null && st.getPrecisionProfile() != null) {
                    Map<String, Object> profile = objectMapper.readValue(st.getPrecisionProfile(),
                        new TypeReference<Map<String, Object>>() {});
                    @SuppressWarnings("unchecked")
                    Map<String, Object> subj = (Map<String, Object>) profile.get(subject);
                    if (subj != null && subj.get("lastPackQuestionIds") instanceof List<?> rawList) {
                        packIds = rawList.stream().map(o -> o instanceof Number n ? n.longValue()
                            : Long.valueOf(String.valueOf(o))).toList();
                    }
                }
            } catch (Exception e) { log.debug("读取学习包题目ID失败，回退随机出题: {}", e.getMessage()); }
            questions = mathService.buildOnlineTestQuestionsFromPack(studentId, packIds);
        } else {
            questions = List.of();
        }
        return Map.of("testId", UUID.randomUUID().toString(), "questions", questions,
            "deadline", LocalDate.now().plusDays(3).toString());
    }

    @Transactional
    public Map<String, Object> submitOnlineTest(Long studentId, Map<String, Object> body) {
        int correct = 0, total = 0;
        String subject = String.valueOf(body.getOrDefault("subject", ""));
        boolean revealAnswers = Boolean.TRUE.equals(body.get("revealAnswers"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) body.get("answers");
        List<Map<String, Object>> itemResults = new ArrayList<>();

        Set<Long> qids = answers != null ? answers.stream()
            .map(a -> PrecisionHelper.toLong(a.get("questionId")))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet()) : Set.of();
        Map<Long, QuestionBank> qMap = qids.isEmpty() ? Map.of()
            : questionMapper.selectBatchIds(qids).stream()
                .collect(Collectors.toMap(QuestionBank::getId, q -> q, (a, b) -> a));

        if (answers != null) {
            total = answers.size();
            for (Map<String, Object> ans : answers) {
                Long qid = PrecisionHelper.toLong(ans.get("questionId"));
                String sa = String.valueOf(ans.getOrDefault("answer", ""));
                String questionType = String.valueOf(ans.getOrDefault("questionType", "FILL_IN"));
                QuestionBank q = qid != null ? qMap.get(qid) : null;

                String frontendExpected = ans.containsKey("expected") ? String.valueOf(ans.get("expected")) : null;
                boolean isVariant = "weekly_pack_variant".equals(String.valueOf(ans.getOrDefault("source", "")));
                if (isVariant && qid != null) {
                    boolean belongsToPack = false;
                    try {
                        List<Long> packIds = getCurrentPackQuestionIds(studentId, subject);
                        belongsToPack = packIds != null && packIds.contains(qid);
                    } catch (Exception e) { log.warn("变体题归属校验失败 qid={}", qid, e); }
                    if (!belongsToPack) {
                        log.warn("安全告警：学生 sid={} 提交的变体题 qid={} 不属于当前学习包，回退原答案判定", studentId, qid);
                        isVariant = false;
                    }
                }

                boolean isCorrect = false;
                String correctAnswer = "";
                String explanation = "";
                String matchMode = "exact";

                if (sa.trim().isEmpty()) {
                    matchMode = "unanswered";
                } else if (q != null) {
                    correctAnswer = isVariant && frontendExpected != null && !frontendExpected.isEmpty()
                        ? frontendExpected
                        : (q.getCorrectAnswer() != null ? q.getCorrectAnswer().trim() : "");
                    if (correctAnswer.isEmpty()) {
                        log.warn("题目 qid={} 无正确答案(变体题={})，标记为需要复核", qid, isVariant);
                        Map<String, Object> skipItem = new LinkedHashMap<>();
                        skipItem.put("questionId", qid);
                        skipItem.put("studentAnswer", sa.trim());
                        skipItem.put("correctAnswer", "");
                        skipItem.put("explanation", "");
                        skipItem.put("isCorrect", false);
                        skipItem.put("matchMode", "needs_review");
                        skipItem.put("questionType", questionType);
                        skipItem.put("questionText", PrecisionHelper.fixEncoding(String.valueOf(ans.getOrDefault("questionText", ""))));
                        itemResults.add(skipItem);
                        continue;
                    }
                    explanation = q.getExplanation() != null ? q.getExplanation().trim() : "";
                    String qType = q.getQuestionType() != null ? q.getQuestionType() : questionType;

                    if ("CALCULATION".equals(qType) || "CALCULATION".equals(questionType)
                        || "PROOF".equals(qType) || "PROOF".equals(questionType)) {
                        // 解答题/证明题：AI 评分 + 三级置信度
                        if (sa.trim().length() >= 5) {
                            if (calcGradingService != null) {
                                Map<String, Object> aiGrade = calcGradingService.grade(q, sa.trim(), 100);
                                String aiMode = String.valueOf(aiGrade.getOrDefault("matchMode", "pending_review"));
                                int aiScore = aiGrade.get("score") instanceof Number n ? n.intValue() : 0;
                                if ("ai_graded".equals(aiMode)) {
                                    isCorrect = Boolean.TRUE.equals(aiGrade.get("isCorrect"));
                                    matchMode = "ai_graded";
                                    explanation = String.valueOf(aiGrade.getOrDefault("feedback", ""));
                                } else {
                                    matchMode = aiMode; // ai_suggested 或 pending_review
                                    explanation = String.valueOf(aiGrade.getOrDefault("feedback", ""));
                                }
                            } else {
                                matchMode = "pending_review";
                            }
                        } else {
                            matchMode = "unanswered";
                        }
                    } else if ("FILL_IN".equals(qType) || "FILL_IN".equals(questionType) || "CLOZE".equals(qType) || "CLOZE".equals(questionType)) {
                        String fallbackType = ("CLOZE".equals(qType) || "CLOZE".equals(questionType)) ? "CLOZE" : "FILL_IN";
                        isCorrect = ExamTaskHandler.answersMatch(fallbackType, correctAnswer, sa.trim());
                        matchMode = isCorrect ? "fuzzy" : "fuzzy_mismatch";
                    } else if ("ESSAY".equals(qType) || "ESSAY".equals(questionType)) {
                        matchMode = sa.trim().length() >= 5 ? "pending_review" : "unanswered";
                        if (sa.trim().length() >= 20 && correctAnswer != null && !correctAnswer.isEmpty()) {
                            int keywordHits = 0;
                            String[] refWords = correctAnswer.replaceAll("[，。！？、；：\"\"''（）\\[\\]\\s]", " ").split(" +");
                            String lowerSa = sa.trim().toLowerCase();
                            for (String w : refWords) {
                                if (w.length() >= 2 && lowerSa.contains(w.toLowerCase())) keywordHits++;
                            }
                            if (keywordHits >= Math.max(2, refWords.length / 3)) {
                                isCorrect = true;
                                matchMode = "keyword_partial";
                            }
                        }
                    } else {
                        isCorrect = ExamTaskHandler.answersMatch(
                            qType != null ? qType : questionType,
                            correctAnswer,
                            sa.trim());
                    }
                } else if (qid != null && qid < 0) {
                    String expected = "";
                    if (englishService instanceof PrecisionEnglishServiceImpl engSvc) {
                        if (qid < -100) {
                            expected = engSvc.getVocabExpected(qid, String.valueOf(ans.getOrDefault("prompt", "")));
                        } else {
                            String[] expArr = engSvc.getExpectedAndExplanation(qid);
                            if (expArr != null) {
                                expected = expArr[0];
                                explanation = expArr.length > 1 ? expArr[1] : "";
                            }
                        }
                    }
                    correctAnswer = expected;
                    if ("SINGLE_CHOICE".equals(questionType) || "TRUE_FALSE".equals(questionType)) {
                        isCorrect = ExamTaskHandler.answersMatch(questionType, expected, sa.trim());
                    } else {
                        isCorrect = PrecisionHelper.matchVocabularyAnswer(sa.trim(), expected);
                        matchMode = isCorrect ? "fuzzy" : "fuzzy_mismatch";
                    }
                }

                if (isCorrect) correct++;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("questionId", qid);
                item.put("studentAnswer", sa.trim());
                item.put("correctAnswer", revealAnswers ? PrecisionHelper.fixEncoding(correctAnswer) : "");
                item.put("explanation", PrecisionHelper.fixEncoding(explanation));
                item.put("isCorrect", isCorrect);
                item.put("matchMode", matchMode);
                item.put("questionType", questionType);
                item.put("questionText", PrecisionHelper.fixEncoding(String.valueOf(ans.getOrDefault("questionText", ""))));
                itemResults.add(item);
            }
        }
        int score = total > 0 ? Math.round((float) correct / total * 100) : 0;
        int passThreshold = systemService.getIntConfig("remedial.pass_mastery", 80);
        boolean passed = score >= passThreshold;

        for (Map<String, Object> it : itemResults) {
            Long qid = it.get("questionId") instanceof Number n ? n.longValue() : null;
            boolean isQCorrect = Boolean.TRUE.equals(it.get("isCorrect"));
            String matchMode = String.valueOf(it.getOrDefault("matchMode", ""));
            String qText = String.valueOf(it.getOrDefault("questionText", ""));
            helper.updateProgressForAnswer(studentId, qid, subject, isQCorrect);
            helper.saveWrongAnswer(studentId, qid, qText, isQCorrect, matchMode);
        }

        synchronized (helper.getProfileLock(studentId)) {
            Map<String, Object> profile = helper.loadProfileForWrite(studentId);
            @SuppressWarnings("unchecked")
            Map<String, Object> sp = (Map<String, Object>) profile.getOrDefault(subject, new LinkedHashMap<>());
            LocalDate now = LocalDate.now();
            String lastDateStr = (String) sp.get("lastSubmitDate");
            int streak = 1;
            if (lastDateStr != null && !lastDateStr.isEmpty()) {
                try {
                    LocalDate lastDate = LocalDate.parse(lastDateStr);
                    long daysBetween = ChronoUnit.DAYS.between(lastDate, now);
                    if (daysBetween >= 1 && daysBetween <= 9) {
                        streak = PrecisionHelper.toInt(sp.get("streakWeeks"), 0) + 1;
                    } else if (daysBetween == 0) {
                        streak = PrecisionHelper.toInt(sp.get("streakWeeks"), 0);
                    }
                } catch (Exception e) { log.warn("解析提交日期失败: {}", lastDateStr); }
            }
            sp.put("streakWeeks", streak);
            sp.put("lastOnlineTestScore", score);
            sp.put("lastSubmitDate", now.toString());
            profile.put(subject, sp);
            helper.saveProfile(studentId, profile);
        }

        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("score", score);
        resultMap.put("passed", passed);
        resultMap.put("correctRate", total > 0 ? Math.round((float) correct / total * 100) : 0);
        resultMap.put("correctCount", correct);
        resultMap.put("totalQuestions", total);
        resultMap.put("itemResults", itemResults);
        resultMap.put("unlockedAnswers", buildUnlockedAnswers(subject, answers));
        resultMap.put("scoringRule", "选择题/判断题：选项字母精确匹配；填空题：去标点+多答案拆分+包含匹配（模糊判分）；问答题：仅检查完成度，标记待教师评阅");
        return resultMap;
    }

    /** 生成本周习题答案（线上小测提交后解锁） */
    private List<Map<String, Object>> buildUnlockedAnswers(String subject, List<Map<String, Object>> submittedAnswers) {
        List<Map<String, Object>> answers = new ArrayList<>();
        if (submittedAnswers != null && !submittedAnswers.isEmpty()) {
            List<Long> qids = submittedAnswers.stream()
                .map(m -> PrecisionHelper.toLong(m.get("questionId")))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            if (!qids.isEmpty()) {
                List<QuestionBank> questions = questionMapper.selectBatchIds(qids);
                Map<Long, QuestionBank> qMap = questions.stream().collect(Collectors.toMap(QuestionBank::getId, q -> q));
                for (Long qid : qids) {
                    QuestionBank q = qMap.get(qid);
                    if (q != null) {
                        answers.add(Map.of(
                            "questionId", q.getId(),
                            "questionText", PrecisionHelper.fixEncoding(q.getQuestionText()),
                            "correctAnswer", PrecisionHelper.fixEncoding(q.getCorrectAnswer()),
                            "explanation", PrecisionHelper.fixEncoding(q.getExplanation() != null ? q.getExplanation() : "")
                        ));
                    }
                }
                if (!answers.isEmpty()) return answers;
            }
        }
        answers.add(Map.of("questionId", 0, "correctAnswer", "",
            "explanation", "题目数据已更新，请查看当前版本小测的答案解析。如有疑问请联系教师。"));
        return answers;
    }

    /** Jsoup 白名单清洗 — 只允许学习包所需的标签和属性，防御 XSS 注入 */
    private String sanitizePackHtml(String rawHtml) {
        if (rawHtml == null || rawHtml.isEmpty()) return "";
        try {
            org.jsoup.safety.Safelist whitelist = org.jsoup.safety.Safelist.relaxed()
                .addTags("h2", "h3", "h4", "ol", "li", "br", "span", "table", "thead", "tbody", "tr", "th", "td", "p", "strong", "em", "hr")
                .addAttributes("span", "style")
                .addAttributes("td", "style", "colspan")
                .addAttributes("th", "style", "colspan")
                .addAttributes("table", "style")
                .addAttributes("p", "style")
                .addAttributes("h2", "style")
                .addAttributes("h3", "style")
                .addAttributes("h4", "style");
            return org.jsoup.Jsoup.clean(rawHtml, whitelist);
        } catch (Exception e) {
            log.warn("Jsoup clean failed, fallback to plain text", e);
            return rawHtml.replaceAll("<[^>]+>", "");
        }
    }

    private String buildPackHtml(String subject, int weekNo, Map<String, Object> packData, boolean isEarlyMathMode) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='zh'><head><meta charset='utf-8'>")
          .append("<meta name='viewport' content='width=device-width,initial-scale=1'>")
          .append("<title>第").append(weekNo).append("周·").append(subject).append("学习包</title>")
          // KaTeX CDN — 渲染 $...$ 行内公式和 $$...$$ 块级公式
          .append("<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.css'>")
          .append("<script src='https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.js'></script>")
          .append("<script>")
          .append("function renderKatex(){if(!window.katex)return;")
          .append("var h=document.body.innerHTML;")
          .append("h=h.replace(/\\$\\$([^$]+)\\$\\$/g,function(m,f){try{return katex.renderToString(f.trim(),{displayMode:true,throwOnError:false})}catch(e){return m}});")
          .append("h=h.replace(/\\$([^$]+)\\$/g,function(m,f){try{return katex.renderToString(f.trim(),{displayMode:false,throwOnError:false})}catch(e){return m}});")
          .append("document.body.innerHTML=h;}")
          .append("if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',renderKatex);else renderKatex();")
          .append("</script>")
          .append("<style>body{font-family:'PingFang SC',sans-serif;max-width:800px;")
          .append("margin:0 auto;padding:16px;line-height:1.8;color:#333}")
          .append(".early-banner{background:#fff3e0;border-left:4px solid #ed6c02;padding:10px 14px;margin-bottom:16px;font-size:13px}")
          .append("h2{border-bottom:2px solid #4361ee;padding-bottom:8px}")
          .append("table{width:100%;border-collapse:collapse;margin:12px 0}")
          .append("td,th{border:1px solid #ddd;padding:8px;text-align:left}")
          .append("@media print{body{font-size:14px}}</style></head><body>")
          .append("<h1>第").append(weekNo).append("周 · ").append(subject);
        if (isEarlyMathMode) {
            sb.append(" · 🧪诊断期每日一练</h1>")
              .append("<div class='early-banner'><strong>🔬 诊断期模式</strong> — 这是精简版的每日练习，旨在帮助你熟悉数学题型和保持手感。正式学习包从第6周开始推送，包含完整的分步提示和解答题拆解。</div>");
        } else {
            sb.append("学习包</h1>");
        }
        Object rawContent = packData.getOrDefault("content", "内容加载中...");
        String safeContent = sanitizePackHtml(rawContent.toString());
        sb.append("<div>").append(safeContent)
          .append("</div></body></html>");
        return sb.toString();
    }

    private List<Long> getCurrentPackQuestionIds(Long studentId, String subject) {
        try {
            Student st = studentMapper.selectById(studentId);
            if (st != null && st.getPrecisionProfile() != null) {
                Map<String, Object> profile = objectMapper.readValue(st.getPrecisionProfile(),
                    new TypeReference<Map<String, Object>>() {});
                @SuppressWarnings("unchecked")
                Map<String, Object> subj = (Map<String, Object>) profile.get(subject);
                if (subj != null && subj.get("lastPackQuestionIds") instanceof List<?> raw) {
                    return raw.stream().map(o -> o instanceof Number n ? n.longValue()
                        : Long.valueOf(String.valueOf(o))).toList();
                }
            }
        } catch (Exception e) { log.debug("获取pack题目ID失败 sid={}", studentId, e); }
        return List.of();
    }

    /** 获取学习包结构化题目数据（在线答题模式，无时间限制） */
    public List<Map<String, Object>> getPackQuestions(Long studentId, String subject) {
        requireDiagnosisDone(studentId, subject);
        List<Long> packIds = getCurrentPackQuestionIds(studentId, subject);

        // 如果 profile 中没有 pack IDs（首次生成前），尝试实时构建
        if (packIds.isEmpty()) {
            if (subject.contains("英语") && englishService != null) {
                return englishService.buildOnlineTestQuestions(studentId);
            } else if (mathService != null) {
                return mathService.buildOnlineTestQuestionsFromPack(studentId, null);
            }
            return List.of();
        }

        // 批量查询题目
        List<QuestionBank> questions = questionMapper.selectBatchIds(packIds);
        Map<Long, QuestionBank> qMap = questions.stream()
            .collect(Collectors.toMap(QuestionBank::getId, q -> q, (a, b) -> a));

        // 按 pack 中原始顺序排列
        List<Map<String, Object>> result = new ArrayList<>();
        for (Long qid : packIds) {
            QuestionBank q = qMap.get(qid);
            if (q == null) continue;
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
            m.put("correctAnswer", PrecisionHelper.fixEncoding(q.getCorrectAnswer()));
            m.put("explanation", q.getExplanation() != null ? PrecisionHelper.fixEncoding(q.getExplanation()) : "");
            m.put("difficultyLevel", q.getDifficultyLevel());
            result.add(m);
        }
        return result;
    }
}
