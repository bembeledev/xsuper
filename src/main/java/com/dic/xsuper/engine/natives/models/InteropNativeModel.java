package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.core.Token;
import com.dic.xsuper.engine.core.TokenType;
import com.dic.xsuper.engine.exceptions.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;
import com.dic.xsuper.dom.node.XplNativeObject;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class InteropNativeModel {

    // ─── Mapa de codificações suportadas ──────────────────────────────────
    private static final Map<String, Charset> CHARSETS = new LinkedHashMap<>();
    static {
        CHARSETS.put("UTF-8", StandardCharsets.UTF_8);
        CHARSETS.put("UTF-16", StandardCharsets.UTF_16);
        CHARSETS.put("ISO-8859-1", StandardCharsets.ISO_8859_1);
        CHARSETS.put("US-ASCII", StandardCharsets.US_ASCII);
        CHARSETS.put("Windows-1252", Charset.forName("windows-1252"));
        CHARSETS.put("GBK", Charset.forName("GBK"));
        CHARSETS.put("Shift_JIS", Charset.forName("Shift_JIS"));
    }

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Interop", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false;

        // ─── Interop.eval(lang, code, [options]) ───────────────────────────
        model.staticFields.put("eval", buildAction(-1, (intp, args) -> {
            if (args.size() < 2) {
                throw new ControlFlow.RuntimeError(null, "Interop.eval precisa de pelo menos 2 argumentos: lang, code");
            }
            String lang = getString(intp, args, 0).toLowerCase();
            String code = getString(intp, args, 1);
            Map<String, Object> opts = args.size() > 2 ? getMap(intp, args, 2) : new HashMap<>();
            String charset = opts.containsKey("charset") ? opts.get("charset").toString() : "UTF-8";
            long timeout = opts.containsKey("timeout") ? ((Number) opts.get("timeout")).longValue() : 0;
            return executeTempScript(lang, code, charset, timeout);
        }));

        // ─── Interop.spawn(command, argsList, [options]) ──────────────────
        model.staticFields.put("spawn", buildAction(-1, (intp, args) -> {
            if (args.size() < 2) {
                throw new ControlFlow.RuntimeError(null, "Interop.spawn precisa de pelo menos 2 argumentos: command, argsList");
            }
            String command = getString(intp, args, 0);
            Object argsListObj = intp.evaluate(args.get(1).expression);

            List<String> cmdArgs = new ArrayList<>();
            cmdArgs.add(command);
            if (argsListObj instanceof List) {
                for (Object arg : (List<?>) argsListObj) {
                    cmdArgs.add(intp.stringify(arg));
                }
            }

            Map<String, Object> opts = args.size() > 2 ? getMap(intp, args, 2) : new HashMap<>();

            String cwd = opts.containsKey("cwd") ? opts.get("cwd").toString() : null;
            @SuppressWarnings("unchecked")
            Map<String, String> env = opts.containsKey("env") ? (Map<String, String>) opts.get("env") : null;
            String charset = opts.containsKey("charset") ? opts.get("charset").toString() : "UTF-8";
            long timeout = opts.containsKey("timeout") ? ((Number) opts.get("timeout")).longValue() : 0;
            boolean streamMode = opts.containsKey("stream") && (boolean) opts.get("stream");
            String stdoutChannel = opts.containsKey("stdoutChannel") ? opts.get("stdoutChannel").toString() : null;
            String stderrChannel = opts.containsKey("stderrChannel") ? opts.get("stderrChannel").toString() : null;

            return new InteropProcess(cmdArgs, cwd, env, charset, timeout, streamMode, stdoutChannel, stderrChannel, intp);
        }));

        // ─── Interop.ffiLoad(libPath) ──────────────────────────────────────
        model.staticFields.put("ffiLoad", buildAction(1, (intp, args) -> {
            String libPath = getString(intp, args, 0);
            // Placeholder – futura integração com JNA/FFM
            throw new ControlFlow.RuntimeError(null,
                    "FFI ainda não implementada. Em breve com JNA ou FFM (Java 22+).");
        }));

        // ─── Interop.ffiCall(libName, functionName, params) ───────────────
        model.staticFields.put("ffiCall", buildAction(3, (intp, args) -> {
            String libName = getString(intp, args, 0);
            String functionName = getString(intp, args, 1);
            Object params = intp.evaluate(args.get(2).expression);
            throw new ControlFlow.RuntimeError(null,
                    "FFI ainda não implementada. Em breve com JNA ou FFM (Java 22+).");
        }));

        // ─── REGISTAR ──────────────────────────────────────────────────────

        interpreter.registry_model.put("Interop", model);
        interpreter.environment.defineConst("Interop", new XplClass(model, interpreter.globals));
    }

    // ─── CLASSE INTERNA: PROCESSO MADURO ──────────────────────────────────

    public static class InteropProcess implements XplNativeObject {
        private final Process process;
        private final Charset charset;
        private final long timeout;
        private final boolean streamMode;
        private final String stdoutChannelName;
        private final String stderrChannelName;
        private final Interpreter parentInterpreter;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private BufferedWriter stdinWriter;
        private BufferedReader stdoutReader;
        private BufferedReader stderrReader;

        private CompletableFuture<Void> stdoutTask;
        private CompletableFuture<Void> stderrTask;

        private long pid = -1;

        public InteropProcess(List<String> command, String cwd, Map<String, String> env,
                              String charsetName, long timeout, boolean streamMode,
                              String stdoutChannel, String stderrChannel,
                              Interpreter interpreter) {
            this.timeout = timeout;
            this.streamMode = streamMode;
            this.stdoutChannelName = stdoutChannel;
            this.stderrChannelName = stderrChannel;
            this.parentInterpreter = interpreter;
            this.charset = CHARSETS.getOrDefault(charsetName, DEFAULT_CHARSET);

            try {
                ProcessBuilder pb = new ProcessBuilder(command);
                if (cwd != null && !cwd.isEmpty()) {
                    pb.directory(new File(cwd));
                }
                if (env != null && !env.isEmpty()) {
                    pb.environment().putAll(env);
                }

                if (streamMode) {
                    pb.redirectErrorStream(false);
                } else {
                    pb.redirectErrorStream(true);
                }

                this.process = pb.start();
                this.pid = process.pid();

                this.stdinWriter = new BufferedWriter(
                        new OutputStreamWriter(process.getOutputStream(), charset));
                this.stdoutReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), charset));
                this.stderrReader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), charset));

                if (streamMode) {
                    startAsyncStreams();
                }

            } catch (IOException e) {
                throw new ControlFlow.RuntimeError(null,
                        "Falha ao iniciar processo externo: " + e.getMessage());
            }
        }

        // ─── MÉTODOS DE I/O ASSÍNCRONO (via Canais) ──────────────────────

        private void startAsyncStreams() {
            if (stdoutChannelName != null && !stdoutChannelName.isEmpty()) {
                this.stdoutTask = CompletableFuture.runAsync(() -> {
                    try {
                        Object channelObj = parentInterpreter.environment.get(stdoutChannelName);
                        String line;
                        while ((line = stdoutReader.readLine()) != null) {
                            sendToChannel(channelObj, line);
                        }
                        sendToChannel(channelObj, "EOF");
                    } catch (Exception e) {
                        System.err.println("[Interop STDOUT] Erro Crítico: " + e.getMessage());
                    }
                });
            }

            if (stderrChannelName != null && !stderrChannelName.isEmpty()) {
                this.stderrTask = CompletableFuture.runAsync(() -> {
                    try {
                        Object channelObj = parentInterpreter.environment.get(stderrChannelName);
                        String line;
                        while ((line = stderrReader.readLine()) != null) {
                            sendToChannel(channelObj, line);
                        }
                        sendToChannel(channelObj, "EOF");
                    } catch (Exception e) {
                        System.err.println("[Interop STDERR] Erro Crítico: " + e.getMessage());
                    }
                });
            }
        }

        private void sendToChannel(Object channelObj, String message) {
            try {
                Expr.Literal target = new Expr.Literal(channelObj);
                Token nameToken = new Token(
                        TokenType.IDENTIFIER, "send", null, 0, 0, "");
                Expr.Get getExpr = new Expr.Get(target, nameToken);
                Object callableObj = parentInterpreter.evaluate(getExpr);
                if (callableObj instanceof XplCallable callable) {
                    callable.call(parentInterpreter, List.of(
                            new Expr.CallArg(null, new Expr.Literal(message))
                    ));
                }
            } catch (Exception e) {
                System.err.println("[NativeInterop] Falha ao injetar dados no canal: " + e.getMessage());
            }
        }

        // ─── XplNativeObject: invokeMethod ────────────────────────────────

        @Override
        public void invokeMethod() {}

        @Override
        public Object invokeMethod(String methodName, List<Object> args, Interpreter interpreter) {
            if (closed.get()) {
                throw new ControlFlow.RuntimeError(null, "Processo já fechado.");
            }

            try {
                switch (methodName) {
                    case "write": {
                        String data = interpreter.stringify(args.get(0));
                        stdinWriter.write(data);
                        stdinWriter.flush();
                        return data.length();
                    }
                    case "writeLine": {
                        String data = interpreter.stringify(args.getFirst());
                        stdinWriter.write(data);
                        stdinWriter.newLine();
                        stdinWriter.flush();
                        return data.length() + 1;
                    }
                    case "read": {
                        return stdoutReader.readLine();
                    }
                    case "readAll": {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = stdoutReader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                        return sb.toString().trim();
                    }
                    case "readError": {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = stderrReader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                        return sb.toString().trim();
                    }
                    case "wait": {
                        long waitTime = args.isEmpty() ? 0 : ((Number) args.getFirst()).longValue();
                        boolean finished;
                        if (waitTime > 0) {
                            finished = process.waitFor(waitTime, TimeUnit.MILLISECONDS);
                            if (!finished) {
                                terminateGracefully();
                                throw new ControlFlow.RuntimeError(null,
                                        "Processo excedeu o tempo limite de " + waitTime + "ms");
                            }
                        } else {
                            process.waitFor();
                        }
                        return (long) process.exitValue();
                    }
                    case "terminate": {
                        return terminateGracefully();
                    }
                    case "kill": {
                        process.destroyForcibly();
                        close();
                        return true;
                    }
                    case "close": {
                        close();
                        return null;
                    }
                    default:
                        throw new ControlFlow.RuntimeError(null,
                                "Método Interop '" + methodName + "' não suportado.");
                }
            } catch (IOException e) {
                throw new ControlFlow.RuntimeError(null,
                        "Erro de I/O na Interoperabilidade: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ControlFlow.RuntimeError(null, "Processo interrompido.");
            }
        }

        // ─── XplNativeObject: getProperty ─────────────────────────────────

        @Override
        public Object getProperty(String propertyName) {
            return switch (propertyName) {
                case "pid" -> pid;
                case "isAlive" -> process.isAlive();
                case "exitCode" -> process.isAlive() ? null : (long) process.exitValue();
                case "charset" -> charset.name();
                case "write", "writeLine", "read", "readAll", "readError", "wait", "terminate", "kill", "close" ->
                        new XplCallable() {
                            @Override
                            public int arity() {
                                return -1;
                            }

                            @Override
                            public Object call(Interpreter intp, List<Expr.CallArg> args) {
                                List<Object> evalArgs = Interpreter.unpackNativeArgs(intp, args);
                                return invokeMethod(propertyName, evalArgs, intp);
                            }
                        };
                default -> null;
            };
        }

        // ─── GESTÃO DE CICLO DE VIDA ──────────────────────────────────────

        private boolean terminateGracefully() {
            if (!process.isAlive()) return true;
            process.destroy();
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(2, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
            close();
            return true;
        }

        private void close() {
            if (!closed.compareAndSet(false, true)) return;
            try {
                if (stdinWriter != null) stdinWriter.close();
                if (stdoutReader != null) stdoutReader.close();
                if (stderrReader != null) stderrReader.close();
            } catch (IOException ignored) {}
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    // ─── AUXILIARES ──────────────────────────────────────────────────────

    private static String executeTempScript(String lang, String code, String charsetName, long timeout) {
        Charset charset = CHARSETS.getOrDefault(charsetName, DEFAULT_CHARSET);
        try {
            String extension = switch (lang) {
                case "python" -> ".py";
                case "node", "javascript", "js" -> ".js";
                case "ruby" -> ".rb";
                case "php" -> ".php";
                case "perl" -> ".pl";
                case "lua" -> ".lua";
                default -> ".txt";
            };
            Path tempFile = Files.createTempFile("xpl_interop_", extension);
            Files.writeString(tempFile, code, charset);

            ProcessBuilder pb = new ProcessBuilder(lang, tempFile.toAbsolutePath().toString());
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), charset));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) out.append(line).append("\n");

            boolean finished = true;
            if (timeout > 0) {
                finished = p.waitFor(timeout, TimeUnit.MILLISECONDS);
                if (!finished) {
                    p.destroyForcibly();
                }
            } else {
                p.waitFor();
            }

            Files.deleteIfExists(tempFile);

            if (!finished) {
                throw new ControlFlow.RuntimeError(null,
                        "Script excedeu o tempo limite de " + timeout + "ms");
            }

            return out.toString().trim();

        } catch (Exception e) {
            throw new ControlFlow.RuntimeError(null,
                    "Erro na execução do script " + lang + ": " + e.getMessage());
        }
    }

    // ─── HELPERS GENÉRICOS ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMap(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        if (val instanceof Map) return (Map<String, Object>) val;
        throw new ControlFlow.RuntimeError(null, "Esperado um dicionário (Map) com as opções.");
    }

    private static String getString(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        return interpreter.stringify(val);
    }

    // ─── Builders ──────────────────────────────────────────────────────────

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