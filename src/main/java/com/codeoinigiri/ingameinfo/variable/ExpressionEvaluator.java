package com.codeoinigiri.ingameinfo.variable;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * ScriptEngineに依存せず、簡単な数式と条件式を評価する。
 * Single Responsibility Principle: 式の評価のみに責任を持つ
 * Open/Closed Principle: 新しい関数の追加が容易
 */
public class ExpressionEvaluator {
    // 1引数の数学関数
    private static final Map<String, Function<Double, Double>> mathFunctions = new HashMap<>();
    // 2引数の数学関数
    private static final Map<String, BiFunction<Double, Double, Double>> mathFunctions2 = new HashMap<>();

    // 静的初期化ブロック: 利用可能な数学関数を登録
    static {
        // 1引数関数
        mathFunctions.put("abs", Math::abs);
        mathFunctions.put("round", d -> (double) Math.round(d));
        mathFunctions.put("floor", Math::floor);
        mathFunctions.put("ceil", Math::ceil);
        mathFunctions.put("sqrt", Math::sqrt);
        mathFunctions.put("sin", Math::sin);
        mathFunctions.put("cos", Math::cos);
        mathFunctions.put("tan", Math::tan);

        // 2引数関数
        mathFunctions2.put("min", Math::min);
        mathFunctions2.put("max", Math::max);
        mathFunctions2.put("pow", Math::pow);
    }

    private ExpressionEvaluator() {
        // ユーティリティクラスのため、インスタンス化を防止
    }

    /**
     * 式を評価して文字列として返す
     *
     * @param expr 評価する式
     * @param vars 変数マップ
     * @return 評価結果の文字列
     */
    public static String eval(String expr, Map<String, String> vars) {
        if (expr == null || expr.isEmpty()) return "";

        expr = substituteVariables(expr, vars);

        try {
            Object val = new Parser(expr).parseTernary();
            return val.toString();
        } catch (Exception e) {
            return "?" + expr + "?";
        }
    }

    public static String getHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- InGameInfo Operators & Functions ---\n");
        sb.append("Operators: +, -, *, /, <, >, <=, >=, ==, !=, ? :\n");
        sb.append("Ternary: (condition ? value_if_true : value_if_false)\n");
        sb.append("Functions (1 arg): ").append(String.join(", ", mathFunctions.keySet())).append("\n");
        sb.append("Functions (2 arg): ").append(String.join(", ", mathFunctions2.keySet())).append("\n");
        sb.append("Special Functions: format(value, \"pattern\")");
        return sb.toString();
    }

    private static String substituteVariables(String expr, Map<String, String> vars) {
        for (var e : vars.entrySet()) {
            String value = e.getValue();
            if (value.matches("-?\\d+(\\.\\d+)?")) {
                expr = expr.replaceAll("\\b" + e.getKey() + "\\b", value);
            } else {
                expr = expr.replaceAll("\\b" + e.getKey() + "\\b", "\"" + value + "\"");
            }
        }
        return expr;
    }

    private static class Parser {
        private final String str;
        private int pos = -1, ch;

        Parser(String s) {
            this.str = s;
            nextChar();
        }

        void nextChar() {
            ch = (++pos < str.length()) ? str.charAt(pos) : -1;
        }

        boolean eat(int charToEat) {
            while (ch == ' ') nextChar();
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        Object parseTernary() {
            Object condition = parseComparison();
            if (eat('?')) {
                Object trueExpr = parseTernary();
                eat(':');
                Object falseExpr = parseTernary();
                boolean cond = toBoolean(condition);
                return cond ? trueExpr : falseExpr;
            }
            return condition;
        }

        Object parseComparison() {
            Object left = parseExpression();
            while (true) {
                if (eat('<')) {
                    if (eat('=')) left = (toDouble(left) <= toDouble(parseExpression()));
                    else left = (toDouble(left) < toDouble(parseExpression()));
                } else if (eat('>')) {
                    if (eat('=')) left = (toDouble(left) >= toDouble(parseExpression()));
                    else left = (toDouble(left) > toDouble(parseExpression()));
                } else if (eat('=')) {
                    eat('=');
                    left = left.toString().equals(parseExpression().toString());
                } else if (eat('!')) {
                    eat('=');
                    left = !left.toString().equals(parseExpression().toString());
                } else return left;
            }
        }

        Object parseExpression() {
            Object x = parseTerm();
            for (;;) {
                if (eat('+')) x = toDouble(x) + toDouble(parseTerm());
                else if (eat('-')) x = toDouble(x) - toDouble(parseTerm());
                else return x;
            }
        }

        Object parseTerm() {
            Object x = parseFactor();
            for (;;) {
                if (eat('*')) x = toDouble(x) * toDouble(parseFactor());
                else if (eat('/')) x = toDouble(x) / toDouble(parseFactor());
                else return x;
            }
        }

        Object parseFactor() {
            if (eat('+')) return parseFactor();
            if (eat('-')) return -toDouble(parseFactor());

            Object x;
            int startPos = this.pos;

            if (eat('(')) {
                x = parseTernary();
                eat(')');
            } else if (isLetter(ch)) {
                while (isLetterOrDigit(ch)) nextChar();
                String func = str.substring(startPos, pos);

                if (eat('(')) {
                    Object arg1 = parseTernary();
                    if (eat(',')) {
                        Object arg2 = parseTernary();
                        eat(')');
                        if (mathFunctions2.containsKey(func)) {
                            x = mathFunctions2.get(func).apply(toDouble(arg1), toDouble(arg2));
                        } else if (func.equals("format")) {
                            x = doFormat(arg1, arg2);
                        } else {
                            throw new RuntimeException("Unknown 2-arg function: " + func);
                        }
                    } else {
                        eat(')');
                        if (mathFunctions.containsKey(func)) {
                            x = mathFunctions.get(func).apply(toDouble(arg1));
                        } else {
                            throw new RuntimeException("Unknown function: " + func);
                        }
                    }
                } else {
                    throw new RuntimeException("Unexpected identifier: " + func);
                }
            } else if (ch == '"') {
                nextChar();
                int start = pos;
                while (ch != '"' && ch != -1) nextChar();
                x = str.substring(start, pos);
                eat('"');
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                x = Double.parseDouble(str.substring(startPos, pos));
            } else {
                throw new RuntimeException("Unexpected char: " + (char) ch);
            }

            return x;
        }

        private static Object doFormat(Object value, Object formatString) {
            String pattern = formatString.toString();
            DecimalFormat df = new DecimalFormat(pattern);
            return df.format(toDouble(value));
        }

        static boolean isLetter(int c) {
            return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
        }

        static boolean isLetterOrDigit(int c) {
            return isLetter(c) || (c >= '0' && c <= '9');
        }

        static double toDouble(Object o) {
            if (o instanceof Number n) return n.doubleValue();
            try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0; }
        }

        static boolean toBoolean(Object o) {
            if (o instanceof Boolean b) return b;
            String s = o.toString().toLowerCase();
            if (s.equals("true")) return true;
            if (s.equals("false")) return false;
            return toDouble(o) != 0;
        }
    }
}
