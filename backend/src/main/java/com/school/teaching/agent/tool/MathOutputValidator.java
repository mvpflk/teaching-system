package com.school.teaching.agent.tool;

import lombok.extern.slf4j.Slf4j;

/**
 * 数学输出质量闸门 — 在流式推送给用户前检测常见 LaTeX 渲染问题。
 * 不修改原始输出（已由 preprocessAI 在前端做 \(...\) → $...$ 转换），
 * 而是统计裸 LaTeX 命令和缺失定界符，超过阈值时追加警告提示。
 */
@Slf4j
public final class MathOutputValidator {

    private MathOutputValidator() {}

    /** 检测的裸 LaTeX 命令（不带 $ 包裹则无法渲染） */
    private static final String[] LATEX_COMMANDS = {
        "\\dfrac", "\\frac", "\\sqrt", "\\text", "\\pm", "\\cdot",
        "\\infty", "\\int", "\\sum", "\\prod", "\\lim", "\\log",
        "\\sin", "\\cos", "\\tan", "\\alpha", "\\beta", "\\theta",
        "\\times", "\\div", "\\leq", "\\geq", "\\neq", "\\approx",
        "\\begin", "\\end"
    };

    /** 缺失 $ 的数学模式标记 */
    private static final String[] BARE_MATH_PATTERNS = {
        "f(x)=", "g(x)=", "h(x)=", "y=", "x=", "= ", " ="
    };

    /**
     * 校验数学输出，返回需要追加的警告文本（null 表示无问题）。
     */
    public static String validate(String raw) {
        if (raw == null || raw.isBlank()) return null;

        // 1. 自动修复 \(...\) → $...$
        String fixed = raw.replace("\\(", "$").replace("\\)", "$");
        boolean hasAutoFix = !fixed.equals(raw);

        // 2. 检测裸 LaTeX 命令（出现在 $...$ 外部）
        int bareCommandCount = 0;
        // 简单策略：移除所有 $...$ 和 $$...$$ 内容后统计
        String stripped = fixed.replaceAll("\\$\\$[^$]+\\$\\$", "")
                              .replaceAll("\\$[^$]+\\$", "");
        for (String cmd : LATEX_COMMANDS) {
            int idx = 0;
            while ((idx = stripped.indexOf(cmd, idx)) >= 0) {
                bareCommandCount++;
                idx += cmd.length();
            }
        }

        // 3. 检测裸数学模式（f(x)= 等模式出现在 $ 外部）
        int barePatternCount = 0;
        for (String pat : BARE_MATH_PATTERNS) {
            int idx = 0;
            while ((idx = stripped.indexOf(pat, idx)) >= 0) {
                barePatternCount++;
                idx += pat.length();
            }
        }

        // 4. 汇总
        int totalIssues = bareCommandCount + barePatternCount;
        if (totalIssues == 0 && !hasAutoFix) return null;

        StringBuilder warn = new StringBuilder();
        warn.append("\n\n---\n\n> ⚠️ **渲染质量提示**：");
        if (hasAutoFix) {
            warn.append("已自动修复 \\(...\\) 定界符（转为 $...$）。");
        }
        if (bareCommandCount > 0) {
            warn.append("检测到 ").append(bareCommandCount)
                .append(" 个 LaTeX 命令缺少 $ 包裹，可能无法正确渲染。");
        }
        if (barePatternCount > 0) {
            warn.append("检测到 ").append(barePatternCount)
                .append(" 处数学表达式缺少 $ 定界符。");
        }
        if (totalIssues > 3) {
            warn.append("建议重新提问以获得更好的数学公式渲染效果。");
        }

        log.info("MathOutputValidator: bareCmd={}, barePat={}, autoFix={}",
                bareCommandCount, barePatternCount, hasAutoFix);
        return warn.toString();
    }
}
