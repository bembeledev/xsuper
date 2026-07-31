package com.dic.xsuper.cli.commands.standard;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class GrepCmd implements Command {
    @Override
    public String getName() { return "grep"; }

    @Override
    public String getDescription() { return "Filtra linhas que contêm um padrão. Uso: grep [-i] <padrão> <arquivo>"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 3) {
            System.out.println(ConsoleTheme.WARNING + "⚠️ Uso: grep [-i] <padrão> <arquivo>" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        boolean ignoreCase = args[1].equals("-i");
        String pattern = ignoreCase ? args[2].toLowerCase() : (args.length > 3 ? args[2] : args[1]);
        String fileName = ignoreCase ? args[3] : args[2];

        Path target = currentDirectory.resolve(fileName).normalize();

        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            System.out.println(ConsoleTheme.ERROR + "❌ Ficheiro inválido ou não encontrado: " + fileName + ConsoleTheme.RESET);
            return currentDirectory;
        }

        AtomicInteger lineNumber = new AtomicInteger(1);
        AtomicInteger matches = new AtomicInteger(0);

        try (Stream<String> lines = Files.lines(target)) {
            lines.forEach(line -> {
                String searchLine = ignoreCase ? line.toLowerCase() : line;
                if (searchLine.contains(pattern)) {
                    System.out.printf(ConsoleTheme.DIRECTORY + "%5d " + ConsoleTheme.TEXT + "| %s%n" + ConsoleTheme.RESET,
                            lineNumber.get(), line.replace(pattern, ConsoleTheme.WARNING + pattern + ConsoleTheme.TEXT));
                    matches.incrementAndGet();
                }
                lineNumber.incrementAndGet();
            });
        } catch (IOException e) {
            System.out.println(ConsoleTheme.ERROR + "❌ Erro ao ler ficheiro: " + e.getMessage() + ConsoleTheme.RESET);
        }

        System.out.println(ConsoleTheme.SUCCESS + "-> Encontradas " + matches.get() + " ocorrências." + ConsoleTheme.RESET);
        return currentDirectory;
    }
}