package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.execution.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LogNativeModel {

    public enum Level { DEBUG, INFO, WARN, ERROR }
    private static Level currentLevel = Level.INFO;
    private static final ConcurrentLinkedQueue<String> history = new ConcurrentLinkedQueue<>();
    private static Path logFile = null;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Log", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false;

        // =========================================================
        // CONSTANTES DE NÍVEL
        // =========================================================
        Map<String, String> levels = new java.util.LinkedHashMap<>();
        levels.put("DEBUG", "DEBUG");
        levels.put("INFO", "INFO");
        levels.put("WARN", "WARN");
        levels.put("ERROR", "ERROR");
        model.staticFields.put("LEVELS", java.util.Collections.unmodifiableMap(levels));

        // =========================================================
        // CONFIGURAÇÕES
        // =========================================================
        model.staticFields.put("setLevel", buildCallable(1, (intp, args) -> {
            String levelStr = getString(intp, args, 0).toUpperCase();
            try {
                currentLevel = Level.valueOf(levelStr);
            } catch (IllegalArgumentException e) {
                throw new ControlFlow.RuntimeError(null, "Log.setLevel: nível inválido. Use Log.LEVELS.INFO, etc.");
            }
            return null;
        }));

        model.staticFields.put("toFile", buildCallable(1, (intp, args) -> {
            String path = getString(intp, args, 0);
            logFile = Paths.get(path);
            if (logFile.getParent() != null) {
                try { Files.createDirectories(logFile.getParent()); } catch (IOException ignored) {}
            }
            return null;
        }));

        // =========================================================
        // AÇÕES DE LOG
        // =========================================================
        model.staticFields.put("debug", buildCallable(1, logAction(Level.DEBUG)));
        model.staticFields.put("info",  buildCallable(1, logAction(Level.INFO)));
        model.staticFields.put("warn",  buildCallable(1, logAction(Level.WARN)));
        model.staticFields.put("error", buildCallable(1, logAction(Level.ERROR)));

        // =========================================================
        // HISTÓRICO
        // =========================================================
        model.staticFields.put("history", buildCallable(0, (intp, args) -> new ArrayList<>(history)));

        interpreter.registry_model.put("Log", model);
        interpreter.environment.defineConst("Log", new XplClass(model, interpreter.globals));
    }

    // --- Máquinas de Log ---
    private static NativeAction logAction(Level level) {
        return (intp, args) -> {
            String msg = getString(intp, args, 0);
            if (level.ordinal() >= currentLevel.ordinal()) {
                String line = String.format("[%s] %s: %s", formatter.format(LocalDateTime.now()), level.name(), msg);
                history.offer(line);
                if (history.size() > 10000) history.poll(); // Evita encher a RAM (máximo 10k logs em memória)

                System.out.println(line); // Output no terminal

                if (logFile != null) { // Output no ficheiro se configurado
                    try {
                        Files.writeString(logFile, line + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException ignored) {}
                }
            }
            return null;
        };
    }

    // --- Helpers ---
    private static String getString(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        return interpreter.stringify(interpreter.evaluate(args.get(index).expression));
    }

    private interface NativeAction { Object execute(Interpreter i, List<Expr.CallArg> a); }
    private static XplCallable buildCallable(int arity, NativeAction action) {
        return new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) { return action.execute(intp, args); }
        };
    }
}