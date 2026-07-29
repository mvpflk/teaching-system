package com.school.teaching.geometry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SimpleExprEvalTest {

    @Test
    @DisplayName("计算 x^2")
    void square() {
        assertEquals(9.0, SimpleExprEval.eval("x^2", 3), 1e-9);
        assertEquals(0.0, SimpleExprEval.eval("x^2", 0), 1e-9);
    }

    @Test
    @DisplayName("计算 2*x + 3")
    void linear() {
        assertEquals(7.0, SimpleExprEval.eval("2*x+3", 2), 1e-9);
    }

    @Test
    @DisplayName("计算 sin(x)")
    void sin() {
        assertEquals(Math.sin(Math.PI / 2), SimpleExprEval.eval("sin(x)", Math.PI / 2), 1e-9);
    }

    @Test
    @DisplayName("计算 cos(x)")
    void cos() {
        assertEquals(Math.cos(0), SimpleExprEval.eval("cos(x)", 0), 1e-9);
    }

    @Test
    @DisplayName("计算 sqrt(x)")
    void sqrt() {
        assertEquals(3.0, SimpleExprEval.eval("sqrt(x)", 9), 1e-9);
    }

    @Test
    @DisplayName("计算 abs(x)")
    void abs() {
        assertEquals(5.0, SimpleExprEval.eval("abs(x)", -5), 1e-9);
    }

    @Test
    @DisplayName("计算复杂表达式")
    void complex() {
        double expected = Math.pow(2, 3) + Math.sin(2) - Math.abs(-5);
        assertEquals(expected, SimpleExprEval.eval("x^3+sin(x)-abs(-5)", 2), 1e-9);
    }

    @Test
    @DisplayName("空表达式返回 NaN")
    void empty() {
        assertTrue(Double.isNaN(SimpleExprEval.eval("", 1)));
        assertTrue(Double.isNaN(SimpleExprEval.eval(null, 1)));
    }

    @Test
    @DisplayName("无 x 的常量表达式")
    void constant() {
        assertEquals(42.0, SimpleExprEval.eval("42", 0), 1e-9);
    }
}
