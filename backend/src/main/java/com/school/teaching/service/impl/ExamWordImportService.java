package com.school.teaching.service.impl;

import com.school.teaching.entity.Classes;
import com.school.teaching.entity.Exam;
import com.school.teaching.entity.ExamQuestion;
import com.school.teaching.mapper.ClassesMapper;
import com.school.teaching.mapper.ExamMapper;
import com.school.teaching.mapper.ExamQuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
// @Service — 410 DISABLED (backup tables dropped)
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
// @Service — 410 DISABLED (backup tables dropped)
public class ExamWordImportService {

    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private ExamQuestionMapper questionMapper;
    @Autowired
    private ClassesMapper classesMapper;

    private static final Pattern META_PATTERN = Pattern.compile(
        "^(试卷标题|科目|考试时长|及格分数|班级|标题|subject|duration|passing|class)\\s*[：:]\\s*(.*)", Pattern.CASE_INSENSITIVE);

    private static final Pattern TYPE_PATTERN = Pattern.compile(
        "^\\[?(单选题|多选题|判断题|填空题|问答题|综合题|代码题|实训题|SINGLE|MULTI|TRUE|FILL|ESSAY|COMPREHENSIVE|CODE|PRACTICAL)");

    @Transactional
    public Exam importFromWord(MultipartFile file, Long teacherId, Long classId, String defaultType) throws IOException {
        List<ParaInfo> paragraphs = readParagraphs(file);

        ImportMeta meta = parseImportMeta(paragraphs, file);
        classId = resolveImportClassId(classId, meta.className);

        Exam exam = createImportExam(meta.title, meta.subject, teacherId, classId, meta.duration, meta.passing);
        int[] scoreAndCount = processImportQuestions(paragraphs, meta.metaEnd, exam.getId(), defaultType);

        exam.setQuestionCount(scoreAndCount[1]);
        exam.setTotalScore(scoreAndCount[0]);
        examMapper.updateById(exam);

        return exam;
    }

    private ImportMeta parseImportMeta(List<ParaInfo> paragraphs, MultipartFile file) {
        String title = null, subject = null, className = null;
        int duration = 60, passing = 60;
        int metaEnd = 0;

        for (int i = 0; i < Math.min(paragraphs.size(), 12); i++) {
            String text = paragraphs.get(i).text;
            Matcher m = META_PATTERN.matcher(text);
            if (m.find()) {
                String key = m.group(1);
                String value = m.group(2).trim();
                if (key.contains("标题")) title = value;
                else if (key.contains("科目") || key.equalsIgnoreCase("subject")) subject = value;
                else if (key.contains("班级") || key.equalsIgnoreCase("class")) className = value;
                else if (key.contains("时长") || key.equalsIgnoreCase("duration")) {
                    try { duration = Integer.parseInt(value.replaceAll("[^0-9]", "")); }
                    catch (NumberFormatException e) {
                        log.warn("Word import: failed to parse duration '{}', using default {}", value, duration);
                    }
                }
                else if (key.contains("及格") || key.equalsIgnoreCase("passing")) {
                    try { passing = Integer.parseInt(value.replaceAll("[^0-9]", "")); }
                    catch (NumberFormatException e) {
                        log.warn("Word import: failed to parse passing score '{}', using default {}", value, passing);
                    }
                }
                metaEnd = i + 1;
            } else if (TYPE_PATTERN.matcher(text).find()) {
                break;
            }
        }

        if (title == null || title.isEmpty()) {
            title = file.getOriginalFilename() != null
                ? file.getOriginalFilename().replaceAll("\\.docx?$", "")
                : "导入试卷";
        }

        return new ImportMeta(title, subject, className, duration, passing, metaEnd);
    }

    private Long resolveImportClassId(Long classId, String className) {
        if (classId == null && className != null && !className.isEmpty()) {
            LambdaQueryWrapper<Classes> cw = new LambdaQueryWrapper<>();
            cw.eq(Classes::getClassName, className);
            Classes cls = classesMapper.selectOne(cw);
            if (cls != null) return cls.getId();
        }
        return classId;
    }

    private Exam createImportExam(String title, String subject, Long teacherId, Long classId,
                                   int duration, int passing) {
        Exam exam = new Exam();
        exam.setTitle(title);
        exam.setSubject(subject);
        exam.setTeacherId(teacherId);
        exam.setClassId(classId);
        exam.setExamType("exam");
        exam.setDurationMinutes(duration);
        exam.setPassingScore(passing);
        exam.setIsRandomOrder(0);
        exam.setStatus("draft");
        exam.setStartTime(LocalDateTime.now());
        exam.setEndTime(LocalDateTime.now().plusDays(30));
        exam.setTotalScore(0);
        exam.setQuestionCount(0);
        examMapper.insert(exam);
        return exam;
    }

    private int[] processImportQuestions(List<ParaInfo> paragraphs, int metaEnd,
                                          Long examId, String defaultType) {
        List<ParaInfo> questionParas = paragraphs.subList(metaEnd, paragraphs.size());
        List<List<ParaInfo>> blocks = splitIntoQuestions(questionParas);
        int totalScore = 0;
        int sort = 1;

        for (List<ParaInfo> block : blocks) {
            ExamQuestion q = parseQuestion(block, examId, sort, defaultType);
            if (q != null) {
                questionMapper.insert(q);
                totalScore += q.getScore() != null ? q.getScore() : 5;
                sort++;
            }
        }
        return new int[]{totalScore, sort - 1};
    }

    private record ImportMeta(String title, String subject, String className, int duration, int passing, int metaEnd) {}

    private List<ParaInfo> readParagraphs(MultipartFile file) throws IOException {
        List<ParaInfo> result = new ArrayList<>();
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = para.getText().trim();
                if (text.isEmpty()) continue;
                boolean bold = false;
                for (XWPFRun run : para.getRuns()) {
                    if (run.isBold()) { bold = true; break; }
                }
                result.add(new ParaInfo(text, bold));
            }
        }
        return result;
    }

    private List<List<ParaInfo>> splitIntoQuestions(List<ParaInfo> paragraphs) {
        List<List<ParaInfo>> blocks = new ArrayList<>();
        List<ParaInfo> current = null;
        boolean seenQuestions = false;
        boolean lastWasOption = false;
        boolean lastWasAnswer = false;

        for (ParaInfo p : paragraphs) {
            String text = p.text.trim();
            if (seenQuestions && (text.contains("═") || text.contains("─") || text.startsWith("编辑说明"))) break;
            if (!seenQuestions && (text.contains("═") || text.contains("─"))) continue;
            // Skip example labels and instruction lines
            if (text.matches(".*示例\\d.*[单多判填].*") || text.startsWith("【示例")) continue;
            if (!seenQuestions && (text.contains("无需")
                || (text.length() >= 2 && text.length() <= 15
                    && !text.matches("^[A-E][.、].*") && !text.matches("^[TF][.、].*")
                    && !text.contains("答案") && !text.contains("？") && !text.contains("?")))) continue;

            boolean isOption = text.matches("^[A-E][.、].*") || text.matches("^[TF][.、].*");
            boolean isAnswer = text.startsWith("答案") || text.startsWith("Answer");
            boolean isTypeMarker = TYPE_PATTERN.matcher(text).find();

            // Start new block when: type marker, non-option text after options, first non-option in questions, or first option
            boolean startNew = isTypeMarker
                || (current != null && lastWasOption && !isOption && !isAnswer)
                || (current != null && lastWasAnswer && !isAnswer && !isOption)
                || (!isOption && !isAnswer && current == null && !seenQuestions)
                || (isOption && current == null);

            if (startNew) {
                seenQuestions = true;
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

    private ExamQuestion parseQuestion(List<ParaInfo> block, Long examId, int sort, String defaultType) {
        if (block == null || block.isEmpty()) return null;

        ExamQuestion q = new ExamQuestion();
        q.setExamId(examId);
        q.setSortOrder(sort);
        q.setDifficultyLevel(1);

        // Auto-detect type from options pattern
        q.setQuestionType(detectType(block, defaultType));

        StringBuilder stem = new StringBuilder();
        List<String> options = new ArrayList<>();
        String answer = null;

        for (ParaInfo p : block) {
            String text = p.text;
            // Skip separators
            if (text.contains("═")) continue;

            text = text.replaceAll("^\\[?(单选题|多选题|判断题|填空题|问答题|综合题|代码题|实训题|SINGLE|MULTI|TRUE|FILL|ESSAY|COMPREHENSIVE|CODE|PRACTICAL)\\]?\\s*", "");
            text = text.replaceAll("^\\d+[.、]\\s*", "");

            if (text.startsWith("答案") || text.startsWith("Answer")) {
                answer = extractAnswer(text);
            } else if (text.matches("^[A-E][.、]\\s*.*")) {
                String optText = text.replaceFirst("^[A-E][.、]\\s*", "");
                options.add(optText);
                if (p.isBold && (q.getQuestionType().equals("SINGLE_CHOICE")
                    || q.getQuestionType().equals("MULTI_CHOICE"))) {
                    String letter = text.substring(0, 1);
                    answer = (answer == null) ? letter : answer + "," + letter;
                }
            } else if (text.matches("^[TF][.、].*")) {
                if (p.isBold && q.getQuestionType().equals("TRUE_FALSE")) {
                    answer = text.substring(0, 1);
                }
            } else {
                if (stem.length() > 0) stem.append(" ");
                stem.append(text);
            }
        }

        String stemText = stem.toString().trim();
        // Clean residual type markers like "题]" or "[题]" or "单选题]"
        stemText = stemText.replaceAll("^[\\[【]?(题|单选题|多选题|判断题|填空题|问答题|综合题|代码题|实训题)[\\]】]?\\s*", "");
        q.setQuestionText(stemText);
        // Auto-add letter prefixes to options that lack them (skip T/F)
        String qType = q.getQuestionType();
        List<String> prefixed = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            String opt = options.get(i);
            if (!"TRUE_FALSE".equals(qType) && !opt.matches("^[A-Z][.、．)\\s].*")) {
                opt = (char)('A' + i) + ". " + opt;
            }
            prefixed.add(opt);
        }
        q.setOptions(prefixed.isEmpty() ? "[]" : toJson(prefixed));
        q.setCorrectAnswer(answer != null ? answer.trim() : "");
        q.setScore(defaultScore(q.getQuestionType()));

        return q;
    }

    /** Auto-detect question type from option patterns */
    private String detectType(List<ParaInfo> block, String defaultType) {
        int optionCount = 0;
        int boldCount = 0;
        boolean hasTF = false;
        boolean hasAnswer = false;

        for (ParaInfo p : block) {
            String text = p.text.trim();
            if (text.startsWith("答案") || text.startsWith("Answer")) {
                hasAnswer = true;
                continue;
            }
            if (text.matches("^[A-D][.、].*")) {
                optionCount++;
                if (p.isBold) boldCount++;
            }
            if (text.matches("^[TF][.、].*")) {
                hasTF = true;
                if (p.isBold) boldCount++;
            }
        }

        // First check explicit text markers (for backward compatibility)
        String firstText = block.get(0).text;
        if (firstText.contains("多选") || firstText.contains("MULTI")) return "MULTI_CHOICE";
        if (firstText.contains("判断") || firstText.contains("TRUE")) return "TRUE_FALSE";
        if (firstText.contains("填空") || firstText.contains("FILL")) return "FILL_IN";
        if (firstText.contains("问答") || firstText.contains("ESSAY")) return "ESSAY";
        if (firstText.contains("综合") || firstText.contains("COMPREHENSIVE")) return "COMPREHENSIVE";
        if (firstText.contains("代码") || firstText.contains("CODE")) return "CODE";
        if (firstText.contains("实训") || firstText.contains("PRACTICAL")) return "PRACTICAL";

        // Auto-detect from pattern
        if (hasTF) return "TRUE_FALSE";
        if (hasAnswer && optionCount == 0) return "FILL_IN";
        if (optionCount >= 2 && boldCount > 1) return "MULTI_CHOICE";
        if (optionCount >= 2 && boldCount == 1) return "SINGLE_CHOICE";

        return defaultType != null ? defaultType : "SINGLE_CHOICE";
    }

    private int defaultScore(String type) {
        return switch (type) {
            case "SINGLE_CHOICE" -> 2;
            case "MULTI_CHOICE" -> 3;
            case "TRUE_FALSE", "FILL_IN" -> 1;
            default -> 10;  // ESSAY/COMPREHENSIVE/CODE/PRACTICAL
        };
    }

    private String extractAnswer(String text) {
        Matcher m = Pattern.compile("答案[：:]?\\s*(.+)").matcher(text);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    private String toJson(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(list.get(i).replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    public byte[] generateTemplate() throws IOException {
        try (XWPFDocument doc = new XWPFDocument()) {
            // Title
            XWPFParagraph titleP = doc.createParagraph();
            titleP.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleR = titleP.createRun();
            titleR.setText("试卷导入模板");
            titleR.setBold(true);
            titleR.setFontSize(16);
            addBlankLine(doc);

            addMetaLine(doc, "试卷标题：期中测试");
            addMetaLine(doc, "科目：Java程序设计");
            addMetaLine(doc, "班级：计算机2025-1班（选填，可留空）");
            addMetaLine(doc, "考试时长：60（分钟）");
            addMetaLine(doc, "及格分数：60（分）");
            addSeparator(doc);

            addRun(doc, "Java的入口方法签名是？", false, 12, null);
            addRun(doc, "A. void main()", false, 11, null);
            addRun(doc, "B. public static void main(String[] args)", true, 11, null);
            addRun(doc, "C. static void main(String[] args)", false, 11, null);
            addRun(doc, "D. void main(String args)", false, 11, null);
            addBlankLine(doc);

            addRun(doc, "以下哪些是Java的基本数据类型？", false, 12, null);
            addRun(doc, "A. int", true, 11, null);
            addRun(doc, "B. String", false, 11, null);
            addRun(doc, "C. boolean", true, 11, null);
            addRun(doc, "D. Integer", false, 11, null);
            addBlankLine(doc);

            addRun(doc, "Java支持多继承。", false, 12, null);
            addRun(doc, "T. 正确", false, 11, null);
            addRun(doc, "F. 错误", true, 11, null);
            addBlankLine(doc);

            addRun(doc, "Java程序的编译命令是______。", false, 12, null);
            addRun(doc, "答案：javac", false, 11, null);
            addBlankLine(doc);

            addSeparator(doc);
            addRun(doc, "编辑说明：", true, 11, null);
            addRun(doc, "1. 修改前5行基本信息（标题/科目/班级/时长/及格分）", false, 10, null);
            addRun(doc, "2. 题目直接写题干，选项以 A. B. C. D. 或 T. F. 开头", false, 10, null);
            addRun(doc, "3. 正确选项的文字加粗，系统自动识别题型和答案", false, 10, null);
            addRun(doc, "4. T./F.选项→判断题，有「答案：」行→填空题，一个加粗=单选，多个加粗=多选", false, 10, null);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            doc.write(bos);
            return bos.toByteArray();
        }
    }

    private void addMetaLine(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setFontSize(11);
        r.setFontFamily("微软雅黑");
    }

    private void addRun(XWPFDocument doc, String text, boolean bold, int fontSize, String color) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(bold);
        r.setFontSize(fontSize);
        r.setFontFamily("微软雅黑");
        if (color != null) r.setColor(color);
    }

    private void addBlankLine(XWPFDocument doc) {
        doc.createParagraph();
    }

    private void addSeparator(XWPFDocument doc) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setText("──────────────────────────────────────");
        r.setFontSize(10);
        r.setColor("AAAAAA");
    }

    static class ParaInfo {
        String text;
        boolean isBold;
        ParaInfo(String t, boolean b) { text = t; isBold = b; }
    }
}
