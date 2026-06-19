package com.dic.xsuper.lang.helpers;

import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;

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

    /**
     * Retorna o valor de uma propriedade da string (ex: length, isEmpty).
     */
    public static Object getProperty(String text, String propertyName) {
        switch (propertyName) {
            case "length":
            case "size":
                return (long) text.length();
            case "isEmpty":
            case "empty":
                return text.isEmpty();
            default:
                throw new RuntimeException(
                        "Propriedade '" + propertyName + "' não encontrada em String.");
        }
    }

    // ==========================================
    // MÉTODOS (retornam XplCallable)
    // ==========================================

    /**
     * Retorna um XplCallable para o método solicitado, operando sobre a string fornecida.
     * O callable recebe os argumentos como List<Object> e pode ter aridade variável (-1).
     */
    public static XplCallable getMethod(String text, String methodName) {
        switch (methodName) {

            // ---------- Transformação de caixa ----------
            case "toUpperCase":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        return text.toUpperCase();
                    }
                };

            case "toLowerCase":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        return text.toLowerCase();
                    }
                };

            // ---------- Informação básica ----------
            case "length":
            case "size":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        return (long) text.length();
                    }
                };

            case "isEmpty":
            case "empty":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        return text.isEmpty();
                    }
                };

            // ---------- Acesso a caracteres e substrings ----------
            case "charAt":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        int index = (int) getIntArg(arguments, 0, 0);
                        if (index < 0 || index >= text.length()) {
                            throw new RuntimeException("Índice fora dos limites: " + index);
                        }
                        return String.valueOf(text.charAt(index));
                    }
                };

            case "at":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        int index = resolveIndex(getIntArg(arguments, 0, 0), text.length());
                        if (index < 0 || index >= text.length()) {
                            return null; // ou poderia lançar erro, mas seguimos o comportamento do Rust
                        }
                        return String.valueOf(text.charAt(index));
                    }
                };

            case "substring":
            case "substr":
                return new XplCallable() {
                    @Override public int arity() { return -1; } // 1 ou 2 args
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        int start = resolveIndex(getIntArg(arguments, 0, 0), text.length());
                        int end;
                        if (arguments.size() >= 2) {
                            end = resolveIndex(getIntArg(arguments, 1, text.length()), text.length());
                        } else {
                            end = text.length();
                        }
                        if (start < 0) start = 0;
                        if (end > text.length()) end = text.length();
                        if (start > end) {
                            // troca para consistência com JavaScript
                            int tmp = start;
                            start = end;
                            end = tmp;
                        }
                        return text.substring(start, end);
                    }
                };

            case "slice":
                return new XplCallable() {
                    @Override public int arity() { return -1; } // 1 ou 2 args
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        int start = resolveIndex(getIntArg(arguments, 0, 0), text.length());
                        int end;
                        if (arguments.size() >= 2) {
                            end = resolveIndex(getIntArg(arguments, 1, text.length()), text.length());
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
                    @Override public int arity() { return -1; } // 1 ou 2 args
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        String search = arguments.isEmpty() ? "" : arguments.get(0).toString();
                        int fromIndex = (int) getIntArg(arguments, 1, 0);
                        if (fromIndex < 0) fromIndex = 0;
                        int idx = text.indexOf(search, fromIndex);
                        return (long) idx;
                    }
                };

            case "lastIndexOf":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        String search = arguments.isEmpty() ? "" : arguments.get(0).toString();
                        int fromIndex = (int) getIntArg(arguments, 1, text.length() - 1);
                        if (fromIndex >= text.length()) fromIndex = text.length() - 1;
                        int idx = text.lastIndexOf(search, fromIndex);
                        return (long) idx;
                    }
                };

            case "includes":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        String search = arguments.isEmpty() ? "" : arguments.get(0).toString();
                        return text.contains(search);
                    }
                };

            case "startsWith":
                return new XplCallable() {
                    @Override public int arity() { return -1; } // 1 ou 2 args
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        String prefix = arguments.isEmpty() ? "" : arguments.get(0).toString();
                        int offset = (int) getIntArg(arguments, 1, 0);
                        return text.startsWith(prefix, offset);
                    }
                };

            case "endsWith":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        String suffix = arguments.isEmpty() ? "" : arguments.get(0).toString();
                        return text.endsWith(suffix);
                    }
                };

            // ---------- Modificação e formatação ----------
            case "trim":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        return text.trim();
                    }
                };

            case "trimStart":
            case "trimLeft":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        int i = 0;
                        while (i < text.length() && Character.isWhitespace(text.charAt(i))) i++;
                        return text.substring(i);
                    }
                };

            case "trimEnd":
            case "trimRight":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        int i = text.length() - 1;
                        while (i >= 0 && Character.isWhitespace(text.charAt(i))) i--;
                        return text.substring(0, i + 1);
                    }
                };

            case "repeat":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        int count = (int) getIntArg(arguments, 0, 0);
                        if (count < 0) throw new RuntimeException("Número de repetições não pode ser negativo.");
                        return text.repeat(count);
                    }
                };

            case "padStart":
                return new XplCallable() {
                    @Override public int arity() { return -1; } // 1 ou 2 args
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        int targetLength = (int) getIntArg(arguments, 0, 0);
                        String padString = arguments.size() > 1 ? arguments.get(1).toString() : " ";
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
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        int targetLength = (int) getIntArg(arguments, 0, 0);
                        String padString = arguments.size() > 1 ? arguments.get(1).toString() : " ";
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
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        String target = arguments.get(0).toString();
                        String replacement = arguments.get(1).toString();
                        return text.replace(target, replacement);
                    }
                };

            case "replaceAll":
                return new XplCallable() {
                    @Override public int arity() { return 2; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        String regex = arguments.get(0).toString();
                        String replacement = arguments.get(1).toString();
                        return text.replaceAll(regex, replacement);
                    }
                };

            case "replaceFirst":
                return new XplCallable() {
                    @Override public int arity() { return 2; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        String regex = arguments.get(0).toString();
                        String replacement = arguments.get(1).toString();
                        return text.replaceFirst(regex, replacement);
                    }
                };

            // ---------- Divisão e junção ----------
            case "split":
                return new XplCallable() {
                    @Override public int arity() { return -1; } // 1 ou 2 args (separador, limite)
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        String separator = arguments.isEmpty() ? "," : arguments.get(0).toString();
                        int limit = (int) getIntArg(arguments, 1, 0);
                        String[] parts;
                        if (limit > 0) {
                            parts = text.split(Pattern.quote(separator), limit);
                        } else {
                            parts = text.split(Pattern.quote(separator));
                        }
                        List<Object> list = new ArrayList<>();
                        for (String p : parts) {
                            list.add(p);
                        }
                        return list; // retorna List para encadear com métodos de array
                    }
                };

            case "join":
                // join é geralmente chamado em arrays, mas podemos ter join em string?
                // Vamos deixar como método de array, mas incluímos aqui por completude.
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        // Não faz sentido em uma única string, mas podemos implementar como concatenação?
                        // Melhor lançar erro.
                        throw new RuntimeException("join não é um método de String. Use Array.join.");
                    }
                };

            // ---------- Conversão para outros tipos ----------
            case "toArray":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        List<Object> chars = new ArrayList<>();
                        for (char c : text.toCharArray()) {
                            chars.add(String.valueOf(c));
                        }
                        return chars;
                    }
                };

            case "toNumber":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        try {
                            return Double.parseDouble(text);
                        } catch (NumberFormatException e) {
                            return null; // ou 0? Seguimos o comportamento do Rust retornando Null
                        }
                    }
                };

            case "toBoolean":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        String lower = text.toLowerCase();
                        return lower.equals("true") || lower.equals("1") || lower.equals("yes");
                    }
                };

            // ---------- Métodos de inspeção ----------
            case "charCodeAt":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        int index = (int) getIntArg(arguments, 0, 0);
                        if (index < 0 || index >= text.length()) {
                            throw new RuntimeException("Índice fora dos limites.");
                        }
                        return (long) text.charAt(index);
                    }
                };

            // ---------- Métodos para inverter e ordenar (simples) ----------
            case "reverse":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        return new StringBuilder(text).reverse().toString();
                    }
                };

            // ---------- Método para obter substring com base em índice negativo ----------
            // Já temos slice e substring que lidam com índices negativos.

            default:
                throw new RuntimeException(
                        "Método '" + methodName + "' não implementado em String.");
        }
    }

    // ==========================================
    // FUNÇÕES AUXILIARES INTERNAS
    // ==========================================

    /**
     * Resolve índices negativos (ex: -1 é o último caractere).
     */
    private static int resolveIndex(long idx, int len) {
        if (idx < 0) {
            return (int) Math.max(0, len + idx);
        } else {
            return (int) Math.min(idx, len);
        }
    }

    /**
     * Extrai um argumento inteiro em segurança, com valor padrão.
     */
    private static long getIntArg(List<Object> args, int index, long defaultValue) {
        if (index >= args.size()) return defaultValue;
        Object val = args.get(index);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return defaultValue;
    }
}