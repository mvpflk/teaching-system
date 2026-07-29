package com.school.teaching.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 响应解析器 — 负责将 DeepSeek 原始文本解析为题目列表。
 * 包含：JSON 解析、多策略回退、选项前缀清洗、答案归一化、去重检测。
 */
@Slf4j
public class DeepSeekResponseParser {

    private final ObjectMapper om;

    public DeepSeekResponseParser(ObjectMapper om) {
        this.om = om;
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> parseQuestions(String content, Map<String, Object> metaOut) {
        String text = content.replace("\r\n", "\n").replace("\r", "\n").trim();
        log.info("AI原始返回(len={}): {}", text.length(), text.length() > 800 ? text.substring(0, 800) + "..." : text);
        text = unwrapToArray(text);

        // 方式1：JSON 解析
        try {
            int jsonStart = text.indexOf('[');
            int jsonEnd = text.lastIndexOf(']');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String json = text.substring(jsonStart, jsonEnd + 1);
                log.info("JSON提取: start={}, end={}, len={}, preview={}", jsonStart, jsonEnd, json.length(),
                    json.length() > 300 ? json.substring(0, 300) + "..." : json);

                List<Map> parsed = om.readValue(json, om.getTypeFactory().constructCollectionType(List.class, Map.class));
                if (parsed != null && !parsed.isEmpty()) {
                    log.info("JSON解析到{}个对象", parsed.size());
                    List<Map<String, Object>> result = new ArrayList<>();
                    int filtered = 0;
                    for (Map m : parsed) {
                        Map<String, Object> q = extractQuestionFromMap(m);
                        boolean hasText = q.get("questionText") != null && !q.get("questionText").toString().isBlank();
                        boolean valid = hasText && isValidQuestion(q);
                        if (hasText && !valid) {
                            filtered++;
                            log.warn("题目被过滤: qText={}, answer={}, opts={}",
                                q.get("questionText") != null ? String.valueOf(q.get("questionText")).substring(0, Math.min(40, String.valueOf(q.get("questionText")).length())) : "null",
                                q.get("correctAnswer"),
                                q.get("options") instanceof List<?> l ? l.size() + "个选项" : "无");
                        }
                        if (valid) result.add(q);
                    }

                    boolean heavyFiltering = filtered > 0 && filtered >= parsed.size() / 3;
                    if (heavyFiltering) metaOut.put("heavyFiltering", true);
                    if (!result.isEmpty() && !heavyFiltering) {
                        sortByType(result);
                        int dedupRemoved = markDuplicates(result);
                        metaOut.put("filtered", filtered);
                        metaOut.put("dedupRemoved", dedupRemoved);
                        log.info("JSON解析成功: {}题 (过滤{}题, 去重{}题)", result.size(), filtered, dedupRemoved);
                        return result;
                    }

                    // 最少校验模式
                    if (heavyFiltering) log.warn("JSON解析: {}题中{}题被过滤({}%), 切换最少校验", parsed.size(), filtered, filtered * 100 / parsed.size());
                    else log.warn("JSON解析: 全部{}题被严格校验过滤, 切换最少校验", parsed.size());

                    List<Map<String, Object>> accepted = new ArrayList<>();
                    for (Map m : parsed) {
                        String qt = stemOf(m);
                        Object ca = answerOf(m);
                        if (qt == null || ca == null || qt.isBlank()) continue;

                        Map<String, Object> q = new LinkedHashMap<>();
                        String cleanedQt = qt.replaceFirst("^\\d+[.、)．]\\s*(单选(题)?|多选(题)?|判断(题)?|填空(题)?|简答(题)?|论述(题)?)?[.、:：]?\\s*", "").trim();
                        q.put("questionText", cleanedQt);
                        q.put("questionType", typeOf(m));
                        q.put("correctAnswer", String.valueOf(ca).trim());
                        q.put("explanation", m.getOrDefault("explanation", ""));
                        if (m.containsKey("knowledgeNodeId") && m.get("knowledgeNodeId") != null) q.put("knowledgeNodeId", m.get("knowledgeNodeId"));
                        if (m.containsKey("intent") && m.get("intent") != null) q.put("intent", m.get("intent"));
                        if (m.containsKey("category") && m.get("category") != null) q.put("category", m.get("category"));
                        if (m.containsKey("knowledgeDim") && m.get("knowledgeDim") != null) q.put("knowledgeDim", m.get("knowledgeDim"));
                        if (m.containsKey("tier") && m.get("tier") != null) q.put("tier", m.get("tier"));
                        if (m.containsKey("_quality") && m.get("_quality") != null) q.put("_quality", m.get("_quality"));
                        if (m.containsKey("diagram") && m.get("diagram") instanceof Map<?,?>) q.put("diagram", m.get("diagram"));

                        Object opts = m.get("options");
                        if (opts instanceof List<?> l) q.put("options", l.stream().map(o -> stripOptionPrefix(String.valueOf(o))).toList());
                        accepted.add(q);
                    }

                    if (!accepted.isEmpty()) {
                        int fallbackFiltered = parsed.size() - accepted.size();
                        sortByType(accepted);
                        int fallbackDedup = markDuplicates(accepted);
                        metaOut.put("filtered", filtered + fallbackFiltered);
                        metaOut.put("dedupRemoved", fallbackDedup);
                        log.warn("最少校验模式接受{}题 (过滤{}题, 去重{}题)", accepted.size(), fallbackFiltered, fallbackDedup);
                        return accepted;
                    }
                } else {
                    log.warn("JSON解析: parsed为null或空，尝试裸字符串提取");
                    List<Map<String, Object>> rawExtract = tryExtractFromRawText(text);
                    if (!rawExtract.isEmpty()) { metaOut.put("filtered", 0); metaOut.put("dedupRemoved", 0); log.warn("裸字符串提取成功: {}题", rawExtract.size()); return rawExtract; }
                }
            } else {
                log.warn("JSON解析: 未找到[...]数组, jsonStart={}, jsonEnd={}", jsonStart, jsonEnd);
                List<Map<String, Object>> rawExtract = tryExtractFromRawText(text);
                if (!rawExtract.isEmpty()) { metaOut.put("filtered", 0); metaOut.put("dedupRemoved", 0); log.warn("裸字符串提取成功: {}题", rawExtract.size()); return rawExtract; }
            }
        } catch (Exception e) {
            log.warn("JSON解析异常: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            List<Map<String, Object>> rawExtract = tryExtractFromRawText(text);
            if (!rawExtract.isEmpty()) { metaOut.put("filtered", 0); metaOut.put("dedupRemoved", 0); log.warn("JSON异常后提取成功: {}题", rawExtract.size()); return rawExtract; }
        }

        // 方式2：文本标记解析
        try {
            List<Map<String, Object>> result = new ArrayList<>();
            String[] blocks = text.split("\\n\\s*\\n(?=题目：|题目 |\\d+[.、)])");
            log.info("文本解析: 分割为{}个块", blocks.length);
            if (blocks.length <= 1) blocks = new String[]{text};
            for (String block : blocks) {
                String b = block.trim();
                if (b.isEmpty()) continue;
                Map<String, Object> q = parseBlock(b);
                if (q != null) result.add(q);
            }
            if (!result.isEmpty()) { metaOut.put("filtered", 0); metaOut.put("dedupRemoved", 0); log.info("文本解析成功: {}题", result.size()); return result; }
            log.warn("文本解析: 0题");
        } catch (Exception e) {
            log.warn("文本解析异常: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        }

        log.error("无法解析AI返回(共{}字符): {}", text.length(),
            text.length() > 500 ? text.substring(0, 500) + "..." : text);
        throw new BusinessException(500, "AI 返回格式无法解析，请重试");
    }

    // ── 内部方法 ──

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractQuestionFromMap(Map m) {
        Map<String, Object> q = new LinkedHashMap<>();
        String qtRaw = stemOf(m);
        String rawQt = qtRaw == null ? "" : qtRaw;
        rawQt = rawQt.replaceFirst("^\\d+[.、)．]\\s*(单选(题)?|多选(题)?|判断(题)?|填空(题)?|简答(题)?|论述(题)?)?[.、:：]?\\s*", "").trim();
        q.put("questionText", rawQt);
        q.put("questionType", typeOf(m));
        q.put("correctAnswer", answerOf(m));
        q.put("explanation", m.getOrDefault("explanation", ""));
        if (m.containsKey("knowledgeNodeId") && m.get("knowledgeNodeId") != null) q.put("knowledgeNodeId", m.get("knowledgeNodeId"));
        if (m.containsKey("intent") && m.get("intent") != null) q.put("intent", m.get("intent"));
        if (m.containsKey("category") && m.get("category") != null) q.put("category", m.get("category"));
        if (m.containsKey("knowledgeDim") && m.get("knowledgeDim") != null) q.put("knowledgeDim", m.get("knowledgeDim"));
        if (m.containsKey("tier") && m.get("tier") != null) q.put("tier", m.get("tier"));
        if (m.containsKey("_quality") && m.get("_quality") != null) q.put("_quality", m.get("_quality"));
        if (m.containsKey("diagram") && m.get("diagram") instanceof Map<?,?>) q.put("diagram", m.get("diagram"));

        Object qt = q.get("questionType");
        if ("CLOZE".equals(qt) || "READING_COMPREHENSION".equals(qt) || "READING".equals(qt)) {
            if (m.get("passage") != null) q.put("passage", m.get("passage"));
            if (m.get("blanks") != null) q.put("blanks", m.get("blanks"));
            if (m.get("questions") != null) q.put("subQuestions", m.get("questions"));
        }
        if ("CALCULATION".equals(qt) || "PROOF".equals(qt)) {
            if (m.get("steps") != null) q.put("steps", m.get("steps"));
            if (m.get("keyPoints") != null) q.put("keyPoints", m.get("keyPoints"));
            if (m.get("subQuestions") != null) q.put("subQuestions", m.get("subQuestions"));
        }
        if ("COMPOSITION".equals(qt)) {
            if (m.get("wordLimit") != null) q.put("wordLimit", m.get("wordLimit"));
            if (m.get("scoringRubric") != null) q.put("scoringRubric", m.get("scoringRubric"));
        }

        Object opts = m.get("options");
        if (opts instanceof List<?> optList) {
            q.put("options", optList.stream().map(o -> stripOptionPrefix(String.valueOf(o))).toList());
        } else if (opts instanceof String s) {
            q.put("options", List.of(stripOptionPrefix(s.split("\\|")[0])));
        }
        return q;
    }

    // ── 格式兼容层（容忍 AI 返回的字段名/结构漂移）──

    /** 题干字段兼容: questionText / stem / question / 题干 / 题目 / content */
    public static String stemOf(Map m) {
        for (String k : new String[]{"questionText", "stem", "question", "题干", "题目", "content"}) {
            Object v = m.get(k);
            if (v != null && !String.valueOf(v).isBlank()) return String.valueOf(v);
        }
        return null;
    }

    /** 答案字段兼容: correctAnswer / answer / 正确答案 / 答案 */
    public static Object answerOf(Map m) {
        for (String k : new String[]{"correctAnswer", "answer", "正确答案", "答案"}) {
            Object v = m.get(k);
            if (v != null && !String.valueOf(v).isBlank()) return v;
        }
        return null;
    }

    /** 题型字段兼容 + 归一化 + 结构推断: questionType / type / 题型 → 标准英文枚举 */
    public static String typeOf(Map m) {
        Object t = m.get("questionType");
        if (t == null) t = m.get("type");
        if (t == null) t = m.get("题型");
        String norm = normalizeType(t);
        // 结构推断: 标称/兜底为单选, 但无选项且答案非 A-D 字母组合 → 实为填空
        // （AI 题型标签会漂移，如 fill_blank/completion，避免填空题被误当单选过滤）
        if ("SINGLE_CHOICE".equals(norm)) {
            Object opts = m.get("options");
            boolean noOpts = !(opts instanceof List<?> l) || l.isEmpty();
            if (noOpts) {
                Object ans = answerOf(m);
                String a = ans == null ? "" : String.valueOf(ans).trim();
                if (!a.isEmpty() && !a.matches("(?i)[A-D]{1,4}")) return "FILL_IN";
            }
        }
        return norm;
    }

    /** 中文题型标签 / 英文枚举 → 标准英文枚举 */
    public static String normalizeType(Object raw) {
        if (raw == null) return "SINGLE_CHOICE";
        String s = String.valueOf(raw).trim();
        if (s.isEmpty()) return "SINGLE_CHOICE";
        switch (s.toUpperCase()) {
            case "SINGLE_CHOICE": case "MULTI_CHOICE": case "TRUE_FALSE":
            case "FILL_IN": case "SHORT_ANSWER": case "ESSAY": case "CLOZE":
            case "READING_COMPREHENSION": case "READING": case "CALCULATION":
            case "PROOF": case "COMPOSITION":
                return s.toUpperCase();
            default:
        }
        if (s.contains("多选") || s.contains("多项选择")) return "MULTI_CHOICE";
        if (s.contains("单选") || s.contains("单项选择") || s.contains("选择")) return "SINGLE_CHOICE";
        if (s.contains("判断")) return "TRUE_FALSE";
        if (s.contains("填空")) return "FILL_IN";
        if (s.contains("完形")) return "CLOZE";
        if (s.contains("阅读")) return "READING_COMPREHENSION";
        if (s.contains("计算")) return "CALCULATION";
        if (s.contains("证明")) return "PROOF";
        if (s.contains("作文")) return "COMPOSITION";
        if (s.contains("简答") || s.contains("问答")) return "SHORT_ANSWER";
        if (s.contains("论述") || s.contains("分析")) return "ESSAY";
        // 英文别名(小写/非标准枚举): fill_blank/completion/gap 等
        String lo = s.toLowerCase();
        if (lo.contains("fill") || lo.contains("blank") || lo.contains("completion") || lo.contains("gap")) return "FILL_IN";
        if (lo.contains("multi")) return "MULTI_CHOICE";
        if (lo.contains("judge") || lo.contains("true") || lo.contains("false") || lo.contains("boolean")) return "TRUE_FALSE";
        if (lo.contains("cloze")) return "CLOZE";
        if (lo.contains("read")) return "READING_COMPREHENSION";
        if (lo.contains("calc")) return "CALCULATION";
        if (lo.contains("proof")) return "PROOF";
        if (lo.contains("composition")) return "COMPOSITION";
        if (lo.contains("essay") || lo.contains("discuss")) return "ESSAY";
        if (lo.contains("short") || lo.contains("brief")) return "SHORT_ANSWER";
        if (lo.contains("single") || lo.contains("choice")) return "SINGLE_CHOICE";
        return "SINGLE_CHOICE";
    }

    /** 若 AI 用 {exam:{questions:[]}} / {questions:[]} 等对象包裹, 解包为纯数组 JSON; 否则原样返回 */
    @SuppressWarnings("unchecked")
    private String unwrapToArray(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            if (nl > 0) t = t.substring(nl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
            t = t.trim();
        }
        if (!t.startsWith("{")) return text;
        try {
            Map<String, Object> root = om.readValue(t, Map.class);
            List<?> arr = findQuestionArray(root, 0);
            if (arr != null && !arr.isEmpty()) {
                log.info("对象包裹解包: 提取到题目数组 {}题", arr.size());
                return om.writeValueAsString(arr);
            }
        } catch (Exception ignore) { log.warn("AI响应解包失败: {}", ignore.getMessage()); }
        return text;
    }

    /** 递归(限深3)找对象里第一个"对象数组"(优先 questions/题目/data/items/list 键) */
    @SuppressWarnings("unchecked")
    private List<?> findQuestionArray(Object node, int depth) {
        if (depth > 3 || node == null) return null;
        if (node instanceof List<?> list) {
            return (!list.isEmpty() && list.get(0) instanceof Map) ? list : null;
        }
        if (node instanceof Map<?, ?> map) {
            for (String k : new String[]{"questions", "题目", "questionList", "data", "items", "list"}) {
                Object v = ((Map<String, Object>) map).get(k);
                if (v instanceof List<?> l && !l.isEmpty() && l.get(0) instanceof Map) return l;
            }
            for (Object v : map.values()) {
                List<?> found = findQuestionArray(v, depth + 1);
                if (found != null) return found;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    boolean isValidQuestion(Map<String, Object> q) {
        String qType = String.valueOf(q.getOrDefault("questionType", "")).trim();
        boolean isNewType = "CLOZE".equals(qType) || "READING_COMPREHENSION".equals(qType)
            || "READING".equals(qType) || "COMPOSITION".equals(qType)
            || "CALCULATION".equals(qType) || "PROOF".equals(qType);
        if (isNewType) {
            String text = String.valueOf(q.getOrDefault("questionText", "")).trim();
            String ans = String.valueOf(q.getOrDefault("correctAnswer", "")).trim();
            if (text.isEmpty() || text.length() < 3) return false;
            // CALCULATION/PROOF 答案可能在 subQuestions 里，不从顶层 correctAnswer 校验
            if ("CALCULATION".equals(qType) || "PROOF".equals(qType)) {
                if (ans.isEmpty() || ans.equals("null")) {
                    // 尝试从 subQuestions 拼装答案
                    Object sq = q.get("subQuestions");
                    if (sq instanceof List<?> sqList && !sqList.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (Object item : sqList) {
                            if (item instanceof Map<?,?> sqMap) {
                                Object sqAns = sqMap.get("correctAnswer");
                                if (sqAns != null && !sqAns.toString().isBlank()) {
                                    if (sb.length() > 0) sb.append("\n");
                                    sb.append(sqAns);
                                }
                            }
                        }
                        if (sb.length() > 0) {
                            q.put("correctAnswer", sb.toString());
                            return true;
                        }
                    }
                    return false; // 既无顶层答案也无子题答案 → 无效
                }
                return true;
            }
            if (ans.isEmpty() || ans.equals("null")) return false;
            return true;
        }

        boolean isMulti = "MULTI_CHOICE".equals(qType);
        boolean isFill = "FILL_IN".equals(qType) || "SHORT_ANSWER".equals(qType) || "ESSAY".equals(qType);
        boolean isTrueFalse = "TRUE_FALSE".equals(qType);

        if (isFill) {
            String ans = String.valueOf(q.getOrDefault("correctAnswer", "")).trim();
            if (ans.isEmpty() || ans.equals("null")) { log.warn("填空答案为空白"); return false; }
        } else if (isMulti) {
            List<String> multiOpts = (List<String>) q.get("options");
            if (multiOpts != null && multiOpts.size() > 4) {
                log.warn("多选题选项数>4({}), 截断为前4个", multiOpts.size());
                q.put("options", multiOpts.subList(0, 4));
            }
            String ans = String.valueOf(q.getOrDefault("correctAnswer", "")).trim().toUpperCase();
            if (!ans.matches("^[A-D]{2,4}$") || hasDup(ans)) {
                String fixed = fixMultiAnswer(ans);
                if (fixed == null) {
                    log.warn("多选题答案格式异常: '{}'", ans.length() > 20 ? ans.substring(0, 20) : ans);
                    return false;
                }
                ans = fixed;
            }
            q.put("correctAnswer", ans);
        } else if (isTrueFalse) {
            String ans = String.valueOf(q.getOrDefault("correctAnswer", "")).trim().toUpperCase();
            String n = normalizeAnswer(ans);
            if (n != null && n.matches("^[AB]$")) { q.put("correctAnswer", n); }
            else if (!ans.matches("^[AB]$")) {
                log.warn("判断题答案不是A/B: '{}'", ans);
                return false;
            }
        } else {
            List<String> singleOpts = (List<String>) q.get("options");
            if (singleOpts != null && singleOpts.size() > 4) {
                log.warn("单选题选项数>4({}), 截断为前4个", singleOpts.size());
                q.put("options", singleOpts.subList(0, 4));
            }
            String ans = String.valueOf(q.getOrDefault("correctAnswer", "")).trim();
            String originalAns = ans;
            String normalized = normalizeAnswer(ans);
            if (normalized == null) {
                List<String> opts = (List<String>) q.get("options");
                if (opts != null && !opts.isEmpty()) normalized = matchAnswerToOption(ans, opts);
            }
            if (normalized == null) {
                String stripped = ans.replaceAll("[^A-Za-z]", "").toUpperCase();
                if (stripped.length() == 1 && stripped.charAt(0) > 'D') {
                    List<String> opts = (List<String>) q.get("options");
                    if (opts != null && !opts.isEmpty()) {
                        String remapped = matchAnswerToOption(ans, opts);
                        if (remapped != null && remapped.matches("^[A-D]$")) {
                            log.warn("答案重映射: 原始答案{}→{}", originalAns, remapped);
                            normalized = remapped;
                        } else {
                            char maxLetter = (char) ('A' + Math.min(opts.size(), 4) - 1);
                            log.warn("答案{}超出选项范围(最大{}), 无法重映射, 回退为{}", originalAns, maxLetter, maxLetter);
                            normalized = String.valueOf(maxLetter);
                        }
                    }
                }
            }
            if (normalized != null && normalized.length() == 1 && normalized.charAt(0) > 'D') {
                List<String> opts = (List<String>) q.get("options");
                if (opts != null && !opts.isEmpty()) {
                    String remapped = matchAnswerToOption(normalized, opts);
                    if (remapped == null) remapped = matchAnswerToOption(ans, opts);
                    if (remapped != null && remapped.matches("^[A-D]$")) {
                        log.warn("答案重映射(parse阶段): {}→{}", normalized, remapped);
                        normalized = remapped;
                    }
                }
            }
            if (normalized == null || !normalized.matches("^[A-D]$")) {
                log.warn("单选题答案无法识别为A-D: '{}'", ans.length() > 60 ? ans.substring(0, 60) : ans);
                return false;
            }
            q.put("correctAnswer", normalized);
        }

        // 检测选项重复
        List<String> opts = (List<String>) q.get("options");
        if (opts != null && opts.size() >= 2) {
            Set<String> seen = new HashSet<>();
            for (String o : opts) {
                String pure = o.replaceFirst("^[A-Da-d]\\s*[.、．)）:：\\-]?\\s*", "").trim();
                if (!seen.add(pure)) {
                    log.warn("选项重复: '{}'", o.length() > 50 ? o.substring(0, 50) : o);
                    return false;
                }
            }
        }
        // 禁止"以上都是/以上都不是"
        if (opts != null) {
            for (String o : opts) {
                if (o.contains("以上都是") || o.contains("以上都不是")) { log.warn("包含以上都是"); return false; }
            }
        }
        // 单选题额外校验
        if (!isMulti && !isFill && !isTrueFalse && !isNewType && opts != null && !opts.isEmpty()) {
            String ans = String.valueOf(q.getOrDefault("correctAnswer", "")).trim().toUpperCase();
            int optCount = Math.min(opts.size(), 4);
            char maxLetter = (char) ('A' + optCount - 1);
            for (char c : ans.replaceAll("[^A-Z]", "").toCharArray()) {
                if (c > maxLetter) {
                    log.warn("答案字母{}超出选项范围(A-{}): qText={}", c, maxLetter,
                        String.valueOf(q.get("questionText")).length() > 30 ?
                            String.valueOf(q.get("questionText")).substring(0, 30) : q.get("questionText"));
                    return false;
                }
            }
        }
        return true;
    }

    boolean hasDup(String s) {
        return s.chars().distinct().count() != s.length();
    }

    String fixMultiAnswer(String raw) {
        if (raw == null) return null;
        String cleaned = raw.toUpperCase().replaceAll("[^A-D]", "");
        if (cleaned.length() >= 2 && cleaned.length() <= 4 && !hasDup(cleaned)) {
            char[] chars = cleaned.toCharArray();
            java.util.Arrays.sort(chars);
            return new String(chars);
        }
        String[] parts = raw.split("[,，、]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            String n = normalizeAnswer(part.trim());
            if (n != null && n.matches("^[A-D]$") && sb.indexOf(n) < 0) sb.append(n);
        }
        return sb.length() >= 2 ? sb.toString() : null;
    }

    String typeLabel(String key) {
        return switch (key) {
            case "SINGLE_CHOICE" -> "单选";
            case "MULTI_CHOICE" -> "多选";
            case "TRUE_FALSE" -> "判断";
            case "FILL_IN" -> "填空";
            case "ESSAY", "SHORT_ANSWER" -> "简答";
            case "CLOZE" -> "完形填空";
            case "READING_COMPREHENSION", "READING" -> "阅读理解";
            case "CALCULATION" -> "计算题";
            case "PROOF" -> "证明题";
            case "COMPOSITION" -> "作文";
            default -> key;
        };
    }

    String normalizeAnswer(String raw) {
        if (raw == null || raw.isBlank()) return null;
        if (raw.matches("^[A-D]$")) return raw;
        if (raw.matches("^[a-d]$")) return raw.toUpperCase();
        if (raw.matches("^[A-D][.、．)）]\\s*$")) return raw.substring(0, 1);
        if (raw.matches("^[a-d][.、．)）]\\s*$")) return raw.substring(0, 1).toUpperCase();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[A-D]").matcher(raw);
        if (m.find() && raw.replaceAll("[^A-Da-d]", "").length() == 1) return m.group().toUpperCase();
        return null;
    }

    String matchAnswerToOption(String answer, List<String> options) {
        String clean = answer.replaceAll("^[.、．)）]\\s*", "").trim();
        if (clean.isEmpty()) return null;
        for (int i = 0; i < options.size() && i < 4; i++) {
            String opt = stripOptionPrefix(options.get(i));
            if (opt.equals(clean) || opt.contains(clean) || clean.contains(opt))
                return String.valueOf((char) ('A' + i));
        }
        return null;
    }

    public static String stripOptionPrefix(String opt) {
        if (opt == null) return "";
        String cleaned = opt.replaceFirst("^[A-Da-d]\\s*[.、．)）:：\\-]\\s*", "").trim();
        if (cleaned.equals(opt.trim())) {
            cleaned = opt.replaceFirst("^[A-Da-d]\\s+(?=[\\u4e00-\\u9fff\\u3000-\\u303f])", "").trim();
        }
        if (cleaned.isEmpty() && opt.trim().length() <= 2) return opt.trim();
        return cleaned;
    }

    private Map<String, Object> parseBlock(String text) {
        String qText = null;
        for (String prefix : new String[]{"题目：", "题目:", "题干：", "题干:"}) {
            int idx = text.indexOf(prefix);
            if (idx >= 0) {
                int end = text.length();
                for (String sep : new String[]{"\n选项：", "\n选项:", "\n答案：", "\n答案:", "\nA.", "\nA.", "\nA "}) {
                    int s = text.indexOf(sep, idx + prefix.length());
                    if (s > 0 && s < end) end = s;
                }
                qText = text.substring(idx + prefix.length(), end).trim();
                break;
            }
        }
        if (qText == null || qText.isBlank()) return null;

        List<String> options = new ArrayList<>();
        for (String label : new String[]{"A.", "B.", "C.", "D.", "A、", "B、", "C、", "D、", "A ", "B ", "C ", "D "}) {
            int idx = text.indexOf("\n" + label);
            if (idx < 0) idx = text.indexOf(label);
            if (idx >= 0) {
                int end = text.indexOf("\n", idx + label.length() + 1);
                if (end < 0) end = text.length();
                String opt = text.substring(idx + (text.charAt(idx) == '\n' ? 1 : 0) + label.length(), end).trim();
                if (!opt.isEmpty() && !options.contains(opt)) options.add(opt);
            }
        }

        String answer = null;
        for (String prefix : new String[]{"答案：", "答案:", "正确答案：", "正确答案:"}) {
            int idx = text.indexOf(prefix);
            if (idx >= 0) {
                int end = text.indexOf('\n', idx + prefix.length());
                answer = text.substring(idx + prefix.length(), end > 0 ? end : text.length()).trim();
                break;
            }
        }

        Map<String, Object> q = new LinkedHashMap<>();
        q.put("questionText", qText);
        String detectedType = "SINGLE_CHOICE";
        if (options.size() >= 2) {
            String lower = qText.toLowerCase();
            if (lower.contains("多选") || lower.contains("multiple")) detectedType = "MULTI_CHOICE";
        } else {
            detectedType = "FILL_IN";
        }
        q.put("questionType", detectedType);
        if (!options.isEmpty()) q.put("options", options);
        if (answer != null) q.put("correctAnswer", answer);
        return q;
    }

    void sortByType(List<Map<String, Object>> list) {
        Map<String, Integer> order = new java.util.HashMap<>();
        order.put("SINGLE_CHOICE", 0); order.put("MULTI_CHOICE", 1);
        order.put("TRUE_FALSE", 2); order.put("FILL_IN", 3);
        order.put("SHORT_ANSWER", 4); order.put("ESSAY", 5);
        order.put("CLOZE", 6); order.put("READING_COMPREHENSION", 7);
        order.put("READING", 7); order.put("CALCULATION", 8);
        order.put("PROOF", 9); order.put("COMPOSITION", 10);
        list.sort(Comparator.comparingInt(q -> order.getOrDefault(
            String.valueOf(q.getOrDefault("questionType", "")), 99)));
    }

    int markDuplicates(List<Map<String, Object>> list) {
        java.util.BitSet removed = new java.util.BitSet(list.size());
        int autoRemoved = 0;
        for (int i = 0; i < list.size(); i++) {
            if (removed.get(i)) continue;
            String a = String.valueOf(list.get(i).getOrDefault("questionText", ""));
            String aAnswer = String.valueOf(list.get(i).getOrDefault("correctAnswer", ""));
            for (int j = i + 1; j < list.size(); j++) {
                if (removed.get(j)) continue;
                String b = String.valueOf(list.get(j).getOrDefault("questionText", ""));
                double sim = textSimilarity(a, b);
                if (sim > 0.75) {
                    String bAnswer = String.valueOf(list.get(j).getOrDefault("correctAnswer", ""));
                    boolean sameAnswer = aAnswer.equalsIgnoreCase(bAnswer.trim());
                    if (sameAnswer) {
                        removed.set(j);
                        autoRemoved++;
                        log.info("去重移除相似题目: qText={}", b.length() > 50 ? b.substring(0, 50) + "..." : b);
                    } else {
                        list.get(i).put("_similarTo", j);
                        list.get(j).put("_similarFrom", i);
                    }
                }
            }
        }
        if (autoRemoved > 0) {
            for (int i = list.size() - 1; i >= 0; i--) {
                if (removed.get(i)) list.remove(i);
            }
        }
        return autoRemoved;
    }

    double textSimilarity(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) return 0;
        Set<String> wa = new HashSet<>(List.of(a.replaceAll("[，。！？、；：\"\"''（）\\[\\]\\s]", " ").split(" +")));
        Set<String> wb = new HashSet<>(List.of(b.replaceAll("[，。！？、；：\"\"''（）\\[\\]\\s]", " ").split(" +")));
        wa.remove(""); wb.remove("");
        if (wa.isEmpty() || wb.isEmpty()) return 0;
        Set<String> union = new HashSet<>(wa); union.addAll(wb);
        Set<String> inter = new HashSet<>(wa); inter.retainAll(wb);
        return (double) inter.size() / union.size();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> tryExtractFromRawText(String text) {
        List<Map<String, Object>> result = new ArrayList<>();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
            "\\{[^}]*\"(?:questionText|stem|question)\"\\s*:\\s*\"([^\"]+)\"[^}]*?\"(?:correctAnswer|answer)\"\\s*:\\s*\"([^\"]*)\"[^}]*\\}");
        java.util.regex.Matcher m = p.matcher(text);
        while (m.find()) {
            Map<String, Object> q = new LinkedHashMap<>();
            q.put("questionText", m.group(1).replace("\\\"", "\""));
            q.put("correctAnswer", m.group(2).trim());
            String blob = m.group(0);
            String qt = "SINGLE_CHOICE";
            java.util.regex.Matcher tm = java.util.regex.Pattern.compile("\"(?:questionType|type)\"\\s*:\\s*\"([^\"]+)\"").matcher(blob);
            if (tm.find()) qt = tm.group(1);
            q.put("questionType", normalizeType(qt));
            java.util.regex.Matcher om = java.util.regex.Pattern.compile("\"options\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(blob);
            if (om.find()) {
                String optsStr = om.group(1);
                List<String> opts = new ArrayList<>();
                java.util.regex.Matcher vm = java.util.regex.Pattern.compile("\"([^\"]+)\"").matcher(optsStr);
                while (vm.find()) opts.add(stripOptionPrefix(vm.group(1)));
                q.put("options", opts);
            } else {
                q.put("options", List.of());
            }
            q.put("explanation", "");
            result.add(q);
        }
        return result;
    }
}
