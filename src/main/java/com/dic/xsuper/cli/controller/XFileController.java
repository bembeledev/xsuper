package com.dic.xsuper.cli.controller;

import com.dic.xsuper.cli.model.FileHelper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Controlador de Ficheiros de Alta Performance.
 * Utiliza exclusivamente a API NIO.2 do Java para máxima eficiência.
 */
public class XFileController {

    /**
     * Procura texto dentro de todos os ficheiros de um diretório de forma otimizada.
     * Utiliza Streams para não sobrecarregar a memória RAM.
     */
    public static List<File> searchTextInFiles(File searchDir, String pattern) throws IOException {
        List<File> foundFiles = new ArrayList<>();
        if (searchDir == null || !searchDir.exists() || !searchDir.isDirectory()) return foundFiles;

        try (Stream<Path> paths = Files.walk(searchDir.toPath())) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                try (Stream<String> lines = Files.lines(path)) {
                    // Tenta encontrar o padrão; se encontrar, adiciona à lista e avança para o próximo ficheiro
                    if (lines.anyMatch(line -> line.toLowerCase().contains(pattern.toLowerCase()))) {
                        foundFiles.add(path.toFile());
                    }
                } catch (MalformedInputException e) {
                    // Ignora ficheiros binários (que não podem ser lidos como texto)
                } catch (IOException ignored) {
                    // Ignora ficheiros sem permissão de leitura
                }
            });
        }
        return foundFiles;
    }

    /**
     * Lista o conteúdo do diretório lendo os atributos nativos do sistema.
     */
    public List<FileInfo> listDirectory(File dir) {
        List<FileInfo> result = new ArrayList<>();
        if (dir == null || !dir.isDirectory()) return result;

        try (Stream<Path> stream = Files.list(dir.toPath())) {
            result = stream.map(path -> {
                try {
                    BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
                    return new FileInfo(path.toFile(), attr);
                } catch (IOException e) {
                    return new FileInfo(path.toFile()); // Fallback seguro
                }
            }).collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("[ERRO] Falha ao ler diretório: " + e.getMessage());
        }
        return result;
    }

    public boolean createDirectory(File parent, String name) {
        try {
            Files.createDirectory(parent.toPath().resolve(name));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean createFile(File parent, String name) {
        try {
            Files.createFile(parent.toPath().resolve(name));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean rename(File src, String newName) {
        if (src == null || !src.exists()) return false;
        Path srcPath = src.toPath();
        Path destPath = srcPath.resolveSibling(newName);
        try {
            Files.move(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Cópia robusta de ficheiros e diretórios (incluindo subpastas).
     */
    public boolean copy(File src, File dest) throws IOException {
        if (src == null || !src.exists()) return false;

        Path sourcePath = src.toPath();
        Path targetPath = dest.toPath();

        if (Files.isDirectory(sourcePath)) {
            // Algoritmo profundo de cópia para pastas
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path targetDir = targetPath.resolve(sourcePath.relativize(dir));
                    if (!Files.exists(targetDir)) {
                        Files.createDirectory(targetDir);
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.copy(file, targetPath.resolve(sourcePath.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return true;
    }

    public boolean move(File src, File dest) {
        try {
            Files.move(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Eliminação madura: processa ficheiros do mais profundo para o mais raso para evitar erros de "Pasta não vazia".
     */
    public boolean delete(File file, boolean recursive) {
        if (!file.exists()) return false;
        Path path = file.toPath();

        try {
            if (Files.isDirectory(path) && recursive) {
                try (Stream<Path> walk = Files.walk(path)) {
                    walk.sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try {
                                    Files.delete(p);
                                } catch (IOException ignored) {} // Força a eliminação sem quebrar o loop
                            });
                }
                return !Files.exists(path);
            } else {
                Files.delete(path);
                return true;
            }
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Força a eliminação usando FileHelper (mata processos pendurados).
     */
    public boolean forceDelete(File file) {
        return FileHelper.forceDelete(file);
    }

    /**
     * Pesquisa de ficheiros com streams, alta performance e sem estourar a memória.
     */
    public List<File> search(File dir, String pattern) {
        if (dir == null || !dir.isDirectory()) return new ArrayList<>();

        try (Stream<Path> walk = Files.walk(dir.toPath())) {
            return walk
                    .filter(p -> p.getFileName().toString().toLowerCase().contains(pattern.toLowerCase()))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("[ERRO] Falha na pesquisa: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // =========================================================
    // Classe interna para metadados otimizada com NIO.2
    // =========================================================
    public static class FileInfo {
        public final String name;
        public final boolean isDirectory;
        public final long size;
        public final long lastModified;
        public final boolean hidden;

        public FileInfo(File f, BasicFileAttributes attr) {
            this.name = f.getName();
            this.isDirectory = attr.isDirectory();
            this.size = attr.size();
            this.lastModified = attr.lastModifiedTime().toMillis();
            this.hidden = f.isHidden();
        }

        public FileInfo(File f) {
            this.name = f.getName();
            this.isDirectory = f.isDirectory();
            this.size = f.length();
            this.lastModified = f.lastModified();
            this.hidden = f.isHidden();
        }

        @Override
        public String toString() {
            return String.format("%s %s %,d bytes [%s]",
                    isDirectory ? "[DIR] " : "[FILE]",
                    name,
                    size,
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(lastModified));
        }
    }
}