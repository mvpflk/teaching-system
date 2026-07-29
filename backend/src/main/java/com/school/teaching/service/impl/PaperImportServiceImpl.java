package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.common.QuestionTypeEnum;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.PaperImportService;
import com.school.teaching.service.TaskService;
import com.school.teaching.service.SystemService;
import com.school.teaching.utils.JsonUtils;
import com.school.teaching.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaperImportServiceImpl implements PaperImportService {

    private final QuestionBankMapper questionMapper;
    private final TaskService taskService;
    private final TaskMapper taskMapper;
    private final TaskQuestionMapper taskQuestionMapper;
    private final TeacherMapper teacherMapper;
    private final ExamPaperMapper paperMapper;
    private final SystemService systemService;

    // 匹配各种题号格式: 1. 1、 1) (1). （1）. 一、 等
    private static final Pattern QUESTION_NUM = Pattern.compile("^\\s*([\\(（]?\\d+[\\)）]?|[一二三四五六七八九十]+)[.、．)]\\s*");
    // 匹配 A-H (常规选项) + T (判断题True)
    private static final Pattern OPTION_LETTER = Pattern.compile("^\\s*([A-HT])[.、．)]\\s*");
    private static final Pattern ANSWER_LINE = Pattern.compile("^(答案|参考答案|正确答案|正确选项|正确(?:答案|选项))\\s*[:：]\\s*(.+)", Pattern.CASE_INSENSITIVE);
    // 题型标题: 一、单选题 / 二、多选题 等（用于跳过而非分组）
    private static final Pattern TYPE_HEADER = Pattern.compile("^[一二三四五六七八九十]+[、.]?\\s*(单[项选]|多[项选]|判[断折]|填[空充]|简答|论[述证]|作[文图]|完形|阅[读解]|计[算]|综[合])(.*)");
    /** 多选题题干特征：含"以下/下列"+"的有/正确的有/说法"等（注意：不含"的是"——那是单选特征） */
    private static final Pattern MULTI_CHOICE_PATTERN = Pattern.compile(
        "(以下.*(?:的有|正确的有|说法正确的|属于.*的有|有哪些))|(?:下列.*(?:正确的|属于的|合适的|的有))", Pattern.CASE_INSENSITIVE);

    @Override
    public Map<String, Object> parse(byte[] fileBytes, String fileName, String title, String subject) {
        String ext = fileName != null && fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')).toLowerCase() : ".txt";
        List<Map<String, Object>> questions;
        if (".xlsx".equals(ext) || ".xls".equals(ext)) {
            questions = parseExcel(fileBytes);
        } else if (".txt".equals(ext)) {
            questions = parseTxt(new String(fileBytes, StandardCharsets.UTF_8));
        } else {
            questions = parseWord(fileBytes);
        }

        if (questions.isEmpty()) throw new BusinessException(400, "未能识别到任何题目，请检查文件格式");

        if (title == null || title.isEmpty()) {
            title = fileName != null && fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : "导入试卷";
        }

        // 按题型统计
        Map<String, Long> typeStats = new LinkedHashMap<>();
        for (Map<String, Object> q : questions) {
            String type = (String) q.get("questionType");
            typeStats.merge(type, 1L, Long::sum);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", title);
        result.put("subject", subject);
        result.put("questions", questions);
        result.put("typeStats", typeStats);
        result.put("totalCount", questions.size());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> create(Map<String, Object> request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "未登录");
        Teacher teacher = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
        if (teacher == null) throw new BusinessException(403, "仅教师可操作，当前用户无教师档案");
        Long teacherId = teacher.getId();

        String title = (String) request.getOrDefault("title", "导入试卷");
        String subject = (String) request.get("subject");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) request.get("questions");
        @SuppressWarnings("unchecked")
        Map<String, Object> scorePresets = (Map<String, Object>) request.get("scorePresets");
        @SuppressWarnings("unchecked")
        Map<String, Object> examConfig = (Map<String, Object>) request.get("examConfig");
        @SuppressWarnings("unchecked")
        List<Integer> targetIdsRaw = (List<Integer>) request.get("targetIds");
        boolean saveToLibrary = Boolean.TRUE.equals(request.get("saveToLibrary"));
        boolean publishNow = Boolean.TRUE.equals(request.get("publishNow"));

        if (questions == null || questions.isEmpty()) throw new BusinessException(400, "题目列表为空");
        if (targetIdsRaw == null || targetIdsRaw.isEmpty()) throw new BusinessException(400, "请选择目标班级");

        // 1. 批量插入题目到题库
        List<Long> questionIds = new ArrayList<>();
        Map<Long, String> qidTypeMap = new HashMap<>();
        Map<String, Long> typeStats = new LinkedHashMap<>();
        for (Map<String, Object> q : questions) {
            QuestionBank qb = new QuestionBank();
            qb.setQuestionType((String) q.get("questionType"));
            qb.setQuestionText((String) q.get("questionText"));
            qb.setSubject(subject);
            qb.setCreatedBy(userId);
            qb.setStatus(1);
            qb.setSource("PAPER_IMPORT");
            qb.setSchoolId(1L);
            @SuppressWarnings("unchecked")
            List<Map<String, String>> optList = (List<Map<String, String>>) q.get("options");
            if (optList != null && !optList.isEmpty()) {
                java.util.List<String> formatted = optList.stream()
                    .map(o -> o.get("label") + ". " + o.get("text"))
                    .collect(java.util.stream.Collectors.toList());
                qb.setOptions(JsonUtils.toJson(formatted));
            }
            qb.setCorrectAnswer((String) q.get("correctAnswer"));
            // TRUE_FALSE 答案归一化：将"对/错/T/F/√/×"等统一转为"A"/"B"
            if ("TRUE_FALSE".equals(qb.getQuestionType()) && qb.getCorrectAnswer() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> tfOpts = (List<Map<String, String>>) q.get("options");
                qb.setCorrectAnswer(normalizeTrueFalseAnswer(qb.getCorrectAnswer(), tfOpts));
            }
            qb.setExplanation((String) q.get("explanation"));
            questionMapper.insert(qb);
            questionIds.add(qb.getId());
            qidTypeMap.put(qb.getId(), qb.getQuestionType());
            typeStats.merge(qb.getQuestionType(), 1L, Long::sum);
        }

        // 2. 计算总分
        double totalScore = 100.0;
        Map<String, Object> normalizedPresets = new LinkedHashMap<>();
        if (scorePresets != null) {
            for (Map.Entry<String, Object> e : scorePresets.entrySet()) {
                normalizedPresets.put(normalizeTypeKey(e.getKey()), e.getValue());
            }
            totalScore = 0;
            for (Map.Entry<String, Long> e : typeStats.entrySet()) {
                double perScore = normalizedPresets.containsKey(e.getKey()) ? ((Number) normalizedPresets.get(e.getKey())).doubleValue() : defaultScore(e.getKey());
                totalScore += perScore * e.getValue();
            }
        }

        // 3. 构建 taskConfig JSON
        Map<String, Object> config = new LinkedHashMap<>();
        if (examConfig != null) config.putAll(examConfig);
        config.putIfAbsent("durationMinutes", 120);
        config.putIfAbsent("passingScore", 60);

        // 4. 为每个班级创建独立任务
        Long firstTaskId = null;
        for (Integer targetIdRaw : targetIdsRaw) {
            Long classId = targetIdRaw.longValue();
            Task task = new Task();
            task.setTitle(title);
            task.setTaskType("FORMATIVE");
            task.setScoreType("POINT_100");
            task.setSubject(subject);
            task.setTotalScore(BigDecimal.valueOf(totalScore));
            task.setTargetType("CLASS");
            task.setTargetId(classId);
            task.setTaskConfig(JsonUtils.toJson(config));
            task.setTeacherId(teacherId);
            task.setIsRequired(1);
            task.setAutoWrongbook(1);
            task.setSchoolId(1L);
            task.setStageId(4L);
            task.setStatus("DRAFT");
            taskMapper.insert(task);

            // 插入题目关联
            int sort = 0;
            for (Long qid : questionIds) {
                TaskQuestion tq = new TaskQuestion();
                tq.setTaskId(task.getId());
                tq.setQuestionId(qid);
                tq.setSortOrder(sort++);
                // 按题型取每题分值（题型在插入时已缓存，避免循环内 N+1 查询）
                String qType = qidTypeMap.get(qid);
                double perScore = normalizedPresets.containsKey(qType) ? ((Number) normalizedPresets.get(qType)).doubleValue() : defaultScore(qType);
                tq.setScore(BigDecimal.valueOf(perScore));
                tq.setSchoolId(1L);
                tq.setStageId(4L);
                taskQuestionMapper.insert(tq);
            }

            if (firstTaskId == null) firstTaskId = task.getId();
            if (publishNow) taskService.publish(task.getId());
        }

        // 5. 保存试卷库
        Long paperId = null;
        if (saveToLibrary) {
            String hash = md5(questions.toString());
            ExamPaper existing = paperMapper.selectOne(new LambdaQueryWrapper<ExamPaper>()
                    .eq(ExamPaper::getContentHash, hash).eq(ExamPaper::getCreatorId, userId));
            if (existing == null) {
                ExamPaper paper = new ExamPaper();
                paper.setTitle(title);
                paper.setSubject(subject);
                paper.setQuestionIds(JsonUtils.toJson(questionIds));
                paper.setQuestionCount(questions.size());
                paper.setTypeStats(JsonUtils.toJson(typeStats));
                paper.setScorePresets(JsonUtils.toJson(normalizedPresets));
                paper.setTotalScore(BigDecimal.valueOf(totalScore));
                paper.setExamConfig(JsonUtils.toJson(config));
                paper.setDurationMinutes(config.get("durationMinutes") instanceof Number ? ((Number) config.get("durationMinutes")).intValue() : 120);
                paper.setSourceFile((String) request.get("sourceFile"));
                paper.setContentHash(hash);
                paper.setCreatorId(userId);
                paper.setStatus(1);
                paper.setLastTaskId(firstTaskId);
                paper.setSchoolId(1L);
                paperMapper.insert(paper);
                paperId = paper.getId();
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", firstTaskId);
        result.put("paperId", paperId);
        result.put("questionCount", questions.size());
        result.put("totalScore", totalScore);
        return result;
    }

    // ── 解析器 ──

    private List<Map<String, Object>> parseWord(byte[] fileBytes) {
        List<Map<String, Object>> questions = new ArrayList<>();
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(fileBytes))) {
            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            List<ParsedLine> lines = new ArrayList<>();
            for (XWPFParagraph p : paragraphs) {
                String text = p.getText();
                if (text == null) continue;
                // 收集加粗文本（整个段落共用）
                List<String> boldTexts = new ArrayList<>();
                for (XWPFRun run : p.getRuns()) {
                    if (run.isBold() && run.text() != null) {
                        boldTexts.add(run.text().trim());
                    }
                }
                // 段落内按换行符拆分（Shift+Enter → \n），每题可能一个段落多行
                String[] subLines = text.split("\\r?\\n");
                for (String sub : subLines) {
                    String trimmed = sub.trim();
                    if (trimmed.isEmpty()) {
                        lines.add(BLANK_MARKER);
                    } else {
                        lines.add(new ParsedLine(trimmed, boldTexts));
                    }
                }
            }

            parseLinesToQuestions(lines, questions);
        } catch (Exception e) {
            throw new BusinessException(500, "Word解析失败: " + e.getMessage());
        }
        return questions;
    }

    private List<Map<String, Object>> parseTxt(String content) {
        List<Map<String, Object>> questions = new ArrayList<>();
        String[] rawLines = content.split("\\r?\\n");
        List<ParsedLine> lines = new ArrayList<>();
        for (String line : rawLines) {
            String text = line.trim();
            if (text.isEmpty()) {
                lines.add(BLANK_MARKER);
            } else {
                lines.add(new ParsedLine(text, Collections.emptyList()));
            }
        }
        parseLinesToQuestions(lines, questions);
        return questions;
    }

    private List<Map<String, Object>> parseExcel(byte[] fileBytes) {
        List<Map<String, Object>> questions = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(fileBytes))) {
            Sheet sheet = wb.getSheetAt(0);
            // 第一行是header
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return questions;
            Map<String, Integer> colMap = new LinkedHashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String h = getCellString(cell).trim();
                    colMap.put(mapExcelHeader(h), i);
                }
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String type = getCellString(row, colMap.get("type"));
                String text = getCellString(row, colMap.get("text"));
                if (text.isEmpty()) continue;

                Map<String, Object> q = new LinkedHashMap<>();
                q.put("questionType", mapTypeName(type));
                q.put("questionText", text);
                q.put("correctAnswer", getCellString(row, colMap.get("answer")));
                q.put("explanation", getCellString(row, colMap.get("explanation")));

                List<Map<String, String>> options = new ArrayList<>();
                for (char label = 'A'; label <= 'E'; label++) {
                    String opt = getCellString(row, colMap.get("option" + label));
                    if (opt != null && !opt.isEmpty()) {
                        Map<String, String> o = new LinkedHashMap<>();
                        o.put("label", String.valueOf(label));
                        o.put("text", opt);
                        options.add(o);
                    }
                }
                if (!options.isEmpty()) q.put("options", options);
                questions.add(q);
            }
        } catch (Exception e) {
            throw new BusinessException(500, "Excel解析失败: " + e.getMessage());
        }
        return questions;
    }

    // ── 通用解析逻辑 ──

    // 空白行标记 — 用于 parseLinesToQuestions 中作为题间分隔
    private static final ParsedLine BLANK_MARKER = new ParsedLine("", java.util.Collections.emptyList());

    private void parseLinesToQuestions(List<ParsedLine> lines, List<Map<String, Object>> questions) {
        // 双策略分题：1) 题号行开始新题  2) 连续空白行作为题间分隔
        List<List<ParsedLine>> groups = new ArrayList<>();
        List<ParsedLine> current = new ArrayList<>();
        int blankCount = 0;
        for (ParsedLine line : lines) {
            if (line == BLANK_MARKER) {
                blankCount++;
                if (blankCount >= 1 && !current.isEmpty()) {
                    // 遇空白行 → 结束当前题目
                    groups.add(current);
                    current = new ArrayList<>();
                }
                continue;
            }
            blankCount = 0;
            // 跳过题型标题行（"一、单选题"等）
            if (TYPE_HEADER.matcher(line.text).find()) continue;
            // 【示例X】等示例标记也跳过
            if (line.text.matches("^【[^】]+】.*")) continue;
            // 题号行开始新题
            if (QUESTION_NUM.matcher(line.text).find() && !current.isEmpty()) {
                groups.add(current);
                current = new ArrayList<>();
            }
            current.add(line);
        }
        if (!current.isEmpty()) groups.add(current);

        for (List<ParsedLine> group : groups) {
            Map<String, Object> q = parseQuestionGroup(group);
            if (q != null) questions.add(q);
        }
    }

    private Map<String, Object> parseQuestionGroup(List<ParsedLine> group) {
        if (group.isEmpty()) return null;
        Map<String, Object> q = new LinkedHashMap<>();

        // 题干：去掉题号后的第一行
        String firstLine = group.get(0).text;
        Matcher numMatcher = QUESTION_NUM.matcher(firstLine);
        String stem = numMatcher.find() ? firstLine.substring(numMatcher.end()).trim() : firstLine;

        List<Map<String, String>> options = new ArrayList<>();
        String correctAnswer = null;
        String explanation = null;

        for (int i = 1; i < group.size(); i++) {
            ParsedLine line = group.get(i);
            String text = line.text;

            // 检查答案行
            Matcher ansM = ANSWER_LINE.matcher(text);
            if (ansM.find()) {
                correctAnswer = ansM.group(2).trim()
                    .replaceAll("[（(][^）)]*[）)]", "").trim();
                continue;
            }

            // 检查选项
            Matcher optM = OPTION_LETTER.matcher(text);
            if (optM.find()) {
                String label = optM.group(1);
                String optText = text.substring(optM.end()).trim();
                Map<String, String> o = new LinkedHashMap<>();
                o.put("label", label);
                o.put("text", optText);
                options.add(o);
                // Word中加粗的选项是正确答案（支持多选：多个加粗选项用逗号拼接）
                if (!line.boldTexts.isEmpty()) {
                    correctAnswer = (correctAnswer == null) ? label : correctAnswer + "," + label;
                }
                continue;
            }

            // 解析内容
            if (explanation == null) explanation = text;
            else explanation += "\n" + text;
        }

        // 推断题型
        String questionType;
        int optionCount = options.size();
        if (optionCount == 2) {
            String optText = options.stream().map(o -> o.get("text")).collect(Collectors.joining());
            String optLabels = options.stream().map(o -> o.get("label")).collect(Collectors.joining());
            // T + F 或 对 + 错 → 判断题
            boolean isTf = optText.contains("对") || optText.contains("错")
                || optText.contains("正确") || optText.contains("错误")
                || (optLabels.contains("T") && optLabels.contains("F"));
            questionType = isTf ? "TRUE_FALSE" : "SINGLE_CHOICE";
        } else if (optionCount >= 3) {
            String cleanAns = correctAnswer != null
                ? correctAnswer.replaceAll("[，、；,\\s]", "") : "";
            boolean isMulti = correctAnswer != null && (
                cleanAns.matches("[A-D]{2,}") || correctAnswer.contains(","));
            if (!isMulti) {
                isMulti = MULTI_CHOICE_PATTERN.matcher(stem).find();
            }
            if (isMulti && correctAnswer != null && correctAnswer.length() == 1) {
                correctAnswer = null;
            }
            questionType = isMulti ? "MULTI_CHOICE" : "SINGLE_CHOICE";
        } else if (optionCount == 1) {
            // 单个选项可能是判断题(T或F单独出现)或填空题
            String label = options.get(0).get("label");
            if ("T".equals(label) || "F".equals(label)) {
                questionType = "TRUE_FALSE";
            } else {
                questionType = "FILL_IN";
            }
        } else if (correctAnswer != null && !correctAnswer.isEmpty()) {
            questionType = "FILL_IN";
        } else {
            questionType = stem.length() > 150 ? "ESSAY" : "SHORT_ANSWER";
        }

        q.put("questionType", questionType);
        q.put("questionText", stem);
        if (!options.isEmpty()) q.put("options", options);
        if (correctAnswer != null && !correctAnswer.isEmpty()) q.put("correctAnswer", correctAnswer);
        if (explanation != null && !explanation.isEmpty() && explanation.length() < 500) q.put("explanation", explanation);

        return q;
    }

    // ── 辅助方法 ──

    /**
     * 判断题答案归一化：无论原始答案是 A/B/T/F/对/错/√/×/正确/错误，
     * 统一转为 "A"(True/正确) 或 "B"(False/错误)，
     * 解决前后端对 TRUE_FALSE 约定一致性问题。
     */
    private String normalizeTrueFalseAnswer(String answer, List<Map<String, String>> options) {
        if (answer == null) return "F";
        String a = answer.trim();
        String upper = a.toUpperCase();

        // 1. 直接语义匹配：答案本身描述了 True/False
        if ("T".equals(upper) || "TRUE".equals(upper) || "对".equals(a) || "正确".equals(a) || "√".equals(a))
            return "T";
        if ("F".equals(upper) || "FALSE".equals(upper) || "错".equals(a) || "错误".equals(a) || "×".equals(a))
            return "F";

        // 2. 答案为字母(A/B等)，需根据选项内容判断该字母代表 True 还是 False
        if (options != null) {
            for (Map<String, String> opt : options) {
                String label = opt.get("label");
                if (label != null && label.equalsIgnoreCase(a)) {
                    String text = opt.get("text");
                    if (text != null) {
                        String t = text.trim();
                        if (t.contains("对") || t.contains("正确") || t.equalsIgnoreCase("T") || t.equalsIgnoreCase("TRUE") || t.contains("√"))
                            return "T";
                        if (t.contains("错") || t.contains("错误") || t.equalsIgnoreCase("F") || t.equalsIgnoreCase("FALSE") || t.contains("×"))
                            return "F";
                    }
                }
            }
        }

        // 3. 兜底：A→True, B→False（历史数据兼容）
        if ("A".equalsIgnoreCase(a)) return "T";
        if ("B".equalsIgnoreCase(a)) return "F";
        return "F";
    }

    private String mapExcelHeader(String h) {
        if (h.contains("题型") || h.contains("题目类型") || h.contains("题目类别")) return "type";
        if (h.contains("题干") || h.contains("题目内容") || h.contains("试题") || h.contains("题目")) return "text";
        if (h.contains("选项A") || h.contains("A选项")) return "optionA";
        if (h.contains("选项B") || h.contains("B选项")) return "optionB";
        if (h.contains("选项C") || h.contains("C选项")) return "optionC";
        if (h.contains("选项D") || h.contains("D选项")) return "optionD";
        if (h.contains("选项E") || h.contains("E选项")) return "optionE";
        if (h.contains("答案") || h.contains("正确")) return "answer";
        if (h.contains("解析")) return "explanation";
        return h.toLowerCase();
    }

    private String mapTypeName(String name) {
        if (name == null) return "SINGLE_CHOICE";
        String n = name.trim();
        if (n.contains("单选")) return "SINGLE_CHOICE";
        if (n.contains("多选")) return "MULTI_CHOICE";
        if (n.contains("判断") || n.contains("是非")) return "TRUE_FALSE";
        if (n.contains("填空")) return "FILL_IN";
        if (n.contains("简答")) return "SHORT_ANSWER";
        if (n.contains("论述") || n.contains("作文")) return "ESSAY";
        return "SINGLE_CHOICE";
    }

    private String normalizeTypeKey(String key) {
        if (key == null) return "SINGLE_CHOICE";
        return switch (key.toLowerCase()) {
            case "single", "single_choice", "单选" -> "SINGLE_CHOICE";
            case "multi", "multi_choice", "multiple_choice", "多选" -> "MULTI_CHOICE";
            case "judge", "true_false", "判断" -> "TRUE_FALSE";
            case "fill", "fill_in", "填空" -> "FILL_IN";
            case "short", "short_answer", "简答" -> "SHORT_ANSWER";
            case "essay", "论述", "作文" -> "ESSAY";
            default -> key.toUpperCase();
        };
    }

    private double defaultScore(String type) {
        return switch (type) {
            case "SINGLE_CHOICE" -> 2;
            case "MULTI_CHOICE" -> 3;
            case "TRUE_FALSE", "FILL_IN" -> 1;
            default -> 10;
        };
    }

    private String getCellString(Row row, Integer col) {
        if (col == null) return "";
        Cell cell = row.getCell(col);
        return cell == null ? "" : getCellString(cell);
    }

    private String getCellString(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield v == Math.floor(v) && !Double.isInfinite(v) ? String.valueOf((long) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return String.format("%032x", new BigInteger(1, digest));
        } catch (Exception e) {
            return "";
        }
    }

    // ── 内部类 ──

    private record ParsedLine(String text, List<String> boldTexts) {}
}
