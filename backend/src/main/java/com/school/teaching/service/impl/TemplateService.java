package com.school.teaching.service.impl;

import com.alibaba.excel.EasyExcel;
import com.school.teaching.dto.ExcelTemplateRow;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** 题库导入模板生成（Word 全题型示例 / Excel 模板） */
@Service
public class TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateService.class);

    /** 构建 Word 导入模板 — 全题型覆盖+图片示例 */
    public ResponseEntity<byte[]> buildWordTemplate() throws IOException {
        try (XWPFDocument doc = new XWPFDocument()) {
            // 标题
            XWPFParagraph tp = doc.createParagraph();
            tp.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun tr = tp.createRun();
            tr.setText("题库导入模板（全题型示例）"); tr.setBold(true); tr.setFontSize(18);
            doc.createParagraph();
            addHint(doc, "本模板涵盖所有题型：单选、多选、判断、填空（多答案）、作答题（含图片）。请按示例格式编辑后导入。");

            // ═══ 示例1: 单选题 ═══
            addSection(doc, "【示例1】单选题 — 正确选项加粗");
            addQbRun(doc, "以下哪个是计算机的输出设备？", false);
            addQbRun(doc, "A. 键盘", false);
            addQbRun(doc, "B. 鼠标", false);
            addQbRun(doc, "C. 显示器", true);
            addQbRun(doc, "D. 扫描仪", false);

            // ═══ 示例2: 多选题 ═══
            addSection(doc, "【示例2】多选题 — 多个正确选项加粗");
            addQbRun(doc, "以下哪些属于计算机输入设备？", false);
            addQbRun(doc, "A. 键盘", true);
            addQbRun(doc, "B. 鼠标", true);
            addQbRun(doc, "C. 显示器", false);
            addQbRun(doc, "D. 麦克风", true);

            // ═══ 示例3: 判断题 ═══
            addSection(doc, "【示例3】判断题 — T./F. 开头，正确项加粗");
            addQbRun(doc, "RAM中存储的信息在断电后会丢失。", false);
            addQbRun(doc, "T. 正确", true);
            addQbRun(doc, "F. 错误", false);

            // ═══ 示例4: 填空题 ═══
            addSection(doc, "【示例4】填空题 — 题干中用 ___ 标记空格，答案行用逗号分隔多答案");
            addQbRun(doc, "HTML中最大的标题标签是______。", false);
            addQbRun(doc, "答案：<h1>,h1,标题一", false);
            doc.createParagraph();
            addQbRun(doc, "计算机中最小的数据单位是______。", false);
            addQbRun(doc, "答案：位,bit,Bit", false);

            // ═══ 示例5: 作答题 ═══
            addSection(doc, "【示例5】作答题 — 无选项无答案行，教师手动评分");
            addQbRun(doc, "1. 请简述计算机的五大组成部分及其功能。（20分）", false);
            doc.createParagraph();
            addQbRun(doc, "2. 请解释冯·诺依曼体系结构的核心思想。（15分）", false);
            doc.createParagraph();
            addQbRun(doc, "3. 请列举三种常见的网络拓扑结构，并分别说明其优缺点。（10分）", false);

            // ═══ 示例6: 含图片的作答题 ═══
            addSection(doc, "【示例6】含图片的作答题 — 图片可嵌入在题干段落中或独立一段");
            // 生成并嵌入示例图片
            BufferedImage img = new BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = img.createGraphics();
            g2d.setColor(Color.WHITE); g2d.fillRect(0, 0, 400, 200);
            g2d.setColor(Color.BLACK); g2d.drawRect(10, 10, 380, 180);
            g2d.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            g2d.drawString("【示例图片】", 140, 60);
            g2d.drawString("这是一个计算机系统结构示意图", 80, 90);
            g2d.drawString("┌─────┐  ┌─────┐  ┌─────┐", 60, 130);
            g2d.drawString("│ CPU │──│内存 │──│硬盘 │", 60, 150);
            g2d.drawString("└─────┘  └─────┘  └─────┘", 60, 170);
            g2d.dispose();
            ByteArrayOutputStream imgBytes = new ByteArrayOutputStream();
            ImageIO.write(img, "png", imgBytes);
            // 嵌入图片到Word
            XWPFParagraph imgPara = doc.createParagraph();
            XWPFRun imgRun = imgPara.createRun();
            try {
                imgRun.addPicture(new ByteArrayInputStream(imgBytes.toByteArray()),
                    XWPFDocument.PICTURE_TYPE_PNG, "example.png", 400 * 9525, 200 * 9525);
            } catch (org.apache.poi.openxml4j.exceptions.InvalidFormatException e) {
                log.error("Word模板示例图片嵌入失败", e);
            }
            doc.createParagraph();
            addQbRun(doc, "如上图所示，请描述计算机系统中CPU、内存和硬盘之间的数据流转关系，并说明各级存储的特点。（15分）", false);

            // ═══ 说明 ═══
            addDivider(doc);
            addHint(doc, "导入规则：");
            addHint(doc, "• 题干直接写，选项以A./B./C./D.或T./F.开头，正确选项加粗");
            addHint(doc, "• 一个加粗选项→单选题，多个加粗选项→多选题");
            addHint(doc, "• 有T./F.选项→判断题（T./F.加粗表示正确答案）");
            addHint(doc, "• 有「答案：」行→填空题（多答案用逗号分隔，中英逗号均可）");
            addHint(doc, "• 无选项且无答案行→作答题（教师手动评分）");
            addHint(doc, "• 题号如「1.」「2.」开头→自动识别为新题起点");
            addHint(doc, "• 内嵌图片→自动提取并关联到所在题目");
            addHint(doc, "• 每道题之间留一个空行，便于自动分题");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            String filename = URLEncoder.encode("题库导入模板（全题型）.docx", StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(baos.toByteArray());
        }
    }

    /** 构建 Excel 导入模板 */
    public ResponseEntity<byte[]> buildExcelTemplate() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<ExcelTemplateRow> rows = new ArrayList<>();
        rows.add(new ExcelTemplateRow("SINGLE_CHOICE", "以下哪个是输出设备？", "键盘", "鼠标", "显示器", "扫描仪", "", "C", "显示器是输出设备", "计算机基础"));
        rows.add(new ExcelTemplateRow("MULTI_CHOICE", "以下哪些是偶数？", "2", "3", "4", "5", "", "A,C", "偶数定义：能被2整除", "数学"));
        rows.add(new ExcelTemplateRow("TRUE_FALSE", "CPU是计算机的核心部件。", "T", "F", "", "", "", "T", "中央处理器是计算机核心", "计算机基础"));
        rows.add(new ExcelTemplateRow("FILL_IN", "HTML中最大的标题标签是______。", "", "", "", "", "", "h1,<h1>,标题一", "多个可接受答案用逗号分隔", "编程"));
        rows.add(new ExcelTemplateRow("ESSAY", "请简述计算机网络的OSI七层模型及各层功能。", "", "", "", "", "", "", "作答题由教师手动评分", "计算机网络"));
        EasyExcel.write(baos, ExcelTemplateRow.class).sheet("题目模板").doWrite(rows);

        String filename = URLEncoder.encode("题库导入模板.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(baos.toByteArray());
    }

    private void addSection(XWPFDocument doc, String title) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setText(title); r.setBold(true); r.setFontSize(13); r.setColor("4472C4");
        r.setFontFamily("微软雅黑");
    }

    private void addHint(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setText(text); r.setFontSize(10); r.setColor("888888");
        r.setFontFamily("微软雅黑");
    }

    private void addDivider(XWPFDocument doc) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setText("──────────────────────────────"); r.setFontSize(10); r.setColor("CCCCCC");
    }

    private void addQbRun(XWPFDocument doc, String text, boolean bold) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setText(text); r.setBold(bold);
        r.setFontSize(bold ? 12 : 11);
        r.setFontFamily("微软雅黑");
    }
}
