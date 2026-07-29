package com.school.teaching.precision.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.common.EncodingUtils;
import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.entity.PrecisionProgress;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.entity.Student;
import com.school.teaching.entity.WrongQuestion;
import com.school.teaching.entity.PrecisionProfile;
import com.school.teaching.mapper.DictSubjectMapper;
import com.school.teaching.mapper.KnowledgeNodeMapper;
import com.school.teaching.mapper.PrecisionProfileMapper;
import com.school.teaching.mapper.PrecisionProgressMapper;
import com.school.teaching.mapper.QuestionBankMapper;
import com.school.teaching.mapper.StudentMapper;
import com.school.teaching.mapper.WrongQuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PrecisionHelper {

    @Autowired(required = false)
    private DictSubjectMapper subjectMapper;

    @Autowired(required = false)
    private QuestionBankMapper questionMapper;

    @Autowired(required = false)
    private KnowledgeNodeMapper nodeMapper;

    @Autowired(required = false)
    private PrecisionProgressMapper progressMapper;

    @Autowired(required = false)
    private WrongQuestionMapper wrongMapper;

    @Autowired(required = false)
    private StudentMapper studentMapper;

    @Autowired(required = false)
    private PrecisionProfileMapper profileMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 锁信息内部类——支持过期清理 */
    private static class LockInfo {
        final Object lock = new Object();
        volatile long lastAccessed;

        LockInfo() {
            this.lastAccessed = System.currentTimeMillis();
        }

        void touch() {
            this.lastAccessed = System.currentTimeMillis();
        }

        boolean isExpired(long timeoutMs) {
            return System.currentTimeMillis() - lastAccessed > timeoutMs;
        }
    }

    /** 学生级细粒度锁——跨 Service 共享，避免并发覆盖 profile */
    private final ConcurrentHashMap<Long, LockInfo> profileLocks = new ConcurrentHashMap<>();
    private volatile long lastLockCleanup = System.currentTimeMillis();
    private static final long LOCK_EXPIRE_MS = 5 * 60 * 1000L; // 5 分钟过期
    private static final long CLEANUP_INTERVAL_MS = 60 * 1000L; // 每分钟清理一次

    private static final ObjectMapper STATIC_OM = new ObjectMapper();

    private static final Map<String, String> SYNONYM_MAP = new LinkedHashMap<>();
    static {
        SYNONYM_MAP.put("对", "对,正确,√,✓,✔,是,true,yes,right,correct,T,F");
        SYNONYM_MAP.put("错", "错,错误,×,✗,✘,否,false,no,wrong,incorrect,F,不对,不正确");
        SYNONYM_MAP.put("无解", "无解,无实数解,∅,空集,没有解,不存在,无,没有,no solution,none");
        SYNONYM_MAP.put("无穷", "无穷,∞,无穷大,无限,infinity,infinite");
        SYNONYM_MAP.put("平行", "平行,∥,parallel");
        SYNONYM_MAP.put("垂直", "垂直,⊥,perpendicular");
        SYNONYM_MAP.put("相交", "相交,交叉,intersect");
        SYNONYM_MAP.put("相等", "相等,等于,=,＝,equal");
        SYNONYM_MAP.put("大于", "大于,>,＞,greater than");
        SYNONYM_MAP.put("小于", "小于,<,＜,less than");
        SYNONYM_MAP.put("度", "度,°,deg");
        SYNONYM_MAP.put("厘米", "厘米,cm,公分");
        SYNONYM_MAP.put("米", "米,m,公尺");
        SYNONYM_MAP.put("千克", "千克,kg,公斤");
        SYNONYM_MAP.put("秒", "秒,s,second");
    }

    // ═══════════════════════════════════════════
    //  Static utility methods (no mapper deps)
    // ═══════════════════════════════════════════

    public static Long toLong(Object v) {
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) {
            try {
                return Long.parseLong((String) v);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public static int toInt(Object val, int def) {
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (Exception e) {
                // String转int失败，使用默认值（预期内）
            }
        }
        return def;
    }

    public static String fixEncoding(String text) {
        return EncodingUtils.fix(text);
    }

    public static String sanitizeAnswer(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String s = raw.trim();
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '\uFF01' && c <= '\uFF5E') {
                sb.append((char) (c - 0xFEE0));
            } else if (c == '\u3000') {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        s = sb.toString();
        s = s.replaceAll("^[\\s。，；：！？、\"'「」『』\\[\\]()（）{}]+", "");
        s = s.replaceAll("[\\s。，；：！？、\"'「」『』\\[\\]()（）{}]+$", "");
        return s.trim();
    }

    public static String expandSynonyms(String answer) {
        if (answer == null || answer.isEmpty()) return answer;
        String key = answer.toLowerCase(java.util.Locale.ROOT)
            .replaceAll("[\\s\\p{Punct}。，；：！？、\"'（）()【】《》\\[\\]{}——…]", "");
        for (var entry : SYNONYM_MAP.entrySet()) {
            for (String form : entry.getValue().split("[,，]")) {
                if (key.equalsIgnoreCase(form.trim())) {
                    return entry.getValue();
                }
            }
        }
        return answer;
    }

    public static String escapeHtml(String raw) {
        if (raw == null) return "";
        return raw
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    public static boolean matchFillInAnswer(String studentAnswer, String expected) {
        if (expected == null || expected.isEmpty()) return false;
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) return false;

        String saRaw = sanitizeAnswer(studentAnswer);
        if (saRaw.isEmpty()) return false;

        String saExpanded = expandSynonyms(saRaw);

        String sa = saExpanded.replaceAll("[\\s。，；：！？、\"'「」『』【】《》——…~`@#$%^&*_=+|\\\\]", "");
        if (sa.isEmpty()) return false;

        String saExact = saRaw.replaceAll("[\\s。，；：！？、\"'「」『』【】《》——…~`@#$%^&*_=+|\\\\]", "");

        String[] parts = expected.split("[；;,，/|、]");
        for (String part : parts) {
            String epRaw = sanitizeAnswer(part);
            if (epRaw.isEmpty()) continue;
            String epExpanded = expandSynonyms(epRaw);
            String ep = epExpanded.replaceAll("[\\s。，；：！？、\"'「」『』【】《》——…~`@#$%^&*_=+|\\\\]", "");
            if (ep.isEmpty()) continue;

            if (saExact.equalsIgnoreCase(ep)) return true;
            if (sa.equalsIgnoreCase(ep)) return true;

            if (saExact.length() >= 3 && ep.length() >= 3) {
                if (saExact.contains(ep) || ep.contains(saExact)) return true;
            }
            if (sa.length() >= 3 && ep.length() >= 3) {
                if (sa.contains(ep) || ep.contains(sa)) return true;
            }

            String saNums = saExact.replaceAll("[^0-9.\\-]", "");
            String epNums = ep.replaceAll("[^0-9.\\-]", "");
            if (saNums.length() >= 1 && epNums.length() >= 1 && saNums.equals(epNums)) {
                String saText = saExact.replaceAll("[0-9.\\-]", "");
                String epText = ep.replaceAll("[0-9.\\-]", "");
                if (saText.isEmpty() && epText.isEmpty()) return true;
                if (!saText.isEmpty() && !epText.isEmpty()) {
                    double ratio = (double) Math.min(saText.length(), epText.length())
                        / Math.max(saText.length(), epText.length());
                    if (ratio >= 0.5) return true;
                }
            }
        }
        return false;
    }

    public static String normalizeTfAnswer(String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return "";
        if (Set.of("T", "True", "TRUE", "true", "正确", "√", "✓").contains(trimmed)) return "A";
        if (Set.of("F", "False", "FALSE", "false", "错误", "×", "✗", "X").contains(trimmed)) return "B";
        return trimmed;
    }

    public static boolean matchVocabularyAnswer(String studentAnswer, String expected) {
        if (expected == null || expected.isEmpty()) return false;
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) return false;
        String sa = studentAnswer.replaceAll("\\s+", "").replaceAll("[，,;；、]", "");
        if (sa.length() < 2) return false;
        for (String part : expected.split("[；;,，/]")) {
            String ep = part.replaceAll("\\s+", "").trim();
            if (ep.isEmpty()) continue;
            if (sa.equalsIgnoreCase(ep)) return true;
            if (sa.length() >= 2 && ep.length() >= 2 && sa.contains(ep)) return true;
        }
        return false;
    }

    public static String determineMatchMode(String answer, String correctAnswer) {
        if (answer == null || correctAnswer == null) return "incorrect";

        String saRaw = sanitizeAnswer(answer);
        if (saRaw.isEmpty()) return "incorrect";

        String saExpanded = expandSynonyms(saRaw);

        String sa = saExpanded.replaceAll("[\\s。，；：！？、\"'「」『』【】《》——…~`@#$%^&*_=+|\\\\]", "")
            .toLowerCase(java.util.Locale.ROOT);
        if (sa.isEmpty()) return "incorrect";

        String saExact = saRaw.replaceAll("[\\s。，；：！？、\"'「」『』【】《》——…~`@#$%^&*_=+|\\\\]", "")
            .toLowerCase(java.util.Locale.ROOT);

        String[] parts = correctAnswer.split("[；;,，/|、]");
        for (String part : parts) {
            String epRaw = sanitizeAnswer(part);
            if (epRaw.isEmpty()) continue;

            String epExpanded = expandSynonyms(epRaw);
            String ep = epExpanded.replaceAll("[\\s。，；：！？、\"'「」『』【】《》——…~`@#$%^&*_=+|\\\\]", "")
                .toLowerCase(java.util.Locale.ROOT);
            if (ep.isEmpty()) continue;

            if (saExact.equals(ep) || sa.equals(ep)) return "exact";

            if (saExact.length() >= 3 && ep.length() >= 3) {
                if (saExact.contains(ep) || ep.contains(saExact)) return "fuzzy";
            }
            if (sa.length() >= 3 && ep.length() >= 3) {
                if (sa.contains(ep) || ep.contains(sa)) return "fuzzy";
            }
        }

        if (matchFillInAnswer(answer, correctAnswer)) return "fuzzy";

        return "incorrect";
    }

    public static String validateQuestion(QuestionBank q) {
        if (q == null) return "题目为空";
        String type = q.getQuestionType();
        String answer = q.getCorrectAnswer();
        String optionsJson = q.getOptions();
        String text = q.getQuestionText();

        if (text == null || text.isBlank()) return "题干为空";
        if (answer == null || answer.isBlank()) return "答案为空（qid=" + q.getId() + "）";

        if ("SINGLE_CHOICE".equals(type) || "MULTI_CHOICE".equals(type) || "TRUE_FALSE".equals(type)) {
            List<String> opts = parseOptionList(optionsJson);
            if (opts.isEmpty()) {
                if ("TRUE_FALSE".equals(type)) return null;
                return "选择题无选项（qid=" + q.getId() + "）";
            }

            Set<String> uniqueOpts = new java.util.LinkedHashSet<>(opts);
            if (uniqueOpts.size() != opts.size()) {
                return "选项重复（qid=" + q.getId() + "）：" + opts;
            }

            int maxIdx = opts.size() - 1;
            char maxLetter = (char) ('A' + maxIdx);

            String letters = answer.replaceAll("[^A-Za-z]", "").toUpperCase();
            if (letters.isEmpty()) {
                return null;
            }

            if (!"TRUE_FALSE".equals(type)) {
                for (char c : letters.toCharArray()) {
                    if (c > maxLetter) {
                        return "答案含超出选项范围的字母 '" + c + "'，选项仅A-" + maxLetter
                            + "（qid=" + q.getId() + "）";
                    }
                }
            }

            if ("MULTI_CHOICE".equals(type) && letters.length() < 2) {
                return "多选题答案仅1个字母 '" + letters + "'，应为多选（qid=" + q.getId() + "）";
            }
        }

        if (text.contains("_____") && type != null && !type.equals("FILL_IN") && !type.equals("CLOZE")) {
            log.warn("题目 qid={} type={} 题干含填空标记 _____", q.getId(), type);
        }

        return null;
    }

    public static Map<Long, String> validateAndFilter(List<QuestionBank> questions) {
        Map<Long, String> skipped = new java.util.LinkedHashMap<>();
        java.util.Iterator<QuestionBank> iter = questions.iterator();
        while (iter.hasNext()) {
            QuestionBank q = iter.next();
            String err = validateQuestion(q);
            if (err != null) {
                skipped.put(q.getId(), err);
                log.warn("过滤无效题目: qid={} reason={}", q.getId(), err);
                iter.remove();
            }
        }
        return skipped;
    }

    public static List<String> parseOptionList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return STATIC_OM.readValue(json, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    public static int estimateScore(String subject, int diagnoseScore) {
        if (subject.contains("数学")) {
            if (diagnoseScore >= 90) return Math.min(150, 130 + (diagnoseScore - 90) / 2);
            if (diagnoseScore >= 80) return 110 + (diagnoseScore - 80);
            if (diagnoseScore >= 70) return 95 + (diagnoseScore - 70) * 3 / 2;
            if (diagnoseScore >= 60) return 75 + (diagnoseScore - 60);
            if (diagnoseScore >= 40) return 40 + (diagnoseScore - 40) * 35 / 20;
            return Math.max(10, diagnoseScore / 2);
        }
        if (subject.contains("英语")) {
            if (diagnoseScore >= 85) return Math.min(100, 80 + (diagnoseScore - 85));
            if (diagnoseScore >= 70) return 60 + (diagnoseScore - 70);
            if (diagnoseScore >= 50) return 40 + (diagnoseScore - 50);
            if (diagnoseScore >= 30) return 20 + (diagnoseScore - 30);
            return Math.max(5, diagnoseScore / 3);
        }
        return diagnoseScore;
    }

    public static List<?> parseJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return STATIC_OM.readValue(json, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    public static Map<String, Object> parseOrCreateProfile(String profileJson) {
        if (profileJson != null) {
            try {
                return STATIC_OM.readValue(profileJson, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("解析profile JSON失败: {}", e.getMessage());
            }
        }
        return new LinkedHashMap<>();
    }

    // ═══════════════════════════════════════════
    //  Instance methods (need mapper deps)
    // ═══════════════════════════════════════════

    public Long getSubjectId(String subject) {
        if (subject == null || subject.isEmpty()) return null;
        try {
            com.school.teaching.entity.DictSubject ds = subjectMapper.selectOne(
                new LambdaQueryWrapper<com.school.teaching.entity.DictSubject>()
                    .eq(com.school.teaching.entity.DictSubject::getSubjectName, subject)
                    .last("LIMIT 1"));
            if (ds != null) return ds.getId();
        } catch (Exception e) {
            log.warn("查询学科ID失败，使用硬编码回退: {}", e.getMessage());
        }
        return subject.contains("数学") ? 22L : subject.contains("英语") ? 24L : null;
    }

    /** 获取学生级锁——跨 Service 共享，避免并发覆盖 profile */
    public Object getProfileLock(Long studentId) {
        long now = System.currentTimeMillis();
        if (now - lastLockCleanup > CLEANUP_INTERVAL_MS) {
            profileLocks.entrySet().removeIf(entry -> entry.getValue().isExpired(LOCK_EXPIRE_MS));
            lastLockCleanup = now;
        }
        LockInfo info = profileLocks.computeIfAbsent(studentId, k -> new LockInfo());
        info.touch();
        return info.lock;
    }

    /** 加载学生 profile（写前读取）- 优先从新表读取 */
    public Map<String, Object> loadProfileForWrite(Long studentId) {
        // 优先从新表读取
        if (profileMapper != null) {
            try {
                List<PrecisionProfile> profiles = profileMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PrecisionProfile>()
                        .eq(PrecisionProfile::getStudentId, studentId));

                if (!profiles.isEmpty()) {
                    Map<String, Object> result = new LinkedHashMap<>();
                    for (PrecisionProfile pp : profiles) {
                        Map<String, Object> subjectProfile = new LinkedHashMap<>();
                        subjectProfile.put("diagnoseScore", pp.getDiagnoseScore());
                        subjectProfile.put("estimatedScore", pp.getEstimatedScore());
                        subjectProfile.put("lastDiagnoseAt", pp.getLastDiagnoseAt() != null ? pp.getLastDiagnoseAt().toString() : null);
                        subjectProfile.put("streakWeeks", pp.getStreakWeeks());
                        subjectProfile.put("lastOnlineTestScore", pp.getLastOnlineTestScore());
                        subjectProfile.put("lastSubmitDate", pp.getLastSubmitDate() != null ? pp.getLastSubmitDate().toString() : null);
                        subjectProfile.put("lastPackWeekNo", pp.getLastPackWeekNo());

                        // 解析 JSON 字段
                        if (pp.getLastPackQuestionIds() != null) {
                            try {
                                subjectProfile.put("lastPackQuestionIds", objectMapper.readValue(pp.getLastPackQuestionIds(), List.class));
                            } catch (Exception ignored) { log.debug("JSON解析lastPackQuestionIds失败: {}", ignored.getMessage()); }
                        }
                        if (pp.getExtraData() != null) {
                            try {
                                Map<String, Object> extra = objectMapper.readValue(pp.getExtraData(), Map.class);
                                subjectProfile.putAll(extra);
                            } catch (Exception ignored) { log.debug("JSON解析extraData失败: {}", ignored.getMessage()); }
                        }

                        result.put(pp.getSubject(), subjectProfile);
                    }
                    return result;
                }
            } catch (Exception e) {
                log.warn("从新表读取 profile 失败，回退到旧表: {}", e.getMessage());
            }
        }

        // 回退到旧表
        if (studentMapper == null) return new LinkedHashMap<>();
        Student st = studentMapper.selectById(studentId);
        if (st == null) return new LinkedHashMap<>();
        return parseOrCreateProfile(st.getPrecisionProfile());
    }

    /** 保存学生 profile - 优先写入新表 */
    public void saveProfile(Long studentId, Map<String, Object> profile) {
        // 优先写入新表
        if (profileMapper != null) {
            try {
                saveProfileToNewTable(studentId, profile);
                // 同步更新旧表（兼容性）
                saveProfileToOldTable(studentId, profile);
                return;
            } catch (Exception e) {
                log.error("保存 profile 到新表失败，回退到旧表: {}", e.getMessage());
            }
        }

        // 回退到旧表
        saveProfileToOldTable(studentId, profile);
    }

    private void saveProfileToNewTable(Long studentId, Map<String, Object> profile) {
        for (Map.Entry<String, Object> entry : profile.entrySet()) {
            String subject = entry.getKey();
            Object value = entry.getValue();

            if (!(value instanceof Map)) continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> subjectProfile = (Map<String, Object>) value;

            // 查找或创建
            PrecisionProfile pp = profileMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PrecisionProfile>()
                    .eq(PrecisionProfile::getStudentId, studentId)
                    .eq(PrecisionProfile::getSubject, subject));

            boolean isNew = (pp == null);
            if (isNew) {
                pp = new PrecisionProfile();
                pp.setStudentId(studentId);
                pp.setSubject(subject);
            }

            // 更新字段
            pp.setDiagnoseScore(getIntValue(subjectProfile, "diagnoseScore"));
            pp.setEstimatedScore(getIntValue(subjectProfile, "estimatedScore"));
            pp.setStreakWeeks(getIntValue(subjectProfile, "streakWeeks"));
            pp.setLastOnlineTestScore(getIntValue(subjectProfile, "lastOnlineTestScore"));
            pp.setLastPackWeekNo(getIntValue(subjectProfile, "lastPackWeekNo"));

            // 解析日期
            String lastDiagnoseAt = getStringValue(subjectProfile, "lastDiagnoseAt");
            if (lastDiagnoseAt != null && !lastDiagnoseAt.isEmpty()) {
                try { pp.setLastDiagnoseAt(java.time.LocalDate.parse(lastDiagnoseAt)); }
                catch (Exception ignored) { log.debug("日期解析lastDiagnoseAt失败: {}", ignored.getMessage()); }
            }
            String lastSubmitDate = getStringValue(subjectProfile, "lastSubmitDate");
            if (lastSubmitDate != null && !lastSubmitDate.isEmpty()) {
                try { pp.setLastSubmitDate(java.time.LocalDate.parse(lastSubmitDate)); }
                catch (Exception ignored) { log.debug("日期解析lastSubmitDate失败: {}", ignored.getMessage()); }
            }

            // 序列化 JSON 字段
            Object lastPackQuestionIds = subjectProfile.get("lastPackQuestionIds");
            if (lastPackQuestionIds != null) {
                try { pp.setLastPackQuestionIds(objectMapper.writeValueAsString(lastPackQuestionIds)); }
                catch (Exception ignored) { log.debug("JSON序列化lastPackQuestionIds失败: {}", ignored.getMessage()); }
            }

            // 其他字段放入 extraData
            Map<String, Object> extraData = new LinkedHashMap<>(subjectProfile);
            extraData.remove("diagnoseScore");
            extraData.remove("estimatedScore");
            extraData.remove("streakWeeks");
            extraData.remove("lastOnlineTestScore");
            extraData.remove("lastDiagnoseAt");
            extraData.remove("lastSubmitDate");
            extraData.remove("lastPackQuestionIds");
            extraData.remove("lastPackWeekNo");
            if (!extraData.isEmpty()) {
                try { pp.setExtraData(objectMapper.writeValueAsString(extraData)); }
                catch (Exception ignored) { log.debug("JSON序列化extraData失败: {}", ignored.getMessage()); }
            }

            if (isNew) {
                profileMapper.insert(pp);
            } else {
                profileMapper.updateById(pp);
            }
        }
    }

    private void saveProfileToOldTable(Long studentId, Map<String, Object> profile) {
        if (studentMapper == null) return;
        try {
            Student st = new Student();
            st.setId(studentId);
            st.setPrecisionProfile(objectMapper.writeValueAsString(profile));
            studentMapper.updateById(st);
        } catch (Exception e) {
            log.error("保存 profile 到旧表失败 sid={}: {}", studentId, e.getMessage());
        }
    }

    private Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) return ((Number) value).intValue();
        return null;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    public void updateProgressForAnswer(Long studentId, Long questionId, String subject, boolean isCorrect) {
        if (questionId == null) return;
        Long nodeId = null;
        if (questionId > 0) {
            QuestionBank qb = questionMapper.selectById(questionId);
            if (qb == null || qb.getCategoryId() == null) return;
            nodeId = qb.getCategoryId();
        } else if ("英语[职高]".equals(subject) || (subject != null && subject.contains("英语"))) {
            Long engSubjectId = getSubjectId(subject != null ? subject : "英语[职高]");
            if (engSubjectId != null) {
                KnowledgeNode engRoot = nodeMapper.selectOne(
                    new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getSubjectId, engSubjectId)
                        .eq(KnowledgeNode::getLevel, 1)
                        .last("LIMIT 1"));
                if (engRoot != null) nodeId = engRoot.getId();
            }
        }
        if (nodeId == null) return;

        if (!"英语[职高]".equals(subject) && (subject == null || !subject.contains("英语"))) {
            KnowledgeNode nd = nodeMapper.selectById(nodeId);
            if (nd == null) {
                log.warn("updateProgressForAnswer: 无效nodeId={} questionId={}, 放弃进度记录", nodeId, questionId);
                return;
            }
            if (nd.getLevel() != null && nd.getLevel() <= 1) {
                List<KnowledgeNode> children = nodeMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getParentId, nodeId)
                        .eq(KnowledgeNode::getLevel, 2)
                        .last("LIMIT 1"));
                if (!children.isEmpty()) {
                    nodeId = children.get(0).getId();
                } else {
                    return;
                }
            }
        }

        PrecisionProgress pp = progressMapper.selectOne(
            new LambdaQueryWrapper<PrecisionProgress>()
                .eq(PrecisionProgress::getStudentId, studentId)
                .eq(PrecisionProgress::getNodeId, nodeId));
        if (pp == null) {
            pp = new PrecisionProgress();
            pp.setStudentId(studentId);
            pp.setNodeId(nodeId);
            pp.setSubject(subject);
            pp.setStatus("learning");
            pp.setMasteryPercent(BigDecimal.ZERO);
            pp.setTotalAttempts(0);
            pp.setTotalCorrect(0);
        }
        int oldMastery = pp.getMasteryPercent() != null ? pp.getMasteryPercent().intValue() : 0;
        // 加权移动平均：减少单次答题对掌握度的影响，避免近因偏差
        // weight 随答题次数增加而增大：N=1→0.12, N=20→0.5
        int totalAttempts = pp.getTotalAttempts() != null ? pp.getTotalAttempts() : 0;
        double weight = Math.min(0.5, 0.1 + totalAttempts * 0.02);
        double currentScore = isCorrect ? 100.0 : 0.0;
        int newMastery = (int) Math.max(0, Math.min(100, Math.round(weight * currentScore + (1 - weight) * oldMastery)));
        pp.setMasteryPercent(BigDecimal.valueOf(newMastery));
        pp.setTotalAttempts((pp.getTotalAttempts() != null ? pp.getTotalAttempts() : 0) + 1);
        if (isCorrect) {
            pp.setTotalCorrect((pp.getTotalCorrect() != null ? pp.getTotalCorrect() : 0) + 1);
        }
        pp.setLastPracticeAt(LocalDateTime.now());
        if (newMastery >= 80) pp.setStatus("mastered");
        else if (newMastery >= 40) pp.setStatus("learning");
        else pp.setStatus("weak");
        if (pp.getId() == null) progressMapper.insert(pp);
        else progressMapper.updateById(pp);
    }

    public void saveWrongAnswer(Long studentId, Long questionId, String questionText,
                                 boolean isCorrect, String matchMode) {
        if (isCorrect || "pending_review".equals(matchMode)) return;
        if (questionId == null) return;
        Long realQid = questionId;
        if (realQid <= 0 && questionText != null && !questionText.isEmpty() && !"null".equals(questionText)) {
            try {
                String likePattern = questionText.substring(0, Math.min(30, questionText.length()))
                    .replace("%", "\\%").replace("_", "\\_");
                QuestionBank matched = questionMapper.selectOne(
                    new LambdaQueryWrapper<QuestionBank>()
                        .like(QuestionBank::getQuestionText, likePattern)
                        .eq(QuestionBank::getStatus, 1)
                        .last("LIMIT 1"));
                if (matched != null) realQid = matched.getId();
            } catch (Exception e) {
                log.debug("错题文本匹配失败: {}", e.getMessage());
            }
            if (realQid <= 0) return;
        }

        WrongQuestion existing = wrongMapper.selectOne(
            new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getStudentId, studentId)
                .eq(WrongQuestion::getQuestionId, realQid));
        if (existing != null) {
            existing.setWrongCount((existing.getWrongCount() != null ? existing.getWrongCount() : 0) + 1);
            existing.setLastWrongTime(LocalDateTime.now());
            existing.setUpdateTime(LocalDateTime.now());
            wrongMapper.updateById(existing);
        } else {
            WrongQuestion nw = new WrongQuestion();
            nw.setStudentId(studentId);
            nw.setQuestionId(realQid);
            nw.setWrongCount(1);
            nw.setLastWrongTime(LocalDateTime.now());
            nw.setSourceType("REMEDIAL");
            wrongMapper.insert(nw);
        }
    }
}
