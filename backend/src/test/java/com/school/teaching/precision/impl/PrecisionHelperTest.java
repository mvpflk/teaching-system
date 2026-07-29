package com.school.teaching.precision.impl;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class PrecisionHelperTest {

    /* ───────── matchFillInAnswer ───────── */

    @Test
    @DisplayName("√ → 正确, SYNONYM_MAP中'对'组, 返回true")
    void matchFillInAnswer_checkMark_shouldMatchCorrect() {
        assertTrue(PrecisionHelper.matchFillInAnswer("√", "正确"));
    }

    @Test
    @DisplayName("不对 → 错误, SYNONYM_MAP已收录'不对', 返回true")
    void matchFillInAnswer_budui_shouldMatch() {
        assertTrue(PrecisionHelper.matchFillInAnswer("不对", "错误"),
            "'不对'已收录在SYNONYM_MAP的'错'组中, 应匹配'错误'");
    }

    @Test
    @DisplayName("平行 → 平行, 同义词表直接匹配, 返回true")
    void matchFillInAnswer_parallel_shouldMatch() {
        assertTrue(PrecisionHelper.matchFillInAnswer("平行", "平行"));
    }

    @Test
    @DisplayName("垂直 → 垂直, 同义词表直接匹配, 返回true")
    void matchFillInAnswer_perpendicular_shouldMatch() {
        assertTrue(PrecisionHelper.matchFillInAnswer("垂直", "垂直"));
    }

    @Test
    @DisplayName("√+空格 → sanitize后匹配正确, 返回true")
    void matchFillInAnswer_checkMarkWithSpace_shouldMatch() {
        assertTrue(PrecisionHelper.matchFillInAnswer("√  ", "正确"));
    }

    @Test
    @DisplayName("多答案expected: '正确;√' 任一匹配即true")
    void matchFillInAnswer_multiExpected_oneMatches() {
        assertTrue(PrecisionHelper.matchFillInAnswer("是", "正确;√"));
    }

    @Test
    @DisplayName("expected按/分割: '正确/对/是', '对'匹配")
    void matchFillInAnswer_multiExpectedWithSlash() {
        assertTrue(PrecisionHelper.matchFillInAnswer("对", "正确/对/是"));
    }

    @Test
    @DisplayName("不相关 → 不在同义词中, 返回false")
    void matchFillInAnswer_unrelated_shouldReturnFalse() {
        assertFalse(PrecisionHelper.matchFillInAnswer("不相关", "正确"));
    }

    @Test
    @DisplayName("studentAnswer为null → 返回false")
    void matchFillInAnswer_nullStudent_shouldReturnFalse() {
        assertFalse(PrecisionHelper.matchFillInAnswer(null, "正确"));
    }

    @Test
    @DisplayName("studentAnswer为空字符串 → 返回false")
    void matchFillInAnswer_emptyStudent_shouldReturnFalse() {
        assertFalse(PrecisionHelper.matchFillInAnswer("", "正确"));
    }

    @Test
    @DisplayName("studentAnswer为纯空格 → 返回false")
    void matchFillInAnswer_blankStudent_shouldReturnFalse() {
        assertFalse(PrecisionHelper.matchFillInAnswer("   ", "正确"));
    }

    @Test
    @DisplayName("expected为null → 返回false")
    void matchFillInAnswer_nullExpected_shouldReturnFalse() {
        assertFalse(PrecisionHelper.matchFillInAnswer("正确", null));
    }

    @Test
    @DisplayName("expected为空字符串 → 返回false")
    void matchFillInAnswer_emptyExpected_shouldReturnFalse() {
        assertFalse(PrecisionHelper.matchFillInAnswer("正确", ""));
    }

    // ═══════════ normalizeTfAnswer ═══════════

    @Test
    @DisplayName("T→A, F→B, True/False→A/B, 正确/错误→A/B, √/×→A/B")
    void normalizeTfAnswer_variants_shouldAllWork() {
        assertAll(
            () -> assertEquals("A", PrecisionHelper.normalizeTfAnswer("T")),
            () -> assertEquals("A", PrecisionHelper.normalizeTfAnswer("True")),
            () -> assertEquals("A", PrecisionHelper.normalizeTfAnswer("正确")),
            () -> assertEquals("A", PrecisionHelper.normalizeTfAnswer("√")),
            () -> assertEquals("B", PrecisionHelper.normalizeTfAnswer("F")),
            () -> assertEquals("B", PrecisionHelper.normalizeTfAnswer("False")),
            () -> assertEquals("B", PrecisionHelper.normalizeTfAnswer("错误")),
            () -> assertEquals("B", PrecisionHelper.normalizeTfAnswer("×")),
            () -> assertEquals("C", PrecisionHelper.normalizeTfAnswer("C")),
            () -> assertEquals("D", PrecisionHelper.normalizeTfAnswer("D"))
        );
    }

    @Test
    @DisplayName("normalizeTfAnswer null/空→原样返回")
    void normalizeTfAnswer_nullOrEmpty_shouldPassThrough() {
        assertEquals("", PrecisionHelper.normalizeTfAnswer(""));
        assertEquals("AB", PrecisionHelper.normalizeTfAnswer("AB"));
        assertEquals("你好", PrecisionHelper.normalizeTfAnswer("你好"));
    }

    // ═══════════ estimateScore 预估分映射 ═══════════

    @Test
    @DisplayName("数学预估分: 诊断80分 → 预估110分")
    void estimateScore_math80_shouldReturn110() {
        assertEquals(110, PrecisionHelper.estimateScore("数学[职高]", 80));
    }

    @Test
    @DisplayName("数学预估分: 诊断90分 → 预估130分")
    void estimateScore_math90_shouldReturn130() {
        assertEquals(130, PrecisionHelper.estimateScore("数学[职高]", 90));
    }

    @Test
    @DisplayName("数学预估分: 诊断100分 → 预估135分")
    void estimateScore_math100_shouldReturn135() {
        assertEquals(135, PrecisionHelper.estimateScore("数学[职高]", 100));
    }

    @Test
    @DisplayName("数学预估分: 诊断60分 → 预估75分")
    void estimateScore_math60_shouldReturn75() {
        assertEquals(75, PrecisionHelper.estimateScore("数学[职高]", 60));
    }

    @Test
    @DisplayName("数学预估分: 诊断40分 → 预估40分")
    void estimateScore_math40_shouldReturn40() {
        assertEquals(40, PrecisionHelper.estimateScore("数学[职高]", 40));
    }

    @Test
    @DisplayName("英语预估分: 诊断85分 → 预估80分")
    void estimateScore_english85_shouldReturn80() {
        assertEquals(80, PrecisionHelper.estimateScore("英语[职高]", 85));
    }

    @Test
    @DisplayName("英语预估分: 诊断70分 → 预估60分")
    void estimateScore_english70_shouldReturn60() {
        assertEquals(60, PrecisionHelper.estimateScore("英语[职高]", 70));
    }

    @Test
    @DisplayName("其他学科预估分: 原样返回")
    void estimateScore_otherSubject_shouldReturnSame() {
        assertEquals(50, PrecisionHelper.estimateScore("物理", 50));
    }

    // ═══════════ getProfileLock 锁系统 ═══════════

    @Test
    @DisplayName("getProfileLock: 同一学生返回同一锁实例")
    void getProfileLock_sameStudent_shouldReturnSameLock() {
        PrecisionHelper helper = new PrecisionHelper();
        Object lock1 = helper.getProfileLock(1001L);
        Object lock2 = helper.getProfileLock(1001L);
        assertSame(lock1, lock2, "同一学生的锁实例应该相同");
    }

    @Test
    @DisplayName("getProfileLock: 不同学生返回不同锁实例")
    void getProfileLock_differentStudents_shouldReturnDifferentLocks() {
        PrecisionHelper helper = new PrecisionHelper();
        Object lock1 = helper.getProfileLock(1001L);
        Object lock2 = helper.getProfileLock(1002L);
        assertNotSame(lock1, lock2, "不同学生的锁实例应该不同");
    }
}
