package com.dic.xsuper.cli.commands.standard;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TailCmd implements Command {
    @Override
    public String getName() { return "tail"; }

    @Override
    public String getDescription() { return "Mostra as últimas linhas de um ficheiro. Uso: tail [-n num] <ficheiro>"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 2) {
            System.out.println(ConsoleTheme.WARNING + "⚠️ Uso: tail [-n 20] <ficheiro>" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        int linesToRead = 10; // Padrão Unix são 10 linhas
        String fileName = args[1];

        if (args[1].equals("-n") && args.length > 3) {
            linesToRead = Integer.parseInt(args[2]);
            fileName = args[3];
        }

        Path target = currentDirectory.resolve(fileName).normalize();

        if (!Files.exists(target)) {
            System.out.println(ConsoleTheme.ERROR + "❌ Ficheiro não encontrado." + ConsoleTheme.RESET);
            return currentDirectory;
        }

        // Lê rapidamente o ficheiro saltando tudo exceto as últimas N linhas
        try (Stream<String> lines = Files.lines(target)) {
            long totalLines = Files.lines(target).count();
            long skipLines = Math.max(0, totalLines - linesToRead);

            List<String> tail = lines.skip(skipLines).collect(Collectors.toList());

            for (String line : tail) {
                System.out.println(ConsoleTheme.TEXT + line + ConsoleTheme.RESET);
            }
        } catch (Exception e) {
            System.out.println(ConsoleTheme.ERROR + "❌ Erro ao ler o ficheiro: " + e.getMessage() + ConsoleTheme.RESET);
        }

        return currentDirectory;
    }
}