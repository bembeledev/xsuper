package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.ControlFlow;
import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;
import com.dic.xsuper.lang.XplFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NativeTask {

    private static final ThreadLocal<CompletableFuture<?>> currentTask = new ThreadLocal<>();
    // Um Pool de Threads dinâmico que cresce conforme o XPL precisar!
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void register(Interpreter interpreter) {

        // 1. task_sleep(ms) -> Pausa apenas a Thread atual, sem travar o resto do sistema
        interpreter.globals.defineConst("task_sleep", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                try {
                    long ms = (long) intp.evaluate(args.getFirst().expression);
                    Thread.sleep(ms);
                    return true;
                } catch (InterruptedException e) {
                    // ⭐ A CURA DO ZOMBIE: Restauramos o sinal e destruímos a pilha de execução!
                    Thread.currentThread().interrupt();
                    throw new ControlFlow.RuntimeError(null, "Tarefa cancelada (InterruptedException).");
                }
            }
        });

        // 2. task_run( () => { ... } )
        interpreter.globals.defineConst("task_run", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                Object funcObj = intp.evaluate(args.getFirst().expression);

                if (!(funcObj instanceof XplFunction func)) {
                    throw new ControlFlow.RuntimeError(null, "task_run exige uma função () => { ... }");
                }

                Interpreter threadInterpreter = intp.fork();

                // ⭐ A CURA DO ZOMBIE (Versão Tipada): Usamos um contentor Atómico!
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
            }
        });

        // 3. task_await(task) -> Espera que a Promessa termine e devolve o valor
        interpreter.globals.defineConst("task_await", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                Object taskObj = intp.evaluate(args.getFirst().expression);

                if (taskObj instanceof CompletableFuture<?> future) {
                    try {
                        return future.join(); // Bloqueia apenas esta chamada até a task terminar
                    } catch (Exception e) { throw new ControlFlow.RuntimeError(null, "Falha ao aguardar Task."); }
                }
                return taskObj; // Se não for uma task, devolve o valor normal
            }
        });

        // 4. task_all([task1, task2]) -> Espera por múltiplas tasks ao mesmo tempo
        interpreter.globals.defineConst("task_all", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
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
                    return results; // Devolve um Array com todos os resultados!
                }
                throw new ControlFlow.RuntimeError(null, "task_all exige um Array de tasks.");
            }
        });

        // 5. task_cancel(task) -> Tenta interromper e cancelar a execução da tarefa
        interpreter.globals.defineConst("task_cancel", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                Object taskObj = intp.evaluate(args.getFirst().expression);

                if (taskObj instanceof CompletableFuture<?> future) {
                    // O "true" diz ao Java para tentar dar um InterruptedException à Thread!
                    return future.cancel(true);
                }
                return false;
            }
        });

        // 6. task_is_done(task) -> Verifica se a tarefa já terminou (com sucesso ou cancelada)
        interpreter.globals.defineConst("task_is_done", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                Object taskObj = intp.evaluate(args.getFirst().expression);

                if (taskObj instanceof CompletableFuture<?> future) {
                    return future.isDone();
                }
                return true; // Se não for uma task, consideramos que já "terminou"
            }
        });

        // 7. task_is_cancelled(task) -> Verifica se a tarefa foi abortada
        interpreter.globals.defineConst("task_is_cancelled", new XplCallable() {
            @Override public int arity() { return 1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                Object taskObj = intp.evaluate(args.getFirst().expression);

                if (taskObj instanceof CompletableFuture<?> future) {
                    return future.isCancelled();
                }
                return false;
            }
        });

        // 8. task_current() -> Devolve a CompletableFuture da tarefa atual
        interpreter.globals.defineConst("task_current", new XplCallable() {
            @Override public int arity() { return 0; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                CompletableFuture<?> task = currentTask.get();
                if (task == null) {
                    throw new ControlFlow.RuntimeError(null, "task_current() só pode ser chamada dentro de uma task.");
                }
                return task;
            }
        });

        // 9. task_timeout.xpl(task, millis) -> Cancela a tarefa se não terminar dentro do tempo limite
        interpreter.globals.defineConst("task_timeout", new XplCallable() {
            @Override public int arity() { return 2; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                Object taskObj = intp.evaluate(args.get(0).expression);
                long millis = (long) intp.evaluate(args.get(1).expression);

                if (!(taskObj instanceof CompletableFuture<?> future)) {
                    throw new ControlFlow.RuntimeError(null, "task_timeout.xpl: primeiro argumento deve ser uma task.");
                }

                // Agenda o cancelamento após o tempo limite
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(millis);
                        future.cancel(true);
                    } catch (InterruptedException ignored) {}
                }, executor);

                return future; // Devolve a task original para encadeamento
            }
        });
    }
}