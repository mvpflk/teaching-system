package com.school.teaching.precision.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.precision.PrecisionEnglishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PrecisionEnglishServiceImpl implements PrecisionEnglishService {

    @Autowired private PrecisionVocabularyMapper vocabMapper;
    @Autowired private PrecisionVocabularySeedMapper seedMapper;
    @Autowired private KnowledgeNodeMapper nodeMapper;
    @Autowired private QuestionBankMapper questionMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private PrecisionEnglishReadingPassageMapper passageMapper;
    @Autowired(required = false) private PrecisionEnglishQuickTestMapper quickTestMapper;
    @Autowired private PrecisionProgressMapper progressMapper;
    @Autowired private WrongQuestionMapper wrongMapper;
    @Autowired(required = false) private com.school.teaching.service.CreditService creditService;
    @Autowired(required = false) private CreditTransactionMapper creditTransactionMapper;
    @Autowired(required = false) private com.school.teaching.service.NotificationService notificationService;
    @Autowired(required = false) private com.school.teaching.service.SystemService systemService;
    @Autowired(required = false) private EnglishQuestionGenerator englishQuestionGenerator;
    @Autowired private PrecisionHelper helper;
    @Autowired private PlatformTransactionManager transactionManager;

    private static final ObjectMapper OM = new ObjectMapper();
    private static final int[] STAGE_VOCAB_TARGETS = {0, 100, 100, 200, 280, 350, 450, 500};
    private static final int[] STAGE_DAILY_QUESTIONS = {0, 6, 8, 8, 8, 8, 8, 8};
    private static final String[] STAGE_NAMES = {
        "", "🌱 生存词汇", "🏛️ 核心时态", "📦 词汇扩展",
        "⚙️ 语法基础", "🔥 语法进阶", "📖 阅读突破", "🌟 综合精通"
    };

    // ── 仪表盘 ──

    @Override
    public Map<String, Object> getDashboard(Long studentId) {
        Map<String, Object> profile = readEnglishProfile(studentId);
        int stage = toInt(profile.get("stage"), 1);
        int vocabKnown = toInt(profile.get("vocabKnown"), 0);
        int streak = toInt(profile.get("streak"), 0);
        int freezeCards = toInt(profile.get("freezeCards"), 0);
        int longestStreak = toInt(profile.get("longestStreak"), 0);
        int totalPractices = toInt(profile.get("totalPractices"), 0);
        int totalMinutes = toInt(profile.get("totalMinutes"), 0);

        Map<String, Object> dash = new LinkedHashMap<>();
        dash.put("stage", stage); dash.put("stageName", STAGE_NAMES[stage]);
        dash.put("vocabKnown", vocabKnown); dash.put("vocabTotal", STAGE_VOCAB_TARGETS[7]);
        dash.put("streak", streak); dash.put("longestStreak", longestStreak);
        dash.put("freezeCards", freezeCards); dash.put("totalPractices", totalPractices);
        dash.put("totalMinutes", totalMinutes);
        dash.put("stageProgress", profile.getOrDefault("stageProgress", Map.of()));
        dash.put("dailyTask", buildDailyTask(studentId, stage, profile));
        dash.put("practiceDates", profile.getOrDefault("practiceDates", List.of()));  // P1-3: 真实练习日期
        @SuppressWarnings("unchecked")
        List<String> achievements = (List<String>) profile.getOrDefault("achievements", List.of());
        dash.put("achievements", achievements);
        return dash;
    }

    @Override
    public Map<String, Object> buildDailyTask(Long studentId, int stage, Map<String, Object> profile) {
        int dailyCount = STAGE_DAILY_QUESTIONS[stage];
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("totalQuestions", dailyCount);
        if (stage == 1) {
            task.put("vocabCount", dailyCount); task.put("grammarCount", 0);
            task.put("vocabQuestions", pickVocabForToday(studentId, dailyCount, profile));
            task.put("grammarQuestions", List.of());
        } else {
            task.put("vocabCount", 4); task.put("grammarCount", 4);
            task.put("vocabQuestions", pickVocabForToday(studentId, 4, profile));
            task.put("grammarQuestions", pickGrammarForToday(studentId, 4, profile));
        }
        return task;
    }

    private List<Map<String, Object>> pickVocabForToday(Long studentId, int count, Map<String, Object> profile) {
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> selectedWords = new HashSet<>();  // 新增：记录本次已选词汇，防止重复
        
        // 1. 先取 dueWords（复习词汇），按 nextReviewAt 排序
        List<PrecisionVocabulary> dueWords = vocabMapper.selectList(
            new LambdaQueryWrapper<PrecisionVocabulary>()
                .eq(PrecisionVocabulary::getStudentId, studentId)
                .le(PrecisionVocabulary::getNextReviewAt, LocalDateTime.now())
                .orderByAsc(PrecisionVocabulary::getNextReviewAt));
        
        // 去重：过滤掉已选过的词汇
        for (PrecisionVocabulary w : dueWords) {
            if (result.size() >= count) break;
            String word = PrecisionHelper.fixEncoding(w.getWord());
            if (selectedWords.add(word)) {  // add 返回 false 表示已存在
                String meaning = lookupMeaning(word);
                result.add(Map.of(
                    "word", word, "type", "review",
                    "direction", Math.random() < 0.5 ? "en2cn" : "cn2en",
                    "meaning", meaning, "phonetic", ""));
            }
        }
        
        // 2. 补充新词（如果复习词不够）
        if (result.size() < count) {
            int need = count - result.size();
            Set<String> known = getKnownWords(studentId);
            List<PrecisionVocabularySeed> seeds = seedMapper.selectList(
                new LambdaQueryWrapper<PrecisionVocabularySeed>()
                    .orderByAsc(PrecisionVocabularySeed::getFrequencyRank)
                    .last("LIMIT 500"));
            seeds.stream()
                .filter(s -> !known.contains(s.getWord()) && !selectedWords.contains(s.getWord()))
                .limit(need)
                .forEach(s -> {
                    String word = PrecisionHelper.fixEncoding(s.getWord());
                    if (selectedWords.add(word)) {  // 再次确认不重复
                        result.add(Map.of(
                            "word", word, "type", "new",
                            "direction", Math.random() < 0.6 ? "en2cn" : "cn2en",
                            "phonetic", s.getPhonetic() != null ? PrecisionHelper.fixEncoding(s.getPhonetic()) : "",
                            "meaning", s.getMeaning() != null ? PrecisionHelper.fixEncoding(s.getMeaning()) : ""));
                    }
                });
        }
        return result;
    }

    /** 从种子表查找单词的中文释义（用于 review 题目补全 meaning 字段） */
    private String lookupMeaning(String word) {
        if (word == null || seedMapper == null) return "";
        try {
            // R112：stream findFirst 替代 .last("LIMIT 1")
            List<PrecisionVocabularySeed> seeds = seedMapper.selectList(
                new LambdaQueryWrapper<PrecisionVocabularySeed>().eq(PrecisionVocabularySeed::getWord, word));
            seeds = seeds.stream().limit(1).collect(Collectors.toList());
            if (seeds != null && !seeds.isEmpty() && seeds.get(0).getMeaning() != null) {
                return PrecisionHelper.fixEncoding(seeds.get(0).getMeaning());
            }
        } catch (Exception ignored) { log.warn("查找单词释义失败 word={}: {}", word, ignored.getMessage()); }
        return "";
    }

    private List<Map<String, Object>> pickGrammarForToday(Long studentId, int count, Map<String, Object> profile) {
        @SuppressWarnings("unchecked")
        List<Long> unlocked = ((List<?>) profile.getOrDefault("grammarNodesUnlocked", List.of())).stream()
            .map(o -> o instanceof Number n ? n.longValue() : Long.valueOf(String.valueOf(o)))
            .filter(id -> id > 0).toList();
        @SuppressWarnings("unchecked")
        List<Long> mastered = ((List<?>) profile.getOrDefault("grammarNodesMastered", List.of())).stream()
            .map(o -> o instanceof Number n ? n.longValue() : Long.valueOf(String.valueOf(o))).toList();
        @SuppressWarnings("unchecked")
        List<Long> skipped = ((List<?>) profile.getOrDefault("grammarNodesSkipped", List.of())).stream()
            .map(o -> o instanceof Number n ? n.longValue() : Long.valueOf(String.valueOf(o))).toList();
        Set<Long> doneSet = new HashSet<>(mastered);
        doneSet.addAll(skipped);
        Long targetNodeId = unlocked.stream().filter(id -> !doneSet.contains(id)).findFirst().orElse(null);
        if (targetNodeId == null) return List.of();
        KnowledgeNode grammarNode = nodeMapper.selectById(targetNodeId);
        if (grammarNode == null) return List.of();

        Set<Long> selectedIds = new HashSet<>();
        List<QuestionBank> finalQs = new ArrayList<>();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        // 1. 优先：已关联语法节点的已审题
        List<QuestionBank> grammarQs = questionMapper.selectList(
            new LambdaQueryWrapper<QuestionBank>()
                .eq(QuestionBank::getSubject, "英语[职高]").eq(QuestionBank::getStatus, 1)
                .eq(QuestionBank::getGrammarNodeId, targetNodeId));
        Collections.shuffle(grammarQs, rnd);
        for (QuestionBank q : grammarQs) {
            if (finalQs.size() >= count) break;
            if (selectedIds.add(q.getId())) {
                finalQs.add(q);
            }
        }

        // 2. 兜底：英语已审语法题（grammar_node_id 未设置时也能出题）
        if (finalQs.size() < count) {
            List<QuestionBank> fallbackQs = questionMapper.selectList(
                new LambdaQueryWrapper<QuestionBank>()
                    .eq(QuestionBank::getSubject, "英语[职高]").eq(QuestionBank::getStatus, 1)
                    .eq(QuestionBank::getQuestionType, "SINGLE_CHOICE")
                    .notIn(!selectedIds.isEmpty(), QuestionBank::getId, selectedIds)
                    .last("ORDER BY RAND() LIMIT " + Math.max(count * 5, 200)));
            Collections.shuffle(fallbackQs, rnd);
            for (QuestionBank q : fallbackQs) {
                if (finalQs.size() >= count) break;
                if (selectedIds.add(q.getId())) {
                    finalQs.add(q);
                }
            }
        }

        // 3. AI 补充（异步写入题库，下次请求生效）
        if (finalQs.size() < count && englishQuestionGenerator != null) {
            try {
                englishQuestionGenerator.generateGrammarQuestions(targetNodeId, count - finalQs.size());
            } catch (Exception e) {
                log.warn("AI语法题生成失败，使用现有题库", e);
            }
        }

        return finalQs.stream().map(q -> Map.<String, Object>of(
            "questionId", q.getId(), "type", "grammar", "questionType", q.getQuestionType(),
            "questionText", q.getQuestionText(), "options", parseOptions(q.getOptions()),
            "grammarNodeName", grammarNode.getName(), "direction", ""
        )).toList();
    }

    // ── 单题提交 + 三级提示 ──

    @Override
    public Map<String, Object> submitDrillAnswer(Long studentId, Long questionId,
            String answer, int hintLevel, String questionType, String subject,
            String word, String direction) {
        Map<String, Object> result = new LinkedHashMap<>();
        QuestionBank q = questionId != null && questionId > 0 ? questionMapper.selectById(questionId) : null;

        // 语法题：走题库判分
        if (q != null && q.getCorrectAnswer() != null) {
            boolean correct = isCorrect(answer, q.getCorrectAnswer(), questionType);
            if (correct) {
                result.put("correct", true); result.put("hintLevel", hintLevel);
                result.put("message", getOwlMessage("correct", hintLevel));
                result.put("explanation", q.getExplanation());
                // 更新间隔重复（如果关联了词汇）
                if (word != null && !word.isEmpty()) recordVocabResult(studentId, word, true);
            } else if (hintLevel < 2) {
                result.put("correct", false); result.put("hintLevel", hintLevel + 1);
                result.put("hint", buildHint(questionType, hintLevel + 1, q, answer));
                result.put("message", getOwlMessage("hint", hintLevel + 1));
            } else {
                result.put("correct", false); result.put("hintLevel", 3);
                result.put("correctAnswer", q.getCorrectAnswer());
                result.put("explanation", q.getExplanation());
                result.put("message", getOwlMessage("reveal", 3));
                if (word != null && !word.isEmpty()) recordVocabResult(studentId, word, false);
            }
            return result;
        }

        // 词汇题：q=null（questionId为负数，不在题库中），从种子表查预期答案
        if (word != null && !word.isEmpty() && seedMapper != null) {
            List<PrecisionVocabularySeed> seeds = seedMapper.selectList(
                new LambdaQueryWrapper<PrecisionVocabularySeed>().eq(PrecisionVocabularySeed::getWord, word));
            PrecisionVocabularySeed seed = seeds != null && !seeds.isEmpty() ? seeds.get(0) : null;

            String expected = null;
            if (seed != null) {
                if ("en2cn".equals(direction) || direction == null) {
                    expected = PrecisionHelper.fixEncoding(seed.getMeaning());
                } else {
                    expected = seed.getWord();
                }
            }

            if (expected != null) {
                boolean correct = isCorrect(answer, expected, "FILL_IN");
                if (correct) {
                    result.put("correct", true); result.put("hintLevel", hintLevel);
                    result.put("message", getOwlMessage("correct", hintLevel));
                    if (seed != null && seed.getExample() != null)
                        result.put("explanation", "📖 " + seed.getExample());
                    recordVocabResult(studentId, word, true);
                } else if (hintLevel < 2) {
                    result.put("correct", false); result.put("hintLevel", hintLevel + 1);
                    result.put("hint", buildVocabHint(hintLevel + 1, seed, word, direction));
                    result.put("message", getOwlMessage("hint", hintLevel + 1));
                } else {
                    result.put("correct", false); result.put("hintLevel", 3);
                    result.put("correctAnswer", "en2cn".equals(direction) || direction == null
                        ? expected : seed.getPhonetic() != null
                            ? expected + " /" + seed.getPhonetic() + "/" : expected);
                    result.put("explanation", seed.getExample() != null
                        ? "📖 " + seed.getExample() : seed.getMeaning() != null
                            ? "释义：" + PrecisionHelper.fixEncoding(seed.getMeaning()) : "");
                    result.put("message", getOwlMessage("reveal", 3));
                    recordVocabResult(studentId, word, false);
                }
                return result;
            }
        }

        // 彻底无法解析 — 仍返回 correctAnswer 避免前端空白
        result.put("correct", false); result.put("hintLevel", Math.min(hintLevel, 3));
        if (word != null) result.put("correctAnswer", "请复习「" + word + "」的释义和例句");
        result.put("message", getOwlMessage("reveal", 3));
        return result;
    }

    private String buildVocabHint(int level, PrecisionVocabularySeed seed, String word, String direction) {
        if (level == 1) {
            if (seed != null && seed.getExample() != null)
                return "📖 例句：" + seed.getExample();
            return "再想想看？语境可能有帮助哦。";
        }
        if (level == 2) {
            if ("en2cn".equals(direction) || direction == null) {
                if (seed != null && seed.getMeaning() != null) {
                    String meaning = PrecisionHelper.fixEncoding(seed.getMeaning());
                    String[] parts = meaning.split("[；;,，/]");
                    return "和「" + parts[0] + "」意思相近的那个词，再试一次？";
                }
            } else {
                if (word.length() >= 2)
                    return "提示：首字母 " + word.charAt(0) + "，共 " + word.length() + " 个字母";
            }
            return "排除两个不对的选项，试试看？";
        }
        return "";
    }

    private void recordVocabResult(Long studentId, String word, boolean correct) {
        try {
            PrecisionVocabulary pv = vocabMapper.selectOne(
                new LambdaQueryWrapper<PrecisionVocabulary>()
                    .eq(PrecisionVocabulary::getStudentId, studentId)
                    .eq(PrecisionVocabulary::getWord, word));
            if (pv == null) {
                pv = new PrecisionVocabulary();
                pv.setStudentId(studentId); pv.setWord(word); pv.setMasterLevel(1);
            }
            int[] intervals = {1, 3, 7, 14, 30, 60, 90};
            int currentLevel = Math.max(pv.getMasterLevel() != null ? pv.getMasterLevel() : 0, 0);
            if (correct) {
                pv.setCorrectCount((pv.getCorrectCount() != null ? pv.getCorrectCount() : 0) + 1);
                int nextLevel = Math.min(currentLevel + 1, 7);
                pv.setMasterLevel(nextLevel);
                int days = intervals[Math.min(nextLevel - 1, intervals.length - 1)];
                pv.setNextReviewAt(LocalDateTime.now().plusDays(days));
            } else {
                pv.setWrongCount((pv.getWrongCount() != null ? pv.getWrongCount() : 0) + 1);
                pv.setMasterLevel(1);
                pv.setNextReviewAt(LocalDateTime.now().plusDays(1));
            }
            pv.setLastReviewAt(LocalDateTime.now());
            if (pv.getId() == null) vocabMapper.insert(pv);
            else vocabMapper.updateById(pv);
        } catch (Exception e) { log.warn("记录词汇结果失败", e); }
    }

    private String buildHint(String questionType, int level, QuestionBank q, String answer) {
        if (level == 1) {
            if ("SINGLE_CHOICE".equals(questionType)) {
                if (q.getGrammarNodeId() != null) {
                    KnowledgeNode node = nodeMapper.selectById(q.getGrammarNodeId());
                    if (node != null && node.getContent() != null) {
                        int idx = node.getContent().indexOf("提示:");
                        if (idx >= 0) return "💡 " + node.getContent().substring(idx + 3).trim();
                    }
                    return "💡 和「" + (node != null ? node.getName() : "这个语法点") + "」有关";
                }
            }
            if ("FILL_IN".equals(questionType)) {
                String word = q.getQuestionText() != null ? q.getQuestionText().replaceAll("[「」]", "") : "";
                if (!word.isEmpty()) {
                    List<PrecisionVocabularySeed> seeds = seedMapper.selectList(
                        new LambdaQueryWrapper<PrecisionVocabularySeed>().eq(PrecisionVocabularySeed::getWord, word));
                    if (seeds != null && !seeds.isEmpty() && seeds.get(0).getExample() != null)
                        return "📖 " + seeds.get(0).getExample();
                }
            }
            return "再想想看？语境可能有帮助哦。";
        }
        if (level == 2) {
            if ("FILL_IN".equals(questionType) && q.getCorrectAnswer() != null && q.getCorrectAnswer().length() > 2)
                return "首字母是 " + q.getCorrectAnswer().charAt(0) + "，共 " + q.getCorrectAnswer().length() + " 个字母";
            return "排除两个明显不对的选项，试试看？";
        }
        return "";
    }

    // ── 完成练习 + 积分 + 宝箱 ──

    @Override
    public Map<String, Object> completeDrill(Long studentId, List<Map<String, Object>> answers, int groupSeq, int elapsedSeconds) {
        // 先获取锁再开启事务，消除 @Transactional + synchronized 窗口期
        synchronized (helper.getProfileLock(studentId)) {
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            return tx.execute(status -> doCompleteDrill(studentId, answers, groupSeq, elapsedSeconds));
        }
    }

    private Map<String, Object> doCompleteDrill(Long studentId, List<Map<String, Object>> answers, int groupSeq, int elapsedSeconds) {
        Map<String, Object> profile = readEnglishProfile(studentId);
        int correctCount = 0, totalHints = 0;
        boolean allFirstCorrect = true, usedShowAnswer = false;
        for (Map<String, Object> a : answers) {
            if (Boolean.TRUE.equals(a.get("correct"))) correctCount++;
            int hl = toInt(a.get("hintLevel"), 0); totalHints += hl;
            if (hl > 0) allFirstCorrect = false;
            if (hl >= 3) usedShowAnswer = true;
        }
        int totalPractices = toInt(profile.get("totalPractices"), 0) + 1;
        int totalMinutes = toInt(profile.get("totalMinutes"), 0) + Math.max(1, elapsedSeconds / 60);
        profile.put("totalPractices", totalPractices);
        profile.put("totalMinutes", totalMinutes);
        LocalDate today = LocalDate.now();
        // R112修复：必须在更新 lastPracticeDate 之前计算 streak，
        // 否则 updateStreak 读到的 lastDate 永远是今天，streak 永远为1
        int streak = updateStreak(profile);
        profile.put("lastPracticeDate", today.toString());
        profile.put("streak", streak);
        // R112修复：更新 longestStreak（之前从未更新，始终为初始值1）
        int longestStreak = toInt(profile.get("longestStreak"), 0);
        if (streak > longestStreak) profile.put("longestStreak", streak);
        @SuppressWarnings("unchecked")
        List<String> practiceDates = new ArrayList<>(
            (List<String>) profile.getOrDefault("practiceDates", List.of()));
        if (practiceDates.isEmpty() || !practiceDates.get(0).equals(today.toString())) {
            practiceDates.add(0, today.toString());
            if (practiceDates.size() > 90) practiceDates = practiceDates.subList(0, 90);
            profile.put("practiceDates", practiceDates);
        }
        List<String> newAchievements = checkAchievements(profile);
        int creditsEarned = 0;
        if (groupSeq == 1) {
            creditsEarned += awardCredit(studentId, "engl_daily", "engl_daily:" + studentId + ":" + today, 2);
            if (allFirstCorrect && !usedShowAnswer)
                creditsEarned += awardCredit(studentId, "engl_perfect", "engl_perfect:" + studentId + ":" + today, 3);
        }
        // 周正确率追踪
        if (groupSeq == 1) {
            int weekCorrect = toInt(profile.get("thisWeekCorrect"), 0) + correctCount;
            int weekTotal = toInt(profile.get("thisWeekTotal"), 0) + answers.size();
            profile.put("thisWeekCorrect", weekCorrect);
            profile.put("thisWeekTotal", weekTotal);
        }
        int vocabKnown = toInt(profile.get("vocabKnown"), 0);
        // P0-2: 重新计算真实 vocabKnown（刚完成的练习可能学会了新词）
        try {
            long mastered = vocabMapper.selectCount(
                new LambdaQueryWrapper<PrecisionVocabulary>()
                    .eq(PrecisionVocabulary::getStudentId, studentId)
                    .ge(PrecisionVocabulary::getMasterLevel, 3));
            vocabKnown = (int) Math.max(vocabKnown, mastered);
            profile.put("vocabKnown", vocabKnown);
        } catch (Exception e) { log.warn("completeDrill: vocabKnown recount failed", e); }
        creditsEarned += awardMilestones(studentId, vocabKnown, profile);
        profile.put("creditsEarned", toInt(profile.get("creditsEarned"), 0) + creditsEarned);
        saveEnglishProfile(studentId, profile);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("correctCount", correctCount); result.put("totalQuestions", answers.size());
        result.put("streak", streak); result.put("vocabKnown", vocabKnown);
        result.put("creditsEarned", creditsEarned); result.put("newAchievements", newAchievements);
        result.put("owlMessage", getOwlCompleteMessage(allFirstCorrect, newAchievements, vocabKnown));
        result.put("freezeCards", profile.get("freezeCards"));
        return result;
    }

    // ── 阶段判定 ──

    @Override
    public Map<String, Object> determineStage(Long studentId, List<Map<String, Object>> diagnosisAnswers) {
        // B3: 同步保护 profile 读写
        synchronized (helper.getProfileLock(studentId)) {
            int vocabCorrect = 0, grammarCorrect = 0, vocabTotal = 0, grammarTotal = 0;
            for (Map<String, Object> a : diagnosisAnswers) {
                String type = String.valueOf(a.getOrDefault("type", ""));
                if (type.contains("语法") || "SINGLE_CHOICE".equals(a.get("questionType"))) {
                    grammarTotal++; if (Boolean.TRUE.equals(a.get("correct"))) grammarCorrect++;
                } else { vocabTotal++; if (Boolean.TRUE.equals(a.get("correct"))) vocabCorrect++; }
            }
            // 估算掌握词汇量（诊断正确率 × 500词池），保持vocabKnown为计数语义，与awardMilestones/completeDrill一致
            int vocabKnown = vocabTotal > 0 ? (int)((double)vocabCorrect / vocabTotal * 500) : 0;
            int stage = 1;
            if (vocabKnown >= 400) stage = 5;
            else if (vocabKnown >= 300) stage = 4;
            else if (vocabKnown >= 200) stage = 3;
            else if (vocabKnown >= 100) stage = 2;
            // P0修复：读取已有 profile 合并，不覆盖 streak/longestStreak/practiceDates 等持续积累数据
            Map<String, Object> profile = readEnglishProfile(studentId);
            profile.put("stage", stage); profile.put("vocabKnown", vocabKnown);
            if (!profile.containsKey("streak")) profile.put("streak", 1);
            if (!profile.containsKey("longestStreak")) profile.put("longestStreak", 1);
            profile.putIfAbsent("perfectDays", 0);
            profile.putIfAbsent("totalPractices", 0);
            profile.putIfAbsent("totalMinutes", 0);
            profile.putIfAbsent("freezeCards", 0);
            profile.putIfAbsent("lastPracticeDate", LocalDate.now().toString());
            profile.put("grammarNodesMastered", profile.getOrDefault("grammarNodesMastered", List.of()));
            profile.put("grammarNodesSkipped", profile.getOrDefault("grammarNodesSkipped", List.of()));
            profile.put("grammarNodesUnlocked", computeUnlockedNodes(stage));
            profile.putIfAbsent("achievements", List.of());
            profile.putIfAbsent("creditsEarned", 0);
            profile.put("stageProgress", Map.of("vocab", 0.0, "grammar", 0.0));
            saveEnglishProfile(studentId, profile);
            return Map.of("stage", stage, "vocabKnown", vocabKnown);
        }
    }

    private List<Long> computeUnlockedNodes(int stage) {
        return nodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>()
                .isNotNull(KnowledgeNode::getGrammarCategory)
                .le(KnowledgeNode::getUnlockStage, stage)
                .orderByAsc(KnowledgeNode::getSortOrder))
            .stream().map(KnowledgeNode::getId).toList();
    }

    // ── 冰冻卡 ──

    @Override
    @Transactional
    public Map<String, Object> useFreezeCard(Long studentId) {
        synchronized (helper.getProfileLock(studentId)) {
            Map<String, Object> profile = readEnglishProfile(studentId);
            int cards = toInt(profile.get("freezeCards"), 0);
            if (cards <= 0) throw new BusinessException(400, "没有冰冻卡可用");
            profile.put("freezeCards", cards - 1);
            profile.put("streak", Math.max(1, toInt(profile.get("streak"), 0)));
            // B2: 标记今日已使用冰冻卡，updateStreak 将跳过重置
            profile.put("freezeUsedToday", LocalDate.now().toString());
            saveEnglishProfile(studentId, profile);
            if (notificationService != null)
                notificationService.notify(studentId, "english_freeze",
                    "🧊 冰冻卡", "冰冻卡已使用！火苗保住了，加油继续打卡！", null);
            return Map.of("freezeCards", cards - 1, "streak", profile.get("streak"));
        }
    }

    @Override
    public Map<String, Object> freezeCardStatus(Long studentId) {
        Map<String, Object> profile = readEnglishProfile(studentId);
        return Map.of("freezeCards", profile.getOrDefault("freezeCards", 0),
            "streak", profile.getOrDefault("streak", 0));
    }

    // ── 教师端 ──

    @Override
    public Map<String, Object> teacherEnglishStudents(Long classId) {
        List<Student> students = studentMapper.selectList(
            new LambdaQueryWrapper<Student>().eq(Student::getClassId, classId));
        List<Map<String, Object>> studentList = new ArrayList<>();
        int totalDiagnosed = 0, stage1to2 = 0, streak5plus = 0;
        for (Student st : students) {
            Map<String, Object> profile = readEnglishProfile(st.getId());
            if (profile.isEmpty() || profile.get("stage") == null) continue;
            totalDiagnosed++;
            int stage = toInt(profile.get("stage"), 1);
            int vocabKnown = toInt(profile.get("vocabKnown"), 0);
            int streak = toInt(profile.get("streak"), 0);
            if (stage <= 2) stage1to2++;
            if (streak >= 5) streak5plus++;
            studentList.add(Map.of("studentId", st.getId(), "studentName", getStudentName(st),
                "stage", stage, "vocabKnown", vocabKnown, "streak", streak));
        }
        return Map.of("totalDiagnosed", totalDiagnosed, "stage1to2", stage1to2,
            "streak5plus", streak5plus, "students", studentList);
    }

    @Override
    public Map<String, Object> teacherEnglishReport(Long classId) {
        Map<String, Object> data = teacherEnglishStudents(classId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) data.get("students");
        Map<Integer, Long> stageDist = students.stream()
            .collect(Collectors.groupingBy(s -> toInt(s.get("stage"), 1), Collectors.counting()));
        data.put("stageDistribution", stageDist);
        return data;
    }

    @Override
    public int remindClass(Long classId, String message) {
        if (notificationService == null || classId == null) return 0;
        notificationService.notifyClassStudents(classId, "english_remind",
            "小P提醒", message != null && !message.isEmpty() ? message : "今天还有英语练习未完成哦~", null);
        return 1;
    }

    // ── 辅助方法 ──

    private Map<String, Object> readEnglishProfile(Long studentId) {
        try {
            Student st = studentMapper.selectById(studentId);
            if (st == null || st.getPrecisionProfile() == null) return new LinkedHashMap<>();
            @SuppressWarnings("unchecked")
            Map<String, Object> full = OM.readValue(st.getPrecisionProfile(), Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> engProfile = (Map<String, Object>) full.getOrDefault("english", new LinkedHashMap<>());
            // B2: 如果为空，回退读 "英语[职高]" 键 (旧诊断路径写入)
            if (engProfile == null || engProfile.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fallback = (Map<String, Object>) full.get("英语[职高]");
                if (fallback != null) engProfile = fallback;
            }
            return engProfile;
        } catch (Exception e) { return new LinkedHashMap<>(); }
    }

    private void saveEnglishProfile(Long studentId, Map<String, Object> eng) {
        Student st = studentMapper.selectById(studentId);
        if (st == null) throw new BusinessException(404, "学生不存在");
        Map<String, Object> full;
        try {
            full = st.getPrecisionProfile() != null
                ? OM.readValue(st.getPrecisionProfile(), Map.class) : new LinkedHashMap<>();
        } catch (Exception e) {
            throw new BusinessException(500, "画像数据损坏，请联系管理员重置英语诊断");
        }
        full.put("english", eng);
        try {
            st.setPrecisionProfile(OM.writeValueAsString(full));
        } catch (Exception e) {
            throw new BusinessException(500, "画像保存失败，请稍后重试");
        }
        studentMapper.updateById(st);
    }

    private int toInt(Object val, int def) {
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) { try { return Integer.parseInt(s); } catch (Exception e) {} }
        return def;
    }

    private Set<String> getKnownWords(Long studentId) {
        return vocabMapper.selectList(
            new LambdaQueryWrapper<PrecisionVocabulary>()
                .eq(PrecisionVocabulary::getStudentId, studentId)
                .ge(PrecisionVocabulary::getMasterLevel, 3))
            .stream().map(PrecisionVocabulary::getWord).collect(Collectors.toSet());
    }

    private int updateStreak(Map<String, Object> profile) {
        String lastDate = String.valueOf(profile.getOrDefault("lastPracticeDate", ""));
        int currentStreak = toInt(profile.get("streak"), 0);
        LocalDate today = LocalDate.now();
        if (lastDate.isEmpty() || lastDate.equals(today.toString())) return Math.max(currentStreak, 1);
        // B2: 如果今天使用了冰冻卡，跳过 streak 重置
        String freezeUsed = String.valueOf(profile.getOrDefault("freezeUsedToday", ""));
        if (freezeUsed.equals(today.toString())) {
            profile.remove("freezeUsedToday"); // 一次性标记，用后清除
            return Math.max(currentStreak, 1);
        }
        try {
            LocalDate last = LocalDate.parse(lastDate);
            if (last.equals(today.minusDays(1))) return currentStreak + 1;
        } catch (Exception e) {}
        return 1;
    }

    private int awardCredit(Long studentId, String ruleCode, String bizKey, int amount) {
        if (creditService == null) return 0;
        try {
            // S6-S7: 防重 — 检查是否已发放过相同描述的积分
            if (creditTransactionMapper != null) {
                long exists = creditTransactionMapper.selectCount(
                    new LambdaQueryWrapper<CreditTransaction>()
                        .eq(CreditTransaction::getStudentId, studentId)
                        .eq(CreditTransaction::getDescription, bizKey));
                if (exists > 0) return 0;
            }
            creditService.awardMoralCredit(studentId, amount, bizKey);
            return amount;
        } catch (Exception e) { log.warn("英语提分处理异常: {}", e.getMessage(), e); return 0; }
    }

    private int awardMilestones(Long studentId, int vocabKnown, Map<String, Object> profile) {
        int earned = 0;
        @SuppressWarnings("unchecked")
        List<String> done = (List<String>) profile.getOrDefault("achievements", List.of());
        int[][] milestones = {{50,3},{100,5},{200,8},{300,10},{400,12},{500,15}};
        for (int[] m : milestones) {
            String key = "vocab" + m[0];
            if (!done.contains(key) && vocabKnown >= m[0]) {
                earned += awardCredit(studentId, "engl_" + key, "engl_" + key + ":" + studentId, m[1]);
            }
        }
        return earned;
    }

    private List<String> checkAchievements(Map<String, Object> profile) {
        List<String> added = new ArrayList<>();
        int vocabKnown = toInt(profile.get("vocabKnown"), 0);
        int streak = toInt(profile.get("streak"), 0);
        int totalPractices = toInt(profile.get("totalPractices"), 0);
        int totalMinutes = toInt(profile.get("totalMinutes"), 0);
        @SuppressWarnings("unchecked")
        List<String> existing = new ArrayList<>((List<String>) profile.getOrDefault("achievements", List.of()));
        String[][] checks = {{"first_word", vocabKnown >= 1 ? "1" : null},
            {"7day_streak", streak >= 7 ? "1" : null}, {"100words", vocabKnown >= 100 ? "1" : null},
            {"10hour", totalMinutes >= 600 ? "1" : null}, {"30day", streak >= 30 ? "1" : null}};
        for (String[] c : checks) {
            if (c[1] != null && !existing.contains(c[0])) { added.add(c[0]); existing.add(c[0]); }
        }
        profile.put("achievements", existing);
        return added;
    }

    private String getOwlMessage(String mood, int level) {
        return switch (mood) {
            case "correct" -> new String[]{"太棒了！🎉", "答对了！继续加油！💪", "完美！掌握得很好！⭐"}[Math.min(level, 2)];
            case "hint" -> "没关系！试试看这个线索？🔍";
            case "reveal" -> "这个词明天还会来见你，别担心！📅";
            default -> "继续前进！";
        };
    }

    private String getOwlCompleteMessage(boolean perfect, List<String> newAch, int vocab) {
        if (!newAch.isEmpty()) return "🎉 新成就：" + String.join("、", newAch) + "！";
        if (perfect) return "⭐ 完美的一天！全对无提示！";
        if (vocab >= 100) return "📦 已掌握 " + vocab + " 词，百词斩达成！";
        return "👍 练习完成，明天继续加油！";
    }

    @SuppressWarnings("unchecked")
    private List<String> parseOptions(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            // 注意:字面量字符串 "null" 会被 readValue 解析成 Java null(非异常)→ 下游 Map.of 会 NPE
            List<String> r = OM.readValue(json, List.class);
            return r != null ? r : List.of();
        } catch (Exception e) { return List.of(); }
    }

    private boolean isCorrect(String answer, String expected, String questionType) {
        if (answer == null || expected == null) return false;
        if ("SINGLE_CHOICE".equals(questionType) || "TRUE_FALSE".equals(questionType))
            return answer.trim().equalsIgnoreCase(expected.trim());
        return PrecisionHelper.matchFillInAnswer(answer, expected);
    }

    private String getStudentName(Student st) {
        if (st == null) return "未知";
        return "学生" + st.getId();
    }

    // ── 语法树 ──

    @Override
    public List<Map<String, Object>> getGrammarTree(Long studentId) {
        Map<String, Object> profile = readEnglishProfile(studentId);
        int stage = toInt(profile.get("stage"), 1);
        @SuppressWarnings("unchecked")
        List<Long> mastered = ((List<?>) profile.getOrDefault("grammarNodesMastered", List.of())).stream()
            .map(o -> o instanceof Number n ? n.longValue() : Long.valueOf(String.valueOf(o))).toList();
        @SuppressWarnings("unchecked")
        List<Long> skipped = ((List<?>) profile.getOrDefault("grammarNodesSkipped", List.of())).stream()
            .map(o -> o instanceof Number n ? n.longValue() : Long.valueOf(String.valueOf(o))).toList();

        List<KnowledgeNode> allGrammar = nodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>()
                .isNotNull(KnowledgeNode::getGrammarCategory)
                .orderByAsc(KnowledgeNode::getSortOrder));
        if (allGrammar.isEmpty()) return List.of();

        // 先查出 level=3 的大类，再附上 level=4 的子节点
        Set<Long> masteredIds = new HashSet<>(mastered);
        masteredIds.addAll(skipped);
        List<Map<String, Object>> tree = new ArrayList<>();
        Map<Long, List<Map<String, Object>>> childrenMap = new HashMap<>();

        for (KnowledgeNode n : allGrammar) {
            String status = "locked";
            if (n.getUnlockStage() != null && n.getUnlockStage() <= stage) {
                if (masteredIds.contains(n.getId())) status = "mastered";
                else if (skipped.contains(n.getId())) status = "skipped";
                else status = "unlocked";
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", n.getId()); item.put("name", n.getName());
            item.put("level", n.getLevel()); item.put("grammarCategory", n.getGrammarCategory());
            item.put("unlockStage", n.getUnlockStage()); item.put("status", status);
            if (n.getLevel() != null && n.getLevel() == 4 && n.getParentId() != null) {
                childrenMap.computeIfAbsent(n.getParentId(), k -> new ArrayList<>()).add(item);
            } else if (n.getLevel() != null && n.getLevel() == 3) {
                item.put("children", new ArrayList<>());
                tree.add(item);
            }
        }
        tree.forEach(parent -> {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
            List<Map<String, Object>> found = childrenMap.get(((Number) parent.get("id")).longValue());
            if (found != null) children.addAll(found);
        });
        return tree;
    }

    @Override
    public List<Map<String, Object>> getGrammarPractice(Long studentId, Long nodeId) {
        KnowledgeNode node = nodeMapper.selectById(nodeId);
        if (node == null || node.getGrammarCategory() == null) return List.of();
        Map<String, Object> profile = readEnglishProfile(studentId);
        // 查题库该语法节点的已审题（R112：stream limit 替代 .last()）
        List<QuestionBank> qs = questionMapper.selectList(
            new LambdaQueryWrapper<QuestionBank>()
                .eq(QuestionBank::getSubject, "英语[职高]").eq(QuestionBank::getStatus, 1)
                .eq(QuestionBank::getGrammarNodeId, nodeId));
        qs = qs.stream().limit(10).collect(Collectors.toList());
        return qs.stream().map(q -> Map.<String, Object>of(
            "questionId", q.getId(), "questionType", q.getQuestionType(),
            "questionText", q.getQuestionText(), "options", parseOptions(q.getOptions()),
            "grammarNodeName", node.getName()
        )).toList();
    }

    // ── 阅读 ──

    @Override
    public Map<String, Object> getReading(Long studentId) {
        Map<String, Object> profile = readEnglishProfile(studentId);
        int vocabKnown = toInt(profile.get("vocabKnown"), 0);
        int stage = toInt(profile.get("stage"), 1);
        if (stage < 6) return Map.of("locked", true, "message", "词汇量达到 350 后解锁阅读训练");
        if (passageMapper == null) return Map.of("locked", true);
        List<PrecisionEnglishReadingPassage> passages = passageMapper.selectList(
            new LambdaQueryWrapper<PrecisionEnglishReadingPassage>()
                .le(PrecisionEnglishReadingPassage::getMinVocabSize, vocabKnown)
                .orderByAsc(PrecisionEnglishReadingPassage::getDifficultyLevel).last("LIMIT 1"));
        if (passages.isEmpty()) return Map.of("locked", true, "message", "暂无匹配你当前水平的阅读材料");
        PrecisionEnglishReadingPassage p = passages.get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", p.getId()); result.put("title", p.getTitle());
        result.put("content", p.getContent()); result.put("wordCount", p.getWordCount());
        result.put("grammarTags", p.getGrammarTags());
        result.put("questionIds", p.getQuestionIds());
        return result;
    }

    /** 获取学生已做过的阶段快测题目ID */
    private Set<Long> getDoneStageTestIds(Long studentId, int stage) {
        Set<Long> doneIds = new HashSet<>();
        try {
            // 从 profile 中读取已做过的测试题记录
            Map<String, Object> profile = readEnglishProfile(studentId);
            @SuppressWarnings("unchecked")
            Map<String, Object> stageTests = (Map<String, Object>) profile.getOrDefault("stageTestsDone", Map.of());
            String key = "stage_" + stage;
            // B4: 安全类型转换，避免 ClassCastException
            Object rawList = stageTests.getOrDefault(key, List.of());
            if (rawList instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Number n) doneIds.add(n.longValue());
                }
            }
        } catch (Exception e) {
            log.warn("获取已做测试题失败", e);
        }
        return doneIds;
    }

    /** 记录学生做过的测试题 */
    private void recordStageTestDone(Long studentId, int stage, List<Long> questionIds) {
        try {
            Map<String, Object> profile = readEnglishProfile(studentId);
            @SuppressWarnings("unchecked")
            Map<String, Object> stageTests = (Map<String, Object>) profile.getOrDefault("stageTestsDone", new LinkedHashMap<>());
            String key = "stage_" + stage;
            @SuppressWarnings("unchecked")
            List<Long> existing = new ArrayList<>((List<Long>) stageTests.getOrDefault(key, List.of()));
            existing.addAll(questionIds);
            // 只保留最近100条记录
            if (existing.size() > 100) {
                existing = existing.subList(existing.size() - 100, existing.size());
            }
            stageTests.put(key, existing);
            profile.put("stageTestsDone", stageTests);
            saveEnglishProfile(studentId, profile);
        } catch (Exception e) {
            log.warn("记录已做测试题失败", e);
        }
    }

    // ── 阶段快测 ──

    @Override
    public List<Map<String, Object>> getStageTest(Long studentId, int stage, String testType) {
        // B3: 同步保护 profile 读写
        synchronized (helper.getProfileLock(studentId)) {
            if (quickTestMapper == null) return List.of();

            // 获取已做过的题目ID
            Set<Long> doneIds = getDoneStageTestIds(studentId, stage);

            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.school.teaching.entity.PrecisionEnglishQuickTest> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            qw.eq("stage", stage);
            if ("vocab".equals(testType)) qw.eq("type", "VOCAB");
            else if ("grammar".equals(testType)) qw.eq("type", "GRAMMAR");
            qw.orderByAsc("sort_order");

            List<com.school.teaching.entity.PrecisionEnglishQuickTest> tests = quickTestMapper.selectList(qw);

            // 排除已做过的题目
            tests = tests.stream()
                .filter(t -> !doneIds.contains(t.getQuestionId()))
                .collect(java.util.stream.Collectors.toList());

            // 限制数量
            int stLimit = "vocab".equals(testType) ? 20 : 3;
            tests = tests.stream().limit(stLimit).collect(java.util.stream.Collectors.toList());

            List<Long> qids = tests.stream().map(com.school.teaching.entity.PrecisionEnglishQuickTest::getQuestionId).toList();
            if (qids.isEmpty()) return List.of();

            // 记录本次做的题目
            recordStageTestDone(studentId, stage, new ArrayList<>(qids));

            List<QuestionBank> qs = questionMapper.selectBatchIds(qids);
            return qs.stream().map(q -> Map.<String, Object>of(
                "questionId", q.getId(), "questionType", q.getQuestionType(),
                "questionText", q.getQuestionText(), "options", parseOptions(q.getOptions())
            )).toList();
        }
    }

    // ── 班级排行 ──

    @Override
    public List<Map<String, Object>> getClassRanking(Long studentId) {
        Student me = studentMapper.selectById(studentId);
        if (me == null || me.getClassId() == null) return List.of();
        List<Student> classmates = studentMapper.selectList(
            new LambdaQueryWrapper<Student>().eq(Student::getClassId, me.getClassId()));
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Student s : classmates) {
            Map<String, Object> profile = readEnglishProfile(s.getId());
            if (profile.isEmpty() || profile.get("stage") == null) continue;
            int vocabKnown = toInt(profile.get("vocabKnown"), 0);
            int streak = toInt(profile.get("streak"), 0);
            int credits = toInt(profile.get("creditsEarned"), 0);
            ranking.add(Map.of(
                "studentId", s.getId(), "studentName", getStudentName(s),
                "vocabKnown", vocabKnown, "streak", streak, "creditsEarned", credits
            ));
        }
        ranking.sort((a, b) -> Integer.compare(
            toInt(b.get("creditsEarned"), 0), toInt(a.get("creditsEarned"), 0)));
        return ranking.subList(0, Math.min(ranking.size(), 10));
    }

    // ── 词汇本 ──

    @Override
    public Map<String, Object> getVocabBook(Long studentId) {
        Map<String, Object> profile = readEnglishProfile(studentId);
        int stage = toInt(profile.get("stage"), 1);
        List<PrecisionVocabulary> allVocab = vocabMapper.selectList(
            new LambdaQueryWrapper<PrecisionVocabulary>()
                .eq(PrecisionVocabulary::getStudentId, studentId)
                .orderByDesc(PrecisionVocabulary::getMasterLevel));
        Map<String, Object> book = new LinkedHashMap<>();
        book.put("vocabTotal", allVocab.size());

        // 批量预加载 seed 数据，避免 N+1 查询
        Map<String, PrecisionVocabularySeed> seedMap = new LinkedHashMap<>();
        if (seedMapper != null && !allVocab.isEmpty()) {
            List<String> words = allVocab.stream()
                .map(PrecisionVocabulary::getWord)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
            if (!words.isEmpty()) {
                List<PrecisionVocabularySeed> allSeeds = seedMapper.selectList(
                    new LambdaQueryWrapper<PrecisionVocabularySeed>()
                        .in(PrecisionVocabularySeed::getWord, words));
                allSeeds.forEach(seed -> seedMap.put(seed.getWord(), seed));
            }
        }

        // 按阶段分组：阶段1≈seed frequencyRank 1-100, 阶段2≈101-200...
        List<Map<String, Object>> groups = new ArrayList<>();
        for (int s = 1; s <= stage; s++) {
            int start = (s - 1) * 100 + 1;
            int end = s * 100;
            List<Map<String, Object>> words = new ArrayList<>();
            for (PrecisionVocabulary v : allVocab) {
                PrecisionVocabularySeed seed = seedMap.get(v.getWord());
                if (seed != null) {
                    int rank = seed.getFrequencyRank() != null ? seed.getFrequencyRank() : 0;
                    if (rank >= start && rank <= end) {
                        words.add(Map.of(
                            "word", v.getWord(), "meaning", seed.getMeaning(),
                            "phonetic", seed.getPhonetic() != null ? seed.getPhonetic() : "",
                            "example", seed.getExample() != null ? seed.getExample() : "",
                            "masterLevel", v.getMasterLevel()
                        ));
                    }
                }
            }
            groups.add(Map.of("stage", s, "stageName", STAGE_NAMES[s], "words", words, "count", words.size()));
        }
        book.put("groups", groups);
        return book;
    }

    // ── 兼容旧接口（PrecisionService 调用） ──

    @Override
    public Map<String, Object> buildEnglishPackData(Long studentId, int weekNo) {
        Map<String, Object> profile = readEnglishProfile(studentId);
        int stage = toInt(profile.get("stage"), 1);
        Map<String, Object> task = buildDailyTask(studentId, stage, profile);
        StringBuilder html = new StringBuilder();
        html.append("<h2>本周英语学习包</h2>");
        html.append("<p>阶段: ").append(STAGE_NAMES[stage]).append(" | 词汇: ").append(profile.get("vocabKnown")).append("/500</p>");
        html.append("<h3>词汇 (").append(task.get("vocabCount")).append("词)</h3><ul>");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> vocabQs = (List<Map<String, Object>>) task.getOrDefault("vocabQuestions", List.of());
        for (Map<String, Object> q : vocabQs) html.append("<li>").append(q.get("word")).append("</li>");
        html.append("</ul>");
        html.append("<h3>语法 (").append(task.get("grammarCount")).append("题)</h3><p>每日通过系统完成</p>");
        return Map.of("content", html.toString(), "wordCount", vocabQs.size(), "grammarCount", task.get("grammarCount"));
    }

    @Override
    public List<Map<String, Object>> buildOnlineTestQuestions(Long studentId) {
        List<Map<String, Object>> qs = new ArrayList<>();
        qs.add(Map.of("type", "英译中", "prompt", "ability", "expected", "能力"));
        qs.add(Map.of("type", "中译英", "prompt", "能力", "expected", "ability"));
        qs.add(Map.of("type", "语法选择", "prompt", "She ___ to school every day.", "options", List.of("A. go","B. goes","C. going","D. gone"), "expected", "B"));
        return qs;
    }

    /** 获取已做过的诊断题词汇 */
    private Set<String> getDoneDiagnoseWords(Long studentId) {
        Set<String> doneWords = new HashSet<>();
        try {
            Map<String, Object> profile = readEnglishProfile(studentId);
            @SuppressWarnings("unchecked")
            List<String> words = (List<String>) profile.getOrDefault("doneDiagnoseWords", List.of());
            doneWords.addAll(words);
        } catch (Exception e) {
            log.warn("获取已做诊断题失败", e);
        }
        return doneWords;
    }

    /** 记录本次诊断使用的词汇 */
    private void recordDiagnoseWords(Long studentId, List<Map<String, Object>> vocabQuestions) {
        try {
            Map<String, Object> profile = readEnglishProfile(studentId);
            @SuppressWarnings("unchecked")
            List<String> existing = new ArrayList<>((List<String>) profile.getOrDefault("doneDiagnoseWords", List.of()));
            for (Map<String, Object> q : vocabQuestions) {
                existing.add((String) q.get("prompt"));
            }
            // 只保留最近200条记录
            if (existing.size() > 200) {
                existing = existing.subList(existing.size() - 200, existing.size());
            }
            profile.put("doneDiagnoseWords", existing);
            saveEnglishProfile(studentId, profile);
        } catch (Exception e) {
            log.warn("记录诊断题失败", e);
        }
    }

    @Override
    public Map<String, Object> diagnose(Long studentId) {
        // B3: 同步保护 profile 读写（recordDiagnoseWords 写入 profile）
        synchronized (helper.getProfileLock(studentId)) {
            List<Map<String, Object>> questions = new ArrayList<>();
            Random rnd = new Random();

        // ── 1. 词汇题：50道，英译中(30) + 中译英(20) ──
        if (seedMapper != null) {
            // 获取已做过的诊断题词汇，排除重复
            Set<String> doneDiagnoseWords = getDoneDiagnoseWords(studentId);

            // 扩充随机池至500词
            List<PrecisionVocabularySeed> seeds = seedMapper.selectList(
                new LambdaQueryWrapper<PrecisionVocabularySeed>()
                    .orderByAsc(PrecisionVocabularySeed::getFrequencyRank));
            seeds = seeds.stream().limit(500).collect(Collectors.toList());
            if (seeds != null && !seeds.isEmpty()) {
                Collections.shuffle(seeds, rnd);
                int total = Math.min(seeds.size(), 50);
                int added = 0;
                for (PrecisionVocabularySeed s : seeds) {
                    if (added >= total) break;
                    String word = PrecisionHelper.fixEncoding(s.getWord());
                    String meaning = PrecisionHelper.fixEncoding(s.getMeaning());
                    String phonetic = s.getPhonetic() != null ? PrecisionHelper.fixEncoding(s.getPhonetic()) : "";
                    boolean en2cn = added % 5 < 3;
                    // 防御：若meaning首段看似英文（全字母），种子数据word/meaning可能互换，交换后再拼装
                    String cnPart = meaning.split("[；;,，/]")[0];
                    String effectiveWord = word;
                    String effectiveMeaning = meaning;
                    if (cnPart.matches("^[a-zA-Z]+$")) {
                        // meaning是英文 → 种子数据疑似换位，交换word和meaning
                        effectiveWord = meaning;
                        effectiveMeaning = word;
                        cnPart = effectiveMeaning.split("[；;,，/]")[0];
                    }
                    // 排除已做过的词汇（使用 effectiveWord 以匹配记录的 key）
                    if (doneDiagnoseWords.contains(effectiveWord)) continue;
                    String questionText = en2cn
                        ? "【英译中】请写出单词「" + effectiveWord + "」的中文释义\n（提示：多个释义写一个即可）"
                        : "【中译英】请写出「" + cnPart + "」对应的英文单词";
                    questions.add(Map.of(
                        "questionId", -((long)(10_000 + added)), "type", en2cn ? "英译中" : "中译英",
                        "questionType", "FILL_IN", "questionText", questionText, "prompt", effectiveWord));
                    added++;
                }
                // 记录本次诊断使用的词汇
                List<Map<String, Object>> vocabQuestions = questions.subList(0, Math.min(50, questions.size()));
                recordDiagnoseWords(studentId, vocabQuestions);
            }
        }

        // ── 2. 语法题：15道选择题，优先题库，兜底硬编码 ──
        java.util.List<QuestionBank> grammarPool = null;
        if (questionMapper != null) {
            // R112：stream limit 替代 .last()
            grammarPool = questionMapper.selectList(
                new LambdaQueryWrapper<QuestionBank>()
                    .eq(QuestionBank::getSubject, "英语[职高]").eq(QuestionBank::getStatus, 1)
                    .isNotNull(QuestionBank::getGrammarNodeId));
            grammarPool = grammarPool.stream().limit(30).collect(Collectors.toList());
        }
        if (grammarPool != null && !grammarPool.isEmpty()) {
            Collections.shuffle(grammarPool, rnd);
            int gc = Math.min(grammarPool.size(), 15);
            for (int i = 0; i < gc; i++) {
                QuestionBank gq = grammarPool.get(i);
                questions.add(Map.of(
                    "questionId", gq.getId(), "type", "语法选择",
                    "questionType", "SINGLE_CHOICE", "questionText", gq.getQuestionText(),
                    "options", parseOptions(gq.getOptions()),
                    "difficultyLevel", gq.getDifficultyLevel() != null ? gq.getDifficultyLevel() : 1,
                    "tier", gq.getTier() != null ? gq.getTier() : "",
                    "knowledgePoints", gq.getKnowledgePoints() != null ? gq.getKnowledgePoints() : ""));
            }
        }

            return Map.of("subject", "英语[职高]", "totalQuestions", questions.size(),
                "vocabSample", Math.min(questions.size(), 50), "grammarQuestions", Math.min(questions.size() - 50, 15),
                "questions", questions);
        } // end synchronized
    }

    @Override
    public Map<String, Object> getEnglishReport(Long studentId) {
        Map<String, Object> profile = readEnglishProfile(studentId);
        int stage = toInt(profile.get("stage"), 1);
        int vocabKnown = toInt(profile.get("vocabKnown"), 0);
        int streak = toInt(profile.get("streak"), 0);
        int totalPractices = toInt(profile.get("totalPractices"), 0);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("subject", "英语[职高]");
        report.put("stage", stage);
        report.put("stageName", STAGE_NAMES[stage]);
        report.put("vocabKnown", vocabKnown);
        report.put("vocabTotal", STAGE_VOCAB_TARGETS[7]);
        report.put("streak", streak);
        report.put("totalPractices", totalPractices);
        report.put("avgMastery", vocabKnown > 0 ? Math.min(100, (int)((double)vocabKnown / 500 * 100)) : 0);
        report.put("nodeCount", 500);

        // 阶段进度
        int masteredCount = 0;
        for (int i = 0; i < stage; i++) masteredCount += (STAGE_VOCAB_TARGETS[i + 1] - STAGE_VOCAB_TARGETS[i]);
        masteredCount += Math.max(0, vocabKnown - STAGE_VOCAB_TARGETS[Math.min(stage, 7)]);
        report.put("masteredCount", masteredCount);

        // 最新诊断分数
        String lastDiagKey = "diagnoseScore";
        Object diagScore = profile.get(lastDiagKey);
        report.put("lastDiagnoseScore", diagScore != null ? toInt(diagScore, 0) : 0);

        @SuppressWarnings("unchecked")
        List<String> practiceDates = (List<String>) profile.getOrDefault("practiceDates", List.of());
        report.put("practiceDates", practiceDates);
        report.put("isFirstDiagnosis", profile.isEmpty() || profile.get("stage") == null);

        return report;
    }

    @Override
    public Map<String, Object> getWeeklyReadings(Long studentId, int weekNo) {
        return Map.of("readings", List.of());
    }

    @Override
    public Map<String, Object> getWeeklyGrammar(Long studentId, int weekNo) {
        return Map.of("grammar", List.of());
    }

    @Override
    public String getVocabExpected(Long questionId, String prompt) {
        if (seedMapper != null) {
            List<PrecisionVocabularySeed> seeds = seedMapper.selectList(
                new LambdaQueryWrapper<PrecisionVocabularySeed>().eq(PrecisionVocabularySeed::getWord, prompt));
            if (seeds != null && !seeds.isEmpty()) return seeds.get(0).getMeaning();
        }
        return null;
    }

    @Override
    public String[] getExpectedAndExplanation(Long questionId) {
        QuestionBank q = questionMapper.selectById(questionId);
        if (q == null) return null;
        return new String[]{q.getCorrectAnswer(), q.getExplanation()};
    }
}
