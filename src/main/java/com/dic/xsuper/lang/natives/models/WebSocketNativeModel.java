package com.dic.xsuper.lang.natives.models;

import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.poo.XPLModel;
import com.dic.xsuper.lang.poo.XplClass;
import com.dic.xsuper.lang.poo.XplInstance;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class WebSocketNativeModel {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final AtomicLong wsIdGen = new AtomicLong(1);

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("WebSocket", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false;

        // ─── WebSocket.connect(url, [options]) ──────────────────────────────
        model.staticFields.put("connect", buildAction(-1, (intp, args) -> {
            if (args.isEmpty()) {
                throw new ControlFlow.RuntimeError(null, "WebSocket.connect: precisa da URL.");
            }
            String url = getString(intp, args, 0);
            Map<String, Object> opts = args.size() > 1 ? getMap(intp, args, 1) : new java.util.HashMap<>();

            // Extrair callbacks das opções
            XplFunction onMessage = extractCallback(opts, "onMessage");
            XplFunction onOpen = extractCallback(opts, "onOpen");
            XplFunction onClose = extractCallback(opts, "onClose");
            XplFunction onError = extractCallback(opts, "onError");
            boolean binary = opts.containsKey("binary") && (boolean) opts.get("binary");

            // Criar handle XPL
            XplInstance wsInstance = new XplInstance(null);
            long wsId = wsIdGen.getAndIncrement();
            wsInstance.fields.put("id", wsId);
            wsInstance.fields.put("_closed", false);
            wsInstance.fields.put("_ws", null);
            wsInstance.fields.put("_binary", binary);

            Interpreter taskInterpreter = intp.fork();

            WebSocket.Listener listener = new WebSocket.Listener() {
                @Override
                public void onOpen(WebSocket webSocket) {
                    wsInstance.fields.put("_ws", webSocket);
                    wsInstance.fields.put("_closed", false);
                    if (onOpen != null) {
                        try {
                            onOpen.call(taskInterpreter, List.of());
                        } catch (Exception e) {
                            // ignora
                        }
                    }
                    WebSocket.Listener.super.onOpen(webSocket);
                }

                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    if (onMessage != null) {
                        String msg = data.toString();
                        try {
                            onMessage.call(taskInterpreter, List.of(
                                    new Expr.CallArg(null, new Expr.Literal(msg))
                            ));
                        } catch (Exception e) {
                            // ignora
                        }
                    }
                    return WebSocket.Listener.super.onText(webSocket, data, last);
                }

                @Override
                public CompletionStage<?> onBinary(WebSocket webSocket, java.nio.ByteBuffer data, boolean last) {
                    if (onMessage != null && binary) {
                        byte[] bytes = new byte[data.remaining()];
                        data.get(bytes);
                        String base64 = Base64.getEncoder().encodeToString(bytes);
                        try {
                            onMessage.call(taskInterpreter, List.of(
                                    new Expr.CallArg(null, new Expr.Literal(base64))
                            ));
                        } catch (Exception e) {
                            // ignora
                        }
                    }
                    return WebSocket.Listener.super.onBinary(webSocket, data, last);
                }

                @Override
                public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                    wsInstance.fields.put("_closed", true);
                    wsInstance.fields.put("_ws", null);
                    if (onClose != null) {
                        try {
                            onClose.call(taskInterpreter, List.of(
                                    new Expr.CallArg(null, new Expr.Literal((long) statusCode)),
                                    new Expr.CallArg(null, new Expr.Literal(reason))
                            ));
                        } catch (Exception e) {
                            // ignora
                        }
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
                        } catch (Exception e) {
                            // ignora
                        }
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
                throw new ControlFlow.RuntimeError(null, "WebSocket.connect: " + e.getMessage());
            }

            // ─── Métodos do handle ────────────────────────────────────────

            wsInstance.fields.put("send", new XplCallable() {
                @Override public int arity() { return 1; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    String text = getString(intp, args, 0);
                    WebSocket w = (WebSocket) wsInstance.fields.get("_ws");
                    if (w == null || Boolean.TRUE.equals(wsInstance.fields.get("_closed"))) {
                        throw new ControlFlow.RuntimeError(null, "WebSocket não está conectado.");
                    }
                    w.sendText(text, true);
                    return null;
                }
            });

            wsInstance.fields.put("sendBytes", new XplCallable() {
                @Override public int arity() { return 1; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    String base64 = getString(intp, args, 0);
                    byte[] bytes = Base64.getDecoder().decode(base64);
                    WebSocket w = (WebSocket) wsInstance.fields.get("_ws");
                    if (w == null || Boolean.TRUE.equals(wsInstance.fields.get("_closed"))) {
                        throw new ControlFlow.RuntimeError(null, "WebSocket não está conectado.");
                    }
                    w.sendBinary(java.nio.ByteBuffer.wrap(bytes), true);
                    return null;
                }
            });

            wsInstance.fields.put("close", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    WebSocket w = (WebSocket) wsInstance.fields.get("_ws");
                    if (w != null) {
                        w.sendClose(WebSocket.NORMAL_CLOSURE, "close");
                    }
                    wsInstance.fields.put("_closed", true);
                    wsInstance.fields.put("_ws", null);
                    return null;
                }
            });

            // ─── Propriedades ──────────────────────────────────────────────

            wsInstance.fields.put("isOpen", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    return !Boolean.TRUE.equals(wsInstance.fields.get("_closed")) &&
                            wsInstance.fields.get("_ws") != null;
                }
            });

            wsInstance.fields.put("id", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    return wsId;
                }
            });

            return wsInstance;
        }));

        // ─── REGISTAR ──────────────────────────────────────────────────────

        interpreter.registry_model.put("WebSocket", model);
        interpreter.environment.defineConst("WebSocket", new XplClass(model, interpreter.globals));
    }

    // ─── AUXILIARES ──────────────────────────────────────────────────────────

    private static XplFunction extractCallback(Map<String, Object> opts, String key) {
        Object cb = opts.get(key);
        if (cb instanceof XplFunction) return (XplFunction) cb;
        if (cb != null) {
            throw new ControlFlow.RuntimeError(null,
                    "WebSocket.connect: a opção '" + key + "' deve ser uma função.");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMap(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        if (val instanceof Map) return (Map<String, Object>) val;
        throw new ControlFlow.RuntimeError(null, "WebSocket.connect: opções devem ser um dicionário.");
    }

    private static String getString(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        return interpreter.stringify(val);
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
}