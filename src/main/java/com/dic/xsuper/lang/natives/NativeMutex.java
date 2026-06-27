package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;
import com.dic.xsuper.lang.poo.XplInstance;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;

public class NativeMutex {
    public static void register(Interpreter interpreter) {

        // mutex_create() -> Cria uma instância de tranca isolada
        interpreter.globals.defineConst("mutex_create", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<com.dic.xsuper.lang.Expr.CallArg> args) {
                XplInstance mutexInstance = new XplInstance(null);
                ReentrantLock lock = new ReentrantLock();

                // Método .lock()
                mutexInstance.fields.put("lock", new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter i, List<com.dic.xsuper.lang.Expr.CallArg> a) {
                        lock.lock();
                        return null;
                    }
                });

                // Método .unlock()
                mutexInstance.fields.put("unlock", new XplCallable() {
                    @Override public int arity() { return 0; }
                    @Override public Object call(Interpreter i, List<com.dic.xsuper.lang.Expr.CallArg> a) {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                        return null;
                    }
                });

                return mutexInstance;
            }
        });
    }
}