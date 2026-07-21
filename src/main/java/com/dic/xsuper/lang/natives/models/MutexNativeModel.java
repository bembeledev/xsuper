package com.dic.xsuper.lang.natives.models;

import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.poo.XPLModel;
import com.dic.xsuper.lang.poo.XplClass;
import com.dic.xsuper.lang.poo.XplInstance;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;

public class MutexNativeModel {

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Mutex", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false;

        // ─── Mutex.create() ──────────────────────────────────────────────
        model.staticFields.put("create", buildAction(0, (intp, args) -> {
            XplInstance mutexInstance = new XplInstance(null);
            ReentrantLock lock = new ReentrantLock();

            // Mutex.lock()
            mutexInstance.fields.put("lock", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                    lock.lock();
                    return null;
                }
            });

            // Mutex.tryLock()
            mutexInstance.fields.put("tryLock", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                    return lock.tryLock();
                }
            });

            // Mutex.tryLock(timeoutMillis)
            mutexInstance.fields.put("tryLockTimeout", new XplCallable() {
                @Override public int arity() { return 1; }
                @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                    long timeout = ((Number) i.evaluate(a.get(0).expression)).longValue();
                    try {
                        return lock.tryLock(timeout, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            });

            // Mutex.unlock()
            mutexInstance.fields.put("unlock", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                    return null;
                }
            });

            // Mutex.isLocked()
            mutexInstance.fields.put("isLocked", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                    return lock.isLocked();
                }
            });

            // Mutex.isHeldByCurrentThread()
            mutexInstance.fields.put("isHeldByCurrentThread", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                    return lock.isHeldByCurrentThread();
                }
            });

            // Mutex.getQueueLength()
            mutexInstance.fields.put("queueLength", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter i, List<Expr.CallArg> a) {
                    return (long) lock.getQueueLength();
                }
            });

            return mutexInstance;
        }));

        // ─── Registar ──────────────────────────────────────────────────────
        interpreter.registry_model.put("Mutex", model);
        interpreter.environment.defineConst("Mutex", new XplClass(model, interpreter.globals));
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