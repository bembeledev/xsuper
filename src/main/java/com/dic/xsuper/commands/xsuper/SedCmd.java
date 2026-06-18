package com.dic.xsuper.commands.xsuper;

import com.dic.xsuper.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class SedCmd implements Command {
    @Override
    public String getName() { return "sed"; }

    @Override
    public String getDescription() { return "Substitui texto usando Expressões Regulares. Uso: sed s/padrão/novo/ <ficheiro>"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 3 || (!args[1].startsWith("s/") && !args[1].startsWith("\"s/"))) {
            System.out.println(ConsoleTheme.WARNING + "⚠️ Uso: sed s/antigo/novo/ <ficheiro>" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        String expression = args[1].replace("\"", "");
        String[] parts = expression.split("/");

        if (parts.length < 3) {
            System.out.println(ConsoleTheme.ERROR + "❌ Expressão inválida. Formato correto: s/regex/substituto/" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        String regex = parts[1];
        String replacement = parts[2];
        Path target = currentDirectory.resolve(args[2]).normalize();

        if (!Files.exists(target)) {
            System.out.println(ConsoleTheme.ERROR + "❌ Ficheiro não encontrado: " + target.getFileName() + ConsoleTheme.RESET);
            return currentDirectory;
        }

        // Processa o ficheiro linha a linha sem o carregar todo para a RAM
        try (Stream<String> lines = Files.lines(target)) {
            lines.forEach(line -> {
                String modifiedLine = line.replaceAll(regex, ConsoleTheme.WARNING + replacement + ConsoleTheme.TEXT);
                System.out.println(ConsoleTheme.TEXT + modifiedLine + ConsoleTheme.RESET);
            });
        } catch (Exception e) {
            System.out.println(ConsoleTheme.ERROR + "❌ Erro ao processar: " + e.getMessage() + ConsoleTheme.RESET);
        }

        return currentDirectory;
    }
}