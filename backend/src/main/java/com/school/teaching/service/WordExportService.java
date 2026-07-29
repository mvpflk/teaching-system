package com.school.teaching.service;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDocument1;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import com.school.teaching.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.*;

/**
 * 将 Markdown 内容导出为排版美化的 DOCX 文档。
 * 支持：标题(#~####)、加粗、斜体、列表、代码块、分割线、普通段落。
 */
@Service
public class WordExportService {

    private static final String FONT_CN = "微软雅黑";
    private static final String FONT_EN = "Calibri";
    private static final int MARGIN = 1440; // 1 inch = 1440 twips

    public byte[] exportMarkdown(String title, String markdown) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            // 页面设置
            CTDocument1 ctDoc = doc.getDocument();
            CTBody body = ctDoc.getBody();
            CTSectPr sect = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
            CTPageMar pm = sect.isSetPgMar() ? sect.getPgMar() : sect.addNewPgMar();
            pm.setLeft(BigInteger.valueOf(MARGIN)); pm.setRight(BigInteger.valueOf(1440));
            pm.setTop(BigInteger.valueOf(1440));   pm.setBottom(BigInteger.valueOf(1260));

            // 标题
            if (title != null && !title.isBlank()) {
                XWPFParagraph tp = doc.createParagraph();
                tp.setAlignment(ParagraphAlignment.CENTER);
                tp.setSpacingAfter(200);
                XWPFRun tr = tp.createRun();
                tr.setText(title); tr.setBold(true); tr.setFontSize(22); tr.setFontFamily(FONT_CN);
            }

            // 逐行解析
            String[] lines = markdown.split("\n");
            boolean inCodeBlock = false;
            StringBuilder codeBuf = new StringBuilder();
            String codeLang = null;

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];

                if (line.startsWith("```")) {
                    if (inCodeBlock) {
                        addCodeBlock(doc, codeBuf.toString(), codeLang);
                        codeBuf.setLength(0); codeLang = null;
                    } else {
                        codeLang = line.substring(3).trim().toLowerCase();
                    }
                    inCodeBlock = !inCodeBlock;
                    continue;
                }
                if (inCodeBlock) { codeBuf.append(line).append("\n"); continue; }
                if (line.isBlank()) continue;
                // 表格检测：续行合并
                if (isTableRow(line)) {
                    List<String[]> tableRows = new java.util.ArrayList<>();
                    tableRows.add(parseTableRow(line));
                    while (i + 1 < lines.length && isTableRow(lines[i + 1])) {
                        i++;
                        tableRows.add(parseTableRow(lines[i]));
                    }
                    // 跳过分隔行（|---|---|）
                    if (tableRows.size() >= 2 && isSeparatorRow(tableRows.get(1))) {
                        String[] header = tableRows.get(0);
                        tableRows.remove(1); // 移除分隔行
                        addTable(doc, header, tableRows.subList(1, tableRows.size()));
                    } else {
                        addTable(doc, null, tableRows);
                    }
                    continue;
                }
                if (line.matches("^[-*_]{3,}$")) { addDivider(doc); continue; }
                if (line.startsWith("# "))    { addHeading(doc, line.substring(2), 1); continue; }
                if (line.startsWith("## "))   { addHeading(doc, line.substring(3), 2); continue; }
                if (line.startsWith("### "))  { addHeading(doc, line.substring(4), 3); continue; }
                if (line.startsWith("#### ")) { addHeading(doc, line.substring(5), 4); continue; }
                if (line.matches("^\\d+[\\.\\)] .*")) { addListItem(doc, line, true); continue; }
                if (line.matches("^[-*+] .*")) { addListItem(doc, line, false); continue; }
                addParagraph(doc, line);
            }
            if (inCodeBlock) addCodeBlock(doc, codeBuf.toString(), codeLang);

            doc.write(bos);
            return bos.toByteArray();
        } catch (Exception e) { throw new BusinessException(500, "导出Word失败: " + e.getMessage()); }
    }

    private void addHeading(XWPFDocument doc, String text, int level) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(level <= 2 ? 240 : 160);
        p.setSpacingAfter(120);
        XWPFRun r = p.createRun();
        r.setText(cleanMarkdown(text)); r.setBold(true);
        r.setFontFamily(FONT_CN);
        int sz = switch (level) { case 1->18; case 2->15; case 3->13; default->12; };
        r.setFontSize(sz);
    }

    private void addParagraph(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(80);
        // 处理内联格式：**粗体** *斜体* `代码`
        parseInline(p, text);
    }

    private void addListItem(XWPFDocument doc, String line, boolean ordered) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(40); p.setIndentationLeft(420);
        String text = line.replaceFirst("^[-*+\\d]+[\\.\\)]\\s*", "");
        parseInline(p, text);
    }

    private void addCodeBlock(XWPFDocument doc, String code, String language) {
        // 流程图/mermaid → 注释提示
        if (language != null && (language.equals("mermaid") || language.equals("flow") || language.equals("flowchart"))) {
            XWPFParagraph note = doc.createParagraph();
            note.setSpacingAfter(80);
            XWPFRun nr = note.createRun();
            nr.setText("[流程图/Mermaid] — 建议在支持Mermaid渲染的编辑器中查看原文"); nr.setItalic(true);
            nr.setFontSize(10); nr.setColor("888888"); nr.setFontFamily(FONT_CN);
        }
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(80);
        CTShd shd = p.getCTP().addNewPPr().addNewShd();
        shd.setFill("F5F5F5"); shd.setVal(STShd.CLEAR);
        XWPFRun r = p.createRun();
        r.setText(code.trim()); r.setFontFamily("Consolas"); r.setFontSize(10);
        r.setColor("333333");
    }

    // ── 表格渲染 ──────────────────────────────────────

    private boolean isTableRow(String line) {
        return line.trim().startsWith("|") && line.trim().endsWith("|");
    }

    private boolean isSeparatorRow(String[] cells) {
        for (String c : cells) {
            if (!c.trim().matches("^[-:]+$")) return false;
        }
        return true;
    }

    private String[] parseTableRow(String line) {
        String s = line.trim();
        if (s.startsWith("|")) s = s.substring(1);
        if (s.endsWith("|")) s = s.substring(0, s.length() - 1);
        String[] cells = s.split("\\|");
        String[] result = new String[cells.length];
        for (int i = 0; i < cells.length; i++) result[i] = cells[i].trim();
        return result;
    }

    private void addTable(XWPFDocument doc, String[] header, List<String[]> rows) {
        int cols = header != null ? header.length : rows.stream().mapToInt(r -> r.length).max().orElse(1);
        XWPFTable table = doc.createTable(rows.size() + (header != null ? 1 : 0), cols);
        table.setWidth("100%");
        // 边框样式
        CTTblBorders borders = table.getCTTbl().addNewTblPr().addNewTblBorders();
        setBorder(borders.addNewTop()); setBorder(borders.addNewBottom());
        setBorder(borders.addNewLeft()); setBorder(borders.addNewInsideH()); setBorder(borders.addNewInsideV());

        int rowIdx = 0;
        if (header != null) {
            fillTableRow(table.getRow(rowIdx++), header, true);
        }
        for (String[] row : rows) {
            fillTableRow(table.getRow(rowIdx++), row, false);
        }
        // 表后空行
        XWPFParagraph sp = doc.createParagraph(); sp.setSpacingAfter(120);
    }

    private void setBorder(CTBorder b) {
        b.setVal(STBorder.SINGLE); b.setSz(BigInteger.valueOf(4));
        b.setColor("BFBFBF"); b.setSpace(BigInteger.valueOf(0));
    }

    private void fillTableRow(XWPFTableRow tableRow, String[] cells, boolean isHeader) {
        for (int i = 0; i < tableRow.getTableCells().size(); i++) {
            XWPFTableCell cell = tableRow.getCell(i);
            // 清空默认段落
            for (int p = cell.getParagraphs().size() - 1; p >= 0; p--) cell.removeParagraph(p);
            XWPFParagraph par = cell.addParagraph();
            par.setSpacingAfter(0);
            if (isHeader) {
                CTShd shd = par.getCTP().addNewPPr().addNewShd();
                shd.setFill("4361EE"); shd.setVal(STShd.CLEAR);
                XWPFRun r = par.createRun();
                r.setText(i < cells.length ? cells[i] : ""); r.setBold(true);
                r.setColor("FFFFFF"); r.setFontSize(10); r.setFontFamily(FONT_CN);
            } else {
                parseInline(par, i < cells.length ? cells[i] : "");
                par.getRuns().forEach(rr -> { rr.setFontSize(10); });
            }
        }
    }

    private void addDivider(XWPFDocument doc) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(120);
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPBdr bdr = p.getCTP().addNewPPr().addNewPBdr();
        CTBorder bottom = bdr.addNewBottom();
        bottom.setVal(STBorder.SINGLE); bottom.setSz(BigInteger.valueOf(6));
        bottom.setColor("CCCCCC"); bottom.setSpace(BigInteger.valueOf(1));
    }

    /** 解析 **粗体** *斜体* `代码` 混合行 */
    private void parseInline(XWPFParagraph p, String text) {
        // 简单状态机：匹配 **xxx** , *xxx* , `xxx`
        Pattern pat = Pattern.compile("(\\*\\*([^*]+)\\*\\*)|(\\*([^*]+)\\*)|(`([^`]+)`)|([^*`]+)");
        Matcher m = pat.matcher(text);
        while (m.find()) {
            if (m.group(2) != null) { addRun(p, m.group(2), true, false, false); }
            else if (m.group(4) != null) { addRun(p, m.group(4), false, true, false); }
            else if (m.group(6) != null) { addRun(p, m.group(6), false, false, true); }
            else if (m.group(7) != null) { addRun(p, m.group(7), false, false, false); }
        }
        if (p.getRuns().isEmpty()) addRun(p, text, false, false, false);
    }

    private void addRun(XWPFParagraph p, String s, boolean bold, boolean italic, boolean code) {
        if (s == null || s.isEmpty()) return;
        XWPFRun r = p.createRun();
        r.setText(s);
        r.setFontFamily(code ? "Consolas" : FONT_CN);
        r.setFontSize(11);
        if (bold) r.setBold(true);
        if (italic) r.setItalic(true);
        if (code) { r.setColor("C7254E"); r.setFontSize(10); }
    }

    private String cleanMarkdown(String s) {
        return s.replaceAll("[*_`]+", "").trim();
    }

    /** 导出试卷格式 — 题目+选项+参考答案（向后兼容旧接口，使用默认参数） */
    public byte[] exportExamPaper(String title, java.util.List<com.school.teaching.entity.QuestionBank> questions) {
        return exportExamPaper(title, questions, 100, 90, java.util.Map.of());
    }

    /**
     * 导出试卷格式 — 可配置总分/时长/题型分值
     * @param title          试卷标题
     * @param questions      题目列表（按全局顺序排列）
     * @param totalScore     试卷满分
     * @param durationMinutes 考试时长（分钟）
     * @param perTypeScores  题型→每题分值映射（key=questionType如"SINGLE_CHOICE", value=每题分数）
     */
    public byte[] exportExamPaper(String title, java.util.List<com.school.teaching.entity.QuestionBank> questions,
                                   int totalScore, int durationMinutes,
                                   java.util.Map<String, Integer> perTypeScores) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            CTDocument1 ctDoc = doc.getDocument();
            CTBody body = ctDoc.getBody();
            CTSectPr sect = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
            CTPageMar pm = sect.isSetPgMar() ? sect.getPgMar() : sect.addNewPgMar();
            pm.setLeft(BigInteger.valueOf(1440)); pm.setRight(BigInteger.valueOf(1260));
            pm.setTop(BigInteger.valueOf(1200)); pm.setBottom(BigInteger.valueOf(1200));

            // 大标题
            XWPFParagraph tp = doc.createParagraph();
            tp.setAlignment(ParagraphAlignment.CENTER); tp.setSpacingAfter(60);
            XWPFRun tr = tp.createRun();
            tr.setText(title != null ? title : "试卷"); tr.setBold(true); tr.setFontSize(20); tr.setFontFamily(FONT_CN);

            // 信息栏：使用可配置参数
            XWPFParagraph info = doc.createParagraph();
            info.setAlignment(ParagraphAlignment.CENTER); info.setSpacingAfter(200);
            XWPFRun ir = info.createRun();
            ir.setText("共 " + questions.size() + " 题    |    满分 " + totalScore + " 分    |    考试时间 " + durationMinutes + " 分钟");
            ir.setFontSize(11); ir.setColor("555555"); ir.setFontFamily(FONT_CN);

            // 题目类型分组（保持全局顺序的分组展示）
            Map<String, java.util.List<com.school.teaching.entity.QuestionBank>> byType = new LinkedHashMap<>();
            for (com.school.teaching.entity.QuestionBank q : questions) {
                String t = q.getQuestionType() != null ? q.getQuestionType() : "OTHER";
                byType.computeIfAbsent(t, k -> new java.util.ArrayList<>()).add(q);
            }

            int globalIdx = 1;
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();

            for (Map.Entry<String, java.util.List<com.school.teaching.entity.QuestionBank>> entry : byType.entrySet()) {
                String typeName = entry.getKey();
                java.util.List<com.school.teaching.entity.QuestionBank> qs = entry.getValue();

                // 题型标题 — 分值来自 perTypeScores，回退默认值
                int perQScore = perTypeScores.getOrDefault(typeName,
                    switch (typeName) {
                        case "COMPOSITION", "ESSAY" -> 30;
                        default -> perTypeScores.isEmpty() ? 5 : 1; // 无配置时默认5分，有配置但未覆盖时1分
                    });
                String cnType = toChineseTypeName(typeName);
                XWPFParagraph hp = doc.createParagraph();
                hp.setSpacingBefore(300); hp.setSpacingAfter(160);
                XWPFRun hr = hp.createRun();
                hr.setText(cnType + "（共 " + qs.size() + " 题，每题 " + perQScore + " 分）");
                hr.setBold(true); hr.setFontSize(14); hr.setFontFamily(FONT_CN);

                for (com.school.teaching.entity.QuestionBank q : qs) {
                    // 题目编号 + 题干
                    XWPFParagraph qp = doc.createParagraph();
                    qp.setSpacingBefore(120); qp.setSpacingAfter(60);
                    XWPFRun qr = qp.createRun();
                    qr.setText(globalIdx + ". " + (q.getQuestionText() != null ? q.getQuestionText() : ""));
                    qr.setFontSize(11); qr.setFontFamily(FONT_CN);
                    globalIdx++;

                    // 选项
                    String optionsJson = q.getOptions();
                    if (optionsJson != null && !optionsJson.isEmpty() && !"[]".equals(optionsJson)) {
                        try {
                            java.util.List<String> opts = om.readValue(optionsJson, java.util.List.class);
                            char label = 'A';
                            for (String opt : opts) {
                                XWPFParagraph op = doc.createParagraph();
                                op.setIndentationLeft(420); op.setSpacingAfter(20);
                                XWPFRun or = op.createRun();
                                or.setText(label + ". " + opt);
                                or.setFontSize(11); or.setFontFamily(FONT_CN);
                                label++;
                            }
                        } catch (Exception e) { /* fallback: raw options */ }
                    }

                    // 填空/简答留空行
                    if ("FILL_IN".equals(typeName) || "SHORT_ANSWER".equals(typeName)) {
                        XWPFParagraph bl = doc.createParagraph();
                        bl.setSpacingAfter(40);
                        XWPFRun br = bl.createRun();
                        br.setText("答：___________________________________");
                        br.setFontSize(11); br.setColor("999999"); br.setFontFamily(FONT_CN);
                    }

                    // 作文/写作：留作文格区域提示
                    if ("ESSAY".equals(typeName) || "COMPOSITION".equals(typeName)) {
                        XWPFParagraph grid = doc.createParagraph();
                        grid.setSpacingAfter(40);
                        XWPFRun gr = grid.createRun();
                        gr.setText("（请在下方空白处作答，不少于规定字数）");
                        gr.setFontSize(10); gr.setColor("999999"); gr.setFontFamily(FONT_CN);
                        // 留 8 行空格
                        for (int line = 0; line < 8; line++) {
                            XWPFParagraph el = doc.createParagraph();
                            el.setSpacingAfter(20);
                            XWPFRun er = el.createRun();
                            er.setText("______________________________________________________________________");
                            er.setFontSize(10); er.setColor("CCCCCC"); er.setFontFamily(FONT_CN);
                        }
                    }
                }
            }

            // 分页: 参考答案
            XWPFParagraph pageBreak = doc.createParagraph();
            pageBreak.setPageBreak(true);

            XWPFParagraph ansTitle = doc.createParagraph();
            ansTitle.setAlignment(ParagraphAlignment.CENTER); ansTitle.setSpacingAfter(200);
            XWPFRun atr = ansTitle.createRun();
            atr.setText("参考答案"); atr.setBold(true); atr.setFontSize(18); atr.setFontFamily(FONT_CN);

            int ansIdx = 1;
            for (com.school.teaching.entity.QuestionBank q : questions) {
                XWPFParagraph ap = doc.createParagraph();
                ap.setSpacingAfter(40);
                XWPFRun ar = ap.createRun();
                String ans = q.getCorrectAnswer() != null ? q.getCorrectAnswer() : "（无）";
                String explain = q.getExplanation() != null && !q.getExplanation().isBlank() ? "  【解析】" + q.getExplanation() : "";
                ar.setText(ansIdx + ". " + ans + explain);
                ar.setFontSize(10); ar.setFontFamily(FONT_CN);
                ansIdx++;
            }

            doc.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new BusinessException(500, "导出试卷Word失败: " + e.getMessage());
        }
    }

    /** 题型英文→中文标题映射 */
    private String toChineseTypeName(String typeName) {
        return switch (typeName) {
            case "SINGLE_CHOICE" -> "一、单选题";
            case "MULTI_CHOICE" -> "二、多选题";
            case "TRUE_FALSE" -> "三、判断题";
            case "FILL_IN" -> "四、填空题";
            case "SHORT_ANSWER" -> "五、简答题";
            case "ESSAY" -> "六、作文题";
            case "COMPOSITION" -> "七、写作题";
            case "DRAG_SORT" -> "排序题";
            case "MATCHING" -> "连线题";
            case "CLOZE" -> "完形填空题";
            case "READING_COMPREHENSION" -> "阅读理解题";
            case "CALCULATION" -> "计算题";
            case "PROOF" -> "证明题";
            case "PROGRAMMING" -> "编程题";
            default -> typeName;
        };
    }
}
