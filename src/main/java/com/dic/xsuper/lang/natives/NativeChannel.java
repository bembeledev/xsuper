package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;
import com.dic.xsuper.lang.poo.XplInstance;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.List;

public class NativeChannel {
    public static void register(Interpreter interpreter) {

        // channel_create() -> Retorna um objeto thread-safe com buffer bloqueante
        interpreter.globals.defineConst("channel_create", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<com.dic.xsuper.lang.Expr.CallArg> args) {
                XplInstance chan = new XplInstance(null);
                LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<>();

                // .send(valor) -> Envia um dado para a fila (Não-bloqueante para quem envia)
                chan.fields.put("send", new XplCallable() {
                    @Override public int arity() { return 1; }
                    @Override public Object call(Interpreter i, List<com.dic.xsuper.lang.Expr.CallArg> a) {
                        Object val = i.evaluate(a.get(0).expression);
                        queue.offer(val);
                        return true;
                    }
                });

                // .receive() -> Bloqueia a thread atual até que um dado caia no canal (Thread-Safe)
                chan.fields.put("receive", new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter i, List<com.dic.xsuper.lang.Expr.CallArg> a) {
                        try {
                            return queue.take(); // Bloqueio nativo ultra-eficiente
                        } catch (InterruptedException e) { return null; }
                    }
                });

                return chan;
            }
        });
    }
}