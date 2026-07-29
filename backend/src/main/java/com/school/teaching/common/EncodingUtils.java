package com.school.teaching.common;

import lombok.extern.slf4j.Slf4j;

/**
 * 编码修复工具 — 修复 MySQL latin1 连接写入 UTF-8 数据导致的
 * 双重编码（Mojibake）问题。
 *
 * <p>问题链路：原始UTF-8字节 → latin1连接写入 → MySQL以latin1解释
 * 并存储为utf8mb4 → 读取时得到乱码（如"偏"→"å"）。</p>
 *
 * <p>用法：所有从数据库读取的中文字段在返回前端前调用
 * {@code EncodingUtils.fix(text)} 即可。</p>
 */
@Slf4j
public final class EncodingUtils {

    private EncodingUtils() {}

    /** Windows-1252 0x80-0x9F → Unicode 反向映射表 */
    private static final byte[] WIN1252_REVERSE = new byte[0x2200];

    static {
        // 0x00-0xFF: identity mapping
        for (int i = 0; i < 256; i++) WIN1252_REVERSE[i] = (byte) i;
        // Windows-1252 special mappings (bytes 0x80-0x9F)
        WIN1252_REVERSE[0x20AC] = (byte) 0x80; // €
        WIN1252_REVERSE[0x201A] = (byte) 0x82; // ‚
        WIN1252_REVERSE[0x0192] = (byte) 0x83; // ƒ
        WIN1252_REVERSE[0x201E] = (byte) 0x84; // „
        WIN1252_REVERSE[0x2026] = (byte) 0x85; // …
        WIN1252_REVERSE[0x2020] = (byte) 0x86; // †
        WIN1252_REVERSE[0x2021] = (byte) 0x87; // ‡
        WIN1252_REVERSE[0x02C6] = (byte) 0x88; // ˆ
        WIN1252_REVERSE[0x2030] = (byte) 0x89; // ‰
        WIN1252_REVERSE[0x0160] = (byte) 0x8A; // Š
        WIN1252_REVERSE[0x2039] = (byte) 0x8B; // ‹
        WIN1252_REVERSE[0x0152] = (byte) 0x8C; // Œ
        WIN1252_REVERSE[0x017D] = (byte) 0x8E; // Ž
        WIN1252_REVERSE[0x2018] = (byte) 0x91; // '
        WIN1252_REVERSE[0x2019] = (byte) 0x92; // '
        WIN1252_REVERSE[0x201C] = (byte) 0x93; // "
        WIN1252_REVERSE[0x201D] = (byte) 0x94; // ”
        WIN1252_REVERSE[0x2022] = (byte) 0x95; // •
        WIN1252_REVERSE[0x2013] = (byte) 0x96; // –
        WIN1252_REVERSE[0x2014] = (byte) 0x97; // —
        WIN1252_REVERSE[0x02DC] = (byte) 0x98; // ˜
        WIN1252_REVERSE[0x2122] = (byte) 0x99; // ™
        WIN1252_REVERSE[0x0161] = (byte) 0x9A; // š
        WIN1252_REVERSE[0x203A] = (byte) 0x9B; // ›
        WIN1252_REVERSE[0x0153] = (byte) 0x9C; // œ
        WIN1252_REVERSE[0x017E] = (byte) 0x9E; // ž
        WIN1252_REVERSE[0x0178] = (byte) 0x9F; // Ÿ
    }

    /**
     * 检测并修复双重 UTF-8 编码。
     * 使用逐字符反向映射，覆盖全部 256 个字节值（含 C1 控制字符 0x80-0x9F）。
     */
    public static String fix(String text) {
        if (text == null || text.isEmpty()) return text;

        if (text.indexOf('�') >= 0) {
            log.warn("fixEncoding: 文本含U+FFFD替换字符（数据已损坏不可恢复）: {}",
                text.substring(0, Math.min(40, text.length())));
            return text;
        }

        int originalCjkCount = countCjk(text);
        if (originalCjkCount * 5 > text.length() * 4) return text;

        // 逐字符反向映射为原始字节，再以 UTF-8 解码
        byte[] bytes = new byte[text.length()];
        for (int i = 0; i < text.length(); i++) {
            int cp = text.codePointAt(i);
            bytes[i] = (cp < WIN1252_REVERSE.length && WIN1252_REVERSE[cp] != 0)
                ? WIN1252_REVERSE[cp]
                : (byte) cp; // fallback: 取低字节（对 C1 控制字符 U+008x/009x 正确）
        }
        String fixed = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        int fixedCjkCount = countCjk(fixed);

        if (fixedCjkCount > originalCjkCount) {
            log.info("fixEncoding: 修复成功 CJK {}→{}, text={}→{}",
                originalCjkCount, fixedCjkCount,
                text.substring(0, Math.min(20, text.length())),
                fixed.substring(0, Math.min(20, fixed.length())));
            return fixed;
        }

        return text;
    }

    private static int countCjk(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS) {
                count++;
            }
        }
        return count;
    }

    private static int countNonPrintable(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '?' || c == '�' || Character.isISOControl(c)
                || Character.getType(c) == Character.UNASSIGNED) {
                count++;
            }
        }
        return count;
    }
}
