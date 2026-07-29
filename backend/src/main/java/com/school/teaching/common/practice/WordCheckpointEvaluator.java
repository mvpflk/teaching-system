package com.school.teaching.common.practice;

import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Word 步骤自动评估器 — 根据检查点规则评估 .docx 文件。
 * 规则类型：page_setup（页面设置）/ font（字体格式）/ paragraph（段落格式）/
 *          border（边框底纹）/ header_footer（页眉页脚）/ table（表格）/
 *          image（图片）/ wordart（艺术字）/ style（样式）/ toc（目录）
 */
public class WordCheckpointEvaluator {

    private static final Logger log = LoggerFactory.getLogger(WordCheckpointEvaluator.class);

    public static Map<String, Object> evaluate(byte[] fileBytes, List<Map<String, Object>> checkpoints) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();
        int passedCount = 0;
        int totalScore = 0;

        try (InputStream is = new ByteArrayInputStream(fileBytes);
             XWPFDocument doc = new XWPFDocument(is)) {

            for (Map<String, Object> cp : checkpoints) {
                Map<String, Object> cpResult = new LinkedHashMap<>();
                String type = (String) cp.getOrDefault("type", "font");
                String desc = (String) cp.getOrDefault("desc", "");
                String expected = (String) cp.getOrDefault("expected", "");
                int cpScore = toInt(cp.get("score"), 10);

                cpResult.put("id", cp.getOrDefault("id", UUID.randomUUID().toString().substring(0, 8)));
                cpResult.put("type", type);
                cpResult.put("desc", desc);
                cpResult.put("score", cpScore);

                boolean passed = false;
                String detail = "";

                try {
                    switch (type) {
                        case "page_setup":
                            passed = checkPageSetup(doc);
                            detail = passed ? "页面设置正确" : "页面设置异常";
                            break;
                        case "font":
                            passed = checkFont(doc, expected);
                            detail = passed ? "字体格式满足要求" : "期望: " + expected;
                            break;
                        case "paragraph":
                            passed = checkParagraph(doc, expected);
                            detail = passed ? "段落格式正确" : "段落格式不满足: " + expected;
                            break;
                        case "border":
                            passed = checkBorder(doc);
                            detail = passed ? "边框底纹存在" : "未找到边框或底纹";
                            break;
                        case "header_footer":
                            passed = checkHeaderFooter(doc);
                            detail = passed ? "页眉页脚存在" : "未找到页眉页脚";
                            break;
                        case "table":
                            passed = checkTable(doc, expected);
                            detail = passed ? "表格存在" : "未找到表格";
                            break;
                        case "image":
                            passed = checkImage(doc);
                            detail = passed ? "图片存在" : "未找到图片嵌入";
                            break;
                        case "wordart":
                            passed = checkWordArt(doc);
                            detail = passed ? "艺术字存在" : "未检测到艺术字";
                            break;
                        case "style":
                            passed = checkStyle(doc);
                            detail = passed ? "样式满足要求" : "未找到指定样式";
                            break;
                        case "toc":
                            passed = checkToc(doc);
                            detail = passed ? "目录存在" : "未找到目录域";
                            break;
                        default:
                            detail = "未知检查类型: " + type;
                    }
                } catch (Exception e) {
                    detail = "评估异常: " + e.getMessage();
                    log.warn("Word checkpoint eval failed: type={}", type, e);
                }

                cpResult.put("passed", passed);
                cpResult.put("detail", detail);
                if (passed) {
                    passedCount++;
                    totalScore += cpScore;
                }
                results.add(cpResult);
            }

        } catch (Exception e) {
            log.error("Failed to evaluate Word file", e);
            Map<String, Object> errCp = new LinkedHashMap<>();
            errCp.put("passed", false);
            errCp.put("desc", "文件解析失败");
            errCp.put("detail", e.getMessage());
            errCp.put("score", 0);
            results.add(errCp);
        }

        result.put("checkpoints", results);
        result.put("passedCount", passedCount);
        result.put("totalCount", checkpoints.size());
        result.put("score", totalScore);
        return result;
    }

    private static boolean checkPageSetup(XWPFDocument doc) {
        if (doc.getDocument() != null && doc.getDocument().getBody() != null) {
            var sectPr = doc.getDocument().getBody().getSectPr();
            if (sectPr != null) return true;
        }
        return !doc.getParagraphs().isEmpty();
    }

    private static boolean checkFont(XWPFDocument doc, String expected) {
        for (XWPFParagraph p : doc.getParagraphs()) {
            for (XWPFRun run : p.getRuns()) {
                if (run.getText(0) == null || run.getText(0).trim().isEmpty()) continue;
                if (expected.contains("加粗") && run.isBold()) return true;
                if (expected.contains("斜体") && run.isItalic()) return true;
                String fontFamily = run.getFontFamily();
                if (fontFamily != null && expected.contains(fontFamily)) return true;
                if (run.getFontSize() > 0) return true;
            }
        }
        return !doc.getParagraphs().isEmpty();
    }

    private static boolean checkParagraph(XWPFDocument doc, String expected) {
        for (XWPFParagraph p : doc.getParagraphs()) {
            if (p.getText() == null || p.getText().trim().isEmpty()) continue;
            if (expected.contains("首行缩进") && p.getIndentationFirstLine() > 0) return true;
            if (expected.contains("居中") && p.getAlignment() == ParagraphAlignment.CENTER) return true;
            if (expected.contains("左对齐") && p.getAlignment() == ParagraphAlignment.LEFT) return true;
            if (expected.contains("右对齐") && p.getAlignment() == ParagraphAlignment.RIGHT) return true;
            if (expected.contains("两端对齐") && p.getAlignment() == ParagraphAlignment.BOTH) return true;
            if (p.getSpacingBetween() > 0 || p.getSpacingAfter() > 0) return true;
        }
        return !doc.getParagraphs().isEmpty();
    }

    private static boolean checkBorder(XWPFDocument doc) {
        for (XWPFParagraph p : doc.getParagraphs()) {
            if (p.getCTP() != null && p.getCTP().getPPr() != null) {
                if (p.getCTP().getPPr().getPBdr() != null) return true;
                var shd = p.getCTP().getPPr().getShd();
                if (shd != null && (shd.getFill() != null || shd.getColor() != null)) return true;
            }
        }
        return !doc.getTables().isEmpty();
    }

    private static boolean checkHeaderFooter(XWPFDocument doc) {
        try {
            for (XWPFHeader h : doc.getHeaderList()) {
                if (h.getText() != null && !h.getText().trim().isEmpty()) return true;
            }
            for (XWPFFooter f : doc.getFooterList()) {
                if (f.getText() != null && !f.getText().trim().isEmpty()) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static boolean checkTable(XWPFDocument doc, String expected) {
        int tableCount = doc.getTables().size();
        if (expected != null && !expected.isEmpty()) {
            try { return tableCount >= Integer.parseInt(expected); }
            catch (NumberFormatException ignored) {}
        }
        return tableCount > 0;
    }

    private static boolean checkImage(XWPFDocument doc) {
        for (XWPFParagraph p : doc.getParagraphs()) {
            for (XWPFRun run : p.getRuns()) {
                if (!run.getEmbeddedPictures().isEmpty()) return true;
            }
        }
        return false;
    }

    private static boolean checkWordArt(XWPFDocument doc) {
        try {
            if (doc.getDocument() != null && doc.getDocument().toString().contains("wordArt"))
                return true;
        } catch (Exception ignored) {}
        for (XWPFParagraph p : doc.getParagraphs()) {
            for (XWPFRun run : p.getRuns()) {
                if (run.getCTR() != null) {
                    var drawings = run.getCTR().getDrawingList();
                    if (drawings != null && !drawings.isEmpty()) return true;
                }
            }
        }
        return false;
    }

    private static boolean checkStyle(XWPFDocument doc) {
        for (XWPFParagraph p : doc.getParagraphs()) {
            String styleId = p.getStyleID();
            if (styleId != null && !styleId.isEmpty() && !"Normal".equalsIgnoreCase(styleId))
                return true;
        }
        return false;
    }

    private static boolean checkToc(XWPFDocument doc) {
        try {
            for (XWPFParagraph p : doc.getParagraphs()) {
                if (p.getCTP() != null && p.getCTP().toString().contains("TOC"))
                    return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static int toInt(Object v, int defaultValue) {
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) {
            try { return Integer.parseInt((String) v); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }
}
