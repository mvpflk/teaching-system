package com.school.teaching.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public final class CheckpointContentHelper {

    private CheckpointContentHelper() {}

    public static final ObjectMapper JSON = new ObjectMapper();

    public static boolean matchKeywordValue(String value, String term, List<String> aliases) {
        if (value == null) value = "";
        String v = value.trim().toLowerCase().replace(" ", "").replace("　", "");
        String t = term.trim().toLowerCase().replace(" ", "").replace("　", "");
        if (v.equals(t)) return true;
        if (v.isEmpty()) return false;
        for (String alias : aliases) {
            String a = alias.trim().toLowerCase().replace(" ", "").replace("　", "");
            if (v.equals(a)) return true;
        }
        if (Math.abs(v.length() - t.length()) <= 1 && levenshteinDistance(v, t) <= 1) return true;
        return false;
    }

    public static int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++)
            for (int j = 1; j <= b.length(); j++)
                dp[i][j] = Math.min(dp[i-1][j-1] + (a.charAt(i-1)==b.charAt(j-1)?0:1),
                          Math.min(dp[i-1][j]+1, dp[i][j-1]+1));
        return dp[a.length()][b.length()];
    }

    public static List<Map<String, Object>> parseKeyPointsJson(String json) {
        try {
            return JSON.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("解析key_points_json失败", e);
            return List.of();
        }
    }

    public static List<Map<String, Object>> extractPracticeQuestions(List<Map<String, Object>> keyPoints, int count) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> kp : keyPoints) {
            Object pq = kp.get("practiceQuestions");
            if (pq instanceof List) {
                for (Object item : (List<?>) pq) {
                    if (item instanceof Map) {
                        result.add((Map<String, Object>) item);
                        if (result.size() >= count) return result;
                    }
                }
            }
        }
        return result;
    }

    public static Map<String, String> parseContentTemplate(String rawContent) {
        Map<String, String> result = new LinkedHashMap<>();
        if (rawContent == null || rawContent.isBlank()) {
            result.put("definitionHtml", "<p>暂无内容</p>");
            result.put("detailHtml", "<div class='kp-definition'><p>暂无内容</p></div>");
            result.put("definitionText", "");
            result.put("quickRead", "");
            result.put("coreTermsCsv", "");
            return result;
        }

        boolean isTemplate = rawContent.contains("【一句话定义】") || rawContent.contains("【定义】")
                          || rawContent.contains("【具体例子】") || rawContent.contains("【出题方向】");

        String definition = "";
        String example = "";
        String pitfalls = "";
        String examDir = "";

        if (isTemplate) {
            String[] sections = rawContent.split("(?=【)");
            for (String sec : sections) {
                String cleaned = sec.trim();
                if (cleaned.startsWith("【一句话定义】") || cleaned.startsWith("【定义】")) {
                    definition = cleaned.replaceFirst("【一句话定义】|【定义】", "").trim();
                } else if (cleaned.startsWith("【具体例子】") || cleaned.startsWith("【例子】")) {
                    example = cleaned.replaceFirst("【具体例子】|【例子】", "").trim();
                } else if (cleaned.startsWith("【常见错误】") || cleaned.startsWith("【易错】")) {
                    pitfalls = cleaned.replaceFirst("【常见错误】|【易错】", "").trim();
                } else if (cleaned.startsWith("【出题方向】") || cleaned.startsWith("【考点】")) {
                    examDir = cleaned.replaceFirst("【出题方向】|【考点】", "").trim();
                }
            }
        } else {
            definition = rawContent;
        }

        if (definition == null || definition.isBlank()) definition = rawContent;

        StringBuilder html = new StringBuilder();
        html.append("<div class='kp-definition'>");
        html.append(renderStudentFriendlyHtml(definition));
        html.append("</div>");

        if (!example.isEmpty()) {
            html.append("<details class='kp-example'><summary>📝 具体例子（点击展开）</summary>");
            html.append(renderStudentFriendlyHtml(example));
            html.append("</details>");
        }
        if (!pitfalls.isEmpty()) {
            html.append("<details class='kp-pitfalls'><summary>⚠️ 常见错误（点击展开）</summary>");
            html.append(renderStudentFriendlyHtml(pitfalls));
            html.append("</details>");
        }
        if (!examDir.isEmpty()) {
            html.append("<div class='kp-exam-tip'>");
            html.append("<span class='kp-exam-label'>考试怎么考：</span>");
            html.append(renderStudentFriendlyHtml(examDir).replaceAll("</?p>", ""));
            html.append("</div>");
        }

        result.put("detailHtml", html.toString());
        result.put("definitionText", definition);
        result.put("quickRead", extractFirstSentence(definition));
        result.put("coreTermsCsv", extractCoreTerms(definition));
        return result;
    }

    public static String renderStudentFriendlyHtml(String text) {
        if (text == null || text.isBlank()) return "";
        text = text.replaceAll("(?m)^#\\s+.*\\n?", "");
        text = text.replaceAll("\\n?---\\n?", "");

        String[] paragraphs = text.split("\\n{2,}");
        StringBuilder sb = new StringBuilder();

        for (String para : paragraphs) {
            String p = para.trim();
            if (p.isEmpty()) continue;

            if (p.startsWith("## ")) {
                sb.append("<h4>").append(inlineFormatting(p.substring(3))).append("</h4>");
                continue;
            }
            if (p.matches("^\\d+[.、)].*")) {
                sb.append("<li>").append(inlineFormatting(p.replaceFirst("^\\d+[.、)]\\s*", ""))).append("</li>");
            } else {
                String[] lines = p.split("\\n");
                sb.append("<p>");
                for (int i = 0; i < lines.length; i++) {
                    sb.append(inlineFormatting(lines[i].trim()));
                    if (i < lines.length - 1) sb.append("<br/>");
                }
                sb.append("</p>");
            }
        }

        String html = sb.toString();
        if (html.contains("<li>")) {
            html = html.replaceAll("(<li>.*?</li>)+", "<ul>$0</ul>");
        }
        return html;
    }

    public static String inlineFormatting(String text) {
        if (text == null) return "";
        text = text.replaceAll("\\*\\*(\\d+[^\\s*]*?)\\*\\*",
            "<strong class='key-number'>$1</strong>");
        text = text.replaceAll("\\*\\*([^*]+?)\\*\\*",
            "<strong class='key-concept'>$1</strong>");
        return text;
    }

    public static String extractFirstSentence(String text) {
        if (text == null || text.isBlank()) return "";
        String cleaned = text.replaceAll("^[#]\\s*[^\\n]+\\n*", "").trim();
        int dot = cleaned.indexOf('。');
        if (dot > 5) return cleaned.substring(0, dot + 1);
        if (cleaned.length() <= 30) return cleaned;
        return cleaned.substring(0, 30) + "…";
    }

    public static String extractCoreTerms(String definitionText) {
        if (definitionText == null || definitionText.isBlank()) return "";
        Matcher m = Pattern.compile("\\*\\*([^*]+?)\\*\\*").matcher(definitionText);
        List<String> terms = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        while (m.find() && terms.size() < 4) {
            String t = m.group(1).trim();
            if (t.matches("\\d+") || t.matches("[A-Za-z]{1,2}")) continue;
            if (t.length() < 2 || t.length() > 20) continue;
            if (seen.add(t.replaceAll("\\s+", ""))) {
                terms.add(t);
            }
        }
        return String.join(",", terms);
    }

    public static String cleanNodeTitle(String name) {
        if (name == null) return "";
        return name.replaceFirst("^\\d+\\s*", "");
    }

    public static List<Map<String, Object>> buildCoreKeywords(String[] coreTerms, String fullContent) {
        List<Map<String, Object>> keywords = new ArrayList<>();
        if (coreTerms == null || coreTerms.length == 0 || (coreTerms.length == 1 && coreTerms[0].isBlank())) {
            return fallbackKeywords(fullContent);
        }
        for (String term : coreTerms) {
            term = term.trim();
            if (term.isEmpty()) continue;
            Map<String, Object> kw = new LinkedHashMap<>();
            kw.put("term", term);
            kw.put("blank", term);
            kw.put("mode", "FILL");
            kw.put("context", findContext(term, fullContent));
            kw.put("acceptAliases", buildAliases(term));
            if (term.matches(".*\\d+.*") && term.length() <= 8) {
                Map<String, String> fw = new LinkedHashMap<>();
                fw.put("question", "这个数字的含义是什么？（提示：结合上下文理解）");
                fw.put("tip", fullContent != null && fullContent.contains(term) ? findContext(term, fullContent) : "");
                kw.put("followup", fw);
            }
            keywords.add(kw);
        }
        return keywords;
    }

    private static List<Map<String, Object>> fallbackKeywords(String content) {
        List<Map<String, Object>> keywords = new ArrayList<>();
        if (content == null) return keywords;
        Matcher m = Pattern.compile("\\*\\*([^*]{2,20})\\*\\*").matcher(content);
        Set<String> seen = new HashSet<>();
        while (m.find() && keywords.size() < 5) {
            String t = m.group(1).trim();
            if (t.matches("\\d+") && seen.add(t)) {
                Map<String, Object> kw = new LinkedHashMap<>();
                kw.put("term", t); kw.put("blank", t); kw.put("mode", "FILL");
                kw.put("acceptAliases", List.of());
                keywords.add(kw);
            }
        }
        return keywords;
    }

    public static String findContext(String term, String content) {
        if (content == null || term.isEmpty()) return "";
        int idx = content.indexOf(term);
        if (idx < 0) return "";
        int start = Math.max(0, idx - 15);
        int end = Math.min(content.length(), idx + term.length() + 15);
        return content.substring(start, end).replace("\n", " ").trim();
    }

    public static List<String> buildAliases(String term) {
        if (term.contains("进制")) return List.of(term.replace("进制", "进制数"));
        if (term.contains("位权")) return List.of("权值", "权重");
        if (term.equals("基数")) return List.of("radix");
        if (term.equals("数码")) return List.of("数字符号");
        return List.of();
    }

    public static Set<Long> parseQuestionIdSet(String json) {
        if (json == null || json.isBlank()) return Collections.emptySet();
        try {
            @SuppressWarnings("unchecked")
            List<Integer> list = JSON.readValue(json, List.class);
            Set<Long> set = new HashSet<>();
            for (Integer i : list) set.add(i.longValue());
            return set;
        } catch (Exception e) { return Collections.emptySet(); }
    }

    public static List<String> validateContentTemplate(String rawContent) {
        List<String> missing = new ArrayList<>();
        if (rawContent == null || rawContent.isBlank()) {
            missing.add("定义（【一句话定义】）");
            missing.add("例子（【具体例子】）");
            missing.add("考点（【出题方向】）");
            return missing;
        }
        if (!rawContent.contains("【一句话定义】") && !rawContent.contains("【定义】")) {
            missing.add("定义（【一句话定义】）");
        }
        if (!rawContent.contains("【具体例子】") && !rawContent.contains("【例子】")) {
            missing.add("例子（【具体例子】）");
        }
        if (!rawContent.contains("【出题方向】") && !rawContent.contains("【考点】")) {
            missing.add("考点（【出题方向】）");
        }
        return missing;
    }

    public static int rateToCreditLevel(double rate) {
        if (rate >= 1.0) return 10;
        if (rate >= 0.9) return 5;
        if (rate >= 0.8) return 3;
        return 0;
    }
}
