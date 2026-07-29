package com.school.teaching.service;

import com.school.teaching.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.agent.config.AgentConfig;
import com.school.teaching.service.impl.DeepSeekGateway;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * PPT 生成服务 — DeepSeek 结构化内容 → Apache POI 渲染 .pptx。
 * 教师输入课题，Agent 自动生成完整课件。
 */
@Slf4j
@Service
public class PptGenerationService {

    private static final double TEMPERATURE = 0.5;
    private static final int MAX_TOKENS = 4096;
    private static final String MODEL = "deepseek-v4-pro";

    private final DeepSeekGateway gateway;
    private final AgentConfig agentConfig;
    private final ObjectMapper om = new ObjectMapper();

    public PptGenerationService(@Qualifier("deepSeekGateway") DeepSeekGateway gateway,
                                 AgentConfig agentConfig) {
        this.gateway = gateway;
        this.agentConfig = agentConfig;
        try { Files.createDirectories(Path.of(agentConfig.getPptDir())); } catch (Exception ignored) {}
    }

    /**
     * 生成 PPT，返回文件路径。
     * @param topic 课题名称
     * @param subject 学科（如"数学[职高]"）
     * @param knowledgeContext 知识库补充内容（可选）
     * @return 本地文件路径
     */
    public String generate(String topic, String subject, String knowledgeContext) {
        // 1. DeepSeek 生成结构化幻灯片内容
        String prompt = buildPrompt(topic, subject, knowledgeContext);
        String rawJson = callDeepSeek(prompt);
        if (rawJson == null) throw new BusinessException(500, "DeepSeek API 调用失败");

        List<Map<String, Object>> slides = parseSlides(rawJson);
        if (slides == null || slides.isEmpty()) throw new BusinessException(500, "幻灯片内容解析失败");

        // 2. POI 渲染 .pptx
        String filePath = agentConfig.getPptDir() + UUID.randomUUID() + ".pptx";
        renderPptx(slides, topic, filePath);
        log.info("PPT生成: topic={}, slides={}, path={}", topic, slides.size(), filePath);
        return filePath;
    }

    // ═══════════════ Prompt ═══════════════

    private String buildPrompt(String topic, String subject, String knowledgeContext) {
        String ctxBlock = knowledgeContext != null && !knowledgeContext.isBlank()
                ? "## 知识库参考内容\n" + knowledgeContext + "\n" : "";

        return String.format("""
                你是四川职高对口升学%s教研专家。请为课题「%s」生成一份课堂教学PPT的完整内容。

                %s
                ## 输出JSON数组，每个元素一张幻灯片。支持的layout：
                | layout | 用途 | 字段 |
                |--------|------|------|
                | COVER | 封面 | title, subtitle |
                | SECTION | 章节分隔页 | title（大标题居中） |
                | CONTENT | 正文要点 | title, bullets（编号列表，每项20-35字） |
                | TABLE | 表格对比 | title, table（二维数组，首行为表头） |
                | TWO_COL | 双栏 | title, leftBullets（要点）, rightBullets（详解） |
                | SUMMARY | 小结 | title, bullets（用✦标记的关键收获+课后练习建议） |

                示例：
                [
                  {"layout":"COVER","title":"一元二次不等式","subtitle":"数学[职高] · 课堂教学课件"},
                  {"layout":"CONTENT","title":"学习目标","bullets":["掌握一元二次不等式的标准形式","能通过因式分解法求解不等式","能将解集用区间表示出来"]},
                  {"layout":"TABLE","title":"解法对比","table":[["方法","步骤","适用场景"],["因式分解法","分解→求根→画图→写解集","可分解为一次因式"],["配方法","配方→开方→解不等式","一般情况"]]},
                  {"layout":"SECTION","title":"重点突破"},
                  {"layout":"TWO_COL","title":"解题步骤","leftBullets":["①化为标准形式","②因式分解","③求出两根","④画出示意图"],"rightBullets":["⑤判断开口方向","⑥确定大于零/小于零区间","⑦写出解集","⑧验证端点"]},
                  {"layout":"SUMMARY","title":"本课小结","bullets":["✦ 解一元二次不等式关键在因式分解","✦ 大于取两边，小于取中间","✦ 建议完成课本P45习题3-5题巩固"]}
                ]

                ## 要求
                - 10-18张，含1张COVER+1张SECTION+至少1张TABLE+1张SUMMARY
                - bullets每项20-35字，有实质内容
                - 职高学生水平，用具体例子
                - 只输出JSON数组
                """,
                subject != null ? subject : "综合", topic, ctxBlock);
    }

    // ═══════════════ DeepSeek ═══════════════

    private String callDeepSeek(String prompt) {
        try {
            List<Map<String, Object>> messages = List.of(Map.of("role", "user", "content", prompt));
            Map<String, Object> result = gateway.callWithTools(messages, Collections.emptyList(),
                    TEMPERATURE, MAX_TOKENS, null, null, MODEL, null);
            String body = (String) result.get("body");
            if (body == null) return null;
            return extractContent(body);
        } catch (Exception e) {
            log.error("PPT DeepSeek 调用失败", e);
            return null;
        }
    }

    private String extractContent(String responseBody) {
        try {
            Map<String, Object> resp = om.readValue(responseBody, new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
            if (choices == null || choices.isEmpty()) return null;
            Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
            return msg != null ? (String) msg.get("content") : null;
        } catch (Exception e) {
            return null;
        }
    }

    private List<Map<String, Object>> parseSlides(String raw) {
        String json = raw.trim();
        if (json.startsWith("```")) {
            int s = json.indexOf('\n'), e = json.lastIndexOf("```");
            if (s > 0 && e > s) json = json.substring(s, e).trim();
        }
        if (!json.startsWith("[")) {
            int start = json.indexOf('['), end = json.lastIndexOf(']');
            if (start >= 0 && end > start) json = json.substring(start, end + 1);
        }
        try {
            return om.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("PPT JSON解析失败: {}", e.getMessage());
            return null;
        }
    }

    // ═══════════════ POI 渲染 ═══════════════

    // 配色方案
    private static final Color BLUE = new Color(0x43, 0x61, 0xee);
    private static final Color DARK = new Color(0x1a, 0x1a, 0x2e);
    private static final Color WHITE = new Color(0xff, 0xff, 0xff);
    private static final Color LIGHT_BG = new Color(0xf5, 0xf5, 0xf7);
    private static final Color GRAY = new Color(0x66, 0x66, 0x66);
    private static final Color ACCENT = new Color(0xe8, 0x6a, 0x17);
    private static final int W = 960, H = 540;
    private static final int MARGIN = 50;

    private void renderPptx(List<Map<String, Object>> slides, String topic, String filePath) {
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            ppt.setPageSize(new java.awt.Dimension(W, H));

            for (int i = 0; i < slides.size(); i++) {
                Map<String, Object> s = slides.get(i);
                String layout = (String) s.getOrDefault("layout", "CONTENT");
                String title = (String) s.get("title");
                String subtitle = (String) s.get("subtitle");
                @SuppressWarnings("unchecked")
                List<String> bullets = (List<String>) s.get("bullets");
                @SuppressWarnings("unchecked")
                List<String> leftB = (List<String>) s.get("leftBullets");
                @SuppressWarnings("unchecked")
                List<String> rightB = (List<String>) s.get("rightBullets");
                @SuppressWarnings("unchecked")
                List<List<String>> table = (List<List<String>>) s.get("table");

                XSLFSlide slide;
                if ("COVER".equals(layout) || i == 0) {
                    slide = ppt.createSlide();
                    drawCover(slide, title != null ? title : topic, subtitle);
                } else if ("SECTION".equals(layout)) {
                    slide = ppt.createSlide();
                    drawSection(slide, title);
                } else if ("TABLE".equals(layout) && table != null) {
                    slide = ppt.createSlide();
                    drawTableSlide(slide, title, table);
                } else if ("TWO_COL".equals(layout)) {
                    slide = ppt.createSlide();
                    drawColoredHeader(slide, title);
                    drawTwoCol(slide, leftB, rightB);
                } else if ("SUMMARY".equals(layout)) {
                    slide = ppt.createSlide();
                    drawColoredHeader(slide, title);
                    drawBullets(slide, bullets, 85, true);
                } else {
                    slide = ppt.createSlide();
                    drawColoredHeader(slide, title);
                    drawBullets(slide, bullets, 85, false);
                }
                drawPageNum(slide, i + 1, slides.size());
            }

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                ppt.write(bos);
                Files.write(Path.of(filePath), bos.toByteArray());
            }
        } catch (Exception e) {
            throw new BusinessException(500, "PPT渲染失败: " + e.getMessage());
        }
    }

    // ── 封面 ──
    private void drawCover(XSLFSlide slide, String title, String subtitle) {
        // 深色背景
        XSLFAutoShape bg = slide.createAutoShape();
        bg.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        bg.setAnchor(new java.awt.Rectangle(0, 0, W, H));
        bg.setFillColor(DARK);
        bg.setLineWidth(0); // 无边框
        // 蓝色装饰条
        XSLFAutoShape bar = slide.createAutoShape();
        bar.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        bar.setAnchor(new java.awt.Rectangle(0, H - 6, W, 6));
        bar.setFillColor(BLUE);
        bar.setLineWidth(0);
        // 标题
        XSLFTextBox tb = slide.createTextBox();
        tb.setAnchor(new java.awt.Rectangle(MARGIN, 140, W - 2 * MARGIN, 120));
        tb.setWordWrap(true);
        XSLFTextParagraph p = tb.addNewTextParagraph();
        p.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.LEFT);
        XSLFTextRun r = p.addNewTextRun();
        r.setText(title);
        r.setFontSize(40.0); r.setBold(true); r.setFontColor(WHITE);
        r.setFontFamily("Microsoft YaHei");
        // 副标题
        if (subtitle != null) {
            XSLFTextBox st = slide.createTextBox();
            st.setAnchor(new java.awt.Rectangle(MARGIN, 280, W - 2 * MARGIN, 60));
            XSLFTextParagraph sp = st.addNewTextParagraph();
            sp.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.LEFT);
            XSLFTextRun sr = sp.addNewTextRun();
            sr.setText(subtitle); sr.setFontSize(20.0); sr.setFontColor(GRAY);
            sr.setFontFamily("Microsoft YaHei");
        }
    }

    // ── 章节分隔页 ──
    private void drawSection(XSLFSlide slide, String title) {
        XSLFAutoShape bg = slide.createAutoShape();
        bg.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        bg.setAnchor(new java.awt.Rectangle(0, 0, W, H));
        bg.setFillColor(BLUE);
        bg.setLineWidth(0);
        XSLFTextBox tb = slide.createTextBox();
        tb.setAnchor(new java.awt.Rectangle(MARGIN, H / 2 - 50, W - 2 * MARGIN, 100));
        XSLFTextParagraph p = tb.addNewTextParagraph();
        p.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.CENTER);
        XSLFTextRun r = p.addNewTextRun();
        r.setText(title != null ? title : "");
        r.setFontSize(36.0); r.setBold(true); r.setFontColor(WHITE);
        r.setFontFamily("Microsoft YaHei");
    }

    // ── 顶部蓝色标题栏（所有正文页） ──
    private void drawColoredHeader(XSLFSlide slide, String title) {
        // 蓝色顶栏
        XSLFAutoShape header = slide.createAutoShape();
        header.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        header.setAnchor(new java.awt.Rectangle(0, 0, W, 70));
        header.setFillColor(BLUE);
        header.setLineWidth(0);
        // 底部装饰线
        XSLFAutoShape line = slide.createAutoShape();
        line.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        line.setAnchor(new java.awt.Rectangle(0, H - 3, W, 3));
        line.setFillColor(BLUE);
        line.setLineWidth(0);
        // 标题文字
        if (title != null) {
            XSLFTextBox tb = slide.createTextBox();
            tb.setAnchor(new java.awt.Rectangle(MARGIN, 15, W - 2 * MARGIN, 45));
            XSLFTextParagraph p = tb.addNewTextParagraph();
            XSLFTextRun r = p.addNewTextRun();
            r.setText(title); r.setFontSize(24.0); r.setBold(true);
            r.setFontColor(WHITE); r.setFontFamily("Microsoft YaHei");
        }
    }

    // ── 要点列表 ──
    private void drawBullets(XSLFSlide slide, List<String> bullets, int y, boolean isSummary) {
        if (bullets == null || bullets.isEmpty()) return;
        XSLFTextBox tb = slide.createTextBox();
        tb.setAnchor(new java.awt.Rectangle(MARGIN + 10, y, W - 2 * MARGIN - 20, H - y - 50));
        tb.setWordWrap(true);
        for (int i = 0; i < bullets.size(); i++) {
            XSLFTextParagraph p = tb.addNewTextParagraph();
            p.setIndentLevel(0);
            p.setSpaceAfter(8.0);
            // 序号 + 文字
            XSLFTextRun num = p.addNewTextRun();
            num.setText((isSummary ? "✦ " : (i + 1) + ". "));
            num.setFontSize(17.0); num.setBold(true);
            num.setFontColor(isSummary ? ACCENT : BLUE);
            num.setFontFamily("Microsoft YaHei");
            XSLFTextRun text = p.addNewTextRun();
            text.setText(bullets.get(i));
            text.setFontSize(17.0);
            text.setFontColor(DARK);
            text.setFontFamily("Microsoft YaHei");
        }
    }

    // ── 双栏布局 ──
    private void drawTwoCol(XSLFSlide slide, List<String> leftB, List<String> rightB) {
        int colW = (W - 3 * MARGIN) / 2;
        if (leftB != null) {
            XSLFTextBox lb = slide.createTextBox();
            lb.setAnchor(new java.awt.Rectangle(MARGIN, 85, colW, H - 140));
            lb.setWordWrap(true);
            // 左栏标题
            XSLFTextParagraph hp = lb.addNewTextParagraph();
            XSLFTextRun hr = hp.addNewTextRun();
            hr.setText("▎要点"); hr.setFontSize(15.0); hr.setBold(true);
            hr.setFontColor(BLUE); hr.setFontFamily("Microsoft YaHei");
            for (String b : leftB) {
                XSLFTextParagraph p = lb.addNewTextParagraph();
                p.setSpaceAfter(6.0);
                XSLFTextRun r = p.addNewTextRun();
                r.setText("• " + b);
                r.setFontSize(15.0); r.setFontColor(DARK); r.setFontFamily("Microsoft YaHei");
            }
        }
        if (rightB != null) {
            XSLFTextBox rb = slide.createTextBox();
            rb.setAnchor(new java.awt.Rectangle(2 * MARGIN + colW, 85, colW, H - 140));
            rb.setWordWrap(true);
            XSLFTextParagraph hp = rb.addNewTextParagraph();
            XSLFTextRun hr = hp.addNewTextRun();
            hr.setText("▎详解"); hr.setFontSize(15.0); hr.setBold(true);
            hr.setFontColor(BLUE); hr.setFontFamily("Microsoft YaHei");
            for (String b : rightB) {
                XSLFTextParagraph p = rb.addNewTextParagraph();
                p.setSpaceAfter(6.0);
                XSLFTextRun r = p.addNewTextRun();
                r.setText("• " + b);
                r.setFontSize(15.0); r.setFontColor(DARK); r.setFontFamily("Microsoft YaHei");
            }
        }
    }

    // ── 表格布局 ──
    private void drawTableSlide(XSLFSlide slide, String title, List<List<String>> table) {
        if (table == null || table.isEmpty()) return;
        int rows = table.size();
        int cols = table.stream().mapToInt(List::size).max().orElse(1);
        XSLFTable tbl = slide.createTable(rows, cols);
        tbl.setAnchor(new java.awt.Rectangle(MARGIN, 90, W - 2 * MARGIN, H - 160));
        for (int r = 0; r < rows; r++) {
            List<String> row = r < table.size() ? table.get(r) : List.of();
            for (int c = 0; c < cols; c++) {
                XSLFTableCell cell = tbl.getCell(r, c);
                cell.clearText();
                XSLFTextParagraph p = cell.addNewTextParagraph();
                XSLFTextRun run = p.addNewTextRun();
                String val = c < row.size() ? row.get(c) : "";
                run.setText(val);
                run.setFontSize(14.0);
                run.setFontFamily("Microsoft YaHei");
                if (r == 0) {
                    run.setBold(true); run.setFontColor(WHITE);
                    cell.setFillColor(BLUE);
                } else {
                    run.setFontColor(DARK);
                    cell.setFillColor(r % 2 == 0 ? WHITE : LIGHT_BG);
                }
            }
        }
    }

    // ── 页码 ──
    private void drawPageNum(XSLFSlide slide, int cur, int total) {
        XSLFTextBox tb = slide.createTextBox();
        tb.setAnchor(new java.awt.Rectangle(W - 120, H - 30, 100, 22));
        XSLFTextParagraph p = tb.addNewTextParagraph();
        p.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.RIGHT);
        XSLFTextRun r = p.addNewTextRun();
        r.setText(cur + " / " + total);
        r.setFontSize(10.0); r.setFontColor(GRAY); r.setFontFamily("Microsoft YaHei");
    }
}
