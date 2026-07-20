package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.ControlFlow;
import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;
import com.dic.xsuper.lang.XplFunction;
import com.dic.xsuper.lang.poo.XplInstance;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cliente WebSocket baseado na API java.net.http (Java 11+).
 * Uso: var ws = ws_connect("wss://echo.websocket.org", onMessage, onOpen, onClose, onError)
 *      ws_send(ws, "mensagem")
 *      ws_close(ws)
 */
public class NativeWebSocket {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final AtomicLong wsIdGen = new AtomicLong(1);

    public static void register(Interpreter interpreter) {
        interpreter.globals.defineConst("ws_connect", buildFunc(5, (intp, args) -> {
            String url = getString(intp, args, 0);
            Object onMessageObj = intp.evaluate(args.get(1).expression);
            Object onOpenObj = intp.evaluate(args.get(2).expression);
            Object onCloseObj = intp.evaluate(args.get(3).expression);
            Object onErrorObj = intp.evaluate(args.get(4).expression);

            if (!(onMessageObj instanceof XplFunction)) {
                throw new ControlFlow.RuntimeError(null, "ws_connect: onMessage deve ser uma função (msg) => { ... }");
            }
            XplFunction onMessage = (XplFunction) onMessageObj;
            XplFunction onOpen = (onOpenObj instanceof XplFunction) ? (XplFunction) onOpenObj : null;
            XplFunction onClose = (onCloseObj instanceof XplFunction) ? (XplFunction) onCloseObj : null;
            XplFunction onError = (onErrorObj instanceof XplFunction) ? (XplFunction) onErrorObj : null;

            long wsId = wsIdGen.getAndIncrement();
            // Criar um objeto XPL para representar a conexão
            XplInstance wsInstance = new XplInstance(null);
            wsInstance.fields.put("id", wsId);

            Interpreter taskInterpreter = intp.fork();

            WebSocket.Listener listener = new WebSocket.Listener() {
                @Override
                public void onOpen(WebSocket webSocket) {
                    wsInstance.fields.put("_ws", webSocket);
                    if (onOpen != null) {
                        try {
                            onOpen.call(taskInterpreter, List.of());
                        } catch (Exception e) { /* ignora */ }
                    }
                    WebSocket.Listener.super.onOpen(webSocket);
                }

                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    String msg = data.toString();
                    if (onMessage != null) {
                        try {
                            onMessage.call(taskInterpreter, List.of(
                                    new Expr.CallArg(null, new Expr.Literal(msg))
                            ));
                        } catch (Exception e) { /* ignora */ }
                    }
                    return WebSocket.Listener.super.onText(webSocket, data, last);
                }

                @Override
                public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                    if (onClose != null) {
                        try {
                            onClose.call(taskInterpreter, List.of(
                                    new Expr.CallArg(null, new Expr.Literal(statusCode)),
                                    new Expr.CallArg(null, new Expr.Literal(reason))
                            ));
                        } catch (Exception e) { /* ignora */ }
                    }
                    return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                }

                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    if (onError != null) {
                        try {
                            onError.call(taskInterpreter, List.of(
                                    new Expr.CallArg(null, new Expr.Literal(error.getMessage()))
                            ));
                        } catch (Exception e) { /* ignora */ }
                    }
                    WebSocket.Listener.super.onError(webSocket, error);
                }
            };

            try {
                WebSocket ws = client.newWebSocketBuilder()
                        .buildAsync(URI.create(url), listener)
                        .join();
                wsInstance.fields.put("_ws", ws);
            } catch (Exception e) {
                throw new ControlFlow.RuntimeError(null, "ws_connect: " + e.getMessage());
            }

            // Métodos para enviar e fechar
            wsInstance.fields.put("send", new XplCallable() {
                @Override public int arity() { return 1; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    String text = getString(intp, args, 0);
                    WebSocket w = (WebSocket) wsInstance.fields.get("_ws");
                    if (w == null) throw new ControlFlow.RuntimeError(null, "WebSocket não inicializado");
                    w.sendText(text, true);
                    return null;
                }
            });

            wsInstance.fields.put("close", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    WebSocket w = (WebSocket) wsInstance.fields.get("_ws");
                    if (w != null) w.sendClose(WebSocket.NORMAL_CLOSURE, "close");
                    return null;
                }
            });

            return wsInstance;
        }));

        interpreter.globals.defineConst("ws_send", buildFunc(2, (intp, args) -> {
            Object wsObj = intp.evaluate(args.getFirst().expression);
            String text = getString(intp, args, 1);
            if (!(wsObj instanceof XplInstance)) {
                throw new ControlFlow.RuntimeError(null, "ws_send: primeiro argumento deve ser um objeto WebSocket");
            }
            XplInstance wsInst = (XplInstance) wsObj;
            WebSocket w = (WebSocket) wsInst.fields.get("_ws");
            if (w == null) throw new ControlFlow.RuntimeError(null, "WebSocket não conectado");
            w.sendText(text, true);
            return null;
        }));

        interpreter.globals.defineConst("ws_close", buildFunc(1, (intp, args) -> {
            Object wsObj = intp.evaluate(args.getFirst().expression);
            if (!(wsObj instanceof XplInstance)) {
                throw new ControlFlow.RuntimeError(null, "ws_close: argumento deve ser um objeto WebSocket");
            }
            XplInstance wsInst = (XplInstance) wsObj;
            WebSocket w = (WebSocket) wsInst.fields.get("_ws");
            if (w != null) w.sendClose(WebSocket.NORMAL_CLOSURE, "close");
            return null;
        }));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private static String getString(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        return interpreter.stringify(val);
    }

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