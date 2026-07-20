package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.ControlFlow;
import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Sistema de logging com níveis (DEBUG, INFO, WARN, ERROR).
 * Uso: log_set_level("DEBUG")
 *      log_info("Mensagem")
 *      log_error("Erro")
 *      log_to_file("app.log")
 *      log_get() -> Lista de logs (para testes)
 */
public class NativeLogging {
    public enum Level { DEBUG, INFO, WARN, ERROR }
    private static Level currentLevel = Level.INFO;
    private static final ConcurrentLinkedQueue<String> history = new ConcurrentLinkedQueue<>();
    private static Path logFile = null;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static void register(Interpreter interpreter) {
        interpreter.globals.defineConst("log_set_level", buildFunc(1, (intp, args) -> {
            String levelStr = getString(intp, args, 0).toUpperCase();
            try {
                currentLevel = Level.valueOf(levelStr);
            } catch (IllegalArgumentException e) {
                throw new ControlFlow.RuntimeError(null, "log_set_level: nível inválido. Use DEBUG, INFO, WARN, ERROR");
            }
            return null;
        }));

        interpreter.globals.defineConst("log_debug", buildFunc(1, logAction(Level.DEBUG)));
        interpreter.globals.defineConst("log_info", buildFunc(1, logAction(Level.INFO)));
        interpreter.globals.defineConst("log_warn", buildFunc(1, logAction(Level.WARN)));
        interpreter.globals.defineConst("log_error", buildFunc(1, logAction(Level.ERROR)));

        interpreter.globals.defineConst("log_to_file", buildFunc(1, (intp, args) -> {
            String path = getString(intp, args, 0);
            logFile = Paths.get(path);
            // Cria o diretório pai se não existir
            if (logFile.getParent() != null) {
                try { Files.createDirectories(logFile.getParent()); } catch (IOException ignored) {}
            }
            return null;
        }));

        interpreter.globals.defineConst("log_get", buildFunc(0, (intp, args) -> {
            return new ArrayList<>(history);
        }));
    }

    private static NativeAction logAction(Level level) {
        return (intp, args) -> {
            String msg = getString(intp, args, 0);
            if (level.ordinal() >= currentLevel.ordinal()) {
                String line = String.format("[%s] %s: %s",
                        formatter.format(LocalDateTime.now()),
                        level.name(),
                        msg);
                history.offer(line);
                if (history.size() > 10000) history.poll(); // evita memória infinita
                // Para saída padrão
                System.out.println(line);
                // Para ficheiro
                if (logFile != null) {
                    try {
                        Files.writeString(logFile, line + System.lineSeparator(),
                                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException ignored) {}
                }
            }
            return null;
        };
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