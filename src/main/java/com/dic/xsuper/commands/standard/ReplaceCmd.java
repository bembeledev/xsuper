package com.dic.xsuper.commands.standard;

import com.dic.xsuper.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

public class ReplaceCmd implements Command {
    @Override
    public String getName() { return "replace"; }

    @Override
    public String getDescription() { return "Substitui texto dentro de um ficheiro. Uso: replace <antigo> <novo> <arquivo>"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 4) {
            System.out.println(ConsoleTheme.WARNING + "⚠️ Uso: replace \"antigo\" \"novo\" <arquivo>" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        String targetStr = args[1].replace("\"", "");
        String replacementStr = args[2].replace("\"", "");
        Path targetFile = currentDirectory.resolve(args[3]).normalize();

        if (!Files.exists(targetFile)) {
            System.out.println(ConsoleTheme.ERROR + "❌ Ficheiro não encontrado." + ConsoleTheme.RESET);
            return currentDirectory;
        }

        try {
            List<String> originalLines = Files.readAllLines(targetFile);
            List<String> modifiedLines = originalLines.stream()
                    .map(line -> line.replace(targetStr, replacementStr))
                    .collect(Collectors.toList());

            Files.write(targetFile, modifiedLines, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println(ConsoleTheme.SUCCESS + "✅ Substituição ('" + targetStr + "' -> '" + replacementStr + "') aplicada ao ficheiro." + ConsoleTheme.RESET);
        } catch (Exception e) {
            System.out.println(ConsoleTheme.ERROR + "❌ Erro ao modificar ficheiro: " + e.getMessage() + ConsoleTheme.RESET);
        }

        return currentDirectory;
    }
}