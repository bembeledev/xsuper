package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.exceptions.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.natives.XplNativeObject;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.List;

public class StreamNativeModel {

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("Stream", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false;

        // =========================================================
        // Stream.open("dados.txt", "rw") -> Retorna StreamHandle
        // =========================================================
        model.staticFields.put("open", buildFunc(2, (intp, args) -> {
            String path = getString(intp, args, 0);
            String mode = getString(intp, args, 1).toLowerCase();

            if (!mode.matches("r|w|rw")) {
                throw new ControlFlow.RuntimeError(null, "Modo inválido. Use 'r' (leitura), 'w' (escrita) ou 'rw' (leitura/escrita).");
            }
            return new StreamHandle(path, mode);
        }));

        // =========================================================
        // Stream.readAll("arquivo.txt")
        // =========================================================
        model.staticFields.put("readAll", buildFunc(1, (intp, args) -> {
            String path = getString(intp, args, 0);
            try {
                return Files.readString(Path.of(path), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new ControlFlow.RuntimeError(null, "Erro ao ler ficheiro: " + e.getMessage());
            }
        }));

        // =========================================================
        // Stream.writeAll("arquivo.txt", "conteúdo")
        // =========================================================
        model.staticFields.put("writeAll", buildFunc(2, (intp, args) -> {
            String path = getString(intp, args, 0);
            String content = getString(intp, args, 1);
            try {
                Files.writeString(Path.of(path), content, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                return true;
            } catch (IOException e) {
                throw new ControlFlow.RuntimeError(null, "Erro ao escrever ficheiro: " + e.getMessage());
            }
        }));

        interpreter.registry_model.put("Stream", model);
        interpreter.environment.defineConst("Stream", new XplClass(model, interpreter.globals));
    }

    // ─── CLASSE INTERNA: StreamHandle (Intacta, apenas delegando pro VisitGetExpr) ───
    private static class StreamHandle implements XplNativeObject {
        private final String path;
        private final String mode;
        private RandomAccessFile raf;
        private boolean closed = false;

        public StreamHandle(String path, String mode) {
            this.path = path;
            this.mode = mode;
            try {
                this.raf = new RandomAccessFile(path, mode.equals("r") ? "r" : "rw");
            } catch (FileNotFoundException e) {
                throw new ControlFlow.RuntimeError(null, "Ficheiro não encontrado: " + path);
            }
        }

        @Override public void invokeMethod() {}

        @Override
        public Object invokeMethod(String methodName, List<Object> args, Interpreter interpreter) {
            if (closed) throw new ControlFlow.RuntimeError(null, "Stream já fechado.");
            try {
                switch (methodName) {
                    case "read":
                        int size = args.isEmpty() ? 1024 : ((Number) args.get(0)).intValue();
                        if (size <= 0) size = 1024;
                        byte[] buffer = new byte[size];
                        int read = raf.read(buffer);
                        if (read == -1) return null;
                        if (read < size) {
                            byte[] trimmed = new byte[read];
                            System.arraycopy(buffer, 0, trimmed, 0, read);
                            return Base64.getEncoder().encodeToString(trimmed);
                        }
                        return Base64.getEncoder().encodeToString(buffer);
                    case "readLine":
                        String line = raf.readLine();
                        return line == null ? null : new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                    case "write":
                        Object data = args.get(0);
                        byte[] bytes;
                        if (data instanceof String) {
                            try { bytes = Base64.getDecoder().decode((String) data); }
                            catch (IllegalArgumentException e) { bytes = ((String) data).getBytes(StandardCharsets.UTF_8); }
                        } else if (data instanceof byte[]) {
                            bytes = (byte[]) data;
                        } else {
                            bytes = interpreter.stringify(data).getBytes(StandardCharsets.UTF_8);
                        }
                        raf.write(bytes);
                        return (long) bytes.length;
                    case "seek":
                        long pos = ((Number) args.getFirst()).longValue();
                        if (pos < 0) throw new ControlFlow.RuntimeError(null, "Posição seek não pode ser negativa.");
                        raf.seek(pos);
                        return null;
                    case "tell": return raf.getFilePointer();
                    case "close": close(); return null;
                    case "flush": raf.getFD().sync(); return null;
                    default: throw new ControlFlow.RuntimeError(null, "Método '" + methodName + "' não suportado no stream.");
                }
            } catch (IOException e) {
                throw new ControlFlow.RuntimeError(null, "Erro no stream: " + e.getMessage());
            }
        }

        @Override
        public Object getProperty(String propertyName) {
            if (closed) throw new ControlFlow.RuntimeError(null, "Stream já fechado.");
            try {
                switch (propertyName) {
                    case "size": return raf.length();
                    case "path": return path;
                    case "mode": return mode;
                    case "read": case "readLine": case "write": case "seek": case "tell": case "close": case "flush":
                        return new XplCallable() {
                            @Override public int arity() { return -1; }
                            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                                return invokeMethod(propertyName, Interpreter.unpackNativeArgs(intp, args), intp);
                            }
                        };
                    default: return null;
                }
            } catch (IOException e) {
                throw new ControlFlow.RuntimeError(null, "Erro ao obter propriedade: " + e.getMessage());
            }
        }

        private void close() throws IOException {
            if (!closed) { raf.close(); closed = true; }
        }
    }

    // ─── AUXILIARES ──────────────────────────────────────────────────────
    private interface NativeAction { Object execute(Interpreter interpreter, List<Expr.CallArg> args); }

    private static XplCallable buildFunc(int arity, NativeAction action) {
        return new XplCallable() {
            @Override public int arity() { return arity; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) { return action.execute(intp, args); }
        };
    }

    private static String getString(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        return interpreter.stringify(interpreter.evaluate(args.get(index).expression));
    }
}