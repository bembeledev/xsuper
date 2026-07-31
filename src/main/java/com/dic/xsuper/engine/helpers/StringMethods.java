package com.dic.xsuper.engine.helpers;

import com.dic.xsuper.engine.execution.ControlFlow;
import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.execution.XplCallable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Fornece métodos e propriedades para strings no interpretador XSuper,
 * espelhando a funcionalidade dos métodos de string presentes em linguagens modernas.
 */
public class StringMethods {

    // ==========================================
    // PROPRIEDADES (acesso direto)
    // ==========================================

    public static Object getProperty(String text, String propertyName) {
        return switch (propertyName) {
            case "length", "size" -> (long) text.length();
            case "isEmpty", "empty" -> text.isEmpty();
            default -> throw new RuntimeException(
                    "Propriedade '" + propertyName + "' não encontrada em String.");
        };
    }

    // ==========================================
    // MÉTODOS (retornam XplCallable)
    // ==========================================

    public static XplCallable getMethod(String text, String methodName) {
        switch (methodName) {

            // ---------- Transformação de caixa ----------
            case "toUpperCase":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        return text.toUpperCase();
                    }
                };

            case "toLowerCase":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        return text.toLowerCase();
                    }
                };

            // ---------- Informação básica ----------
            case "length":
            case "size":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        return (long) text.length();
                    }
                };

            case "isEmpty":
            case "empty":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        return text.isEmpty();
                    }
                };

            // ---------- Acesso a caracteres e substrings ----------
            case "charAt":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        checkArgCount(args, 1, "charAt");
                        int index = (int) getIntArg(args, 0, 0);
                        if (index < 0 || index >= text.length()) {
                            throw new RuntimeException("Índice fora dos limites: " + index);
                        }
                        return String.valueOf(text.charAt(index));
                    }
                };

            case "at":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        checkArgCount(args, 1, "at");
                        int index = resolveIndex(getIntArg(args, 0, 0), text.length());
                        if (index < 0 || index >= text.length()) {
                            return null; // comportamento consistente com Array.at
                        }
                        return String.valueOf(text.charAt(index));
                    }
                };

            case "substring":
            case "substr":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        if (args.isEmpty()) {
                            throw new RuntimeException("substring/substr requer pelo menos 1 argumento.");
                        }
                        int start = resolveIndex(getIntArg(args, 0, 0), text.length());
                        int end;
                        if (args.size() >= 2) {
                            end = resolveIndex(getIntArg(args, 1, text.length()), text.length());
                        } else {
                            end = text.length();
                        }
                        if (start < 0) start = 0;
                        if (end > text.length()) end = text.length();
                        if (start > end) {
                            int tmp = start;
                            start = end;
                            end = tmp;
                        }
                        return text.substring(start, end);
                    }
                };

            case "slice":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        if (args.isEmpty()) {
                            throw new RuntimeException("slice requer pelo menos 1 argumento.");
                        }
                        int start = resolveIndex(getIntArg(args, 0, 0), text.length());
                        int end;
                        if (args.size() >= 2) {
                            end = resolveIndex(getIntArg(args, 1, text.length()), text.length());
                        } else {
                            end = text.length();
                        }
                        if (start < 0) start = 0;
                        if (end > text.length()) end = text.length();
                        if (start >= end) return "";
                        return text.substring(start, end);
                    }
                };

            // ---------- Pesquisa e localização ----------
            case "indexOf":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        if (args.isEmpty()) {
                            throw new RuntimeException("indexOf requer pelo menos 1 argumento (substring a buscar).");
                        }
                        String search = args.get(0).toString();
                        int fromIndex = (int) getIntArg(args, 1, 0);
                        if (fromIndex < 0) fromIndex = 0;
                        int idx = text.indexOf(search, fromIndex);
                        return (long) idx;
                    }
                };

            case "lastIndexOf":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        if (args.isEmpty()) {
                            throw new RuntimeException("lastIndexOf requer pelo menos 1 argumento (substring a buscar).");
                        }
                        String search = args.get(0).toString();
                        int fromIndex = (int) getIntArg(args, 1, text.length() - 1);
                        if (fromIndex >= text.length()) fromIndex = text.length() - 1;
                        int idx = text.lastIndexOf(search, fromIndex);
                        return (long) idx;
                    }
                };

            case "includes", "contains":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        checkArgCount(args, 1, "contains");
                        String search = args.getFirst().toString();
                        return text.contains(search);
                    }
                };

            case "startsWith":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        if (args.isEmpty()) {
                            throw new RuntimeException("startsWith requer pelo menos 1 argumento (prefixo).");
                        }
                        String prefix = args.get(0).toString();
                        int offset = (int) getIntArg(args, 1, 0);
                        return text.startsWith(prefix, offset);
                    }
                };

            case "endsWith":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        checkArgCount(args, 1, "endsWith");
                        String suffix = args.get(0).toString();
                        return text.endsWith(suffix);
                    }
                };

            // ---------- Modificação e formatação ----------
            case "trim":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        return text.trim();
                    }
                };

            case "trimStart":
            case "trimLeft":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        int i = 0;
                        while (i < text.length() && Character.isWhitespace(text.charAt(i))) i++;
                        return text.substring(i);
                    }
                };

            case "trimEnd":
            case "trimRight":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        int i = text.length() - 1;
                        while (i >= 0 && Character.isWhitespace(text.charAt(i))) i--;
                        return text.substring(0, i + 1);
                    }
                };

            case "repeat":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        checkArgCount(args, 1, "repeat");
                        int count = (int) getIntArg(args, 0, 0);
                        if (count < 0) throw new RuntimeException("Número de repetições não pode ser negativo.");
                        return text.repeat(count);
                    }
                };

            case "padStart":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        if (args.isEmpty()) {
                            throw new RuntimeException("padStart requer pelo menos 1 argumento (comprimento alvo).");
                        }
                        int targetLength = (int) getIntArg(args, 0, 0);
                        String padString = args.size() > 1 ? args.get(1).toString() : " ";
                        if (targetLength <= text.length()) return text;
                        int padLen = targetLength - text.length();
                        StringBuilder sb = new StringBuilder();
                        while (sb.length() < padLen) {
                            sb.append(padString);
                        }
                        return sb.substring(0, padLen) + text;
                    }
                };

            case "padEnd":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        if (args.isEmpty()) {
                            throw new RuntimeException("padEnd requer pelo menos 1 argumento (comprimento alvo).");
                        }
                        int targetLength = (int) getIntArg(args, 0, 0);
                        String padString = args.size() > 1 ? args.get(1).toString() : " ";
                        if (targetLength <= text.length()) return text;
                        int padLen = targetLength - text.length();
                        StringBuilder sb = new StringBuilder(text);
                        while (sb.length() < targetLength) {
                            sb.append(padString);
                        }
                        return sb.substring(0, targetLength);
                    }
                };

            case "replace":
                return new XplCallable() {
                    @Override public int arity() { return 2; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        checkArgCount(args, 2, "replace");
                        String target = args.get(0).toString();
                        String replacement = args.get(1).toString();
                        return text.replace(target, replacement);
                    }
                };

            case "replaceAll":
                return new XplCallable() {
                    @Override public int arity() { return 2; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        checkArgCount(args, 2, "replaceAll");
                        String regex = args.get(0).toString();
                        String replacement = args.get(1).toString();
                        return text.replaceAll(regex, replacement);
                    }
                };

            case "replaceFirst":
                return new XplCallable() {
                    @Override public int arity() { return 2; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        checkArgCount(args, 2, "replaceFirst");
                        String regex = args.get(0).toString();
                        String replacement = args.get(1).toString();
                        return text.replaceFirst(regex, replacement);
                    }
                };

            // ---------- Divisão e junção ----------
            case "split":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        if (args.isEmpty()) {
                            throw new RuntimeException("split requer pelo menos 1 argumento (separador).");
                        }
                        String separator = args.get(0).toString();
                        int limit = (int) getIntArg(args, 1, 0);

                        String[] parts;
                        if (separator.isEmpty()) {
                            // Divide em caracteres individuais
                            parts = text.split("(?!^)");
                        } else {
                            if (limit > 0) {
                                parts = text.split(Pattern.quote(separator), limit);
                            } else {
                                parts = text.split(Pattern.quote(separator));
                            }
                        }
                        List<Object> list = new ArrayList<>();
                        for (String p : parts) {
                            list.add(p);
                        }
                        return list;
                    }
                };

            // ---------- Conversão para outros tipos ----------
            case "toArray":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> chars = new ArrayList<>();
                        for (char c : text.toCharArray()) {
                            chars.add(String.valueOf(c));
                        }
                        return chars;
                    }
                };


            case "fromJson":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter intp, java.util.List<Expr.CallArg> args) {
                        try {
                            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(text, Object.class);
                        } catch (Exception e) {
                            throw new ControlFlow.RuntimeError(null, "Erro ao fazer parse de JSON: " + e.getMessage());
                        }
                    }
                };

            case "toNumber":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        try {
                            return Double.parseDouble(text);
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    }
                };

            case "toBoolean":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        String lower = text.toLowerCase();
                        return lower.equals("true") || lower.equals("1") || lower.equals("yes");
                    }
                };

            // ---------- Métodos de inspeção ----------
            case "charCodeAt":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        checkArgCount(args, 1, "charCodeAt");
                        int index = (int) getIntArg(args, 0, 0);
                        if (index < 0 || index >= text.length()) {
                            throw new RuntimeException("Índice fora dos limites.");
                        }
                        return (long) text.charAt(index);
                    }
                };

            // ---------- Métodos para inverter ----------
            case "reverse":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        return new StringBuilder(text).reverse().toString();
                    }
                };

            // ---------- Validação e Regex ----------
            case "matches":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        checkArgCount(args, 1, "matches");
                        String regex = args.getFirst().toString();
                        return Pattern.matches(regex, text);
                    }
                };

            // ---------- Inspetores de Tipo de Caracteres ----------
            case "isNumeric":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        return text.matches("-?\\d+(\\.\\d+)?");
                    }
                };

            case "isAlpha":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        return text.matches("[a-zA-Z]+");
                    }
                };

            case "isAlphanumeric":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        return text.matches("[a-zA-Z0-9]+");
                    }
                };

            case "isBlank":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        return text.isBlank();
                    }
                };

            // ---------- Hashing e Segurança (Alinhado com o SDM) ----------
            case "toSHA256":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        try {
                            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                            byte[] hash = md.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            StringBuilder hexString = new StringBuilder();
                            for (byte b : hash) {
                                String hex = Integer.toHexString(0xff & b);
                                if (hex.length() == 1) hexString.append('0');
                                hexString.append(hex);
                            }
                            return hexString.toString();
                        } catch (Exception e) {
                            throw new ControlFlow.RuntimeError(null, "Erro ao gerar SHA-256: " + e.getMessage());
                        }
                    }
                };

            case "toBase64":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        return java.util.Base64.getEncoder().encodeToString(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    }
                };

            case "fromBase64":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        try {
                            byte[] decoded = java.util.Base64.getDecoder().decode(text);
                            return new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            throw new ControlFlow.RuntimeError(null, "Erro ao descodificar Base64: " + e.getMessage());
                        }
                    }
                };

            default:
                throw new RuntimeException(
                        "Método '" + methodName + "' não implementado em String.");
        }
    }

    // ==========================================
    // FUNÇÕES AUXILIARES INTERNAS
    // ==========================================

    private static int resolveIndex(long idx, int len) {
        if (idx < 0) {
            return (int) Math.max(0, len + idx);
        } else {
            return (int) Math.min(idx, len);
        }
    }

    private static long getIntArg(List<Object> args, int index, long defaultValue) {
        if (index >= args.size()) return defaultValue;
        Object val = args.get(index);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return defaultValue;
    }

    private static void checkArgCount(List<Object> args, int expected, String methodName) {
        if (args.size() < expected) {
            throw new RuntimeException("Método '" + methodName + "' requer " + expected +
                    " argumento(s), mas recebeu " + args.size());
        }
    }
}