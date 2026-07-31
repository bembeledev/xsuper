package com.dic.xsuper.cli.commands.standard;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class SortCmd implements Command {
    @Override
    public String getName() { return "sort"; }

    @Override
    public String getDescription() { return "Ordena as linhas de um ficheiro de texto. Uso: sort [-r] <ficheiro>"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 2) {
            System.out.println(ConsoleTheme.WARNING + "⚠️ Uso: sort [-r] <ficheiro>" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        boolean reverse = args[1].equals("-r");
        String fileName = reverse && args.length > 2 ? args[2] : args[1];

        Path target = currentDirectory.resolve(fileName).normalize();

        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            System.out.println(ConsoleTheme.ERROR + "❌ Ficheiro não encontrado." + ConsoleTheme.RESET);
            return currentDirectory;
        }

        List<String> lines = Files.readAllLines(target);

        if (reverse) {
            lines.sort(Collections.reverseOrder());
        } else {
            Collections.sort(lines);
        }

        // Imprime o resultado. Pode ser redirecionado com > para outro ficheiro!
        for (String line : lines) {
            System.out.println(ConsoleTheme.TEXT + line + ConsoleTheme.RESET);
        }

        return currentDirectory;
    }
}