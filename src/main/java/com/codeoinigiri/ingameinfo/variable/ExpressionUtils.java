package com.codeoinigiri.ingameinfo.variable;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文字列内の ${...} 形式の式を評価して置換します。
 * (旧 ExpressionUtils から移動)
 */
public class ExpressionUtils {
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{([^{}]+)}");

    public static String evaluateEmbedded(String input, Map<String, String> vars) {
        Matcher matcher = EXPRESSION_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String expr = matcher.group(1).trim();
            String value = ExpressionEvaluator.eval(expr, vars);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
