package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.ControlFlow;
import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;
import com.dic.xsuper.lang.ui.document.XplNativeObject;

import java.io.*;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Módulo de Interoperabilidade Industrial para o XPL.
 * <p>
 * Fornece:
 * <ul>
 *   <li>Execução de scripts (Python, Node, etc.) com codificação dinâmica</li>
 *   <li>Spawn de processos com controlo total (CWD, ENV, charset, timeout)</li>
 *   <li>I/O assíncrono via canais (integração com NativeChannel)</li>
 *   <li>Gestão de ciclo de vida (PID, sinais, watchdogs)</li>
 *   <li>Base para FFI (JNA/FFM) – futura expansão</li>
 * </ul>
 */
public class NativeInterop {

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
        // Adiciona mais conforme necessário
    }

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    public static void register(Interpreter interpreter) {

        // ─── OBJETO GLOBAL: Interop ──────────────────────────────────────
        Map<String, Object> interopLib = new LinkedHashMap<>();

        // 1. EVAL (Scripting Rápido)
        interopLib.put("eval", buildFunc(2, (intp, args) -> {
            String lang = getString(intp, args, 0).toLowerCase();
            String code = getString(intp, args, 1);
            Map<String, Object> opts = args.size() > 2 ? getMap(intp, args, 2) : new HashMap<>();
            String charset = opts.containsKey("charset") ? opts.get("charset").toString() : "UTF-8";
            long timeout = opts.containsKey("timeout") ? ((Number) opts.get("timeout")).longValue() : 0;
            return executeTempScript(lang, code, charset, timeout);
        }));

        // 2. SPAWN (Processos Vivos com Controlo Total)
        interopLib.put("spawn", buildFunc(2, (intp, args) -> {
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

            // Extrai opções
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

        // 3. FFI (Placeholder para chamada a bibliotecas nativas)
        interopLib.put("ffi_load", buildFunc(1, (intp, args) -> {
            String libPath = getString(intp, args, 0);
            // Placeholder – futura integração com JNA/FFM
            throw new ControlFlow.RuntimeError(null,
                    "FFI ainda não implementada. Em breve com JNA ou FFM (Java 22+).");
        }));

        interopLib.put("ffi_call", buildFunc(3, (intp, args) -> {
            String libName = getString(intp, args, 0);
            String functionName = getString(intp, args, 1);
            Object params = intp.evaluate(args.get(2).expression);
            throw new ControlFlow.RuntimeError(null,
                    "FFI ainda não implementada. Em breve com JNA ou FFM (Java 22+).");
        }));

        interpreter.globals.defineConst("Interop", interopLib);
    }

    // ─── CLASSE INTERNA: PROCESSO MADURO ──────────────────────────────────

    private static class InteropProcess implements XplNativeObject {
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

                // Redireciona ou captura
                if (streamMode) {
                    pb.redirectErrorStream(false); // separa stdout/stderr
                } else {
                    pb.redirectErrorStream(true);  // junta para simplificar
                }

                this.process = pb.start();
                this.pid = process.pid();

                // Inicializa streams com a codificação correta
                this.stdinWriter = new BufferedWriter(
                        new OutputStreamWriter(process.getOutputStream(), charset));
                this.stdoutReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), charset));
                this.stderrReader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), charset));

                // Se estiver em modo stream, inicia as tarefas assíncronas
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
                        // ⭐ A CURA: Envia o sinal de fim de transmissão para destrancar o XPL!
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

        // ⭐ O MOTOR UNIVERSAL DE INJEÇÃO
        private void sendToChannel(Object channelObj, String message) {
            try {
                // 1. Cria uma árvore AST falsa na RAM (equivalente a escrever 'canal.send')
                com.dic.xsuper.lang.Expr.Literal target = new com.dic.xsuper.lang.Expr.Literal(channelObj);
                com.dic.xsuper.lang.Token nameToken = new com.dic.xsuper.lang.Token(com.dic.xsuper.lang.TokenType.IDENTIFIER, "send", null, 0, 0, "");
                com.dic.xsuper.lang.Expr.Get getExpr = new com.dic.xsuper.lang.Expr.Get(target, nameToken);

                // 2. O Interpretador descobre o que o 'send' é na realidade
                Object callableObj = parentInterpreter.evaluate(getExpr);

                // 3. Executamos o Call passando a linha de texto limpa!
                if (callableObj instanceof XplCallable callable) {
                    callable.call(parentInterpreter, List.of(
                            new com.dic.xsuper.lang.Expr.CallArg(null, new com.dic.xsuper.lang.Expr.Literal(message))
                    ));
                }
            } catch (Exception e) {
                System.err.println("[NativeInterop] Falha ao injetar dados no canal: " + e.getMessage());
            }
        }

        // ─── XplNativeObject: invokeMethod ────────────────────────────────

        @Override
        public void invokeMethod() {

        }

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
                        String data = interpreter.stringify(args.get(0));
                        stdinWriter.write(data);
                        stdinWriter.newLine();
                        stdinWriter.flush();
                        return data.length() + 1;
                    }
                    case "read": {
                        // Lê uma linha (bloqueante)
                        String line = stdoutReader.readLine();
                        return line != null ? line : null;
                    }
                    case "readAll": {
                        // Lê todo o STDOUT (bloqueante)
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = stdoutReader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                        return sb.toString().trim();
                    }
                    case "readError": {
                        // Lê todo o STDERR (bloqueante)
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = stderrReader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                        return sb.toString().trim();
                    }
                    case "wait": {
                        long waitTime = args.isEmpty() ? 0 : ((Number) args.get(0)).longValue();
                        boolean finished;
                        if (waitTime > 0) {
                            finished = process.waitFor(waitTime, TimeUnit.MILLISECONDS);
                            if (!finished) {
                                // Timeout – mata o processo
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
                        // SIGTERM (graceful)
                        return terminateGracefully();
                    }
                    case "kill": {
                        // SIGKILL (forçado)
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
            switch (propertyName) {
                case "pid":
                    return pid;
                case "isAlive":
                    return process.isAlive();
                case "exitCode":
                    return process.isAlive() ? null : (long) process.exitValue();
                case "charset":
                    return charset.name();

                // ⭐ DELEGAÇÃO DE MÉTODOS (Igual à NativeStream)
                case "write":
                case "writeLine":
                case "read":
                case "readAll":
                case "readError":
                case "wait":
                case "terminate":
                case "kill":
                case "close":
                    return new XplCallable() {
                        @Override public int arity() { return -1; }
                        @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                            List<Object> evalArgs = Interpreter.unpackNativeArgs(intp, args);
                            return invokeMethod(propertyName, evalArgs, intp);
                        }
                    };
                default:
                    return null;
            }
        }

        // ─── GESTÃO DE CICLO DE VIDA ──────────────────────────────────────

        private boolean terminateGracefully() {
            if (!process.isAlive()) return true;
            // Tenta SIGTERM primeiro
            process.destroy();
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    // Se não terminou em 5 segundos, força SIGKILL
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