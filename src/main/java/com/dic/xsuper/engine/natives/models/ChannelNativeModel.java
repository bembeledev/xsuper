package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;
import com.dic.xsuper.engine.poo.XplInstance;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ChannelNativeModel {

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Channel", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false;

        // ─── Channel.create() ─────────────────────────────────────────────
        model.staticFields.put("create", buildAction(0, (intp, args) -> {
            XplInstance chan = new XplInstance(null);
            LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<>();

            // .send(valor) -> não‑bloqueante (retorna true se enfileirou)
            chan.fields.put("send", new XplCallable() {
                @Override public int arity() { return 1; }
                @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                    Object val = i.evaluate(a.getFirst().expression);
                    return queue.offer(val);
                }
            });

            // .sendBlocking(valor) -> bloqueante até conseguir enfileirar
            chan.fields.put("sendBlocking", new XplCallable() {
                @Override public int arity() { return 1; }
                @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                    Object val = i.evaluate(a.getFirst().expression);
                    try {
                        queue.put(val);
                        return true;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            });

            // .receive() -> bloqueante (retorna o valor ou null se interrompido)
            chan.fields.put("receive", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                    try {
                        return queue.take();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            });

            // .receiveNonBlock() -> não‑bloqueante (retorna null se vazio)
            chan.fields.put("receiveNonBlock", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                    return queue.poll();
                }
            });

            // .receiveTimeout(timeoutMs) -> espera até timeout
            chan.fields.put("receiveTimeout", new XplCallable() {
                @Override public int arity() { return 1; }
                @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                    long timeout = ((Number) i.evaluate(a.getFirst().expression)).longValue();
                    try {
                        return queue.poll(timeout, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            });

            // .size()
            chan.fields.put("size", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                    return (long) queue.size();
                }
            });

            // .isEmpty()
            chan.fields.put("isEmpty", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                    return queue.isEmpty();
                }
            });

            // .clear()
            chan.fields.put("clear", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                    queue.clear();
                    return null;
                }
            });

            // .remainingCapacity()
            chan.fields.put("remainingCapacity", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                    return (long) queue.remainingCapacity();
                }
            });

            // .close() -> liberta recursos (apenas sinalização, não há fecho real)
            chan.fields.put("close", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                    // Podemos adicionar um flag de fechado se desejar, mas o LinkedBlockingQueue não suporta close nativo.
                    // Aqui apenas limpamos a fila e marcamos como fechado no campo.
                    queue.clear();
                    chan.fields.put("_closed", true);
                    return null;
                }
            });

            // .isClosed()
            chan.fields.put("isClosed", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                    return Boolean.TRUE.equals(chan.fields.get("_closed"));
                }
            });

            return chan;
        }));

        interpreter.registry_model.put("Channel", model);
        interpreter.environment.defineConst("Channel", new XplClass(model, interpreter.globals));
    }

    // ─── Helper ──────────────────────────────────────────────────────────
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