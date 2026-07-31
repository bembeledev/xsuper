package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.execution.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.execution.XplFunction;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskNativeModel {

    // Mantido rigorosamente igual: Guarda a promessa atual
    private static final ThreadLocal<CompletableFuture<?>> currentTask = new ThreadLocal<>();

    // Mantido rigorosamente igual: Pool dinâmico
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Task", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false; // Modelo 100% Estático

        // =========================================================
        // 1. SLEEP E YIELD (Controlo de Thread)
        // =========================================================

        model.staticFields.put("sleep", buildCallable(1, (intp, args) -> {
            try {
                long ms = ((Number) intp.evaluate(args.getFirst().expression)).longValue();
                Thread.sleep(ms);
                return true;
            } catch (InterruptedException e) {
                // ⭐ A CURA DO ZOMBIE MANTIDA INTACTA ⭐
                Thread.currentThread().interrupt();
                throw new ControlFlow.RuntimeError(null, "Tarefa cancelada (InterruptedException).");
            }
        }));

        model.staticFields.put("yield", buildCallable(0, (intp, args) -> {
            Thread.yield(); // Deixa o processador respirar e processar sinais de cancelamento noutras threads
            return null;
        }));

        // =========================================================
        // 2. RUN (A TUA LÓGICA CORE DE EXECUÇÃO E CANCELAMENTO)
        // =========================================================

        model.staticFields.put("run", buildCallable(1, (intp, args) -> {
            Object funcObj = intp.evaluate(args.getFirst().expression);

            if (!(funcObj instanceof XplFunction func)) {
                throw new ControlFlow.RuntimeError(null, "Task.run exige uma função () => { ... }");
            }

            Interpreter threadInterpreter = intp.fork();

            // ⭐ A CURA DO ZOMBIE (Versão Tipada): Usamos um contentor Atómico! ⭐
            java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<?>> runningTaskRef = new java.util.concurrent.atomic.AtomicReference<>();

            CompletableFuture<Object> future = new CompletableFuture<Object>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    java.util.concurrent.Future<?> realTask = runningTaskRef.get();
                    // Se a tarefa já estiver no pool, disparamos o tiro letal (Interrupt) nela!
                    if (realTask != null) {
                        realTask.cancel(mayInterruptIfRunning);
                    }
                    return super.cancel(mayInterruptIfRunning);
                }
            };

            // Submetemos a tarefa e guardamos a referência no contentor Atómico
            runningTaskRef.set(executor.submit(() -> {
                currentTask.set(future);
                try {
                    Object result = func.call(threadInterpreter, new ArrayList<>());
                    future.complete(result);
                } catch (ControlFlow.ReturnException ret) {
                    future.complete(ret.value);
                } catch (ControlFlow.RuntimeError re) {
                    // Capturamos a nossa mensagem de morte silenciosa do Interpreter
                    if (re.getMessage() != null && re.getMessage().contains("morta e abortada")) {
                        future.cancel(true);
                    } else {
                        future.completeExceptionally(re);
                    }
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                } finally {
                    currentTask.remove(); // Limpeza
                }
            }));

            return future;
        }));

        // =========================================================
        // 3. DELAY (Executar após X milissegundos)
        // =========================================================

        model.staticFields.put("delay", buildCallable(2, (intp, args) -> {
            long ms = ((Number) intp.evaluate(args.get(0).expression)).longValue();
            Object funcObj = intp.evaluate(args.get(1).expression);

            if (!(funcObj instanceof XplFunction func)) {
                throw new ControlFlow.RuntimeError(null, "Task.delay exige uma função () => { ... } no segundo argumento");
            }

            Interpreter threadInterpreter = intp.fork();
            java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<?>> runningTaskRef = new java.util.concurrent.atomic.AtomicReference<>();

            CompletableFuture<Object> future = new CompletableFuture<Object>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    java.util.concurrent.Future<?> realTask = runningTaskRef.get();
                    if (realTask != null) realTask.cancel(mayInterruptIfRunning);
                    return super.cancel(mayInterruptIfRunning);
                }
            };

            runningTaskRef.set(executor.submit(() -> {
                currentTask.set(future);
                try {
                    Thread.sleep(ms); // O Atraso implementado de forma limpa
                    Object result = func.call(threadInterpreter, new ArrayList<>());
                    future.complete(result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    future.cancel(true);
                } catch (ControlFlow.ReturnException ret) {
                    future.complete(ret.value);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                } finally {
                    currentTask.remove();
                }
            }));
            return future;
        }));

        // =========================================================
        // 4. AWAIT, ALL, RACE E ALLSETTLED (Gestão de Promessas)
        // =========================================================

        model.staticFields.put("await", buildCallable(1, (intp, args) -> {
            Object taskObj = intp.evaluate(args.getFirst().expression);
            if (taskObj instanceof CompletableFuture<?> future) {
                try {
                    return future.join();
                } catch (Exception e) { throw new ControlFlow.RuntimeError(null, "Falha ao aguardar Task."); }
            }
            return taskObj;
        }));

        model.staticFields.put("all", buildCallable(1, (intp, args) -> {
            Object listObj = intp.evaluate(args.getFirst().expression);
            if (listObj instanceof List<?> list) {
                List<Object> results = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof CompletableFuture<?> future) {
                        results.add(future.join());
                    } else {
                        results.add(item);
                    }
                }
                return results;
            }
            throw new ControlFlow.RuntimeError(null, "Task.all exige um Array de tasks.");
        }));

        // NOVO: Task.race -> O primeiro a terminar ganha
        model.staticFields.put("race", buildCallable(1, (intp, args) -> {
            Object listObj = intp.evaluate(args.getFirst().expression);
            if (listObj instanceof List<?> list) {
                CompletableFuture<?>[] futuresArray = list.stream()
                        .filter(item -> item instanceof CompletableFuture<?>)
                        .toArray(CompletableFuture<?>[]::new);

                if (futuresArray.length == 0) return null;

                try {
                    return CompletableFuture.anyOf(futuresArray).join();
                } catch (Exception e) { throw new ControlFlow.RuntimeError(null, "Erro na execução de Task.race"); }
            }
            throw new ControlFlow.RuntimeError(null, "Task.race exige um Array de tasks.");
        }));

        // NOVO: Task.allSettled -> Espera tudo, mas não explode se houver erros
        model.staticFields.put("allSettled", buildCallable(1, (intp, args) -> {
            Object listObj = intp.evaluate(args.getFirst().expression);
            if (listObj instanceof List<?> list) {
                List<Map<String, Object>> results = new ArrayList<>();

                for (Object item : list) {
                    Map<String, Object> statusObj = new HashMap<>();
                    if (item instanceof CompletableFuture<?> future) {
                        try {
                            Object res = future.join();
                            statusObj.put("status", "fulfilled");
                            statusObj.put("value", res);
                        } catch (Exception e) {
                            statusObj.put("status", "rejected");
                            statusObj.put("error", e.getMessage());
                        }
                    } else {
                        statusObj.put("status", "fulfilled");
                        statusObj.put("value", item);
                    }
                    results.add(statusObj);
                }
                return results;
            }
            throw new ControlFlow.RuntimeError(null, "Task.allSettled exige um Array de tasks.");
        }));

        // =========================================================
        // 5. GESTÃO E INFORMAÇÃO DA TAREFA
        // =========================================================

        model.staticFields.put("cancel", buildCallable(1, (intp, args) -> {
            Object taskObj = intp.evaluate(args.getFirst().expression);
            if (taskObj instanceof CompletableFuture<?> future) {
                return future.cancel(true);
            }
            return false;
        }));

        model.staticFields.put("isDone", buildCallable(1, (intp, args) -> {
            Object taskObj = intp.evaluate(args.getFirst().expression);
            if (taskObj instanceof CompletableFuture<?> future) return future.isDone();
            return true;
        }));

        model.staticFields.put("isCancelled", buildCallable(1, (intp, args) -> {
            Object taskObj = intp.evaluate(args.getFirst().expression);
            if (taskObj instanceof CompletableFuture<?> future) return future.isCancelled();
            return false;
        }));

        model.staticFields.put("current", buildCallable(0, (intp, args) -> {
            CompletableFuture<?> task = currentTask.get();
            if (task == null) throw new ControlFlow.RuntimeError(null, "Task.current() só pode ser chamada dentro de uma task ativa.");
            return task;
        }));

        model.staticFields.put("timeout", buildCallable(2, (intp, args) -> {
            Object taskObj = intp.evaluate(args.get(0).expression);
            long millis = ((Number) intp.evaluate(args.get(1).expression)).longValue();

            if (!(taskObj instanceof CompletableFuture<?> future)) {
                throw new ControlFlow.RuntimeError(null, "Task.timeout: o primeiro argumento deve ser uma task.");
            }

            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(millis);
                    future.cancel(true);
                } catch (InterruptedException ignored) {}
            }, executor);

            return future;
        }));

        interpreter.registry_model.put("Task", model);
        interpreter.environment.defineConst("Task", new XplClass(model, interpreter.globals));
    }

    // --- Helper Interno ---
    private interface NativeAction { Object execute(Interpreter i, List<Expr.CallArg> a); }
    private static XplCallable buildCallable(int arity, NativeAction action) {
        return new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) { return action.execute(intp, args); }
        };
    }
}