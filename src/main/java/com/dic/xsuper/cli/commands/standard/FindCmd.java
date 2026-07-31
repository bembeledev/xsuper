package com.dic.xsuper.cli.commands.standard;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;

public class FindCmd implements Command {
    @Override
    public String getName() { return "find"; }

    @Override
    public String getDescription() { return "Pesquisa avançada de ficheiros. Uso: find [dir] [-name \"*.txt\"] [-type f/d]"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        Path searchDir = currentDirectory;
        String namePattern = null;
        String typeFilter = null; // 'f' para ficheiro, 'd' para pasta

        // Analisador de Argumentos Básico
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("-name") && i + 1 < args.length) {
                namePattern = args[++i].replace("\"", "").replace("*", "");
            } else if (args[i].equals("-type") && i + 1 < args.length) {
                typeFilter = args[++i].toLowerCase();
            } else if (i == 1 && !args[i].startsWith("-")) {
                searchDir = currentDirectory.resolve(args[i]).normalize();
            }
        }

        if (!Files.exists(searchDir)) {
            System.out.println(ConsoleTheme.ERROR + "❌ Diretório de pesquisa não existe." + ConsoleTheme.RESET);
            return currentDirectory;
        }

        System.out.println(ConsoleTheme.TEXT + "🔍 A pesquisar em: " + searchDir.toAbsolutePath() + "..." + ConsoleTheme.RESET);
        AtomicInteger count = new AtomicInteger(0);

        final String finalNamePattern = namePattern;
        final String finalTypeFilter = typeFilter;

        try {
            Path finalSearchDir = searchDir;
            Files.walkFileTree(searchDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    checkAndPrint(file, attrs.isDirectory(), finalNamePattern, finalTypeFilter, count);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!dir.equals(finalSearchDir)) {
                        checkAndPrint(dir, true, finalNamePattern, finalTypeFilter, count);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE; // Ignora ficheiros ocultos sem permissão
                }
            });

            System.out.println(ConsoleTheme.SUCCESS + "-> Encontrados " + count.get() + " resultados." + ConsoleTheme.RESET);

        } catch (IOException e) {
            System.out.println(ConsoleTheme.ERROR + "❌ Erro durante a pesquisa: " + e.getMessage() + ConsoleTheme.RESET);
        }

        return currentDirectory;
    }

    private void checkAndPrint(Path path, boolean isDir, String pattern, String type, AtomicInteger count) {
        if (type != null) {
            if (type.equals("f") && isDir) return;
            if (type.equals("d") && !isDir) return;
        }

        if (pattern != null) {
            if (!path.getFileName().toString().toLowerCase().contains(pattern.toLowerCase())) return;
        }

        String typeLabel = isDir ? ConsoleTheme.DIRECTORY + "[DIR] " : ConsoleTheme.TEXT + "[FILE] ";
        System.out.println(typeLabel + ConsoleTheme.TEXT + path.toAbsolutePath() + ConsoleTheme.RESET);
        count.incrementAndGet();
    }
}