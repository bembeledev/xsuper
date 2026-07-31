package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.exceptions.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class UrlNativeModel {

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Url", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false;

        // ─── Url.encode(string) ────────────────────────────────────────────
        model.staticFields.put("encode", buildAction(1, (intp, args) -> {
            String input = getString(intp, args, 0);
            try {
                return URLEncoder.encode(input, StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "Erro ao codificar URL: " + e.getMessage());
            }
        }));

        // ─── Url.decode(string) ────────────────────────────────────────────
        model.staticFields.put("decode", buildAction(1, (intp, args) -> {
            String input = getString(intp, args, 0);
            try {
                return URLDecoder.decode(input, StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "Erro ao descodificar URL: " + e.getMessage());
            }
        }));

        // ─── Url.parse(url) ────────────────────────────────────────────────
        // Devolve um mapa com todos os componentes do URL: scheme, host, port, path, query, fragment
        model.staticFields.put("parse", buildAction(1, (intp, args) -> {
            String url = getString(intp, args, 0);
            try {
                URI uri = new URI(url);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("scheme", uri.getScheme());
                result.put("host", uri.getHost());
                result.put("port", uri.getPort());
                result.put("path", uri.getPath());
                result.put("query", uri.getQuery());
                result.put("fragment", uri.getFragment());
                result.put("raw", uri.toString());
                return result;
            } catch (URISyntaxException e) {
                throw new ControlFlow.RuntimeError(null, "URL inválido: " + e.getMessage());
            }
        }));

        // ─── Url.parseHost(url) ────────────────────────────────────────────
        model.staticFields.put("parseHost", buildAction(1, (intp, args) -> {
            String url = getString(intp, args, 0);
            try {
                URI uri = new URI(url);
                String host = uri.getHost();
                return host != null ? host : "";
            } catch (Exception e) {
                return "";
            }
        }));

        // ─── Url.parseQuery(queryString) ──────────────────────────────────
        // Converte uma query string (ex: "a=1&b=2") num mapa de chave -> lista de valores
        model.staticFields.put("parseQuery", buildAction(1, (intp, args) -> {
            String query = getString(intp, args, 0);
            Map<String, List<String>> params = new LinkedHashMap<>();
            if (query == null || query.isEmpty()) return params;
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                params.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            }
            return params;
        }));

        // ─── Url.buildQuery(params) ────────────────────────────────────────
        // Constrói uma query string a partir de um mapa (chave -> string ou lista de strings)
        model.staticFields.put("buildQuery", buildAction(1, (intp, args) -> {
            Object val = intp.evaluate(args.get(0).expression);
            if (!(val instanceof Map)) {
                throw new ControlFlow.RuntimeError(null, "Url.buildQuery: esperado um mapa de parâmetros.");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) val;
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
                Object value = entry.getValue();
                List<String> values = new ArrayList<>();
                if (value instanceof List) {
                    for (Object v : (List<?>) value) values.add(v.toString());
                } else {
                    values.add(value.toString());
                }
                for (String v : values) {
                    if (sb.length() > 0) sb.append('&');
                    sb.append(key).append('=').append(URLEncoder.encode(v, StandardCharsets.UTF_8));
                }
            }
            return sb.toString();
        }));

        // ─── REGISTAR ──────────────────────────────────────────────────────

        interpreter.registry_model.put("Url", model);
        interpreter.environment.defineConst("Url", new XplClass(model, interpreter.globals));
    }

    // ─── Builders ──────────────────────────────────────────────────────────

    @FunctionalInterface
    private interface NativeAction {
        Object execute(Interpreter interpreter, List<Expr.CallArg> args);
    }

    private static XplCallable buildAction(int arity, NativeAction action) {
        return new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return action.execute(intp, args);
            }
        };
    }

    private static String getString(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        return interpreter.stringify(val);
    }
}