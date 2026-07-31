package com.dic.xsuper.cli.model;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

/**
 * Classe utilitária para leitura e escrita de ficheiros de texto.
 * Suporta diferentes encodings e operações básicas (copiar, mover, apagar).
 */
public class XFileIO {

    // ==================== LEITURA ====================

    /**
     * Lê todo o conteúdo de um ficheiro como String (UTF-8).
     */
    public static String readAllText(File file) throws IOException {
        return readAllText(file, StandardCharsets.UTF_8);
    }

    /**
     * Lê todo o conteúdo de um ficheiro como String com encoding específico.
     */
    public static String readAllText(File file, Charset charset) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new IOException("Ficheiro não existe ou não é ficheiro: " + file.getAbsolutePath());
        }
        return Files.readString(file.toPath(), charset);
    }

    /**
     * Lê todas as linhas de um ficheiro para uma List<String> (UTF-8).
     */
    public static List<String> readAllLines(File file) throws IOException {
        return readAllLines(file, StandardCharsets.UTF_8);
    }

    /**
     * Lê todas as linhas de um ficheiro para uma List<String> com encoding.
     */
    public static List<String> readAllLines(File file, Charset charset) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new IOException("Ficheiro não existe ou não é ficheiro: " + file.getAbsolutePath());
        }
        return Files.readAllLines(file.toPath(), charset);
    }

    /**
     * Lê o conteúdo de um ficheiro linha a linha (stream) – útil para ficheiros grandes.
     */
    public static Stream<String> lines(File file) throws IOException {
        return lines(file, StandardCharsets.UTF_8);
    }

    public static Stream<String> lines(File file, Charset charset) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new IOException("Ficheiro não existe ou não é ficheiro: " + file.getAbsolutePath());
        }
        return Files.lines(file.toPath(), charset);
    }

    // ==================== ESCRITA ====================

    /**
     * Escreve uma String num ficheiro (substitui o conteúdo existente) – UTF-8.
     */
    public static void writeAllText(File file, String content) throws IOException {
        writeAllText(file, content, StandardCharsets.UTF_8);
    }

    /**
     * Escreve uma String num ficheiro com encoding específico (substitui).
     */
    public static void writeAllText(File file, String content, Charset charset) throws IOException {
        ensureParentExists(file);
        Files.write(file.toPath(), content.getBytes(charset),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Escreve uma lista de linhas num ficheiro (substitui) – UTF-8.
     */
    public static void writeAllLines(File file, List<String> lines) throws IOException {
        writeAllLines(file, lines, StandardCharsets.UTF_8);
    }

    /**
     * Escreve uma lista de linhas num ficheiro com encoding (substitui).
     */
    public static void writeAllLines(File file, List<String> lines, Charset charset) throws IOException {
        ensureParentExists(file);
        Files.write(file.toPath(), lines, charset,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Adiciona uma String ao final do ficheiro (append) – UTF-8.
     */
    public static void appendText(File file, String content) throws IOException {
        appendText(file, content, StandardCharsets.UTF_8);
    }

    /**
     * Adiciona uma String ao final do ficheiro com encoding.
     */
    public static void appendText(File file, String content, Charset charset) throws IOException {
        ensureParentExists(file);
        Files.write(file.toPath(), content.getBytes(charset),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * Adiciona uma lista de linhas ao final do ficheiro (append) – UTF-8.
     */
    public static void appendLines(File file, List<String> lines) throws IOException {
        appendLines(file, lines, StandardCharsets.UTF_8);
    }

    public static void appendLines(File file, List<String> lines, Charset charset) throws IOException {
        ensureParentExists(file);
        Files.write(file.toPath(), lines, charset,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    // ==================== OPERAÇÕES DE FICHEIRO ====================

    /**
     * Copia um ficheiro (origem) para outro (destino) com sobrescrita.
     * Usa StandardCopyOption.REPLACE_EXISTING.
     */
    public static void copy(File source, File dest) throws IOException {
        if (!source.exists() || !source.isFile()) {
            throw new IOException("Ficheiro origem não existe ou não é ficheiro: " + source.getAbsolutePath());
        }
        ensureParentExists(dest);
        Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Move/renomeia um ficheiro.
     */
    public static void move(File source, File dest) throws IOException {
        if (!source.exists()) {
            throw new IOException("Ficheiro origem não existe: " + source.getAbsolutePath());
        }
        ensureParentExists(dest);
        Files.move(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Apaga um ficheiro (não pastas).
     */
    public static void delete(File file) throws IOException {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            throw new IOException("Não é possível apagar uma pasta com este método. Use deleteDirectory.");
        }
        Files.delete(file.toPath());
    }

    /**
     * Apaga um diretório recursivamente (cuidado!).
     */
    public static void deleteDirectory(File dir) throws IOException {
        if (!dir.exists()) return;
        if (!dir.isDirectory()) {
            throw new IOException("Não é um diretório: " + dir.getAbsolutePath());
        }
        Files.walk(dir.toPath())
                .sorted((a, b) -> b.compareTo(a)) // ordem inversa (apaga primeiro os filhos)
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /**
     * Cria o directório pai se não existir.
     */
    private static void ensureParentExists(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            Files.createDirectories(parent.toPath());
        }
    }

    /**
     * Cria um ficheiro vazio (se não existir) com os directórios pais.
     */
    public static void createFile(File file) throws IOException {
        ensureParentExists(file);
        if (!file.exists()) {
            Files.createFile(file.toPath());
        }
    }

    /**
     * Verifica se o ficheiro existe e é legível.
     */
    public static boolean existsAndReadable(File file) {
        return file != null && file.exists() && file.canRead();
    }
}