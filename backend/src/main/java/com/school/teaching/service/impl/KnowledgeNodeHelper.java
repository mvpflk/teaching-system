package com.school.teaching.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public final class KnowledgeNodeHelper {

    private KnowledgeNodeHelper() {}

    public static boolean isLikelyHeading(XWPFParagraph p) {
        String text = p.getText().trim();
        if (text.isEmpty() || text.length() > 50) return false;
        if (text.endsWith("。") || text.endsWith("，") || text.endsWith("；")) return false;
        List<XWPFRun> runs = p.getRuns();
        if (runs.isEmpty()) return false;
        for (XWPFRun r : runs) {
            if (r.isBold()) return true;
        }
        return false;
    }

    public static Charset detectZipCharset(byte[] zipBytes) {
        try (ZipInputStream probe = new ZipInputStream(
                new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = probe.getNextEntry()) != null) {
                if (!entry.isDirectory() && containsGarbledChars(entry.getName())) {
                    return Charset.forName("GBK");
                }
            }
        } catch (Exception ignored) { log.debug("ZIP编码检测失败", ignored); }
        return StandardCharsets.UTF_8;
    }

    public static boolean isValidContent(String content, String fileName) {
        if (content == null || content.isBlank()) return false;
        String plain = content.replaceAll("[#*>`\\-|\\[\\]()!_~]", "")
                .replaceAll("\\s+", "").trim();
        if (plain.matches("^(https?://|!\\[).*")) return false;
        String noLinks = plain.replaceAll("https?://[^\\s]+", "");
        return noLinks.length() >= 30;
    }

    public static boolean containsGarbledChars(String name) {
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) == '�') return true;
        }
        return false;
    }

    public static String decodeContent(byte[] bytes) {
        String s = new String(bytes, StandardCharsets.UTF_8);
        if (containsGarbledChars(s)) {
            try { return new String(bytes, Charset.forName("GBK")); } catch (Exception ignored) { log.debug("GBK解码失败", ignored); }
        }
        return s;
    }

    public static String getCellStr(Row row, int idx) {
        org.apache.poi.ss.usermodel.Cell cell = row.getCell(idx);
        if (cell == null) return "";
        return cell.toString().trim();
    }

    public static String extractBvId(String url) {
        if (url == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("BV[a-zA-Z0-9]{10}").matcher(url);
        return m.find() ? m.group() : null;
    }
}
