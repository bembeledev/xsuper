package com.dic.xsuper.engine.natives.models;

import com.dic.xsuper.engine.ast.Expr;
import com.dic.xsuper.engine.core.Interpreter;
import com.dic.xsuper.engine.exceptions.ControlFlow;
import com.dic.xsuper.engine.execution.XplCallable;
import com.dic.xsuper.engine.poo.XPLModel;
import com.dic.xsuper.engine.poo.XplClass;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileNativeModel {

    // Handles para operações de baixo nível
    private static final Map<Long, RandomAccessFile> handles = new ConcurrentHashMap<>();
    private static long nextHandle = 1;

    public static void Registry(Interpreter interpreter) {
        XPLModel model = new XPLModel("File", null);
        model.hasBaseImplementation = true;
        model.canBeInstantiated = false;

        // ─── 1. LEITURA E ESCRITA BÁSICA ──────────────────────────────
        model.staticFields.put("read", buildAction(1, (intp, args) -> {
            try {
                return Files.readString(Path.of(getString(intp, args, 0)), StandardCharsets.UTF_8);
            } catch (IOException e) { throw ioError("Erro ao ler ficheiro", e); }
        }));

        model.staticFields.put("write", buildAction(2, (intp, args) -> {
            try {
                Files.writeString(Path.of(getString(intp, args, 0)), getString(intp, args, 1),
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                return true;
            } catch (IOException e) { throw ioError("Erro ao escrever ficheiro", e); }
        }));

        model.staticFields.put("append", buildAction(2, (intp, args) -> {
            try {
                Files.writeString(Path.of(getString(intp, args, 0)), getString(intp, args, 1),
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                return true;
            } catch (IOException e) { throw ioError("Erro ao adicionar texto", e); }
        }));

        // ─── 2. METADADOS E INFORMAÇÃO ─────────────────────────────────
        model.staticFields.put("exists", buildAction(1, (intp, args) ->
                Files.exists(Path.of(getString(intp, args, 0)))
        ));

        model.staticFields.put("size", buildAction(1, (intp, args) -> {
            try { return Files.size(Path.of(getString(intp, args, 0))); }
            catch (IOException e) { throw ioError("Erro ao obter tamanho", e); }
        }));

        model.staticFields.put("modified", buildAction(1, (intp, args) -> {
            try {
                BasicFileAttributes attrs = Files.readAttributes(Path.of(getString(intp, args, 0)), BasicFileAttributes.class);
                return attrs.lastModifiedTime().toMillis();
            } catch (IOException e) { throw ioError("Erro ao obter data de modificação", e); }
        }));

        model.staticFields.put("created", buildAction(1, (intp, args) -> {
            try {
                BasicFileAttributes attrs = Files.readAttributes(Path.of(getString(intp, args, 0)), BasicFileAttributes.class);
                return attrs.creationTime().toMillis();
            } catch (IOException e) { throw ioError("Erro ao obter data de criação", e); }
        }));

        model.staticFields.put("isDir", buildAction(1, (intp, args) ->
                Files.isDirectory(Path.of(getString(intp, args, 0)))
        ));

        model.staticFields.put("isFile", buildAction(1, (intp, args) ->
                Files.isRegularFile(Path.of(getString(intp, args, 0)))
        ));

        model.staticFields.put("isReadonly", buildAction(1, (intp, args) ->
                !Files.isWritable(Path.of(getString(intp, args, 0)))
        ));

        model.staticFields.put("isHidden", buildAction(1, (intp, args) -> {
            try { return Files.isHidden(Path.of(getString(intp, args, 0))); }
            catch (IOException e) { throw ioError("Erro ao verificar oculto", e); }
        }));

        model.staticFields.put("type", buildAction(1, (intp, args) -> {
            Path p = Path.of(getString(intp, args, 0));
            if (Files.isDirectory(p)) return "directory";
            if (Files.isRegularFile(p)) return "file";
            if (Files.isSymbolicLink(p)) return "symlink";
            return "other";
        }));

        // ─── 3. DIRETÓRIOS E LISTAGEM ──────────────────────────────────
        model.staticFields.put("list", buildAction(1, (intp, args) -> {
            try (Stream<Path> stream = Files.list(Path.of(getString(intp, args, 0)))) {
                return stream.map(p -> p.getFileName().toString()).collect(Collectors.toList());
            } catch (IOException e) { throw ioError("Erro ao listar diretório", e); }
        }));

        model.staticFields.put("mkdir", buildAction(1, (intp, args) -> {
            try { Files.createDirectory(Path.of(getString(intp, args, 0))); return true; }
            catch (IOException e) { throw ioError("Erro ao criar diretório", e); }
        }));

        model.staticFields.put("mkdirAll", buildAction(1, (intp, args) -> {
            try { Files.createDirectories(Path.of(getString(intp, args, 0))); return true; }
            catch (IOException e) { throw ioError("Erro ao criar diretórios recursivos", e); }
        }));

        model.staticFields.put("rmdir", buildAction(1, (intp, args) -> {
            try { return Files.deleteIfExists(Path.of(getString(intp, args, 0))); }
            catch (IOException e) { throw ioError("Erro ao remover diretório", e); }
        }));

        // ─── 4. OPERAÇÕES DE FICHEIROS ──────────────────────────────────
        model.staticFields.put("delete", buildAction(1, (intp, args) -> {
            try { return Files.deleteIfExists(Path.of(getString(intp, args, 0))); }
            catch (IOException e) { throw ioError("Erro ao eliminar", e); }
        }));

        model.staticFields.put("copy", buildAction(2, (intp, args) -> {
            try {
                Files.copy(Path.of(getString(intp, args, 0)), Path.of(getString(intp, args, 1)),
                        StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException e) { throw ioError("Erro ao copiar", e); }
        }));

        model.staticFields.put("move", buildAction(2, (intp, args) -> {
            try {
                Files.move(Path.of(getString(intp, args, 0)), Path.of(getString(intp, args, 1)),
                        StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException e) { throw ioError("Erro ao mover", e); }
        }));

        // ─── 5. MANIPULAÇÃO DE CAMINHOS ─────────────────────────────────
        model.staticFields.put("name", buildAction(1, (intp, args) -> {
            Path p = Path.of(getString(intp, args, 0)).getFileName();
            return p != null ? p.toString() : "";
        }));

        model.staticFields.put("dir", buildAction(1, (intp, args) -> {
            Path p = Path.of(getString(intp, args, 0)).getParent();
            return p != null ? p.toString() : "";
        }));

        model.staticFields.put("extension", buildAction(1, (intp, args) -> {
            String name = Path.of(getString(intp, args, 0)).getFileName().toString();
            int dotIndex = name.lastIndexOf('.');
            return (dotIndex == -1) ? "" : name.substring(dotIndex + 1);
        }));

        model.staticFields.put("stem", buildAction(1, (intp, args) -> {
            String name = Path.of(getString(intp, args, 0)).getFileName().toString();
            int dotIndex = name.lastIndexOf('.');
            return (dotIndex == -1) ? name : name.substring(0, dotIndex);
        }));

        // ─── 6. LEITURA DE LINHAS E CHUNKS ─────────────────────────────
        model.staticFields.put("readLines", buildAction(1, (intp, args) -> {
            try { return Files.readAllLines(Path.of(getString(intp, args, 0)), StandardCharsets.UTF_8); }
            catch (IOException e) { throw ioError("Erro ao ler linhas", e); }
        }));

        model.staticFields.put("writeLines", buildAction(2, (intp, args) -> {
            try {
                Object obj = intp.evaluate(args.get(1).expression);
                List<String> lines;
                if (obj instanceof List) {
                    lines = ((List<?>) obj).stream().map(Object::toString).collect(Collectors.toList());
                } else {
                    throw new ControlFlow.RuntimeError(null, "writeLines espera uma lista de strings.");
                }
                Files.write(Path.of(getString(intp, args, 0)), lines, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                return true;
            } catch (IOException e) { throw ioError("Erro ao escrever linhas", e); }
        }));

        // ─── 7. OPERAÇÕES DE BAIXO NÍVEL (HANDLES) ─────────────────────
        model.staticFields.put("open", buildAction(2, (intp, args) -> {
            String path = getString(intp, args, 0);
            String mode = getString(intp, args, 1); // "r", "rw", etc.
            try {
                RandomAccessFile raf = new RandomAccessFile(path, mode);
                long handle = nextHandle++;
                handles.put(handle, raf);
                return handle;
            } catch (IOException e) { throw ioError("Erro ao abrir ficheiro", e); }
        }));

        model.staticFields.put("close", buildAction(1, (intp, args) -> {
            long handle = getLong(intp, args, 0);
            RandomAccessFile raf = handles.remove(handle);
            if (raf == null) throw new ControlFlow.RuntimeError(null, "Handle inválido ou já fechado.");
            try { raf.close(); } catch (IOException e) { throw ioError("Erro ao fechar ficheiro", e); }
            return true;
        }));

        model.staticFields.put("seek", buildAction(2, (intp, args) -> {
            long handle = getLong(intp, args, 0);
            long pos = getLong(intp, args, 1);
            RandomAccessFile raf = handles.get(handle);
            if (raf == null) throw new ControlFlow.RuntimeError(null, "Handle inválido.");
            try { raf.seek(pos); } catch (IOException e) { throw ioError("Erro ao posicionar cursor", e); }
            return true;
        }));

        model.staticFields.put("tell", buildAction(1, (intp, args) -> {
            long handle = getLong(intp, args, 0);
            RandomAccessFile raf = handles.get(handle);
            if (raf == null) throw new ControlFlow.RuntimeError(null, "Handle inválido.");
            try { return raf.getFilePointer(); } catch (IOException e) { throw ioError("Erro ao obter posição", e); }
        }));

        model.staticFields.put("readChunk", buildAction(3, (intp, args) -> {
            long handle = getLong(intp, args, 0);
            long offset = getLong(intp, args, 1);
            int length = (int) getLong(intp, args, 2);
            RandomAccessFile raf = handles.get(handle);
            if (raf == null) throw new ControlFlow.RuntimeError(null, "Handle inválido.");
            try {
                raf.seek(offset);
                byte[] buffer = new byte[length];
                int read = raf.read(buffer, 0, length);
                if (read <= 0) return "";
                return new String(buffer, 0, read, StandardCharsets.UTF_8);
            } catch (IOException e) { throw ioError("Erro ao ler chunk", e); }
        }));

        // ─── 8. OPERAÇÕES ADICIONAIS ────────────────────────────────────
        model.staticFields.put("truncate", buildAction(2, (intp, args) -> {
            Path path = Path.of(getString(intp, args, 0));
            long newSize = getLong(intp, args, 1);
            try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
                channel.truncate(newSize);
                return true;
            } catch (IOException e) { throw ioError("Erro ao truncar", e); }
        }));

        model.staticFields.put("touch", buildAction(1, (intp, args) -> {
            Path path = Path.of(getString(intp, args, 0));
            try {
                if (!Files.exists(path)) {
                    Files.createFile(path);
                } else {
                    Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis()));
                }
                return true;
            } catch (IOException e) { throw ioError("Erro ao tocar ficheiro", e); }
        }));

        model.staticFields.put("temp", buildAction(0, (intp, args) -> {
            try { return Files.createTempFile("xpl_", ".tmp").toString(); }
            catch (IOException e) { throw ioError("Erro ao criar ficheiro temporário", e); }
        }));

        // ─── 9. GLOB E SEARCH ────────────────────────────────────────────
        model.staticFields.put("glob", buildAction(1, (intp, args) -> {
            String pattern = getString(intp, args, 0);
            Path base = Paths.get("").toAbsolutePath();
            String globPattern = "glob:" + pattern;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(base, globPattern)) {
                List<String> result = new ArrayList<>();
                for (Path p : stream) {
                    result.add(p.toString());
                }
                return result;
            } catch (IOException e) { throw ioError("Erro na operação glob", e); }
        }));

        model.staticFields.put("search", buildAction(2, (intp, args) -> {
            String root = getString(intp, args, 0);
            String pattern = getString(intp, args, 1);
            Path rootPath = Path.of(root);
            String globPattern = "glob:" + pattern;
            FileSystem fs = FileSystems.getDefault();
            PathMatcher matcher = fs.getPathMatcher(globPattern);
            List<String> result = new ArrayList<>();
            try (Stream<Path> walk = Files.walk(rootPath)) {
                walk.filter(p -> matcher.matches(p.getFileName()))
                        .map(Path::toString)
                        .forEach(result::add);
            } catch (IOException e) { throw ioError("Erro na pesquisa recursiva", e); }
            return result;
        }));

        // ─── 10. CHECKSUM ──────────────────────────────────────────────
        model.staticFields.put("checksum", buildAction(1, (intp, args) -> {
            Path path = Path.of(getString(intp, args, 0));
            try (InputStream in = Files.newInputStream(path)) {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    md.update(buffer, 0, read);
                }
                byte[] digest = md.digest();
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new ControlFlow.RuntimeError(null, "Algoritmo MD5 não disponível.");
            } catch (IOException e) {
                throw ioError("Erro ao calcular checksum", e);
            }
        }));

        // ─── Registar a classe File ────────────────────────────────────
        interpreter.registry_model.put("File", model);
        interpreter.environment.defineConst("File", new XplClass(model, interpreter.globals));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

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

    private static String getString(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        return interpreter.stringify(val);
    }

    private static long getLong(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        if (val instanceof Number) return ((Number) val).longValue();
        throw new ControlFlow.RuntimeError(null, "Esperado um número inteiro.");
    }

    private static ControlFlow.RuntimeError ioError(String msg, IOException e) {
        return new ControlFlow.RuntimeError(null, "I/O Error: " + msg + " (" + e.getMessage() + ")");
    }
}