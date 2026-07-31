package com.dic.xsuper.dom.reactivity;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XplExpressionEvaluator {

    // ⭐ Regex para Atributos: Captura { expressao } (Ignora se for duplo {{ )
    private static final Pattern ATTR_PATTERN = Pattern.compile("(?<!\\{)\\{([^}]+)\\}(?!\\})");

    // ⭐ Regex para Texto Livre: Captura {{ expressao }}
    private static final Pattern TEXT_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    public static String evaluateAttribute(String text, Map<String, Object> context) {
        if (text == null || !text.contains("{")) return text;
        return processPattern(ATTR_PATTERN, text, context);
    }

    public static String evaluateText(String text, Map<String, Object> context) {
        if (text == null || !text.contains("{{")) return text;
        return processPattern(TEXT_PATTERN, text, context);
    }

    // O "Cérebro" unificado que resolve a matemática e os ternários de ambos
    private static String processPattern(Pattern pattern, String text, Map<String, Object> context) {
        Matcher m = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            String expression = m.group(1).trim(); // Extrai a expressão limpa

            // É um operador Ternário? (ex: isNotificationVisible ? 'eu' : 'ele')
            if (expression.contains("?")) {
                String[] parts = expression.split("\\?", 2);
                String condition = parts[0].trim();

                String[] branches = parts[1].split(":", 2);
                String trueBranch = branches[0].trim().replace("\"", "").replace("'", "");
                String falseBranch = branches.length > 1 ? branches[1].trim().replace("\"", "").replace("'", "") : "";

                boolean isTrue = evaluateCondition(condition, context);
                // Usa quoteReplacement para evitar erros se o texto contiver '$' ou '\'
                m.appendReplacement(sb, Matcher.quoteReplacement(isTrue ? trueBranch : falseBranch));
            }
            // É uma variável simples? (ex: index)
            else {
                Object val = context.get(expression);
                m.appendReplacement(sb, Matcher.quoteReplacement(val != null ? String.valueOf(val) : ""));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static boolean evaluateCondition(String condition, Map<String, Object> context) {
        boolean negate = condition.startsWith("!");
        String cleanCond = negate ? condition.substring(1).trim() : condition;

        Object val = context.get(cleanCond);
        boolean isTruthy = false;

        if (val instanceof Boolean) {
            isTruthy = (Boolean) val;
        } else if (val != null && !val.toString().equalsIgnoreCase("false")
                && !val.toString().equals("0") && !val.toString().isEmpty()) {
            isTruthy = true; // Valores Truthy do W3C
        }

        return negate ? !isTruthy : isTruthy;
    }
}