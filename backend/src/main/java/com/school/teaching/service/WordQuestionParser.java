package com.school.teaching.service;

import com.school.teaching.entity.QuestionBank;
import com.school.teaching.mapper.QuestionBankMapper;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHighlight;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.school.teaching.common.QuestionTypeEnum;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Word文档题库解析器
 * 支持格式:
 * [单选题] 题干
 * A. 选项A  (正确选项为加粗)
 * B. 选项B
 * 答案：A
 *
 * [多选题] 题干
 * A. 选项A
 * B. 选项B  (正确选项为加粗)
 * 答案：A,B
 *
 * [判断题] 题干
 * 答案：T  (正确加粗)
 */
@Service
public class WordQuestionParser {

    private static final Logger log = LoggerFactory.getLogger(WordQuestionParser.class);

    @Autowired
    private QuestionBankMapper bankMapper;
    @Autowired(required = false)
    private com.school.teaching.mapper.KnowledgeNodeMapper nodeMapper;

    public List<QuestionBank> parse(MultipartFile file, Long categoryId, Long userId) throws IOException {
        List<QuestionBank> result = new ArrayList<>();
        int skippedDup = 0;
        String catName = resolveCategoryName(categoryId);

        try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
            List<ParaInfo> paragraphs = new ArrayList<>();
            for (XWPFParagraph para : doc.getParagraphs()) {
                List<String> imgUrls = extractImages(para, file.getOriginalFilename());
                String text = para.getText().trim();
                if (text.isEmpty() && imgUrls.isEmpty()) continue;
                boolean isBold = isParagraphBold(para);
                boolean hasImage = !imgUrls.isEmpty();
                if (hasImage) {
                    for (String url : imgUrls) {
                        text = text.isEmpty() ? "[图片](" + url + ")" : text + "\n[图片](" + url + ")";
                    }
                }
                if (text.isEmpty()) continue;
                paragraphs.add(new ParaInfo(text, isBold, hasImage));
            }

            List<List<ParaInfo>> questionBlocks = splitIntoQuestions(paragraphs);

            for (List<ParaInfo> block : questionBlocks) {
                QuestionBank q = parseQuestion(block);
                if (q != null) {
                    // 去重：同知识点 + 同题干 → 跳过
                    if (categoryId != null) {
                        long exists = bankMapper.selectCount(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QuestionBank>()
                                .eq(QuestionBank::getCategoryId, categoryId)
                                .eq(QuestionBank::getQuestionText, q.getQuestionText()));
                        if (exists > 0) { skippedDup++; continue; }
                    }
                    q.setStatus(1); q.setCreateTime(LocalDateTime.now());
                    q.setCreatedBy(userId);
                    q.setSource("WORD_IMPORT");
                    if (categoryId != null) q.setCategoryId(categoryId);
                    if (catName != null) q.setSubject(catName);
                    bankMapper.insert(q);
                    result.add(q);
                }
            }
        }
        lastSkippedDup = skippedDup;
        return result;
    }

    /** 上次parse调用跳过的重复题数 */
    public int getLastSkippedDup() { return lastSkippedDup; }
    private int lastSkippedDup = 0;

    /** 提取段落中的图片，保存到 uploads/questions/ 并返回 URL 列表 */
    private List<String> extractImages(XWPFParagraph para, String docFilename) {
        List<String> urls = new ArrayList<>();
        for (XWPFRun run : para.getRuns()) {
            for (org.apache.poi.xwpf.usermodel.XWPFPicture pic : run.getEmbeddedPictures()) {
                try {
                    String ext = pic.getPictureData().getFileName();
                    if (ext == null || ext.isEmpty() || !ext.contains(".")) ext = "png";
                    else ext = ext.substring(ext.lastIndexOf('.') + 1);
                    String filename = "qimg_" + System.currentTimeMillis() + "_" + (urls.size() + 1) + "." + ext;
                    java.io.File dir = new java.io.File("uploads/questions");
                    if (!dir.exists()) dir.mkdirs();
                    java.io.File out = new java.io.File(dir, filename);
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
                    fos.write(pic.getPictureData().getData());
                    fos.close();
                    urls.add("/uploads/questions/" + filename);
                } catch (Exception ignored) { log.error("提取Word图片失败", ignored); }
            }
        }
        return urls;
    }

    /** 判断段落是否有任意部分的加粗 */
    private boolean isParagraphBold(XWPFParagraph para) {
        for (XWPFRun run : para.getRuns()) {
            if (run.isBold()) return true;
        }
        return false;
    }

    /** 将段落列表分割为题目块（基于选项行和答案行检测，无需题型标记） */
    private List<List<ParaInfo>> splitIntoQuestions(List<ParaInfo> paragraphs) {
        List<List<ParaInfo>> blocks = new ArrayList<>();
        List<ParaInfo> current = null;
        boolean lastWasOption = false;
        boolean lastWasAnswer = false;

        for (ParaInfo p : paragraphs) {
            String text = p.text.trim();
            if (text.contains("─") || text.contains("═") || text.startsWith("编辑说明")) {
                if (current != null) break;
                continue;
            }
            if (current == null && (text.contains("模板") || text.length() <= 4)) continue;

            boolean isOption = text.matches("^[A-E][.、].*") || text.matches("^[TF][.、].*");
            boolean isImageOnly = p.hasImage && text.replaceAll("\\[图片\\]\\([^)]+\\)", "").trim().isEmpty();
            boolean isAnswer = text.startsWith("答案") || text.startsWith("Answer");
            boolean isTypeMarker = text.matches("^\\[?(单选|多选|判断|SINGLE|MULTI|TRUE|FILL|ESSAY|填空)");

            // 纯文本作答题边界：序号开头（如"1." "2、") 或显式标记
            boolean isNumberedStart = !isOption && !isAnswer && !isTypeMarker
                && text.matches("^\\d+[.、)\\s].*") && text.length() > 5;
            // 当前块是作答题（无选项）→ 遇到新文本自动切新题
            boolean isEssayBoundary = current != null && !isImageOnly
                && !lastWasOption && !lastWasAnswer && !isOption && !isAnswer && !isTypeMarker
                && current.stream().noneMatch(t -> t.text.matches("^[A-E][.、].*"));

            // 答案行后的图片段落应归下一题（其余情况图片粘在当前题）
            boolean startNew = (isTypeMarker || isNumberedStart
                || (current != null && lastWasOption && !isOption && !isAnswer)
                || (current != null && lastWasAnswer && !isAnswer && (!isOption || isImageOnly))
                || (!isOption && !isAnswer && current == null)
                || (isOption && current == null)
                || isEssayBoundary)
                && !(isImageOnly && !lastWasAnswer && current != null && !isTypeMarker);

            if (startNew) {
                current = new ArrayList<>();
                blocks.add(current);
            }
            if (current != null) {
                current.add(p);
            }
            lastWasOption = isOption;
            lastWasAnswer = isAnswer;
        }
        return blocks;
    }

    /** 解析单个题目块 */
    private QuestionBank parseQuestion(List<ParaInfo> block) {
        if (block == null || block.isEmpty()) return null;

        QuestionBank q = new QuestionBank();
        q.setDifficultyLevel(1);
        q.setSubject("");  // 由 importFromWord 统一设置

        // Auto-detect question type
        String questionType = detectTypeFromBlock(block);
        q.setQuestionType(questionType);

        StringBuilder stemBuilder = new StringBuilder();
        List<String> options = new ArrayList<>();
        String answer = null;

        for (ParaInfo p : block) {
            String text = p.text;
            // Clean residual type markers
            text = text.replaceAll("^\\[?(单选|多选|判断|SINGLE|MULTI|TRUE|FILL|ESSAY|填空)\\]?\\s*", "");
            text = text.replaceAll("^\\d+[.、]\\s*", "");
            // Clean "题]" residues
            text = text.replaceAll("^[\\[【]?(题|单选题|多选题|判断题|填空题|问答题)[\\]】]?\\s*", "");

            if (text.startsWith("答案") || text.startsWith("Answer")) {
                answer = extractAnswer(text);
            } else if (text.matches("^[A-E][.、].*") || text.matches("^[TF][.、].*")) {
                String letter = text.substring(0, 1);
                String optText = text.replaceFirst("^[A-Z][.、]\\s*", "");
                options.add(optText);
                if (p.isBold && (questionType.equals("SINGLE_CHOICE") || questionType.equals("MULTI_CHOICE") || questionType.equals("TRUE_FALSE"))) {
                    answer = (answer == null) ? letter : answer + "," + letter;
                }
            } else {
                if (stemBuilder.length() > 0) stemBuilder.append(" ");
                stemBuilder.append(text);
            }
        }

        // Clean and set question text
        String stemText = stemBuilder.toString().trim();
        stemText = stemText.replaceAll("^[\\[【]?(题|单选题|多选题|判断题|填空题|问答题)[\\]】]?\\s*", "");
        q.setQuestionText(stemText);

        // Auto-add letter prefixes to options lacking them (skip T/F)
        List<String> prefixed = new ArrayList<>();
        boolean isTF = "TRUE_FALSE".equals(questionType);
        for (int i = 0; i < options.size(); i++) {
            String opt = options.get(i);
            if (!isTF && !opt.matches("^[A-Z][.、).]\\s*.*")) {
                opt = (char)('A' + i) + ". " + opt;
            }
            prefixed.add(opt);
        }
        q.setOptions(prefixed.isEmpty() ? "[]" : toJsonArray(prefixed));
        q.setCorrectAnswer(answer != null ? answer.trim() : "");
        q.setExplanation("");

        return q;
    }

    /** Auto-detect question type from option patterns */
    private String detectTypeFromBlock(List<ParaInfo> block) {
        int optionCount = 0, boldCount = 0;
        boolean hasTF = false, hasAnswer = false;
        String firstText = block.get(0).text;

        // Check explicit markers first
        if (firstText.contains("多选") || firstText.contains("MULTI")) return "MULTI_CHOICE";
        if (firstText.contains("判断") || firstText.contains("TRUE")) return "TRUE_FALSE";
        if (firstText.contains("填空") || firstText.contains("FILL")) return QuestionTypeEnum.FILL_IN.name();
        if (firstText.contains("作答") || firstText.contains("简答") || firstText.contains("ESSAY") || firstText.contains("论述")) return "ESSAY";

        for (ParaInfo p : block) {
            String t = p.text.trim();
            if (t.startsWith("答案") || t.startsWith("Answer")) { hasAnswer = true; continue; }
            if (t.matches("^[A-D][.、].*")) { optionCount++; if (p.isBold) boldCount++; }
            if (t.matches("^[TF][.、].*")) { hasTF = true; if (p.isBold) boldCount++; }
        }

        if (hasTF) return "TRUE_FALSE";
        if (hasAnswer && optionCount == 0) return QuestionTypeEnum.FILL_IN.name();
        if (optionCount >= 2 && boldCount > 1) return "MULTI_CHOICE";
        if (optionCount >= 2) return "SINGLE_CHOICE";
        // 无选项、无答案行、无T/F → 作答题（教师/AI人工评分）
        return "ESSAY";
    }

    /** 从"答案：X"格式中提取答案 */
    private String extractAnswer(String text) {
        Matcher m = Pattern.compile("答案[：:]?\\s*(.+)").matcher(text);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    /** 列表转JSON数组字符串 */
    private String toJsonArray(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(list.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String resolveCategoryName(Long categoryId) {
        if (categoryId == null || nodeMapper == null) return null;
        java.util.List<String> parts = new java.util.ArrayList<>();
        Long cur = categoryId; int d = 0;
        while (cur != null && d < 5) {
            var cat = nodeMapper.selectById(cur);
            if (cat == null) break;
            parts.add(0, cat.getName());
            cur = cat.getParentId(); d++;
        }
        return parts.isEmpty() ? null : String.join(" > ", parts);
    }

    static class ParaInfo {
        String text;
        boolean isBold;
        boolean hasImage;
        ParaInfo(String text, boolean isBold) {
            this.text = text; this.isBold = isBold; this.hasImage = false;
        }
        ParaInfo(String text, boolean isBold, boolean hasImage) {
            this.text = text; this.isBold = isBold; this.hasImage = hasImage;
        }
    }
}
