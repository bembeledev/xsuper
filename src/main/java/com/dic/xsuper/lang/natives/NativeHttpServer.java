package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.ControlFlow;
import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;
import com.dic.xsuper.lang.XplFunction;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP embutido (baseado em com.sun.net.httpserver).
 * Uso: var server = http_serve(port, (request, response) => { ... })
 *      server.stop()
 *      http_stop(server.id)
 */
public class NativeHttpServer {
    private static final Map<Long, HttpServer> servers = new HashMap<>();
    private static long nextId = 1;

    public static void register(Interpreter interpreter) {
        interpreter.globals.defineConst("http_serve", buildFunc(2, (intp, args) -> {
            int port = ((Number) intp.evaluate(args.get(0).expression)).intValue();
            Object handlerObj = intp.evaluate(args.get(1).expression);
            if (!(handlerObj instanceof XplFunction)) {
                throw new ControlFlow.RuntimeError(null, "http_serve: handler deve ser uma função (request, response) => { ... }");
            }
            XplFunction handler = (XplFunction) handlerObj;

            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
                server.createContext("/", new XplHttpHandler(handler, intp.fork()));
                server.setExecutor(Executors.newCachedThreadPool());
                server.start();

                long id = nextId++;
                servers.put(id, server);

                Map<String, Object> result = new HashMap<>();
                result.put("id", id);
                result.put("port", port);
                result.put("stop", new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                        server.stop(0);
                        servers.remove(id);
                        return null;
                    }
                });
                return result;
            } catch (IOException e) {
                throw new ControlFlow.RuntimeError(null, "http_serve: " + e.getMessage());
            }
        }));

        interpreter.globals.defineConst("http_stop", buildFunc(1, (intp, args) -> {
            long id = ((Number) intp.evaluate(args.get(0).expression)).longValue();
            HttpServer server = servers.remove(id);
            if (server != null) {
                server.stop(0);
                return true;
            }
            return false;
        }));
    }

    private static class XplHttpHandler implements HttpHandler {
        private final XplFunction handler;
        private final Interpreter interpreter;

        public XplHttpHandler(XplFunction handler, Interpreter interpreter) {
            this.handler = handler;
            this.interpreter = interpreter;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                // Construir objeto request
                Map<String, Object> request = new HashMap<>();
                request.put("method", exchange.getRequestMethod());
                request.put("path", exchange.getRequestURI().getPath());
                request.put("query", exchange.getRequestURI().getQuery());
                request.put("headers", new HashMap<>(exchange.getRequestHeaders()));
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                request.put("body", body);

                // Criar objeto response (será preenchido pelo handler)
                Map<String, Object> response = new HashMap<>();
                response.put("status", 200);
                response.put("headers", new HashMap<String, String>());
                response.put("body", "");

                // Chamar a função XPL
                List<Expr.CallArg> args = List.of(
                        new Expr.CallArg(null, new Expr.Literal(request)),
                        new Expr.CallArg(null, new Expr.Literal(response))
                );
                handler.call(interpreter, args);

                // Enviar resposta
                int status = ((Number) response.getOrDefault("status", 200)).intValue();
                @SuppressWarnings("unchecked")
                Map<String, String> headers = (Map<String, String>) response.getOrDefault("headers", new HashMap<>());
                String responseBody = response.getOrDefault("body", "").toString();

                // Headers padrão
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    exchange.getResponseHeaders().set(entry.getKey(), entry.getValue());
                }
                byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                exchange.sendResponseHeaders(500, 0);
                exchange.getResponseBody().close();
            }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    @FunctionalInterface
    private interface NativeAction {
        Object execute(Interpreter interpreter, List<Expr.CallArg> args);
    }

    private static XplCallable buildFunc(int arity, NativeAction action) {
        return new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                return action.execute(intp, args);
            }
        };
    }
}