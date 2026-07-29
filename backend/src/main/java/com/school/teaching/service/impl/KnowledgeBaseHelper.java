package com.school.teaching.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.KnowledgeArticle;
import com.school.teaching.entity.KnowledgeFlashcard;

import java.util.*;

public final class KnowledgeBaseHelper {

    private KnowledgeBaseHelper() {}

    public static void extractSections(String content, KnowledgeArticle article) {
        java.util.regex.Pattern memPattern = java.util.regex.Pattern.compile(
            "^#{2,3}\\s*记忆口诀\\s*$", java.util.regex.Pattern.MULTILINE);
        java.util.regex.Matcher memMatcher = memPattern.matcher(content);
        if (memMatcher.find()) {
            int start = memMatcher.end();
            int end = content.indexOf("\n## ", start);
            if (end < 0) end = content.indexOf("\n---", start);
            if (end < 0) end = content.length();
            article.setMemoryTips(content.substring(start, end).trim());
        }
        int examIdx = content.indexOf("## 考试重点提示");
        if (examIdx >= 0) {
            int end = content.indexOf("\n---", examIdx + "## 考试重点提示".length());
            if (end < 0) end = content.length();
            article.setExamFocus(content.substring(examIdx, end).trim());
        }
    }

    public static List<KnowledgeFlashcard> extractFlashcards(KnowledgeArticle article) {
        List<KnowledgeFlashcard> cards = new ArrayList<>();
        String content = article.getContentMd();
        if (content == null || content.isEmpty()) return cards;
        int sortOrder = 0;

        java.util.regex.Pattern termPattern = java.util.regex.Pattern.compile(
            "\\*\\*([^*\n]+?)\\*\\*[：:](.+?)(?:[。\n]|$)", java.util.regex.Pattern.MULTILINE);
        java.util.regex.Matcher termMatcher = termPattern.matcher(content);
        while (termMatcher.find() && cards.size() < 8) {
            String term = termMatcher.group(1).trim();
            String def = termMatcher.group(2).trim();
            if (term.length() >= 2 && def.length() >= 4 && !term.matches("[0-9\\s\\-|]+")) {
                KnowledgeFlashcard card = new KnowledgeFlashcard();
                card.setArticleId(article.getId());
                card.setFrontText("什么是" + term + "？");
                card.setBackText(def.length() > 300 ? def.substring(0, 297) + "..." : def);
                card.setSortOrder(sortOrder++);
                cards.add(card);
            }
        }

        java.util.regex.Pattern sectionPattern = java.util.regex.Pattern.compile(
            "^#{2,3}\\s+(.+?)$\\n+((?:(?!#{1,3}\\s).+?\\n)*)", java.util.regex.Pattern.MULTILINE);
        java.util.regex.Matcher sectionMatcher = sectionPattern.matcher(content);
        while (sectionMatcher.find() && cards.size() < 8) {
            String heading = sectionMatcher.group(1).trim();
            String body = sectionMatcher.group(2).trim();
            if (heading.contains("考纲") || heading.contains("考试重点") || heading.contains("记忆口诀")
                || heading.contains("习题") || heading.contains("实训") || heading.contains("附录"))
                continue;
            String cleanBody = body.replaceAll("[*#>\\-|]", " ").replaceAll("\\s+", " ").trim();
            if (heading.length() >= 4 && cleanBody.length() >= 20) {
                KnowledgeFlashcard card = new KnowledgeFlashcard();
                card.setArticleId(article.getId());
                card.setFrontText("简述" + heading);
                card.setBackText(cleanBody.length() > 300 ? cleanBody.substring(0, 297) + "..." : cleanBody);
                card.setSortOrder(sortOrder++);
                cards.add(card);
            }
        }

        String memoryTips = article.getMemoryTips();
        if (memoryTips != null && !memoryTips.isEmpty() && cards.size() < 6) {
            String[] lines = memoryTips.split("\\n");
            for (String line : lines) {
                String trimmed = line.replaceAll("^[-*>\\s]+|\\s+$", "").trim();
                if (trimmed.length() >= 8 && trimmed.length() <= 200) {
                    KnowledgeFlashcard card = new KnowledgeFlashcard();
                    card.setArticleId(article.getId());
                    card.setFrontText("记忆口诀：这道口诀讲的是什么？");
                    card.setBackText(trimmed);
                    card.setSortOrder(sortOrder++);
                    cards.add(card);
                    if (cards.size() >= 8) break;
                }
            }
        }

        if (cards.size() < 2) {
            java.util.regex.Pattern hPattern = java.util.regex.Pattern.compile(
                "^#{1,2}\\s+(.+?)$", java.util.regex.Pattern.MULTILINE);
            java.util.regex.Matcher hMatcher = hPattern.matcher(content);
            while (hMatcher.find() && cards.size() < 3) {
                String h = hMatcher.group(1).trim();
                if (h.length() >= 4 && !h.contains("考纲") && !h.contains("重点") && !h.contains("口诀")) {
                    KnowledgeFlashcard card = new KnowledgeFlashcard();
                    card.setArticleId(article.getId());
                    card.setFrontText("请简述：" + h);
                    card.setBackText("请阅读文章「" + article.getTitle() + "」中关于「" + h + "」的内容");
                    card.setSortOrder(sortOrder++);
                    cards.add(card);
                }
            }
        }

        return cards;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> parseCardArray(String response) {
        try {
            int start = response.indexOf('['), end = response.lastIndexOf(']');
            if (start >= 0 && end > start) response = response.substring(start, end + 1);
            return new ObjectMapper().readValue(response, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    public static Map<String, Object> toSimpleMap(KnowledgeArticle a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("title", a.getTitle());
        m.put("chapter", a.getChapter());
        m.put("task", a.getTask());
        m.put("difficulty", a.getDifficulty());
        m.put("tags", a.getTags());
        m.put("excerpt", a.getExcerpt());
        return m;
    }
}
