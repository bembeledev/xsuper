package com.dic.xsuper.cli.model;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class XFileWriter {

    /**
     * Escreve conteúdo num ficheiro (sobrescreve) com UTF-8.
     */
    public static void write(File file, String content) throws IOException {
        write(file, content, StandardCharsets.UTF_8, false);
    }

    /**
     * Escreve conteúdo num ficheiro com um encoding específico (sobrescreve).
     */
    public static void write(File file, String content, Charset charset) throws IOException {
        write(file, content, charset, false);
    }

    /**
     * Escreve conteúdo num ficheiro com opção de append (adicionar ao final).
     */
    public static void write(File file, String content, Charset charset, boolean append) throws IOException {
        if (file == null) throw new IOException("Ficheiro nulo.");
        ensureParentExists(file);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), charset))) {
            writer.write(content);
        }
    }

    /**
     * Escreve uma lista de linhas no ficheiro (sobrescreve) com UTF-8.
     */
    public static void writeLines(File file, List<String> lines) throws IOException {
        writeLines(file, lines, StandardCharsets.UTF_8, false);
    }

    /**
     * Escreve uma lista de linhas no ficheiro com encoding e opção de append.
     */
    public static void writeLines(File file, List<String> lines, Charset charset, boolean append) throws IOException {
        if (file == null) throw new IOException("Ficheiro nulo.");
        ensureParentExists(file);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), charset))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    /**
     * Escreve bytes num ficheiro (sobrescreve).
     */
    public static void writeBytes(File file, byte[] data) throws IOException {
        if (file == null) throw new IOException("Ficheiro nulo.");
        ensureParentExists(file);
        Files.write(file.toPath(), data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Adiciona bytes ao final do ficheiro (append).
     */
    public static void appendBytes(File file, byte[] data) throws IOException {
        if (file == null) throw new IOException("Ficheiro nulo.");
        ensureParentExists(file);
        Files.write(file.toPath(), data, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * Obtém um {@link PrintWriter} para escrita formatada.
     */
    public static PrintWriter getPrintWriter(File file, Charset charset, boolean append) throws IOException {
        if (file == null) throw new IOException("Ficheiro nulo.");
        ensureParentExists(file);
        return new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, append), charset));
    }

    /**
     * Obtém um {@link BufferedWriter} para escrita eficiente.
     */
    public static BufferedWriter getBufferedWriter(File file, Charset charset, boolean append) throws IOException {
        if (file == null) throw new IOException("Ficheiro nulo.");
        ensureParentExists(file);
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), charset));
    }

    /**
     * Obtém um {@link OutputStream} para escrita binária.
     */
    public static OutputStream getOutputStream(File file, boolean append) throws IOException {
        if (file == null) throw new IOException("Ficheiro nulo.");
        ensureParentExists(file);
        return new FileOutputStream(file, append);
    }

    /**
     * Copia um ficheiro (origem) para outro (destino) com sobrescrita.
     */
    public static void copy(File source, File dest) throws IOException {
        if (!source.exists() || !source.isFile()) {
            throw new IOException("Ficheiro origem não existe ou não é ficheiro: " + source.getAbsolutePath());
        }
        ensureParentExists(dest);
        Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Garante que o diretório pai do ficheiro existe.
     */
    private static void ensureParentExists(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                throw new IOException("Não foi possível criar o diretório pai: " + parent.getAbsolutePath());
            }
        }
    }
}