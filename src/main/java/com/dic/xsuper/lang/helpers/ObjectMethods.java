package com.dic.xsuper.lang.helpers;

import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Fornece métodos e propriedades para objetos (mapas) no interpretador XSuper,
 * espelhando completamente a implementação Rust fornecida.
 */
public class ObjectMethods {

    // ==========================================
    // 1. PROPRIEDADES (acesso direto)
    // ==========================================

    public static Object getProperty(Map<String, Object> map, String property) {
        return switch (property) {
            case "length", "size" -> (long) map.size();
            case "isEmpty" -> map.isEmpty();
            default -> {
                if (map.containsKey(property)) {
                    yield map.get(property);
                }
                throw new RuntimeException("Propriedade '" + property + "' não encontrada no objeto.");
            }
        };
    }

    // ==========================================
    // 2. MÉTODOS DE INSTÂNCIA (retornam XplCallable)
    // ==========================================

    public static XplCallable getMethod(Map<String, Object> map, String methodName) {
        switch (methodName) {

            // ---------- Informação básica ----------
            case "size":
            case "length":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        return (long) map.size();
                    }
                };
            case "isEmpty":
            case "is_empty":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        return map.isEmpty();
                    }
                };

            case "keys":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        return map.keySet().stream()
                                .filter(k -> !k.startsWith("__"))
                                .collect(Collectors.toList());
                    }
                };

            case "values":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        return new ArrayList<>(map.values());
                    }
                };

            case "entries":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> entries = new ArrayList<>();
                        for (Map.Entry<String, Object> e : map.entrySet()) {
                            if (e.getKey().startsWith("__")) continue;
                            List<Object> pair = new ArrayList<>();
                            pair.add(e.getKey());
                            pair.add(e.getValue());
                            entries.add(pair);
                        }
                        return entries;
                    }
                };

            // ---------- Consulta ----------
            case "has":
            case "containsKey":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        checkArgCount(args, 1, methodName);
                        String key = args.get(0).toString();
                        return map.containsKey(key);
                    }
                };

            case "get":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        checkArgCount(args, 1, methodName);
                        String key = args.get(0).toString();
                        return map.getOrDefault(key, null);
                    }
                };

            // ---------- Modificação ----------
            case "set":
                return new XplCallable() {
                    @Override public int arity() { return 2; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        checkArgCount(args, 2, methodName);
                        String key = args.get(0).toString();
                        Object value = args.get(1);
                        map.put(key, value);
                        return map; // chaining
                    }
                };

            case "remove":
            case "delete":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        checkArgCount(args, 1, methodName);
                        String key = args.get(0).toString();
                        return map.remove(key);
                    }
                };

            case "clear":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        map.clear();
                        return null;
                    }
                };

            case "clone":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        return new HashMap<>(map);
                    }
                };

            // ---------- Pesquisa recursiva ----------
            case "search":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        checkArgCount(args, 1, methodName);
                        Object arg = args.get(0);
                        return search(map, arg);
                    }
                };

            case "searchByKey":
            case "search_by_key":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        checkArgCount(args, 1, methodName);
                        String key = args.get(0).toString();
                        return searchByKey(map, key);
                    }
                };

            case "searchByValue":
            case "search_by_value":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        checkArgCount(args, 1, methodName);
                        Object value = args.get(0);
                        return searchByValue(map, value);
                    }
                };

            case "findPaths":
            case "find_paths":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        checkArgCount(args, 1, methodName);
                        Object arg = args.get(0);
                        return findPaths(map, arg);
                    }
                };

            // ---------- Transformações ----------
            case "flatten":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        return flatten(map);
                    }
                };

            case "pick":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        if (args.isEmpty()) {
                            throw new RuntimeException("pick requer pelo menos uma chave ou uma lista de chaves.");
                        }
                        List<String> keys = new ArrayList<>();
                        if (args.size() == 1 && args.get(0) instanceof List) {
                            for (Object o : (List<?>) args.get(0)) {
                                keys.add(o.toString());
                            }
                        } else {
                            for (Object o : args) {
                                keys.add(o.toString());
                            }
                        }
                        return pick(map, keys);
                    }
                };

            case "omit":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        List<String> keys = new ArrayList<>();
                        for (Object o : args) {
                            keys.add(o.toString());
                        }
                        return omit(map, keys);
                    }
                };

            case "merge":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        List<Map<String, Object>> others = new ArrayList<>();
                        for (Object arg : args) {
                            if (arg instanceof Map) {
                                others.add((Map<String, Object>) arg);
                            } else {
                                throw new RuntimeException("merge só aceita objetos como argumentos.");
                            }
                        }
                        return merge(map, others);
                    }
                };

            case "hasPath":
            case "has_path":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Expr.CallArg> arguments) {
                        List<Object> args = Interpreter.unpackNativeArgs(interpreter, arguments);
                        checkArgCount(args, 1, methodName);
                        String path = args.get(0).toString();
                        return hasPath(map, path);
                    }
                };

            default:
                throw new RuntimeException("Método '" + methodName + "' não implementado para objeto.");
        }
    }

    // ==========================================
    // 3. IMPLEMENTAÇÃO DOS MÉTODOS PÚBLICOS
    // ==========================================

    // ---------- Pesquisa recursiva ----------

    public static List<Map<String, Object>> search(Map<String, Object> obj, Object arg) {
        List<Map<String, Object>> results = new ArrayList<>();
        int[] counter = new int[]{0};
        if (arg instanceof String) {
            String key = (String) arg;
            searchRecursive(obj, key, null, "", 0, results, counter);
        } else {
            searchRecursive(obj, null, arg, "", 0, results, counter);
        }
        return results;
    }

    public static List<Map<String, Object>> searchByKey(Map<String, Object> obj, String key) {
        List<Map<String, Object>> results = new ArrayList<>();
        int[] counter = new int[]{0};
        searchRecursive(obj, key, null, "", 0, results, counter);
        return results;
    }

    public static List<Map<String, Object>> searchByValue(Map<String, Object> obj, Object value) {
        List<Map<String, Object>> results = new ArrayList<>();
        int[] counter = new int[]{0};
        searchRecursive(obj, null, value, "", 0, results, counter);
        return results;
    }

    public static List<String> findPaths(Map<String, Object> obj, Object arg) {
        List<Map<String, Object>> results = search(obj, arg);
        return results.stream()
                .map(r -> (String) r.get("path"))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private static void searchRecursive(
            Object data,
            String targetKey,
            Object targetValue,
            String currentPath,
            int depth,
            List<Map<String, Object>> results,
            int[] counter) {

        if (data instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) data;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String k = entry.getKey();
                if (k.startsWith("__")) continue;
                Object v = entry.getValue();
                String fullPath = currentPath.isEmpty() ? k : currentPath + "." + k;

                if (targetKey != null && k.equals(targetKey)) {
                    counter[0]++;
                    results.add(makeResult(k, v, depth, counter[0], fullPath));
                }
                if (targetValue != null && Objects.equals(v, targetValue)) {
                    counter[0]++;
                    results.add(makeResult(k, v, depth, counter[0], fullPath));
                }
                searchRecursive(v, targetKey, targetValue, fullPath, depth + 1, results, counter);
            }
        } else if (data instanceof List) {
            List<Object> list = (List<Object>) data;
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                String idxPath = currentPath + "[" + i + "]";
                if (targetValue != null && Objects.equals(item, targetValue)) {
                    counter[0]++;
                    results.add(makeResult("[" + i + "]", item, depth, counter[0], idxPath));
                }
                searchRecursive(item, targetKey, targetValue, idxPath, depth + 1, results, counter);
            }
        }
    }

    private static Map<String, Object> makeResult(String key, Object value, int depth, int pos, String path) {
        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("value", value);
        result.put("node", (long) depth);
        result.put("pos", (long) pos);
        result.put("path", path);
        return result;
    }

    // ---------- Transformações ----------

    public static Map<String, Object> flatten(Map<String, Object> obj) {
        Map<String, Object> result = new HashMap<>();
        flattenRecursive(obj, "", result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void flattenRecursive(Object data, String prefix, Map<String, Object> out) {
        if (data instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) data;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String newPrefix = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                flattenRecursive(entry.getValue(), newPrefix, out);
            }
        } else if (data instanceof List) {
            List<Object> list = (List<Object>) data;
            for (int i = 0; i < list.size(); i++) {
                String newPrefix = prefix + "[" + i + "]";
                flattenRecursive(list.get(i), newPrefix, out);
            }
        } else {
            out.put(prefix, data);
        }
    }

    public static Map<String, Object> pick(Map<String, Object> obj, List<String> keys) {
        Map<String, Object> result = new HashMap<>();
        for (String k : keys) {
            if (obj.containsKey(k)) {
                result.put(k, obj.get(k));
            }
        }
        return result;
    }

    public static Map<String, Object> omit(Map<String, Object> obj, List<String> keys) {
        Map<String, Object> result = new HashMap<>(obj);
        for (String k : keys) {
            result.remove(k);
        }
        return result;
    }

    public static Map<String, Object> merge(Map<String, Object> base, List<Map<String, Object>> others) {
        Map<String, Object> result = new HashMap<>(base);
        for (Map<String, Object> other : others) {
            result.putAll(other);
        }
        return result;
    }

    // ---------- Verificação de caminho ----------

    public static boolean hasPath(Map<String, Object> obj, String path) {
        Object current = obj;
        int start = 0;
        while (start < path.length()) {
            int end = start;
            boolean isIndex = false;
            int bracketPos = -1;

            while (end < path.length()) {
                char c = path.charAt(end);
                if (c == '.') {
                    break;
                } else if (c == '[') {
                    isIndex = true;
                    bracketPos = end;
                    int bracketEnd = end + 1;
                    while (bracketEnd < path.length() && path.charAt(bracketEnd) != ']') {
                        bracketEnd++;
                    }
                    if (bracketEnd == path.length()) {
                        return false;
                    }
                    end = bracketEnd + 1;
                    break;
                } else {
                    end++;
                }
            }

            String part = path.substring(start, end);
            if (isIndex) {
                if (bracketPos == -1) return false;
                String key = bracketPos > start ? path.substring(start, bracketPos) : "";
                String idxStr = path.substring(bracketPos + 1, end - 1);
                int idx;
                try {
                    idx = Integer.parseInt(idxStr);
                } catch (NumberFormatException e) {
                    return false;
                }

                if (!key.isEmpty()) {
                    if (current instanceof Map) {
                        Map<String, Object> map = (Map<String, Object>) current;
                        if (map.containsKey(key)) {
                            current = map.get(key);
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
                if (current instanceof List) {
                    List<Object> list = (List<Object>) current;
                    if (idx >= 0 && idx < list.size()) {
                        current = list.get(idx);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                if (current instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) current;
                    if (map.containsKey(part)) {
                        current = map.get(part);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }

            start = (end < path.length() && path.charAt(end) == '.') ? end + 1 : end;
        }
        return true;
    }

    // ==========================================
    // 4. FUNÇÕES AUXILIARES
    // ==========================================

    private static void checkArgCount(List<Object> args, int expected, String methodName) {
        if (args.size() < expected) {
            throw new RuntimeException("Método '" + methodName + "' requer " + expected +
                    " argumento(s), mas recebeu " + args.size());
        }
    }
}