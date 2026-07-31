package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.ast.Stmt;
import com.dic.xsuper.engine.ast.TypeNode;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.core.Token;
import com.dic.xsuper.engine.core.TokenType;
import com.dic.xsuper.engine.exceptions.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.execution.XplFunction;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;
import com.dic.xsuper.engine.poo.XplInstance;
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

public class HttpServerNativeModel {

    // Registo interno para gerir os servidores ativos
    private static final Map<Long, HttpServer> servers = new HashMap<>();
    private static long nextId = 1;

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("HttpServer", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false; // Modelo Estático

        Token pubToken = new Token(TokenType.PUBLIC, "pub", null, 0, 0);

        // Campos (readonly) que a instância do servidor irá possuir
        model.addField(new Stmt.FieldDecl(pubToken, false, false, true, new Token(TokenType.IDENTIFIER, "id", null, 0, 0), new TypeNode.Simple(new Token(TokenType.T_INT, "int", null, 0, 0))));
        model.addField(new Stmt.FieldDecl(pubToken, false, false, true, new Token(TokenType.IDENTIFIER, "port", null, 0, 0), new TypeNode.Simple(new Token(TokenType.T_INT, "int", null, 0, 0))));

        XplClass serverClass = new XplClass(model, interpreter.globals);

        // =========================================================
        // HttpServer.serve(port, (request, response) => { ... })
        // =========================================================
        model.staticFields.put("serve", buildFunc(2, (intp, args) -> {
            int port = ((Number) intp.evaluate(args.get(0).expression)).intValue();
            Object handlerObj = intp.evaluate(args.get(1).expression);

            if (!(handlerObj instanceof XplFunction handler)) {
                throw new ControlFlow.RuntimeError(null, "HttpServer.serve: o handler deve ser uma função (request, response) => { ... }");
            }

            try {
                // Criação do servidor nativo
                HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
                server.createContext("/", new XplHttpHandler(handler, intp.fork()));
                server.setExecutor(Executors.newCachedThreadPool());
                server.start();

                long id = nextId++;
                servers.put(id, server);

                // Em vez de devolver um Dicionário, devolvemos uma verdadeira Instância XPL!
                XplInstance instance = new XplInstance(serverClass);
                instance.fields.put("id", id);
                instance.fields.put("port", (long) port);

                // Método .stop() acoplado à instância
                instance.fields.put("stop", new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                        server.stop(0);
                        servers.remove(id);
                        return null;
                    }
                });

                return instance;
            } catch (IOException e) {
                throw new ControlFlow.RuntimeError(null, "Falha ao iniciar HttpServer: " + e.getMessage());
            }
        }));

        // =========================================================
        // HttpServer.stop(id) -> Útil caso o programador perca a variável da instância
        // =========================================================
        model.staticFields.put("stop", buildFunc(1, (intp, args) -> {
            long id = ((Number) intp.evaluate(args.getFirst().expression)).longValue();
            HttpServer server = servers.remove(id);
            if (server != null) {
                server.stop(0);
                return true;
            }
            return false;
        }));

        interpreter.registry_model.put("HttpServer", model);
        interpreter.environment.defineConst("HttpServer", serverClass);
    }

    // =========================================================
    // HANDLER INTERNO (Mantido intacto da tua lógica original)
    // =========================================================
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
            }catch (Exception e) {
                // ⭐ AGORA O JAVA VAI GRITAR O ERRO DO XPL NO TERMINAL! ⭐
                System.err.println("\n🔥 [HttpServer] ERRO CRÍTICO NA ROTA " + exchange.getRequestURI().getPath() + " 🔥");
                if (e instanceof ControlFlow.RuntimeError) {
                    System.err.println(((ControlFlow.RuntimeError) e).getMessage());
                } else {
                    e.printStackTrace();
                }

                // Devolve o 500 com a mensagem de erro para o browser também!
                String errorMsg = "Internal Server Error: " + e.getMessage();
                byte[] errorBytes = errorMsg.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, errorBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorBytes);
                }
                exchange.getResponseBody().close();
            }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    @FunctionalInterface
    private interface NativeAction { Object execute(Interpreter interpreter, List<Expr.CallArg> args); }

    private static XplCallable buildFunc(int arity, NativeAction action) {
        return new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) { return action.execute(intp, args); }
        };
    }
}