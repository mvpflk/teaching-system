package com.school.teaching.agent.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MathOutputValidatorTest {

    @Test
    @DisplayName("validate: null/空字符串返回 null")
    void nullOrBlankReturnsNull() {
        assertNull(MathOutputValidator.validate(null));
        assertNull(MathOutputValidator.validate(""));
        assertNull(MathOutputValidator.validate("   "));
    }

    @Test
    @DisplayName("validate: 纯文本无 LaTeX 返回 null")
    void plainTextReturnsNull() {
        assertNull(MathOutputValidator.validate("这是一段普通的文本内容。"));
        assertNull(MathOutputValidator.validate("hello world"));
    }

    @Test
    @DisplayName("validate: 正确 $...$ 包裹的 LaTeX 返回 null")
    void properlyWrappedLatexReturnsNull() {
        String input = "公式：$\\frac{1}{2}$ 和 $\\sqrt{4}$";
        assertNull(MathOutputValidator.validate(input));
    }

    @Test
    @DisplayName("validate: \\(...\\) 自动修复为 $...$")
    void autoFixParenthesis() {
        String result = MathOutputValidator.validate("公式：\\(\\frac{1}{2}\\)");
        assertNotNull(result);
        assertTrue(result.contains("已自动修复"));
    }

    @Test
    @DisplayName("validate: 裸 \\dfrac 无 $ 包裹 → 警告")
    void bareLatexCommandWarning() {
        String result = MathOutputValidator.validate("答案等于 \\dfrac{1}{2}");
        assertNotNull(result);
        assertTrue(result.contains("LaTeX 命令缺少 $ 包裹"));
    }

    @Test
    @DisplayName("validate: 裸数学模式 f(x)= 无 $ → 警告")
    void bareMathPatternWarning() {
        String result = MathOutputValidator.validate("f(x)=x^2+1");
        assertNotNull(result);
        assertTrue(result.contains("数学表达式缺少 $ 定界符"));
    }

    @Test
    @DisplayName("validate: 多个问题超过3个 → 建议重新提问")
    void tooManyIssuesSuggestsRetry() {
        StringBuilder sb = new StringBuilder();
        sb.append("\\dfrac{1}{2} \\sqrt{3} \\sin(0) \\cos(1) \\tan(2) ");
        sb.append("f(x)=x g(x)=x h(x)=x y=2x+1");
        String result = MathOutputValidator.validate(sb.toString());
        assertNotNull(result);
        assertTrue(result.contains("建议重新提问"));
    }

    @Test
    @DisplayName("validate: 混合正确 LaTeX 和裸命令 → 只警告裸命令")
    void mixedWrappedAndBare() {
        String input = "正确公式：$\\frac{1}{2}$，但是这里裸用了 \\sqrt{4}";
        String result = MathOutputValidator.validate(input);
        assertNotNull(result);
        assertTrue(result.contains("1 个 LaTeX 命令缺少 $ 包裹"));
    }

    @Test
    @DisplayName("validate: $$...$$ 块级数学不触发警告")
    void blockMathNotTriggered() {
        String input = "块级公式：$$\\frac{1}{2} + \\sqrt{4}$$ 正常";
        assertNull(MathOutputValidator.validate(input));
    }
}