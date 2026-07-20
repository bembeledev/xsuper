package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.ControlFlow;
import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;
import com.dic.xsuper.lang.XplFunction;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Agendador de tarefas com suporte a intervalos fixos e expressões cron.
 * Uso: var job = schedule_interval(segundos, () => { ... })
 *      schedule_cron("0 9 * * *", () => { ... })
 *      schedule_cancel(job.id)
 */
public class NativeScheduler {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private static final Map<Long, ScheduledFuture<?>> jobs = new ConcurrentHashMap<>();
    private static final AtomicLong jobIdGen = new AtomicLong(1);

    public static void register(Interpreter interpreter) {
        // ─── Intervalo fixo ─────────────────────────────────────────────────
        interpreter.globals.defineConst("schedule_interval", buildFunc(2, (intp, args) -> {
            long seconds = ((Number) intp.evaluate(args.get(0).expression)).longValue();
            Object taskObj = intp.evaluate(args.get(1).expression);
            if (!(taskObj instanceof XplFunction)) {
                throw new ControlFlow.RuntimeError(null, "schedule_interval: tarefa deve ser uma função () => { ... }");
            }
            XplFunction task = (XplFunction) taskObj;
            Interpreter taskInterpreter = intp.fork();

            long id = jobIdGen.getAndIncrement();
            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
                try {
                    task.call(taskInterpreter, List.of());
                } catch (Exception e) {
                    System.err.println("[Scheduler] Erro na tarefa " + id + ": " + e.getMessage());
                }
            }, seconds, seconds, TimeUnit.SECONDS);

            jobs.put(id, future);

            Map<String, Object> jobInfo = new HashMap<>();
            jobInfo.put("id", id);
            jobInfo.put("cancel", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    return cancelJob(id);
                }
            });
            return jobInfo;
        }));

        // ─── Cron ──────────────────────────────────────────────────────────
        interpreter.globals.defineConst("schedule_cron", buildFunc(2, (intp, args) -> {
            String cronExpr = getString(intp, args, 0);
            Object taskObj = intp.evaluate(args.get(1).expression);
            if (!(taskObj instanceof XplFunction)) {
                throw new ControlFlow.RuntimeError(null, "schedule_cron: tarefa deve ser uma função () => { ... }");
            }
            XplFunction task = (XplFunction) taskObj;
            Interpreter taskInterpreter = intp.fork();

            // Parse da expressão cron (simples)
            CronSchedule cron = parseCron(cronExpr);
            long id = jobIdGen.getAndIncrement();

            // Agendar a primeira execução
            scheduleCronTask(id, cron, task, taskInterpreter);

            Map<String, Object> jobInfo = new HashMap<>();
            jobInfo.put("id", id);
            jobInfo.put("cancel", new XplCallable() {
                @Override public int arity() { return 0; }
                @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                    return cancelJob(id);
                }
            });
            return jobInfo;
        }));

        // ─── Cancelar ──────────────────────────────────────────────────────
        interpreter.globals.defineConst("schedule_cancel", buildFunc(1, (intp, args) -> {
            long id = ((Number) intp.evaluate(args.get(0).expression)).longValue();
            return cancelJob(id);
        }));
    }

    private static boolean cancelJob(long id) {
        ScheduledFuture<?> future = jobs.remove(id);
        if (future != null) {
            return future.cancel(false);
        }
        return false;
    }

    // ─── Cron Scheduler ──────────────────────────────────────────────────────
    private static void scheduleCronTask(long id, CronSchedule cron, XplFunction task, Interpreter interpreter) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime next = cron.nextExecution(now);
        if (next == null) {
            System.err.println("[Scheduler] Cron inválido, tarefa " + id + " cancelada.");
            return;
        }
        long delay = next.toEpochSecond() - now.toEpochSecond();
        if (delay < 0) delay = 0;

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                task.call(interpreter, List.of());
            } catch (Exception e) {
                System.err.println("[Scheduler] Erro na tarefa cron " + id + ": " + e.getMessage());
            }
            // Reagendar para a próxima execução
            if (!jobs.containsKey(id)) return; // cancelado
            scheduleCronTask(id, cron, task, interpreter);
        }, delay, TimeUnit.SECONDS);

        jobs.put(id, future);
    }

    // ─── Parser Cron simples ────────────────────────────────────────────────
    private static CronSchedule parseCron(String expr) {
        String[] parts = expr.trim().split("\\s+");
        if (parts.length != 5) {
            throw new ControlFlow.RuntimeError(null, "schedule_cron: expressão deve ter 5 campos (minuto hora dia mês diaSemana)");
        }
        return new CronSchedule(parts[0], parts[1], parts[2], parts[3], parts[4]);
    }

    private static class CronSchedule {
        private final String minuteExpr;
        private final String hourExpr;
        private final String dayOfMonthExpr;
        private final String monthExpr;
        private final String dayOfWeekExpr;

        public CronSchedule(String minute, String hour, String dayOfMonth, String month, String dayOfWeek) {
            this.minuteExpr = minute;
            this.hourExpr = hour;
            this.dayOfMonthExpr = dayOfMonth;
            this.monthExpr = month;
            this.dayOfWeekExpr = dayOfWeek;
        }

        public ZonedDateTime nextExecution(ZonedDateTime from) {
            // Implementação simplificada: incrementa minuto a minuto e testa
            ZonedDateTime candidate = from.withSecond(0).withNano(0);
            // Limite de busca para evitar loop infinito (próximas 24h)
            ZonedDateTime limit = from.plusDays(1);
            while (candidate.isBefore(limit)) {
                if (matches(candidate)) {
                    return candidate;
                }
                candidate = candidate.plusMinutes(1);
            }
            return null;
        }

        private boolean matches(ZonedDateTime t) {
            return matchesField(t.getMinute(), minuteExpr) &&
                    matchesField(t.getHour(), hourExpr) &&
                    matchesField(t.getDayOfMonth(), dayOfMonthExpr) &&
                    matchesField(t.getMonthValue(), monthExpr) &&
                    matchesField(t.getDayOfWeek().getValue() % 7, dayOfWeekExpr); // 0=Sunday
        }

        private boolean matchesField(int value, String expr) {
            if (expr.equals("*")) return true;
            for (String part : expr.split(",")) {
                if (part.contains("-")) {
                    String[] range = part.split("-");
                    int min = Integer.parseInt(range[0]);
                    int max = Integer.parseInt(range[1]);
                    if (value >= min && value <= max) return true;
                } else if (part.contains("/")) {
                    String[] step = part.split("/");
                    int base = step[0].equals("*") ? 0 : Integer.parseInt(step[0]);
                    int increment = Integer.parseInt(step[1]);
                    if (value >= base && (value - base) % increment == 0) return true;
                } else {
                    if (Integer.parseInt(part) == value) return true;
                }
            }
            return false;
        }
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