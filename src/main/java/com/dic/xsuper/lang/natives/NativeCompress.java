package com.dic.xsuper.lang.natives;

import com.dic.xsuper.lang.ControlFlow;
import com.dic.xsuper.lang.Expr;
import com.dic.xsuper.lang.Interpreter;
import com.dic.xsuper.lang.XplCallable;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class NativeCompress {

    public static void register(Interpreter interpreter) {
        // ─── compress(input, output, format, options) ────────────────
        interpreter.globals.defineConst("compress", new XplCallable() {
            @Override public int arity() { return -1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                if (args.size() < 3) {
                    throw new ControlFlow.RuntimeError(null,
                            "compress precisa de pelo menos 3 argumentos: input, output, format");
                }
                String input = getString(intp, args, 0);
                String output = getString(intp, args, 1);
                String format = getString(intp, args, 2).toLowerCase();
                Map<String, Object> options = args.size() > 3 ? getMap(intp, args, 3) : new HashMap<>();

                try {
                    switch (format) {
                        case "zip":
                            compressZip(input, output, options);
                            break;
                        case "tar":
                            compressTar(input, output, false, false, false);
                            break;
                        case "tar.gz":
                        case "tgz":
                            compressTar(input, output, true, false, false);
                            break;
                        case "tar.bz2":
                        case "tbz2":
                            compressTar(input, output, false, true, false);
                            break;
                        case "tar.xz":
                        case "txz":
                            compressTar(input, output, false, false, true);
                            break;
                        case "gzip":
                        case "gz":
                            compressSingleFile(input, output, "gzip");
                            break;
                        case "bzip2":
                        case "bz2":
                            compressSingleFile(input, output, "bzip2");
                            break;
                        case "xz":
                            compressSingleFile(input, output, "xz");
                            break;
                        case "7z":
                            compress7z(input, output, options);
                            break;
                        default:
                            throw new ControlFlow.RuntimeError(null,
                                    "Formato não suportado: " + format);
                    }
                } catch (Exception e) {
                    throw new ControlFlow.RuntimeError(null,
                            "compress: " + e.getMessage());
                }
                return true;
            }
        });

        // ─── decompress(input, output, format, options) ──────────────
        interpreter.globals.defineConst("decompress", new XplCallable() {
            @Override public int arity() { return -1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                if (args.size() < 3) {
                    throw new ControlFlow.RuntimeError(null,
                            "decompress precisa de pelo menos 3 argumentos: input, output, format");
                }
                String input = getString(intp, args, 0);
                String output = getString(intp, args, 1);
                String format = getString(intp, args, 2).toLowerCase();
                Map<String, Object> options = args.size() > 3 ? getMap(intp, args, 3) : new HashMap<>();

                try {
                    switch (format) {
                        case "zip":
                            decompressZip(input, output, options);
                            break;
                        case "tar":
                            decompressTar(input, output, false, false, false);
                            break;
                        case "tar.gz":
                        case "tgz":
                            decompressTar(input, output, true, false, false);
                            break;
                        case "tar.bz2":
                        case "tbz2":
                            decompressTar(input, output, false, true, false);
                            break;
                        case "tar.xz":
                        case "txz":
                            decompressTar(input, output, false, false, true);
                            break;
                        case "gzip":
                        case "gz":
                            decompressSingleFile(input, output, "gzip");
                            break;
                        case "bzip2":
                        case "bz2":
                            decompressSingleFile(input, output, "bzip2");
                            break;
                        case "xz":
                            decompressSingleFile(input, output, "xz");
                            break;
                        case "7z":
                            decompress7z(input, output, options);
                            break;
                        default:
                            throw new ControlFlow.RuntimeError(null,
                                    "Formato não suportado: " + format);
                    }
                } catch (Exception e) {
                    throw new ControlFlow.RuntimeError(null,
                            "decompress: " + e.getMessage());
                }
                return true;
            }
        });

        // ─── list_archive(archive, format, options) ──────────────────
        interpreter.globals.defineConst("list_archive", new XplCallable() {
            @Override public int arity() { return -1; }
            @Override public Object call(Interpreter intp, List<Expr.CallArg> args) {
                if (args.size() < 2) {
                    throw new ControlFlow.RuntimeError(null,
                            "list_archive precisa de 2 argumentos: archive, format");
                }
                String archive = getString(intp, args, 0);
                String format = getString(intp, args, 1).toLowerCase();
                Map<String, Object> options = args.size() > 2 ? getMap(intp, args, 2) : new HashMap<>();

                try {
                    List<Map<String, Object>> entries = new ArrayList<>();
                    switch (format) {
                        case "zip":
                            entries = listZip(archive, options);
                            break;
                        case "tar":
                        case "tar.gz": case "tgz":
                        case "tar.bz2": case "tbz2":
                        case "tar.xz": case "txz":
                            entries = listTar(archive, format);
                            break;
                        case "7z":
                            entries = list7z(archive, options);
                            break;
                        default:
                            throw new ControlFlow.RuntimeError(null,
                                    "list_archive: formato não suportado para listagem: " + format);
                    }
                    return entries;
                } catch (Exception e) {
                    throw new ControlFlow.RuntimeError(null,
                            "list_archive: " + e.getMessage());
                }
            }
        });
    }

    // ──── ZIP ────
    private static void compressZip(String input, String output, Map<String, Object> options) throws IOException {
        Path inputPath = Paths.get(input);
        String password = options.containsKey("password") ? options.get("password").toString() : null;
        int level = options.containsKey("level") ? ((Number) options.get("level")).intValue() : 5;
        level = Math.max(0, Math.min(9, level));

        ZipParameters params = new ZipParameters();
        params.setCompressionMethod(CompressionMethod.DEFLATE);
        params.setCompressionLevel(getCompressionLevel(level));

        if (password != null && !password.isEmpty()) {
            params.setEncryptFiles(true);
            params.setEncryptionMethod(EncryptionMethod.AES);
            params.setAesKeyStrength(net.lingala.zip4j.model.enums.AesKeyStrength.KEY_STRENGTH_256);
        }

        ZipFile zipFile = new ZipFile(output, password != null ? password.toCharArray() : null);
        if (Files.isDirectory(inputPath)) {
            zipFile.addFolder(inputPath.toFile(), params);
        } else {
            zipFile.addFile(inputPath.toFile(), params);
        }
    }

    private static void decompressZip(String input, String output, Map<String, Object> options) throws IOException {
        String password = options.containsKey("password") ? options.get("password").toString() : null;
        ZipFile zipFile = new ZipFile(input, password != null ? password.toCharArray() : null);
        zipFile.extractAll(output);
    }

    private static List<Map<String, Object>> listZip(String archive, Map<String, Object> options) throws IOException {
        String password = options.containsKey("password") ? options.get("password").toString() : null;
        ZipFile zipFile = new ZipFile(archive, password != null ? password.toCharArray() : null);
        List<Map<String, Object>> entries = new ArrayList<>();
        for (FileHeader header : zipFile.getFileHeaders()) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", header.getFileName());
            info.put("size", header.getUncompressedSize());
            info.put("compressedSize", header.getCompressedSize());
            info.put("directory", header.isDirectory());
            entries.add(info);
        }
        return entries;
    }

    // ──── TAR ────
    private static void compressTar(String input, String output, boolean gzip, boolean bzip2, boolean xz) throws IOException {
        Path inputPath = Paths.get(input);
        try (OutputStream fos = Files.newOutputStream(Paths.get(output));
             OutputStream compressor = getCompressorOutputStream(fos, gzip, bzip2, xz);
             TarArchiveOutputStream taos = new TarArchiveOutputStream(compressor)) {

            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

            if (Files.isDirectory(inputPath)) {
                try (Stream<Path> walk = Files.walk(inputPath)) {
                    walk.forEach(p -> {
                        try {
                            String entryName = inputPath.relativize(p).toString();
                            if (entryName.isEmpty()) return;
                            TarArchiveEntry entry = new TarArchiveEntry(p.toFile(), entryName);
                            taos.putArchiveEntry(entry);
                            if (!Files.isDirectory(p)) {
                                Files.copy(p, taos);
                            }
                            taos.closeArchiveEntry();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
                }
            } else {
                TarArchiveEntry entry = new TarArchiveEntry(inputPath.toFile(), inputPath.getFileName().toString());
                taos.putArchiveEntry(entry);
                Files.copy(inputPath, taos);
                taos.closeArchiveEntry();
            }
        }
    }

    private static void decompressTar(String input, String output, boolean gzip, boolean bzip2, boolean xz) throws IOException {
        Path outputDir = Paths.get(output);
        if (!Files.exists(outputDir)) Files.createDirectories(outputDir);

        try (InputStream fis = Files.newInputStream(Paths.get(input));
             InputStream decompressor = getCompressorInputStream(fis, gzip, bzip2, xz);
             TarArchiveInputStream tais = new TarArchiveInputStream(decompressor)) {

            TarArchiveEntry entry;
            while ((entry = tais.getNextTarEntry()) != null) {
                Path target = outputDir.resolve(entry.getName()).normalize();
                if (!target.startsWith(outputDir)) {
                    throw new IOException("Tentativa de sair do diretório de destino.");
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(tais, target);
                }
            }
        }
    }

    private static List<Map<String, Object>> listTar(String archive, String format) throws IOException {
        boolean gzip = format.equals("tar.gz") || format.equals("tgz");
        boolean bzip2 = format.equals("tar.bz2") || format.equals("tbz2");
        boolean xz = format.equals("tar.xz") || format.equals("txz");

        List<Map<String, Object>> entries = new ArrayList<>();
        try (InputStream fis = Files.newInputStream(Paths.get(archive));
             InputStream decompressor = getCompressorInputStream(fis, gzip, bzip2, xz);
             TarArchiveInputStream tais = new TarArchiveInputStream(decompressor)) {

            TarArchiveEntry entry;
            while ((entry = tais.getNextTarEntry()) != null) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("name", entry.getName());
                info.put("size", entry.getSize());
                info.put("directory", entry.isDirectory());
                entries.add(info);
            }
        }
        return entries;
    }

    // ──── FICHEIRO ÚNICO ────
    private static void compressSingleFile(String input, String output, String type) throws IOException {
        try (InputStream is = Files.newInputStream(Paths.get(input));
             OutputStream os = Files.newOutputStream(Paths.get(output));
             OutputStream compressor = getSingleCompressor(os, type)) {
            IOUtils.copy(is, compressor);
        }
    }

    private static void decompressSingleFile(String input, String output, String type) throws IOException {
        try (InputStream is = Files.newInputStream(Paths.get(input));
             InputStream decompressor = getSingleDecompressor(is, type);
             OutputStream os = Files.newOutputStream(Paths.get(output))) {
            IOUtils.copy(decompressor, os);
        }
    }

    // ──── 7-ZIP (usando ZIP com AES) ────
    private static void compress7z(String input, String output, Map<String, Object> options) throws IOException {
        options.put("level", 9);
        compressZip(input, output, options);
    }

    private static void decompress7z(String input, String output, Map<String, Object> options) throws IOException {
        decompressZip(input, output, options);
    }

    private static List<Map<String, Object>> list7z(String archive, Map<String, Object> options) throws IOException {
        return listZip(archive, options);
    }

    // ──── FÁBRICAS DE STREAMS ────
    private static OutputStream getCompressorOutputStream(OutputStream out, boolean gzip, boolean bzip2, boolean xz) throws IOException {
        if (gzip) return new GzipCompressorOutputStream(out);
        if (bzip2) return new BZip2CompressorOutputStream(out);
        if (xz) return new XZCompressorOutputStream(out);
        return out;
    }

    private static InputStream getCompressorInputStream(InputStream in, boolean gzip, boolean bzip2, boolean xz) throws IOException {
        if (gzip) return new GzipCompressorInputStream(in);
        if (bzip2) return new BZip2CompressorInputStream(in);
        if (xz) return new XZCompressorInputStream(in);
        return in;
    }

    private static OutputStream getSingleCompressor(OutputStream out, String type) throws IOException {
        switch (type) {
            case "gzip": return new GzipCompressorOutputStream(out);
            case "bzip2": return new BZip2CompressorOutputStream(out);
            case "xz": return new XZCompressorOutputStream(out);
            default: throw new IOException("Tipo inválido: " + type);
        }
    }

    private static InputStream getSingleDecompressor(InputStream in, String type) throws IOException {
        switch (type) {
            case "gzip": return new GzipCompressorInputStream(in);
            case "bzip2": return new BZip2CompressorInputStream(in);
            case "xz": return new XZCompressorInputStream(in);
            default: throw new IOException("Tipo inválido: " + type);
        }
    }

    // ──── AUXILIARES ────
    private static CompressionLevel getCompressionLevel(int level) {
        switch (level) {
            case 0: return CompressionLevel.NO_COMPRESSION;
            case 1: return CompressionLevel.FASTEST;
            case 2: return CompressionLevel.FASTER;
            case 3: return CompressionLevel.FAST;
            case 4: return CompressionLevel.MEDIUM_FAST;
            case 5: return CompressionLevel.NORMAL;
            case 6: return CompressionLevel.HIGHER;
            case 7: return CompressionLevel.MAXIMUM;
            case 8: return CompressionLevel.PRE_ULTRA;
            case 9: return CompressionLevel.ULTRA;
            default: return CompressionLevel.NORMAL;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMap(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        if (val instanceof Map) return (Map<String, Object>) val;
        throw new ControlFlow.RuntimeError(null, "compress/decompress: esperado um dicionário para opções.");
    }

    private static String getString(Interpreter interpreter, List<Expr.CallArg> args, int index) {
        Object val = interpreter.evaluate(args.get(index).expression);
        return interpreter.stringify(val);
    }
}