package com.dic.xsuper.lang.helpers;

import com.dic.xsuper.lang.ControlFlow;
import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;

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

    public static Object getProperty(List<Object> list, String propertyName) {
        return switch (propertyName) {
            case "length" -> (long) list.size();
            case "isEmpty" -> list.isEmpty();
            case "first" -> list.isEmpty() ? null : list.getFirst();
            case "last" -> list.isEmpty() ? null : list.getLast();
            default -> throw new RuntimeException(
                    "Propriedade '" + propertyName + "' não encontrada em Array/List.");
        };
    }

    // ==========================================
    // MÉTODOS (retornam XplCallable)
    // ==========================================

    public static XplCallable getMethod(List<Object> list, String methodName) {
        switch (methodName) {

            // -------------------- ADIÇÃO E REMOÇÃO --------------------

            case "push":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        list.addAll(args);
                        return (long) list.size();
                    }
                };

            case "pop":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        return list.isEmpty() ? null : list.removeLast();
                    }
                };

            case "unshift":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        for (int i = args.size() - 1; i >= 0; i--) {
                            list.addFirst(args.get(i));
                        }
                        return (long) list.size();
                    }
                };

            case "shift":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        return list.isEmpty() ? null : list.removeFirst();
                    }
                };

            case "remove":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        int idx = (int) getIntArg(args, 0, 0);
                        if (idx < 0 || idx >= list.size()) {
                            throw new RuntimeException("Índice " + idx + " fora dos limites.");
                        }
                        return list.remove(idx);
                    }
                };
            case "toJson":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter intp, java.util.List<Expr.CallArg> args) {
                        try {
                            return new com.fasterxml.jackson.databind.ObjectMapper()
                                    .enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT)
                                    .writeValueAsString(list);
                        } catch (Exception e) {
                            throw new ControlFlow.RuntimeError(null, "Erro ao converter array para JSON: " + e.getMessage());
                        }
                    }
                };

            case "splice":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        int start = resolveIndex(getIntArg(args, 0, 0), list.size());
                        int deleteCount = (int) getIntArg(args, 1, list.size() - start);
                        if (deleteCount < 0) deleteCount = 0;
                        int actualDelete = Math.min(deleteCount, list.size() - start);

                        List<Object> removed = new ArrayList<>(list.subList(start, start + actualDelete));
                        for (int i = 0; i < actualDelete; i++) {
                            list.remove(start);
                        }

                        int insertIdx = start;
                        for (int i = 2; i < args.size(); i++) {
                            list.add(insertIdx++, args.get(i));
                        }
                        return removed;
                    }
                };

            case "concat":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        List<Object> newList = new ArrayList<>(list);
                        for (Object arg : args) {
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
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        Object value = args.isEmpty() ? null : args.getFirst();
                        int start = resolveIndex(getIntArg(args, 1, 0), list.size());
                        int end = resolveIndex(getIntArg(args, 2, list.size()), list.size());
                        for (int i = start; i < end && i < list.size(); i++) {
                            list.set(i, value);
                        }
                        return list;
                    }
                };

            case "copyWithin":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        int target = resolveIndex(getIntArg(args, 0, 0), list.size());
                        int start = resolveIndex(getIntArg(args, 1, 0), list.size());
                        int end = resolveIndex(getIntArg(args, 2, list.size()), list.size());

                        if (start < end && target < list.size()) {
                            int count = Math.min(end - start, list.size() - target);
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
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        Object target = args.isEmpty() ? null : args.getFirst();
                        int start = resolveIndex(getIntArg(args, 1, 0), list.size());
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
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        Object target = args.isEmpty() ? null : args.getFirst();
                        int start = resolveIndex(getIntArg(args, 1, list.size() - 1), list.size());
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
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        Object target = args.isEmpty() ? null : args.getFirst();
                        return list.contains(target);
                    }
                };

            case "at":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        int idx = resolveIndex(getIntArg(args, 0, 0), list.size());
                        return (idx >= 0 && idx < list.size()) ? list.get(idx) : null;
                    }
                };

            // -------------------- ORDENAÇÃO E REVERSÃO --------------------

            case "reverse":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
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
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
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
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        int start = resolveIndex(getIntArg(args, 0, 0), list.size());
                        int end = resolveIndex(getIntArg(args, 1, list.size()), list.size());
                        if (start > end) return new ArrayList<>();
                        return new ArrayList<>(list.subList(start, Math.min(end, list.size())));
                    }
                };

            case "flat":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        long depth = getIntArg(args, 0, 1);
                        return flattenArray(list, depth);
                    }
                };

            case "join":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        String sep = args.isEmpty() ? "," : args.getFirst().toString();
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
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
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
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
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
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        return new ArrayList<>(list);
                    }
                };

            case "entries":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
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
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        if (args.isEmpty()) {
                            throw new RuntimeException("forEach exige uma função callback.");
                        }
                        Object callback = args.get(0);
                        if (!(callback instanceof XplCallable)) {
                            throw new RuntimeException("forEach exige uma função callback.");
                        }
                        XplCallable fn = (XplCallable) callback;
                        for (int i = 0; i < list.size(); i++) {
                            List<Object> callArgs = new ArrayList<>();
                            callArgs.add(list.get(i));
                            callArgs.add((long) i);
                            // Converte os argumentos avaliados para CallArg com literais
                            fn.call(interpreter, packArgs(callArgs));
                        }
                        return null;
                    }
                };

            case "map":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        if (args.isEmpty()) {
                            throw new RuntimeException("map exige uma função callback.");
                        }
                        Object callback = args.get(0);
                        if (!(callback instanceof XplCallable)) {
                            throw new RuntimeException("map exige uma função callback.");
                        }
                        XplCallable fn = (XplCallable) callback;
                        List<Object> result = new ArrayList<>();
                        for (int i = 0; i < list.size(); i++) {
                            List<Object> callArgs = new ArrayList<>();
                            callArgs.add(list.get(i));
                            callArgs.add((long) i);
                            Object mapped = fn.call(interpreter, packArgs(callArgs));
                            result.add(mapped);
                        }
                        return result;
                    }
                };

            case "filter":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        if (args.isEmpty()) {
                            throw new RuntimeException("filter exige uma função callback.");
                        }
                        Object callback = args.get(0);
                        if (!(callback instanceof XplCallable)) {
                            throw new RuntimeException("filter exige uma função callback.");
                        }
                        XplCallable fn = (XplCallable) callback;
                        List<Object> result = new ArrayList<>();
                        for (int i = 0; i < list.size(); i++) {
                            List<Object> callArgs = new ArrayList<>();
                            callArgs.add(list.get(i));
                            callArgs.add((long) i);
                            Object keep = fn.call(interpreter, packArgs(callArgs));
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
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        if (args.isEmpty()) {
                            throw new RuntimeException(methodName + " exige uma função callback.");
                        }
                        Object callback = args.get(0);
                        if (!(callback instanceof XplCallable)) {
                            throw new RuntimeException(methodName + " exige uma função callback.");
                        }
                        XplCallable fn = (XplCallable) callback;
                        boolean isRight = methodName.equals("reduceRight");

                        Object accumulator;
                        int startIdx;
                        int endIdx;

                        if (args.size() > 1) {
                            accumulator = args.get(1);
                            startIdx = isRight ? list.size() - 1 : 0;
                            endIdx = isRight ? -1 : list.size();
                        } else {
                            if (list.isEmpty()) {
                                throw new RuntimeException("Reduce em lista vazia sem valor inicial.");
                            }
                            if (isRight) {
                                accumulator = list.getLast();
                                startIdx = list.size() - 2;
                                endIdx = -1;
                            } else {
                                accumulator = list.getFirst();
                                startIdx = 1;
                                endIdx = list.size();
                            }
                        }

                        int step = isRight ? -1 : 1;
                        for (int i = startIdx; (isRight ? i > endIdx : i < endIdx); i += step) {
                            List<Object> callArgs = new ArrayList<>();
                            callArgs.add(accumulator);
                            callArgs.add(list.get(i));
                            callArgs.add((long) i);
                            accumulator = fn.call(interpreter, packArgs(callArgs));
                        }
                        return accumulator;
                    }
                };

            case "flatMap":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        if (args.isEmpty()) {
                            throw new RuntimeException("flatMap exige uma função callback.");
                        }
                        Object callback = args.get(0);
                        if (!(callback instanceof XplCallable)) {
                            throw new RuntimeException("flatMap exige uma função callback.");
                        }
                        XplCallable fn = (XplCallable) callback;
                        List<Object> result = new ArrayList<>();
                        for (int i = 0; i < list.size(); i++) {
                            List<Object> callArgs = new ArrayList<>();
                            callArgs.add(list.get(i));
                            callArgs.add((long) i);
                            Object mapped = fn.call(interpreter, packArgs(callArgs));
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
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        if (args.isEmpty()) {
                            throw new RuntimeException("find exige uma função callback.");
                        }
                        Object callback = args.get(0);
                        if (!(callback instanceof XplCallable)) {
                            throw new RuntimeException("find exige uma função callback.");
                        }
                        XplCallable fn = (XplCallable) callback;
                        for (int i = 0; i < list.size(); i++) {
                            List<Object> callArgs = new ArrayList<>();
                            callArgs.add(list.get(i));
                            callArgs.add((long) i);
                            Object keep = fn.call(interpreter, packArgs(callArgs));
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
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        if (args.isEmpty()) {
                            throw new RuntimeException("findIndex exige uma função callback.");
                        }
                        Object callback = args.get(0);
                        if (!(callback instanceof XplCallable)) {
                            throw new RuntimeException("findIndex exige uma função callback.");
                        }
                        XplCallable fn = (XplCallable) callback;
                        for (int i = 0; i < list.size(); i++) {
                            List<Object> callArgs = new ArrayList<>();
                            callArgs.add(list.get(i));
                            callArgs.add((long) i);
                            Object keep = fn.call(interpreter, packArgs(callArgs));
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
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        if (args.isEmpty()) {
                            throw new RuntimeException("findLast exige uma função callback.");
                        }
                        Object callback = args.get(0);
                        if (!(callback instanceof XplCallable)) {
                            throw new RuntimeException("findLast exige uma função callback.");
                        }
                        XplCallable fn = (XplCallable) callback;
                        for (int i = list.size() - 1; i >= 0; i--) {
                            List<Object> callArgs = new ArrayList<>();
                            callArgs.add(list.get(i));
                            callArgs.add((long) i);
                            Object keep = fn.call(interpreter, packArgs(callArgs));
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
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        if (args.isEmpty()) {
                            throw new RuntimeException("findLastIndex exige uma função callback.");
                        }
                        Object callback = args.get(0);
                        if (!(callback instanceof XplCallable)) {
                            throw new RuntimeException("findLastIndex exige uma função callback.");
                        }
                        XplCallable fn = (XplCallable) callback;
                        for (int i = list.size() - 1; i >= 0; i--) {
                            List<Object> callArgs = new ArrayList<>();
                            callArgs.add(list.get(i));
                            callArgs.add((long) i);
                            Object keep = fn.call(interpreter, packArgs(callArgs));
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
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        if (args.isEmpty()) {
                            throw new RuntimeException("some exige uma função callback.");
                        }
                        Object callback = args.get(0);
                        if (!(callback instanceof XplCallable)) {
                            throw new RuntimeException("some exige uma função callback.");
                        }
                        XplCallable fn = (XplCallable) callback;
                        for (int i = 0; i < list.size(); i++) {
                            List<Object> callArgs = new ArrayList<>();
                            callArgs.add(list.get(i));
                            callArgs.add((long) i);
                            Object keep = fn.call(interpreter, packArgs(callArgs));
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
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        if (args.isEmpty()) {
                            throw new RuntimeException("every exige uma função callback.");
                        }
                        Object callback = args.get(0);
                        if (!(callback instanceof XplCallable)) {
                            throw new RuntimeException("every exige uma função callback.");
                        }
                        XplCallable fn = (XplCallable) callback;
                        for (int i = 0; i < list.size(); i++) {
                            List<Object> callArgs = new ArrayList<>();
                            callArgs.add(list.get(i));
                            callArgs.add((long) i);
                            Object keep = fn.call(interpreter, packArgs(callArgs));
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
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        boolean inPlace = methodName.equals("sort");
                        List<Object> target = inPlace ? list : new ArrayList<>(list);

                        if (!args.isEmpty() && args.get(0) instanceof XplCallable) {
                            XplCallable comparator = (XplCallable) args.get(0);
                            for (int i = 1; i < target.size(); i++) {
                                int j = i;
                                while (j > 0) {
                                    List<Object> cmpArgs = new ArrayList<>();
                                    cmpArgs.add(target.get(j));
                                    cmpArgs.add(target.get(j - 1));
                                    Object result = comparator.call(interpreter, packArgs(cmpArgs));
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

                        return inPlace ? list : target;
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

    /**
     * Converte uma lista de valores avaliados em uma lista de CallArg com expressões literais.
     * Necessário para invocar callbacks que esperam List<Expr.CallArg>.
     */
    private static List<Expr.CallArg> packArgs(List<Object> values) {
        List<Expr.CallArg> callArgs = new ArrayList<>();
        for (Object value : values) {
            // Assumindo que Expr.Literal existe e aceita um Object
            callArgs.add(new Expr.CallArg(null, new Expr.Literal(value)));
        }
        return callArgs;
    }
}