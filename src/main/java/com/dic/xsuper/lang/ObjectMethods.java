package com.dic.xsuper.lang;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Fornece métodos e propriedades para objetos (mapas) no interpretador XSuper,
 * espelhando completamente a implementação Rust fornecida.
 */
public class ObjectMethods {

    // ==========================================
    // 1. PROPRIEDADES (acesso direto)
    // ==========================================

    /**
     * Retorna o valor de uma propriedade do objeto (ex: length, size, isEmpty).
     */
    public static Object getProperty(Map<String, Object> map, String property) {
        switch (property) {
            case "length":
            case "size":
                return (long) map.size();
            case "isEmpty":
                return map.isEmpty();
            default:
                if (map.containsKey(property)) {
                    return map.get(property);
                }
                throw new RuntimeException("Propriedade '" + property + "' não encontrada no objeto.");
        }
    }

    // ==========================================
    // 2. MÉTODOS DE INSTÂNCIA (retornam XplCallable)
    // ==========================================

    /**
     * Retorna um XplCallable para o método solicitado, operando sobre o mapa fornecido.
     * O callable recebe os argumentos como List<Object> e pode ter aridade variável (-1).
     */
    public static XplCallable getMethod(Map<String, Object> map, String methodName) {
        switch (methodName) {

            // ---------- Informação básica ----------
            case "size":
            case "length":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        return (long) map.size();
                    }
                };

            case "isEmpty":
            case "is_empty":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        return map.isEmpty();
                    }
                };

            case "keys":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        // Filtra chaves internas (que começam com "__") se desejar, igual ao Rust
                        return map.keySet().stream()
                                .filter(k -> !k.startsWith("__"))
                                .collect(Collectors.toList());
                    }
                };

            case "values":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        return new ArrayList<>(map.values());
                    }
                };

            case "entries":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
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
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        if (arguments.isEmpty()) {
                            throw new RuntimeException("has(key) precisa de um argumento.");
                        }
                        String key = arguments.get(0).toString();
                        return map.containsKey(key);
                    }
                };

            case "get":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        if (arguments.isEmpty()) {
                            throw new RuntimeException("get(key) precisa de um argumento.");
                        }
                        String key = arguments.get(0).toString();
                        return map.getOrDefault(key, null);
                    }
                };

            // ---------- Modificação ----------
            case "set":
                return new XplCallable() {
                    @Override public int arity() { return 2; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        if (arguments.size() < 2) {
                            throw new RuntimeException("set(key, value) precisa de dois argumentos.");
                        }
                        String key = arguments.get(0).toString();
                        Object value = arguments.get(1);
                        map.put(key, value);
                        return map; // chaining
                    }
                };

            case "remove":
            case "delete":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        if (arguments.isEmpty()) {
                            throw new RuntimeException("remove(key) precisa de um argumento.");
                        }
                        String key = arguments.get(0).toString();
                        return map.remove(key); // retorna o valor removido ou null
                    }
                };

            case "clear":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        map.clear();
                        return null; // void
                    }
                };

            case "clone":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        return new HashMap<>(map);
                    }
                };

            // ---------- Pesquisa recursiva ----------
            case "search":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        if (arguments.isEmpty()) {
                            throw new RuntimeException("search(keyOrValue) precisa de um argumento.");
                        }
                        Object arg = arguments.get(0);
                        return search(map, arg);
                    }
                };

            case "searchByKey":
            case "search_by_key":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        if (arguments.isEmpty()) {
                            throw new RuntimeException("searchByKey(key) precisa de um argumento string.");
                        }
                        String key = arguments.get(0).toString();
                        return searchByKey(map, key);
                    }
                };

            case "searchByValue":
            case "search_by_value":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        if (arguments.isEmpty()) {
                            throw new RuntimeException("searchByValue(value) precisa de um argumento.");
                        }
                        Object value = arguments.get(0);
                        return searchByValue(map, value);
                    }
                };

            case "findPaths":
            case "find_paths":
                return new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        if (arguments.isEmpty()) {
                            throw new RuntimeException("findPaths(keyOrValue) precisa de um argumento.");
                        }
                        Object arg = arguments.get(0);
                        return findPaths(map, arg);
                    }
                };

            // ---------- Transformações ----------
            case "flatten":
                return new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        return flatten(map);
                    }
                };

            case "pick":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        List<String> keys = new ArrayList<>();
                        if (arguments.size() == 1 && arguments.get(0) instanceof List) {
                            // Se for uma lista de chaves
                            for (Object o : (List<?>) arguments.get(0)) {
                                keys.add(o.toString());
                            }
                        } else {
                            for (Object o : arguments) {
                                keys.add(o.toString());
                            }
                        }
                        return pick(map, keys);
                    }
                };

            case "omit":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        List<String> keys = new ArrayList<>();
                        for (Object o : arguments) {
                            keys.add(o.toString());
                        }
                        return omit(map, keys);
                    }
                };

            case "merge":
                return new XplCallable() {
                    @Override public int arity() { return -1; }
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        List<Map<String, Object>> others = new ArrayList<>();
                        for (Object arg : arguments) {
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
                    @Override public Object call(Interpreter interpreter, List<Object> arguments) {
                        if (arguments.isEmpty()) {
                            throw new RuntimeException("has_path(path) precisa de um argumento string.");
                        }
                        String path = arguments.get(0).toString();
                        return hasPath(map, path);
                    }
                };

            default:
                throw new RuntimeException("Método '" + methodName + "' não implementado para objeto.");
        }
    }

    // ==========================================
    // 3. IMPLEMENTAÇÃO DOS MÉTODOS PÚBLICOS (chamados pelos callables)
    // ==========================================

    // ---------- Pesquisa recursiva ----------

    /**
     * Pesquisa por chave OU valor (ambiguidade: se o argumento for String, pesquisa por chave e valor).
     */
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
        // outros tipos (primitivos) não são percorridos
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

            // Percorre até encontrar '.' ou '[' ou fim
            while (end < path.length()) {
                char c = path.charAt(end);
                if (c == '.') {
                    break;
                } else if (c == '[') {
                    isIndex = true;
                    bracketPos = end;
                    // encontra o ']' correspondente
                    int bracketEnd = end + 1;
                    while (bracketEnd < path.length() && path.charAt(bracketEnd) != ']') {
                        bracketEnd++;
                    }
                    if (bracketEnd == path.length()) {
                        return false; // colchete não fechado
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
                // chave simples
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
    // 4. FUNÇÕES AUXILIARES (para compatibilidade com outros métodos)
    // ==========================================

    /**
     * Resolve índices negativos (ex: -1 é o último elemento). Útil para arrays,
     * mas mantido aqui para consistência.
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