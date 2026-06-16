package com.dic.xsuper.model;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class XFileReader {

    /**
     * Lê todo o conteúdo do ficheiro como String com o encoding padrão UTF-8.
     */
    public static String readAll(File file) throws IOException {
        return readAll(file, StandardCharsets.UTF_8);
    }

    /**
     * Lê todo o conteúdo do ficheiro como String com um encoding específico.
     */
    public static String readAll(File file, Charset charset) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new IOException("Ficheiro não existe ou não é um ficheiro: " + file.getAbsolutePath());
        }
        return new String(Files.readAllBytes(file.toPath()), charset);
    }

    /**
     * Lê todas as linhas do ficheiro como List<String> com UTF-8.
     */
    public static List<String> readAllLines(File file) throws IOException {
        return readAllLines(file, StandardCharsets.UTF_8);
    }

    /**
     * Lê todas as linhas do ficheiro como List<String> com um encoding específico.
     */
    public static List<String> readAllLines(File file, Charset charset) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new IOException("Ficheiro não existe ou não é um ficheiro: " + file.getAbsolutePath());
        }
        return Files.readAllLines(file.toPath(), charset);
    }

    /**
     * Lê o ficheiro linha a linha (para ficheiros grandes) e processa cada linha.
     * @param file O ficheiro
     * @param charset O encoding (ex: StandardCharsets.UTF_8)
     * @param lineHandler Interface funcional para processar cada linha (retorna false para parar)
     */
    public static void readLines(File file, Charset charset, LineHandler lineHandler) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new IOException("Ficheiro não existe ou não é um ficheiro: " + file.getAbsolutePath());
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (!lineHandler.handle(line, lineNumber)) {
                    break;
                }
            }
        }
    }

    /**
     * Lê o ficheiro em chunks (útil para binários).
     */
    public static byte[] readBytes(File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new IOException("Ficheiro não existe ou não é um ficheiro: " + file.getAbsolutePath());
        }
        return Files.readAllBytes(file.toPath());
    }

    /**
     * Lê o ficheiro com um {@link InputStream} (para streaming).
     */
    public static InputStream getInputStream(File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new IOException("Ficheiro não existe ou não é um ficheiro: " + file.getAbsolutePath());
        }
        return new FileInputStream(file);
    }

    /**
     * Lê o ficheiro com um {@link BufferedReader} (para leitura eficiente de texto).
     */
    public static BufferedReader getBufferedReader(File file, Charset charset) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new IOException("Ficheiro não existe ou não é um ficheiro: " + file.getAbsolutePath());
        }
        return new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
    }

    /**
     * Detecta o encoding do ficheiro (heurística simples - pode não ser 100% fiável).
     */
    public static Charset detectCharset(File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new IOException("Ficheiro não existe ou não é um ficheiro: " + file.getAbsolutePath());
        }
        try (InputStream is = new FileInputStream(file)) {
            byte[] bom = new byte[4];
            int read = is.read(bom);
            if (read >= 3 && bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
                return StandardCharsets.UTF_8;
            }
            // Pode adicionar outros checks (UTF-16LE, etc.)
        }
        return StandardCharsets.UTF_8; // fallback
    }

    @FunctionalInterface
    public interface LineHandler {
        boolean handle(String line, int lineNumber);
    }
}