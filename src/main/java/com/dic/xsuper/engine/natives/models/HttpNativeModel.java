package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.exceptions.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Módulo HTTP para a linguagem XPL.
 * <p>
 * Uso:
 *   var response = Http.get("https://api.exemplo.com")
 *   var result = Http.request("https://api.exemplo.com", HTTP.POST, '{"name":"XPL"}')
 *   var custom = Http.request("https://api.exemplo.com", HTTP.PUT, '{"id":1}', { headers: {"X-Token": "abc"} })
 * <p>
 * Os métodos disponíveis:
 *   Http.get(url) -> body (String)
 *   Http.post(url, body) -> response object
 *   Http.put(url, body) -> response object
 *   Http.delete(url) -> response object
 *   Http.patch(url, body) -> response object
 *   Http.head(url) -> response object
 *   Http.options(url) -> response object
 *   Http.request(url, method, body, options) -> response object
 */
public class HttpNativeModel {

    // Cliente HTTP global com timeout e redirecionamento
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Http", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false;

        // ─── MÉTODO get(url) ─────────────────────────────────────────────
        model.staticFields.put("get", buildCallable(1, (intp, args) -> {
            String url = getString(intp, args, 0);
            return sendRequest(intp, url, "GET", null, null);
        }));

        // ─── MÉTODO post(url, body) ─────────────────────────────────────
        model.staticFields.put("post", buildCallable(2, (intp, args) -> {
            String url = getString(intp, args, 0);
            Object body = intp.evaluate(args.get(1).expression);
            return sendRequest(intp, url, "POST", body, null);
        }));

        // ─── MÉTODO put(url, body) ──────────────────────────────────────
        model.staticFields.put("put", buildCallable(2, (intp, args) -> {
            String url = getString(intp, args, 0);
            Object body = intp.evaluate(args.get(1).expression);
            return sendRequest(intp, url, "PUT", body, null);
        }));

        // ─── MÉTODO delete(url) ──────────────────────────────────────────
        model.staticFields.put("delete", buildCallable(1, (intp, args) -> {
            String url = getString(intp, args, 0);
            return sendRequest(intp, url, "DELETE", null, null);
        }));

        // ─── MÉTODO patch(url, body) ─────────────────────────────────────
        model.staticFields.put("patch", buildCallable(2, (intp, args) -> {
            String url = getString(intp, args, 0);
            Object body = intp.evaluate(args.get(1).expression);
            return sendRequest(intp, url, "PATCH", body, null);
        }));

        // ─── MÉTODO head(url) ────────────────────────────────────────────
        model.staticFields.put("head", buildCallable(1, (intp, args) -> {
            String url = getString(intp, args, 0);
            return sendRequest(intp, url, "HEAD", null, null);
        }));

        // ─── MÉTODO options(url) ─────────────────────────────────────────
        model.staticFields.put("options", buildCallable(1, (intp, args) -> {
            String url = getString(intp, args, 0);
            return sendRequest(intp, url, "OPTIONS", null, null);
        }));

        // ─── MÉTODO request(url, method, body, options) ────────────────
        model.staticFields.put("request", buildCallable(4, (intp, args) -> {
            String url = getString(intp, args, 0);
            // O método pode ser HTTP.GET, HTTP.POST, etc. (String)
            Object methodObj = intp.evaluate(args.get(1).expression);
            String method = intp.stringify(methodObj).toUpperCase();

            Object body = intp.evaluate(args.get(2).expression);
            Map<String, Object> options = getOptions(intp, args, 3);

            return sendRequest(intp, url, method, body, options);
        }));

        // =========================================================
        // ⭐ CONSTANTES DO MODELO HTTP ⭐
        // =========================================================

        // 1. Métodos HTTP (RFC 7231 + extensões comuns)
        java.util.Map<String, String> httpMethods = new java.util.LinkedHashMap<>();
        httpMethods.put("GET", "GET");
        httpMethods.put("POST", "POST");
        httpMethods.put("PUT", "PUT");
        httpMethods.put("DELETE", "DELETE");
        httpMethods.put("PATCH", "PATCH");
        httpMethods.put("HEAD", "HEAD");
        httpMethods.put("OPTIONS", "OPTIONS");
        httpMethods.put("TRACE", "TRACE");
        httpMethods.put("CONNECT", "CONNECT");
        model.staticFields.put("METHODS", java.util.Collections.unmodifiableMap(httpMethods));

        // 2. Códigos de Status HTTP (categorizados)
        java.util.Map<String, Object> httpStatus = new java.util.LinkedHashMap<>();

        // 1xx – Informação
        java.util.Map<String, Integer> informational = new java.util.LinkedHashMap<>();
        informational.put("CONTINUE", 100);
        informational.put("SWITCHING_PROTOCOLS", 101);
        informational.put("PROCESSING", 102);
        informational.put("EARLY_HINTS", 103);
        httpStatus.put("INFORMATIONAL", java.util.Collections.unmodifiableMap(informational));

        // 2xx – Sucesso
        java.util.Map<String, Integer> success = new java.util.LinkedHashMap<>();
        success.put("OK", 200);
        success.put("CREATED", 201);
        success.put("ACCEPTED", 202);
        success.put("NON_AUTHORITATIVE_INFORMATION", 203);
        success.put("NO_CONTENT", 204);
        success.put("RESET_CONTENT", 205);
        success.put("PARTIAL_CONTENT", 206);
        success.put("MULTI_STATUS", 207);
        success.put("ALREADY_REPORTED", 208);
        success.put("IM_USED", 226);
        httpStatus.put("SUCCESS", java.util.Collections.unmodifiableMap(success));

        // 3xx – Redirecionamento
        java.util.Map<String, Integer> redirection = new java.util.LinkedHashMap<>();
        redirection.put("MULTIPLE_CHOICES", 300);
        redirection.put("MOVED_PERMANENTLY", 301);
        redirection.put("FOUND", 302);
        redirection.put("SEE_OTHER", 303);
        redirection.put("NOT_MODIFIED", 304);
        redirection.put("USE_PROXY", 305);
        redirection.put("SWITCH_PROXY", 306);
        redirection.put("TEMPORARY_REDIRECT", 307);
        redirection.put("PERMANENT_REDIRECT", 308);
        httpStatus.put("REDIRECTION", java.util.Collections.unmodifiableMap(redirection));

        // 4xx – Erro do cliente
        java.util.Map<String, Integer> clientError = new java.util.LinkedHashMap<>();
        clientError.put("BAD_REQUEST", 400);
        clientError.put("UNAUTHORIZED", 401);
        clientError.put("PAYMENT_REQUIRED", 402);
        clientError.put("FORBIDDEN", 403);
        clientError.put("NOT_FOUND", 404);
        clientError.put("METHOD_NOT_ALLOWED", 405);
        clientError.put("NOT_ACCEPTABLE", 406);
        clientError.put("PROXY_AUTHENTICATION_REQUIRED", 407);
        clientError.put("REQUEST_TIMEOUT", 408);
        clientError.put("CONFLICT", 409);
        clientError.put("GONE", 410);
        clientError.put("LENGTH_REQUIRED", 411);
        clientError.put("PRECONDITION_FAILED", 412);
        clientError.put("PAYLOAD_TOO_LARGE", 413);
        clientError.put("URI_TOO_LONG", 414);
        clientError.put("UNSUPPORTED_MEDIA_TYPE", 415);
        clientError.put("RANGE_NOT_SATISFIABLE", 416);
        clientError.put("EXPECTATION_FAILED", 417);
        clientError.put("IM_A_TEAPOT", 418);
        clientError.put("MISDIRECTED_REQUEST", 421);
        clientError.put("UNPROCESSABLE_ENTITY", 422);
        clientError.put("LOCKED", 423);
        clientError.put("FAILED_DEPENDENCY", 424);
        clientError.put("TOO_EARLY", 425);
        clientError.put("UPGRADE_REQUIRED", 426);
        clientError.put("PRECONDITION_REQUIRED", 428);
        clientError.put("TOO_MANY_REQUESTS", 429);
        clientError.put("REQUEST_HEADER_FIELDS_TOO_LARGE", 431);
        clientError.put("UNAVAILABLE_FOR_LEGAL_REASONS", 451);
        httpStatus.put("CLIENT_ERROR", java.util.Collections.unmodifiableMap(clientError));

        // 5xx – Erro do servidor
        java.util.Map<String, Integer> serverError = new java.util.LinkedHashMap<>();
        serverError.put("INTERNAL_SERVER_ERROR", 500);
        serverError.put("NOT_IMPLEMENTED", 501);
        serverError.put("BAD_GATEWAY", 502);
        serverError.put("SERVICE_UNAVAILABLE", 503);
        serverError.put("GATEWAY_TIMEOUT", 504);
        serverError.put("HTTP_VERSION_NOT_SUPPORTED", 505);
        serverError.put("VARIANT_ALSO_NEGOTIATES", 506);
        serverError.put("INSUFFICIENT_STORAGE", 507);
        serverError.put("LOOP_DETECTED", 508);
        serverError.put("NOT_EXTENDED", 510);
        serverError.put("NETWORK_AUTHENTICATION_REQUIRED", 511);
        httpStatus.put("SERVER_ERROR", java.util.Collections.unmodifiableMap(serverError));

        model.staticFields.put("STATUS", java.util.Collections.unmodifiableMap(httpStatus));

        // 3. Cabeçalhos HTTP comuns
        java.util.Map<String, String> httpHeaders = new java.util.LinkedHashMap<>();
        httpHeaders.put("ACCEPT", "Accept");
        httpHeaders.put("ACCEPT_CHARSET", "Accept-Charset");
        httpHeaders.put("ACCEPT_ENCODING", "Accept-Encoding");
        httpHeaders.put("ACCEPT_LANGUAGE", "Accept-Language");
        httpHeaders.put("ACCEPT_RANGES", "Accept-Ranges");
        httpHeaders.put("AGE", "Age");
        httpHeaders.put("ALLOW", "Allow");
        httpHeaders.put("AUTHORIZATION", "Authorization");
        httpHeaders.put("CACHE_CONTROL", "Cache-Control");
        httpHeaders.put("CONNECTION", "Connection");
        httpHeaders.put("CONTENT_ENCODING", "Content-Encoding");
        httpHeaders.put("CONTENT_LANGUAGE", "Content-Language");
        httpHeaders.put("CONTENT_LENGTH", "Content-Length");
        httpHeaders.put("CONTENT_LOCATION", "Content-Location");
        httpHeaders.put("CONTENT_MD5", "Content-MD5");
        httpHeaders.put("CONTENT_RANGE", "Content-Range");
        httpHeaders.put("CONTENT_TYPE", "Content-Type");
        httpHeaders.put("COOKIE", "Cookie");
        httpHeaders.put("DATE", "Date");
        httpHeaders.put("ETAG", "ETag");
        httpHeaders.put("EXPECT", "Expect");
        httpHeaders.put("EXPIRES", "Expires");
        httpHeaders.put("FROM", "From");
        httpHeaders.put("HOST", "Host");
        httpHeaders.put("IF_MATCH", "If-Match");
        httpHeaders.put("IF_MODIFIED_SINCE", "If-Modified-Since");
        httpHeaders.put("IF_NONE_MATCH", "If-None-Match");
        httpHeaders.put("IF_RANGE", "If-Range");
        httpHeaders.put("IF_UNMODIFIED_SINCE", "If-Unmodified-Since");
        httpHeaders.put("LAST_MODIFIED", "Last-Modified");
        httpHeaders.put("LOCATION", "Location");
        httpHeaders.put("MAX_FORWARDS", "Max-Forwards");
        httpHeaders.put("ORIGIN", "Origin");
        httpHeaders.put("PRAGMA", "Pragma");
        httpHeaders.put("PROXY_AUTHENTICATE", "Proxy-Authenticate");
        httpHeaders.put("PROXY_AUTHORIZATION", "Proxy-Authorization");
        httpHeaders.put("RANGE", "Range");
        httpHeaders.put("REFERER", "Referer");
        httpHeaders.put("RETRY_AFTER", "Retry-After");
        httpHeaders.put("SERVER", "Server");
        httpHeaders.put("SET_COOKIE", "Set-Cookie");
        httpHeaders.put("TE", "TE");
        httpHeaders.put("TRAILER", "Trailer");
        httpHeaders.put("TRANSFER_ENCODING", "Transfer-Encoding");
        httpHeaders.put("UPGRADE", "Upgrade");
        httpHeaders.put("USER_AGENT", "User-Agent");
        httpHeaders.put("VARY", "Vary");
        httpHeaders.put("VIA", "Via");
        httpHeaders.put("WARNING", "Warning");
        httpHeaders.put("WWW_AUTHENTICATE", "WWW-Authenticate");
        model.staticFields.put("HEADERS", java.util.Collections.unmodifiableMap(httpHeaders));

        // 4. MIME Types comuns
        java.util.Map<String, String> mimeTypes = new java.util.LinkedHashMap<>();
        mimeTypes.put("JSON", "application/json");
        mimeTypes.put("XML", "application/xml");
        mimeTypes.put("HTML", "text/html");
        mimeTypes.put("TEXT", "text/plain");
        mimeTypes.put("CSS", "text/css");
        mimeTypes.put("JS", "text/javascript");
        mimeTypes.put("PDF", "application/pdf");
        mimeTypes.put("ZIP", "application/zip");
        mimeTypes.put("FORM", "application/x-www-form-urlencoded");
        mimeTypes.put("MULTIPART_FORM", "multipart/form-data");
        mimeTypes.put("OCTET_STREAM", "application/octet-stream");
        mimeTypes.put("PNG", "image/png");
        mimeTypes.put("JPEG", "image/jpeg");
        mimeTypes.put("GIF", "image/gif");
        mimeTypes.put("SVG", "image/svg+xml");
        mimeTypes.put("WEBP", "image/webp");
        mimeTypes.put("AVIF", "image/avif");
        mimeTypes.put("MP4", "video/mp4");
        mimeTypes.put("MP3", "audio/mpeg");
        mimeTypes.put("WAV", "audio/wav");
        model.staticFields.put("MIME_TYPES", java.util.Collections.unmodifiableMap(mimeTypes));

        // Registar a classe Http
        interpreter.registry_model.put("Http", model);
        interpreter.environment.defineConst("Http", new XplClass(model, interpreter.globals));
    }

    // ─── Motor de requisição ──────────────────────────────────────────────

    private static Object sendRequest(Interpreter interpreter, String url, String method, Object body, Map<String, Object> options) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url));

            // Define o método e o corpo
            if (body != null && !body.toString().isEmpty()) {
                String bodyStr = interpreter.stringify(body);
                builder.method(method, HttpRequest.BodyPublishers.ofString(bodyStr));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            // Headers padrão (JSON)
            builder.header("Content-Type", "application/json");
            builder.header("Accept", "application/json");

            // Headers adicionais vindos das options
            if (options != null && options.containsKey("headers")) {
                Object headersObj = options.get("headers");
                if (headersObj instanceof Map<?, ?> headers) {
                    for (Map.Entry<?, ?> entry : headers.entrySet()) {
                        builder.header(entry.getKey().toString(), entry.getValue().toString());
                    }
                }
            }

            // Timeout personalizado
            if (options != null && options.containsKey("timeout")) {
                long timeoutMs = ((Number) options.get("timeout")).longValue();
                builder.timeout(Duration.ofMillis(timeoutMs));
            }

            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            // Construir o objeto de resposta
            Map<String, Object> responseObj = new HashMap<>();
            responseObj.put("status", (long) response.statusCode());
            responseObj.put("body", response.body());
            responseObj.put("headers", new HashMap<>(response.headers().map())); // headers como Map<String, List<String>>
            responseObj.put("ok", response.statusCode() >= 200 && response.statusCode() < 300);

            return responseObj;

        } catch (Exception e) {
            throw new ControlFlow.RuntimeError(null, "Erro HTTP " + method + " " + url + ": " + e.getMessage());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    @FunctionalInterface
    private interface NativeAction {
        Object execute(Interpreter interpreter, List<Expr.CallArg> args);
    }

    private static XplCallable buildCallable(int arity, NativeAction action) {
        return new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return action.execute(intp, args);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getOptions(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        if (index >= args.size()) return null;
        Object val = interpreter.evaluate(args.get(index).expression);
        if (val instanceof Map) return (Map<String, Object>) val;
        return null;
    }

    private static String getString(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        return interpreter.stringify(val);
    }
}