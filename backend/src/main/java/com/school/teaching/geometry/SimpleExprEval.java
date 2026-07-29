package com.school.teaching.geometry;

import java.util.*;

public class SimpleExprEval {

    private static final Set<String> FUNCS = Set.of(
        "sin", "cos", "tan", "asin", "acos", "atan",
        "sqrt", "abs", "log", "log10", "ceil", "floor"
    );

    public static double eval(String expr, double x) {
        if (expr == null || expr.isBlank()) return Double.NaN;
        List<String> tokens = tokenize(expr);
        List<String> rpn = shuntingYard(tokens);
        return evaluateRpn(rpn, x);
    }

    private static List<String> tokenize(String expr) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if ("+-*/^()".indexOf(c) >= 0) {
                tokens.add(String.valueOf(c));
                i++;
            } else if (Character.isDigit(c) || c == '.') {
                int start = i;
                while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) i++;
                tokens.add(expr.substring(start, i));
            } else if (Character.isLetter(c)) {
                int start = i;
                while (i < expr.length() && Character.isLetterOrDigit(expr.charAt(i))) i++;
                String name = expr.substring(start, i);
                if ("x".equals(name)) {
                    tokens.add("x");
                } else if (FUNCS.contains(name)) {
                    tokens.add(name);
                } else {
                    tokens.add(name);
                }
            } else {
                i++;
            }
        }
        return tokens;
    }

    private static List<String> shuntingYard(List<String> tokens) {
        Map<String, Integer> prec = new HashMap<>();
        prec.put("+", 2); prec.put("-", 2); prec.put("*", 3);
        prec.put("/", 3); prec.put("^", 4); prec.put("u-", 5);

        List<String> output = new ArrayList<>();
        Deque<String> stack = new ArrayDeque<>();
        boolean expectUnary = true;

        for (String t : tokens) {
            if (isNumber(t) || "x".equals(t)) {
                output.add(t);
                expectUnary = false;
            } else if ("(".equals(t)) {
                stack.push(t);
                expectUnary = true;
            } else if (")".equals(t)) {
                while (!stack.isEmpty() && !"(".equals(stack.peek())) {
                    output.add(stack.pop());
                }
                stack.pop();
                if (!stack.isEmpty() && FUNCS.contains(stack.peek())) {
                    output.add(stack.pop());
                }
                expectUnary = false;
            } else if (FUNCS.contains(t)) {
                stack.push(t);
                expectUnary = false;
            } else if (isOperator(t)) {
                String op = t;
                if (expectUnary && ("-".equals(t) || "+".equals(t))) {
                    op = "-".equals(t) ? "u-" : "u+";
                }
                while (!stack.isEmpty() && isOperator(stack.peek())) {
                    int p1 = prec.getOrDefault(op, 0);
                    int p2 = prec.getOrDefault(stack.peek(), 0);
                    if (p2 > p1 || (p2 == p1 && !"^".equals(op))) {
                        output.add(stack.pop());
                    } else break;
                }
                stack.push(op);
                expectUnary = true;
            }
        }
        while (!stack.isEmpty()) output.add(stack.pop());
        return output;
    }

    private static double evaluateRpn(List<String> rpn, double x) {
        Deque<Double> stack = new ArrayDeque<>();
        for (String t : rpn) {
            if ("x".equals(t)) {
                stack.push(x);
            } else if (isNumber(t)) {
                stack.push(Double.parseDouble(t));
            } else if (FUNCS.contains(t)) {
                double a = stack.pop();
                switch (t) {
                    case "sin" -> stack.push(Math.sin(a));
                    case "cos" -> stack.push(Math.cos(a));
                    case "tan" -> stack.push(Math.tan(a));
                    case "asin" -> stack.push(Math.asin(a));
                    case "acos" -> stack.push(Math.acos(a));
                    case "atan" -> stack.push(Math.atan(a));
                    case "sqrt" -> stack.push(Math.sqrt(a));
                    case "abs" -> stack.push(Math.abs(a));
                    case "log" -> stack.push(Math.log(a));
                    case "log10" -> stack.push(Math.log10(a));
                    case "ceil" -> stack.push(Math.ceil(a));
                    case "floor" -> stack.push(Math.floor(a));
                }
            } else if (isOperator(t)) {
                double b = stack.pop();
                double a = "u-".equals(t) || "u+".equals(t) ? 0 : stack.pop();
                switch (t) {
                    case "+" -> stack.push(a + b);
                    case "-" -> stack.push(a - b);
                    case "*" -> stack.push(a * b);
                    case "/" -> stack.push(a / b);
                    case "^" -> stack.push(Math.pow(a, b));
                    case "u-" -> stack.push(-b);
                    case "u+" -> stack.push(b);
                }
            }
        }
        return stack.isEmpty() ? Double.NaN : stack.pop();
    }

    private static boolean isNumber(String s) {
        try { Double.parseDouble(s); return true; } catch (NumberFormatException e) { return false; }
    }

    private static boolean isOperator(String s) {
        return "+-*/^".contains(s) || "u-".equals(s) || "u+".equals(s);
    }
}
