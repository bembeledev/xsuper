package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.exceptions.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.execution.XplFunction;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class EventLoopNativeModel {

    // ─── Filas de tarefas ───────────────────────────────────────────────────
    private static final ConcurrentLinkedQueue<Runnable> nextTickQueue = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Runnable> macroTaskQueue = new ConcurrentLinkedQueue<>();

    // ─── Executor para tarefas agendadas (setTimeout / setInterval) ──────
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4, r -> {
        Thread t = new Thread(r, "EventLoop-Scheduler");
        t.setDaemon(true);
        return t;
    });

    // ─── Registo de tarefas agendadas (para cancelamento) ────────────────
    private static final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private static final AtomicLong taskIdGenerator = new AtomicLong(1);

    // ─── Flag para controlar a execução do loop (para shutdown) ──────────
    private static volatile boolean running = true;

    // ─── Registar o módulo ──────────────────────────────────────────────────
    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("EventLoop", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false;

        // ─── EventLoop.nextTick(callback) ──────────────────────────────
        model.staticFields.put("nextTick", buildCallable(1, (intp, args) -> {
            XplFunction task = extractFunction(intp, args, 0);
            Interpreter taskInterpreter = intp.fork();
            nextTickQueue.offer(() -> safeRun(taskInterpreter, task, "[nextTick]"));
            return true;
        }));

        // ─── EventLoop.defer(callback) ──────────────────────────────────
        model.staticFields.put("defer", buildCallable(1, (intp, args) -> {
            XplFunction task = extractFunction(intp, args, 0);
            Interpreter taskInterpreter = intp.fork();
            macroTaskQueue.offer(() -> safeRun(taskInterpreter, task, "[defer]"));
            return true;
        }));

        // ─── EventLoop.setTimeout(callback, delayMs) ────────────────────
        model.staticFields.put("setTimeout", buildCallable(2, (intp, args) -> {
            XplFunction task = extractFunction(intp, args, 0);
            long delay = extractDelay(intp, args, 1);
            Interpreter taskInterpreter = intp.fork();

            long id = taskIdGenerator.getAndIncrement();
            ScheduledFuture<?> future = scheduler.schedule(() -> {
                safeRun(taskInterpreter, task, "[setTimeout #" + id + "]");
                scheduledTasks.remove(id);
            }, Math.max(delay, 0), TimeUnit.MILLISECONDS);

            scheduledTasks.put(id, future);
            return id;
        }));

        // ─── EventLoop.clearTimeout(id) ──────────────────────────────────
        model.staticFields.put("clearTimeout", buildCallable(1, (intp, args) -> {
            long id = extractId(intp, args, 0);
            ScheduledFuture<?> future = scheduledTasks.remove(id);
            if (future != null) future.cancel(false);
            return true;
        }));

        // ─── EventLoop.setInterval(callback, intervalMs) ─────────────────
        model.staticFields.put("setInterval", buildCallable(2, (intp, args) -> {
            XplFunction task = extractFunction(intp, args, 0);
            long interval = extractDelay(intp, args, 1);
            if (interval < 0) interval = 0;
            Interpreter taskInterpreter = intp.fork();

            long id = taskIdGenerator.getAndIncrement();
            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
                safeRun(taskInterpreter, task, "[setInterval #" + id + "]");
            }, interval, Math.max(interval, 1), TimeUnit.MILLISECONDS);

            scheduledTasks.put(id, future);
            return id;
        }));

        // ─── EventLoop.clearInterval(id) ──────────────────────────────────
        model.staticFields.put("clearInterval", buildCallable(1, (intp, args) -> {
            long id = extractId(intp, args, 0);
            ScheduledFuture<?> future = scheduledTasks.remove(id);
            if (future != null) future.cancel(false);
            return true;
        }));

        // ─── EventLoop.runMicrotasks() ────────────────────────────────────
        model.staticFields.put("runMicrotasks", buildCallable(0, (intp, args) -> {
            runMicrotasks();
            return null;
        }));

        // ─── EventLoop.runMacrotasks() ────────────────────────────────────
        model.staticFields.put("runMacrotasks", buildCallable(0, (intp, args) -> {
            runMacrotasks();
            return null;
        }));

        // ─── EventLoop.runAllTasks() ──────────────────────────────────────
        model.staticFields.put("runTick", buildCallable(0, (intp, args) -> {
            runAllTasks();
            return null;
        }));

        // ─── EventLoop.pendingCount() ─────────────────────────────────────
        model.staticFields.put("pendingCount", buildCallable(0, (intp, args) -> {
            return (long) (nextTickQueue.size() + macroTaskQueue.size() + scheduledTasks.size());
        }));

        // ─── EventLoop.shutdown() ──────────────────────────────────────────
        model.staticFields.put("shutdown", buildCallable(0, (intp, args) -> {
            running = false;
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        }));

        interpreter.registry_model.put("EventLoop", model);
        interpreter.environment.defineConst("EventLoop", new XplClass(model, interpreter.globals));
    }

    // ─── Processamento das filas ───────────────────────────────────────────

    public static void runMicrotasks() {
        Runnable task;
        while ((task = nextTickQueue.poll()) != null) {
            task.run();
        }
    }

    public static void runMacrotasks() {
        Runnable task;
        while ((task = macroTaskQueue.poll()) != null) {
            task.run();
        }
    }

    public static void runAllTasks() {
        runMicrotasks();
        runMacrotasks();
    }

    // ─── Execução segura de tarefas XPL ───────────────────────────────────

    private static void safeRun(Interpreter interpreter, XplFunction task, String label) {
        try {
            task.call(interpreter, new ArrayList<>());
        } catch (ControlFlow.ReturnException e) {
            // Ignora retornos (função pode ter return)
        } catch (Exception e) {
            System.err.println("[EventLoop] Erro em " + label + ": " + e.getMessage());
            if (e instanceof ControlFlow.RuntimeError re) {
                if (re.token != null) {
                    System.err.println("    em: " + re.token.filePath + ":" + re.token.line);
                }
            }
        }
    }

    // ─── Extratores de argumentos ──────────────────────────────────────────

    private static XplFunction extractFunction(Interpreter intp, List<Expr.CallArg> args, int index) {
        Object obj = intp.evaluate(args.get(index).expression);
        if (!(obj instanceof XplFunction)) {
            throw new ControlFlow.RuntimeError(null, "EventLoop: espera uma função no argumento " + (index + 1));
        }
        return (XplFunction) obj;
    }

    private static long extractDelay(Interpreter intp, List<Expr.CallArg> args, int index) {
        Object val = intp.evaluate(args.get(index).expression);
        if (!(val instanceof Number)) {
            throw new ControlFlow.RuntimeError(null, "EventLoop: espera um número (milissegundos) no argumento " + (index + 1));
        }
        return ((Number) val).longValue();
    }

    private static long extractId(Interpreter intp, List<Expr.CallArg> args, int index) {
        Object val = intp.evaluate(args.get(index).expression);
        if (!(val instanceof Number)) {
            throw new ControlFlow.RuntimeError(null, "EventLoop: espera um identificador numérico no argumento " + (index + 1));
        }
        return ((Number) val).longValue();
    }

    // ─── Builders ──────────────────────────────────────────────────────────

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
}