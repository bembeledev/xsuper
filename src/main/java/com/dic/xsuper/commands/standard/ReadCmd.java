package com.dic.xsuper.commands.standard;

import com.dic.xsuper.core.Command;
import com.dic.xsuper.model.XFileReader;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Path;
import java.io.File;
import java.util.List;

public class ReadCmd implements Command {
    @Override
    public String getName() { return "read"; }

    @Override
    public String getDescription() { return "Lê o conteúdo de um ficheiro de texto. Uso: read <ficheiro> [lines]"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 2) {
            System.out.println(ConsoleTheme.WARNING + "Uso: read <ficheiro> [lines|all]" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        File readFile = currentDirectory.resolve(args[1]).toFile();
        if (!readFile.exists()) {
            System.out.println(ConsoleTheme.ERROR + "❌ Ficheiro não existe." + ConsoleTheme.RESET);
            return currentDirectory;
        }

        try {
            if (args.length >= 3 && args[2].equalsIgnoreCase("lines")) {
                List<String> lines = XFileReader.readAllLines(readFile);
                for (int i = 0; i < lines.size(); i++) {
                    System.out.printf("%4d: %s%n", i + 1, lines.get(i));
                }
            } else {
                System.out.println(ConsoleTheme.TEXT + XFileReader.readAll(readFile) + ConsoleTheme.RESET);
            }
        } catch (Exception e) {
            System.out.println(ConsoleTheme.ERROR + "❌ Erro ao ler: " + e.getMessage() + ConsoleTheme.RESET);
        }
        return currentDirectory;
    }
}