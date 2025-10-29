package com.codeoinigiri.ingameinfo.variable;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文字列内の ${...} 形式の式を評価して置換します。
 * Single Responsibility Principle: 式の検出と置換のみに責任を持つ
 * 実際の評価はExpressionEvaluatorに委譲
 */
public class ExpressionUtils {
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{([^{}]+)}");

    private ExpressionUtils() {
        // ユーティリティクラスのため、インスタンス化を防止
    }

    /**
     * 文字列内の ${...} 式を評価して置換
     *
     * @param input 入力文字列
     * @param vars 変数マップ
     * @return 評価後の文字列
     */
    public static String evaluateEmbedded(String input, Map<String, String> vars) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        Matcher matcher = EXPRESSION_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String expr = matcher.group(1).trim();
            String value = ExpressionEvaluator.eval(expr, vars);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 式パターンを取得（テスト用）
     */
    static Pattern getExpressionPattern() {
        return EXPRESSION_PATTERN;
    }
}
