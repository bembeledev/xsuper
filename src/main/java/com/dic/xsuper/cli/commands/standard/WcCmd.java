package com.dic.xsuper.cli.commands.standard;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class WcCmd implements Command {
    @Override
    public String getName() { return "wc"; }

    @Override
    public String getDescription() { return "Conta linhas, palavras e bytes de um ficheiro. Uso: wc <arquivo>"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 2) {
            System.out.println(ConsoleTheme.WARNING + "⚠️ Uso: wc <arquivo>" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        Path target = currentDirectory.resolve(args[1]).normalize();

        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            System.out.println(ConsoleTheme.ERROR + "❌ Ficheiro não encontrado: " + args[1] + ConsoleTheme.RESET);
            return currentDirectory;
        }

        try {
            List<String> lines = Files.readAllLines(target);
            long lineCount = lines.size();
            long wordCount = lines.stream()
                    .mapToLong(line -> line.trim().split("\\s+").length)
                    .sum();
            long byteCount = Files.size(target);

            System.out.println(ConsoleTheme.TEXT + "Estatísticas para: " + ConsoleTheme.HEADER + target.getFileName() + ConsoleTheme.RESET);
            System.out.printf(ConsoleTheme.SUCCESS + "  %8d" + ConsoleTheme.TEXT + " Linhas%n", lineCount);
            System.out.printf(ConsoleTheme.SUCCESS + "  %8d" + ConsoleTheme.TEXT + " Palavras%n", wordCount);
            System.out.printf(ConsoleTheme.SUCCESS + "  %8d" + ConsoleTheme.TEXT + " Bytes%n" + ConsoleTheme.RESET, byteCount);

        } catch (IOException e) {
            System.out.println(ConsoleTheme.ERROR + "❌ Erro ao processar ficheiro." + ConsoleTheme.RESET);
        }

        return currentDirectory;
    }
}