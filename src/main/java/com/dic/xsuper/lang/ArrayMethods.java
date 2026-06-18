package com.dic.xsuper.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fornece métodos e propriedades para objetos do tipo List (arrays/lists)
 * no interpretador XSuper, espelhando a implementação Rust fornecida.
 */
public class ArrayMethods {

    // ==========================================
    // PROPRIEDADES (acesso direto)
    // ==========================================

    /**
     * Retorna o valor de uma propriedade da lista (ex: length, isEmpty, first, last).
     */
    public static Object getProperty(List<Object> list, String propertyName) {
        switch (propertyName) {
            case "length":
                return (long) list.size();
            case "isEmpty":
                return list.isEmpty();
            case "first":
                return list.isEmpty() ? null : list.get(0);
            case "last":
                return list.isEmpty() ? null : list.get(list.size() - 1);
            default:
                throw new RuntimeException(
                        "Propriedade '" + propertyName + "' não encontrada em Array/List.");
        }
    }

    // ==========================================
    // MÉTODOS (retornam XplCallable)
    // ==========================================

    /**
     * Retorna um XplCallable para o método solicitado, operando sobre a lista fornecida.
     * O callable recebe os argumentos como List<Object> e pode ter aridade variável (-1).
     */
    public static XplCallable getMethod(List<Object> list, String methodName) {
        switch (methodName) {

            // -------------------- ADIÇÃO E REMOÇÃO --------------------

            case "push":
                return new XplCallable() {
                    @Override public int arity() { return -1; } // variádico
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        for (Object arg : arguments) {
                            list.add(arg);
                        }
                        return (long) list.size(); // retorna novo tamanho
                    }
                };

            case "pop":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        return list.isEmpty() ? null : list.remove(list.size() - 1);
                    }
                };

            case "unshift":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        // Insere no início na ordem inversa para manter a ordem original
                        for (int i = arguments.size() - 1; i >= 0; i--) {
                            list.add(0, arguments.get(i));
                        }
                        return (long) list.size();
                    }
                };

            case "shift":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        return list.isEmpty() ? null : list.remove(0);
                    }
                };

            case "remove":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        int idx = (int) getIntArg(arguments, 0, 0);
                        if (idx < 0 || idx >= list.size()) {
                            throw new RuntimeException("Índice " + idx + " fora dos limites.");
                        }
                        return list.remove(idx);
                    }
                };

            case "splice":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        int start = resolveIndex(getIntArg(arguments, 0, 0), list.size());
                        int deleteCount = (int) getIntArg(arguments, 1, list.size() - start);
                        if (deleteCount < 0) deleteCount = 0;
                        int actualDelete = Math.min(deleteCount, list.size() - start);

                        // Itens removidos
                        List<Object> removed = new ArrayList<>(list.subList(start, start + actualDelete));
                        // Remove
                        for (int i = 0; i < actualDelete; i++) {
                            list.remove(start);
                        }

                        // Insere novos itens (a partir do argumento 2)
                        int insertIdx = start;
                        for (int i = 2; i < arguments.size(); i++) {
                            list.add(insertIdx++, arguments.get(i));
                        }
                        return removed; // retorna lista dos removidos
                    }
                };

            case "concat":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        List<Object> newList = new ArrayList<>(list);
                        for (Object arg : arguments) {
                            if (arg instanceof List) {
                                newList.addAll((List<?>) arg);
                            } else {
                                newList.add(arg);
                            }
                        }
                        return newList;
                    }
                };

            case "fill":
                return new XplCallable() {
                    @Override public int arity() { return -1; } // 1 a 3 args
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        Object value = arguments.isEmpty() ? null : arguments.get(0);
                        int start = resolveIndex(getIntArg(arguments, 1, 0), list.size());
                        int end = resolveIndex(getIntArg(arguments, 2, list.size()), list.size());
                        for (int i = start; i < end && i < list.size(); i++) {
                            list.set(i, value);
                        }
                        return list; // chaining
                    }
                };

            case "copyWithin":
                return new XplCallable() {
                    @Override public int arity() { return -1; } // 1 a 3 args
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        int target = resolveIndex(getIntArg(arguments, 0, 0), list.size());
                        int start = resolveIndex(getIntArg(arguments, 1, 0), list.size());
                        int end = resolveIndex(getIntArg(arguments, 2, list.size()), list.size());

                        if (start < end && target < list.size()) {
                            int count = Math.min(end - start, list.size() - target);
                            // Cópia temporária
                            List<Object> temp = new ArrayList<>(list.subList(start, start + count));
                            for (int i = 0; i < count; i++) {
                                list.set(target + i, temp.get(i));
                            }
                        }
                        return list;
                    }
                };

            // -------------------- PESQUISA E LOCALIZAÇÃO --------------------

            case "indexOf":
                return new XplCallable() {
                    @Override public int arity() { return -1; } // 1 ou 2 args
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        Object target = arguments.isEmpty() ? null : arguments.get(0);
                        int start = resolveIndex(getIntArg(arguments, 1, 0), list.size());
                        for (int i = start; i < list.size(); i++) {
                            if (Objects.equals(list.get(i), target)) {
                                return (long) i;
                            }
                        }
                        return -1L;
                    }
                };

            case "lastIndexOf":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        Object target = arguments.isEmpty() ? null : arguments.get(0);
                        int start = resolveIndex(getIntArg(arguments, 1, list.size() - 1), list.size());
                        for (int i = Math.min(start, list.size() - 1); i >= 0; i--) {
                            if (Objects.equals(list.get(i), target)) {
                                return (long) i;
                            }
                        }
                        return -1L;
                    }
                };

            case "includes":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        Object target = arguments.isEmpty() ? null : arguments.get(0);
                        return list.contains(target);
                    }
                };

            case "at":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        int idx = resolveIndex(getIntArg(arguments, 0, 0), list.size());
                        return (idx >= 0 && idx < list.size()) ? list.get(idx) : null;
                    }
                };

            // -------------------- ORDENAÇÃO E REVERSÃO --------------------

            case "reverse":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        int n = list.size();
                        for (int i = 0; i < n / 2; i++) {
                            Object tmp = list.get(i);
                            list.set(i, list.get(n - 1 - i));
                            list.set(n - 1 - i, tmp);
                        }
                        return list;
                    }
                };

            case "toReversed":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        List<Object> copy = new ArrayList<>(list);
                        int n = copy.size();
                        for (int i = 0; i < n / 2; i++) {
                            Object tmp = copy.get(i);
                            copy.set(i, copy.get(n - 1 - i));
                            copy.set(n - 1 - i, tmp);
                        }
                        return copy;
                    }
                };

            // -------------------- TRANSFORMAÇÃO DE ARRAYS --------------------

            case "slice":
                return new XplCallable() {
                    @Override public int arity() { return -1; } // 0 a 2 args
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        int start = resolveIndex(getIntArg(arguments, 0, 0), list.size());
                        int end = resolveIndex(getIntArg(arguments, 1, list.size()), list.size());
                        if (start > end) return new ArrayList<>();
                        return new ArrayList<>(list.subList(start, Math.min(end, list.size())));
                    }
                };

            case "flat":
                return new XplCallable() {
                    @Override public int arity() { return -1; } // 0 ou 1 (depth)
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        long depth = getIntArg(arguments, 0, 1);
                        return flattenArray(list, depth);
                    }
                };

            case "join":
                return new XplCallable() {
                    @Override public int arity() { return -1; } // 0 ou 1 (separator)
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        String sep = arguments.isEmpty() ? "," : arguments.get(0).toString();
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < list.size(); i++) {
                            if (i > 0) sb.append(sep);
                            sb.append(list.get(i) == null ? "null" : list.get(i).toString());
                        }
                        return sb.toString();
                    }
                };

            case "toString":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < list.size(); i++) {
                            if (i > 0) sb.append(",");
                            sb.append(list.get(i) == null ? "null" : list.get(i).toString());
                        }
                        return sb.toString();
                    }
                };

            case "keys":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        List<Object> keys = new ArrayList<>();
                        for (int i = 0; i < list.size(); i++) {
                            keys.add((long) i);
                        }
                        return keys;
                    }
                };

            case "values":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        return new ArrayList<>(list);
                    }
                };

            case "entries":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        List<Object> entries = new ArrayList<>();
                        for (int i = 0; i < list.size(); i++) {
                            List<Object> pair = new ArrayList<>();
                            pair.add((long) i);
                            pair.add(list.get(i));
                            entries.add(pair);
                        }
                        return entries;
                    }
                };

            // -------------------- CALLBACKS E FUNÇÕES DE ORDEM SUPERIOR --------------------

            case "forEach":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        Object callback = arguments.get(0);
                        if (!(callback instanceof XplCallable)) {
                            throw new RuntimeException("forEach exige uma função callback.");
                        }
                        XplCallable fn = (XplCallable) callback;
                        for (int i = 0; i < list.size(); i++) {
                            List<Object> args = new ArrayList<>();
                            args.add(list.get(i));
                            args.add((long) i);
                            fn.call(interpreter, args);
                        }
                        return null; // void
                    }
                };

            case "map":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        Object callback = arguments.get(0);
                        if (!(callback instanceof XplCallable)) {
                            throw new RuntimeException("map exige uma função callback.");
                        }
                        XplCallable fn = (XplCallable) callback;
                        List<Object> result = new ArrayList<>();
                        for (int i = 0; i < list.size(); i++) {
                            List<Object> args = new ArrayList<>();
                            args.add(list.get(i));
                            args.add((long) i);
                            result.add(fn.call(interpreter, args));
                        }
                        return result;
                    }
                };

            case "filter":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        Object callback = arguments.get(0);
                        if (!(callback instanceof XplCallable)) {
                            throw new RuntimeException("filter exige uma função callback.");
                        }
                        XplCallable fn = (XplCallable) callback;
                        List<Object> result = new ArrayList<>();
                        for (int i = 0; i < list.size(); i++) {
                            List<Object> args = new ArrayList<>();
                            args.add(list.get(i));
                            args.add((long) i);
                            Object keep = fn.call(interpreter, args);
                            if (keep instanceof Boolean && (Boolean) keep) {
                                result.add(list.get(i));
                            }
                        }
                        return result;
                    }
                };

            case "reduce":
            case "reduceRight":
                return new XplCallable() {
                    @Override public int arity() { return -1; } // 1 ou 2 args
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        Object callback = arguments.get(0);
                        if (!(callback instanceof XplCallable)) {
                            throw new RuntimeException(methodName + " exige uma função callback.");
                        }
                        XplCallable fn = (XplCallable) callback;
                        boolean isRight = methodName.equals("reduceRight");

                        Object accumulator;
                        int startIdx;
                        int endIdx;

                        if (arguments.size() > 1) {
                            accumulator = arguments.get(1);
                            startIdx = isRight ? list.size() - 1 : 0;
                            endIdx = isRight ? -1 : list.size();
                        } else {
                            if (list.isEmpty()) {
                                throw new RuntimeException("Reduce em lista vazia sem valor inicial.");
                            }
                            if (isRight) {
                                accumulator = list.get(list.size() - 1);
                                startIdx = list.size() - 2;
                                endIdx = -1;
                            } else {
                                accumulator = list.get(0);
                                startIdx = 1;
                                endIdx = list.size();
                            }
                        }

                        int step = isRight ? -1 : 1;
                        for (int i = startIdx; (isRight ? i > endIdx : i < endIdx); i += step) {
                            List<Object> args = new ArrayList<>();
                            args.add(accumulator);
                            args.add(list.get(i));
                            args.add((long) i);
                            accumulator = fn.call(interpreter, args);
                        }
                        return accumulator;
                    }
                };

            case "flatMap":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        Object callback = arguments.get(0);
                        if (!(callback instanceof XplCallable)) {
                            throw new RuntimeException("flatMap exige uma função callback.");
                        }
                        XplCallable fn = (XplCallable) callback;
                        List<Object> result = new ArrayList<>();
                        for (int i = 0; i < list.size(); i++) {
                            List<Object> args = new ArrayList<>();
                            args.add(list.get(i));
                            args.add((long) i);
                            Object mapped = fn.call(interpreter, args);
                            if (mapped instanceof List) {
                                result.addAll((List<?>) mapped);
                            } else {
                                result.add(mapped);
                            }
                        }
                        return result;
                    }
                };

            case "find":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        Object callback = arguments.get(0);
                        if (!(callback instanceof XplCallable)) {
                            throw new RuntimeException("find exige uma função callback.");
                        }
                        XplCallable fn = (XplCallable) callback;
                        for (int i = 0; i < list.size(); i++) {
                            List<Object> args = new ArrayList<>();
                            args.add(list.get(i));
                            args.add((long) i);
                            Object keep = fn.call(interpreter, args);
                            if (keep instanceof Boolean && (Boolean) keep) {
                                return list.get(i);
                            }
                        }
                        return null;
                    }
                };

            case "findIndex":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        Object callback = arguments.get(0);
                        if (!(callback instanceof XplCallable)) {
                            throw new RuntimeException("findIndex exige uma função callback.");
                        }
                        XplCallable fn = (XplCallable) callback;
                        for (int i = 0; i < list.size(); i++) {
                            List<Object> args = new ArrayList<>();
                            args.add(list.get(i));
                            args.add((long) i);
                            Object keep = fn.call(interpreter, args);
                            if (keep instanceof Boolean && (Boolean) keep) {
                                return (long) i;
                            }
                        }
                        return -1L;
                    }
                };

            case "findLast":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        Object callback = arguments.get(0);
                        if (!(callback instanceof XplCallable)) {
                            throw new RuntimeException("findLast exige uma função callback.");
                        }
                        XplCallable fn = (XplCallable) callback;
                        for (int i = list.size() - 1; i >= 0; i--) {
                            List<Object> args = new ArrayList<>();
                            args.add(list.get(i));
                            args.add((long) i);
                            Object keep = fn.call(interpreter, args);
                            if (keep instanceof Boolean && (Boolean) keep) {
                                return list.get(i);
                            }
                        }
                        return null;
                    }
                };

            case "findLastIndex":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        Object callback = arguments.get(0);
                        if (!(callback instanceof XplCallable)) {
                            throw new RuntimeException("findLastIndex exige uma função callback.");
                        }
                        XplCallable fn = (XplCallable) callback;
                        for (int i = list.size() - 1; i >= 0; i--) {
                            List<Object> args = new ArrayList<>();
                            args.add(list.get(i));
                            args.add((long) i);
                            Object keep = fn.call(interpreter, args);
                            if (keep instanceof Boolean && (Boolean) keep) {
                                return (long) i;
                            }
                        }
                        return -1L;
                    }
                };

            case "some":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        Object callback = arguments.get(0);
                        if (!(callback instanceof XplCallable)) {
                            throw new RuntimeException("some exige uma função callback.");
                        }
                        XplCallable fn = (XplCallable) callback;
                        for (int i = 0; i < list.size(); i++) {
                            List<Object> args = new ArrayList<>();
                            args.add(list.get(i));
                            args.add((long) i);
                            Object keep = fn.call(interpreter, args);
                            if (keep instanceof Boolean && (Boolean) keep) {
                                return true;
                            }
                        }
                        return false;
                    }
                };

            case "every":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        Object callback = arguments.get(0);
                        if (!(callback instanceof XplCallable)) {
                            throw new RuntimeException("every exige uma função callback.");
                        }
                        XplCallable fn = (XplCallable) callback;
                        for (int i = 0; i < list.size(); i++) {
                            List<Object> args = new ArrayList<>();
                            args.add(list.get(i));
                            args.add((long) i);
                            Object keep = fn.call(interpreter, args);
                            if (!(keep instanceof Boolean) || !(Boolean) keep) {
                                return false;
                            }
                        }
                        return true;
                    }
                };

            case "sort":
            case "toSorted":
                return new XplCallable() {
                    @Override public int arity() { return -1; } // 0 ou 1 (callback)
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        boolean inPlace = methodName.equals("sort");
                        List<Object> target = inPlace ? list : new ArrayList<>(list);

                        if (!arguments.isEmpty() && arguments.get(0) instanceof XplCallable) {
                            XplCallable comparator = (XplCallable) arguments.get(0);
                            // Ordenação com insertion sort (falível) para respeitar callback
                            for (int i = 1; i < target.size(); i++) {
                                int j = i;
                                while (j > 0) {
                                    List<Object> cmpArgs = new ArrayList<>();
                                    cmpArgs.add(target.get(j));
                                    cmpArgs.add(target.get(j - 1));
                                    Object result = comparator.call(interpreter, cmpArgs);
                                    boolean swap = false;
                                    if (result instanceof Long) {
                                        swap = (Long) result < 0;
                                    } else if (result instanceof Integer) {
                                        swap = (Integer) result < 0;
                                    } else if (result instanceof Double) {
                                        swap = (Double) result < 0;
                                    }
                                    if (swap) {
                                        Object tmp = target.get(j);
                                        target.set(j, target.get(j - 1));
                                        target.set(j - 1, tmp);
                                        j--;
                                    } else {
                                        break;
                                    }
                                }
                            }
                        } else {
                            // Ordenação natural: números primeiro, depois strings, etc.
                            target.sort((a, b) -> {
                                if (a == null && b == null) return 0;
                                if (a == null) return -1;
                                if (b == null) return 1;
                                if (a instanceof Number && b instanceof Number) {
                                    double da = ((Number) a).doubleValue();
                                    double db = ((Number) b).doubleValue();
                                    return Double.compare(da, db);
                                }
                                return a.toString().compareTo(b.toString());
                            });
                        }

                        if (inPlace) {
                            // Se in-place, a lista original já foi alterada (target é a mesma referência)
                            return list;
                        } else {
                            return target;
                        }
                    }
                };

            default:
                throw new RuntimeException(
                        "Método '" + methodName + "' não implementado em Array/List.");
        }
    }

    // ==========================================
    // FUNÇÕES AUXILIARES INTERNAS
    // ==========================================

    /**
     * Resolve índices negativos (ex: -1 é o último elemento).
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

    /**
     * Achata arrays recursivamente para o método flat().
     */
    private static List<Object> flattenArray(List<Object> items, long depth) {
        if (depth <= 0) {
            return new ArrayList<>(items);
        }
        List<Object> result = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof List) {
                result.addAll(flattenArray((List<Object>) item, depth - 1));
            } else {
                result.add(item);
            }
        }
        return result;
    }
}