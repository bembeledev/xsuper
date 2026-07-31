package com.dic.xsuper.cli.commands.standard;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

public class DuCmd implements Command {
    @Override
    public String getName() { return "du"; }

    @Override
    public String getDescription() { return "Mede o espaço em disco usado por uma pasta. Uso: du [pasta]"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        Path target = args.length > 1 ? currentDirectory.resolve(args[1]).normalize() : currentDirectory;

        if (!Files.exists(target) || !Files.isDirectory(target)) {
            System.out.println(ConsoleTheme.ERROR + "❌ Diretório inválido." + ConsoleTheme.RESET);
            return currentDirectory;
        }

        System.out.println(ConsoleTheme.TEXT + "A calcular uso de disco de '" + target.getFileName() + "'..." + ConsoleTheme.RESET);

        AtomicLong totalSize = new AtomicLong(0);
        AtomicLong fileCount = new AtomicLong(0);

        try {
            Files.walkFileTree(target, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    totalSize.addAndGet(attrs.size());
                    fileCount.incrementAndGet();
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    System.out.println(ConsoleTheme.WARNING + "   Acesso negado: " + file.getFileName() + ConsoleTheme.RESET);
                    return FileVisitResult.CONTINUE; // Continua a calcular mesmo se falhar nalgum ficheiro oculto
                }
            });

            System.out.println(ConsoleTheme.SUCCESS + "✅ Resumo de Disco:" + ConsoleTheme.RESET);
            System.out.println(ConsoleTheme.TEXT + "   Ficheiros verificados : " + fileCount.get());
            System.out.println("   Tamanho Total         : " + formatSize(totalSize.get()) + ConsoleTheme.RESET);

        } catch (IOException e) {
            System.out.println(ConsoleTheme.ERROR + "❌ Falha crítica ao calcular tamanho." + ConsoleTheme.RESET);
        }

        return currentDirectory;
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        int exp = (int) (Math.log(size) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.2f %cB", size / Math.pow(1024, exp), pre);
    }
}