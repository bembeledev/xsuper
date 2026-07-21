package com.dic.xsuper.lang.natives.models;

import com.dic.xsuper.lang.*;
import com.dic.xsuper.lang.poo.XPLModel;
import com.dic.xsuper.lang.poo.XplClass;
import com.dic.xsuper.lang.poo.XplInstance;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class SchedulerNativeModel {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private static final Map<Long, ScheduledFuture<?>> jobs = new ConcurrentHashMap<>();
    private static final AtomicLong jobIdGen = new AtomicLong(1);

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Scheduler", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false;

        // =========================================================
        // ⭐ MOLDE DA INSTÂNCIA DE TAREFA (Job) ⭐
        // =========================================================
        XPLModel jobModel = new XPLModel("Job", null);
        jobModel.hasBaseImplementation = true;
        jobModel.canBeInstantiated = false;

        Token pubToken = new Token(TokenType.PUBLIC, "pub", null, 0, 0);
        jobModel.addField(new Stmt.FieldDecl(pubToken, false, false, true, new Token(TokenType.IDENTIFIER, "id", null, 0, 0), new TypeNode.Simple(new Token(TokenType.T_INT, "int", null, 0, 0))));

        XplClass jobClass = new XplClass(jobModel, interpreter.globals);

        // =========================================================
        // Scheduler.interval(segundos, () => { ... })
        // =========================================================
        model.staticFields.put("interval", buildFunc(2, (intp, args) -> {
            long seconds = ((Number) intp.evaluate(args.get(0).expression)).longValue();
            Object taskObj = intp.evaluate(args.get(1).expression);

            if (!(taskObj instanceof XplFunction task)) {
                throw new ControlFlow.RuntimeError(null, "Scheduler.interval: A tarefa deve ser uma função () => { ... }");
            }

            Interpreter taskInterpreter = intp.fork();
            long id = jobIdGen.getAndIncrement();

            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
                try { task.call(taskInterpreter, List.of()); }
                catch (Exception e) { System.err.println("[Scheduler] Erro na tarefa " + id + ": " + e.getMessage()); }
            }, seconds, seconds, TimeUnit.SECONDS);

            jobs.put(id, future);

            // Devolvemos uma instância elegante!
            XplInstance instance = new XplInstance(jobClass);
            instance.fields.put("id", id);
            instance.fields.put("cancel", buildFunc(0, (i, a) -> cancelJob(id)));
            return instance;
        }));

        // =========================================================
        // Scheduler.cron("0 9 * * *", () => { ... })
        // =========================================================
        model.staticFields.put("cron", buildFunc(2, (intp, args) -> {
            String cronExpr = getString(intp, args, 0);
            Object taskObj = intp.evaluate(args.get(1).expression);

            if (!(taskObj instanceof XplFunction task)) {
                throw new ControlFlow.RuntimeError(null, "Scheduler.cron: A tarefa deve ser uma função () => { ... }");
            }

            Interpreter taskInterpreter = intp.fork();
            CronSchedule cron = parseCron(cronExpr);
            long id = jobIdGen.getAndIncrement();

            scheduleCronTask(id, cron, task, taskInterpreter);

            XplInstance instance = new XplInstance(jobClass);
            instance.fields.put("id", id);
            instance.fields.put("cancel", buildFunc(0, (i, a) -> cancelJob(id)));
            return instance;
        }));

        // =========================================================
        // Scheduler.cancel(id) -> Rota de segurança caso o dev perca a variável Job
        // =========================================================
        model.staticFields.put("cancel", buildFunc(1, (intp, args) -> {
            long id = ((Number) intp.evaluate(args.get(0).expression)).longValue();
            return cancelJob(id);
        }));

        interpreter.registry_model.put("Scheduler", model);
        interpreter.environment.defineConst("Scheduler", new XplClass(model, interpreter.globals));
    }

    private static boolean cancelJob(long id) {
        ScheduledFuture<?> future = jobs.remove(id);
        return future != null && future.cancel(false);
    }

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
            try { task.call(interpreter, List.of()); }
            catch (Exception e) { System.err.println("[Scheduler] Erro na tarefa cron " + id + ": " + e.getMessage()); }

            if (!jobs.containsKey(id)) return;
            scheduleCronTask(id, cron, task, interpreter); // Reagenda!
        }, delay, TimeUnit.SECONDS);

        jobs.put(id, future);
    }

    private static CronSchedule parseCron(String expr) {
        String[] parts = expr.trim().split("\\s+");
        if (parts.length != 5) throw new ControlFlow.RuntimeError(null, "Scheduler.cron: expressão deve ter 5 campos (minuto hora dia mês diaSemana)");
        return new CronSchedule(parts[0], parts[1], parts[2], parts[3], parts[4]);
    }

    private static class CronSchedule {
        private final String minuteExpr, hourExpr, dayOfMonthExpr, monthExpr, dayOfWeekExpr;

        public CronSchedule(String min, String hour, String dayM, String month, String dayW) {
            this.minuteExpr = min; this.hourExpr = hour; this.dayOfMonthExpr = dayM; this.monthExpr = month; this.dayOfWeekExpr = dayW;
        }

        public ZonedDateTime nextExecution(ZonedDateTime from) {
            ZonedDateTime candidate = from.withSecond(0).withNano(0);
            ZonedDateTime limit = from.plusDays(1);
            while (candidate.isBefore(limit)) {
                if (matches(candidate)) return candidate;
                candidate = candidate.plusMinutes(1);
            }
            return null;
        }

        private boolean matches(ZonedDateTime t) {
            return matchesField(t.getMinute(), minuteExpr) && matchesField(t.getHour(), hourExpr) &&
                    matchesField(t.getDayOfMonth(), dayOfMonthExpr) && matchesField(t.getMonthValue(), monthExpr) &&
                    matchesField(t.getDayOfWeek().getValue() % 7, dayOfWeekExpr);
        }

        private boolean matchesField(int value, String expr) {
            if (expr.equals("*")) return true;
            for (String part : expr.split(",")) {
                if (part.contains("-")) {
                    String[] range = part.split("-");
                    if (value >= Integer.parseInt(range[0]) && value <= Integer.parseInt(range[1])) return true;
                } else if (part.contains("/")) {
                    String[] step = part.split("/");
                    int base = step[0].equals("*") ? 0 : Integer.parseInt(step[0]);
                    if (value >= base && (value - base) % Integer.parseInt(step[1]) == 0) return true;
                } else if (Integer.parseInt(part) == value) return true;
            }
            return false;
        }
    }

    private static String getString(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        return interpreter.stringify(interpreter.evaluate(args.get(index).expression));
    }

    private interface NativeAction { Object execute(Interpreter i, List<Expr.CallArg> a); }
    private static XplCallable buildFunc(int arity, NativeAction action) {
        return new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) { return action.execute(intp, args); }
        };
    }
}