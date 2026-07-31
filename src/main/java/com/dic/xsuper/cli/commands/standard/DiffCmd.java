package com.dic.xsuper.cli.commands.standard;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DiffCmd implements Command {
    @Override
    public String getName() { return "diff"; }

    @Override
    public String getDescription() { return "Compara dois ficheiros de texto linha a linha. Uso: diff <file1> <file2>"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 3) {
            System.out.println(ConsoleTheme.WARNING + "⚠️ Uso: diff <ficheiro1> <ficheiro2>" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        Path f1 = currentDirectory.resolve(args[1]).normalize();
        Path f2 = currentDirectory.resolve(args[2]).normalize();

        if (!Files.exists(f1) || !Files.exists(f2)) {
            System.out.println(ConsoleTheme.ERROR + "❌ Um ou ambos os ficheiros não existem." + ConsoleTheme.RESET);
            return currentDirectory;
        }

        List<String> lines1 = Files.readAllLines(f1);
        List<String> lines2 = Files.readAllLines(f2);

        int maxLines = Math.max(lines1.size(), lines2.size());
        boolean hasDifferences = false;

        System.out.println(ConsoleTheme.TEXT + "A comparar: " + f1.getFileName() + " <--> " + f2.getFileName() + ConsoleTheme.RESET);
        System.out.println("--------------------------------------------------");

        for (int i = 0; i < maxLines; i++) {
            String l1 = i < lines1.size() ? lines1.get(i) : null;
            String l2 = i < lines2.size() ? lines2.get(i) : null;

            if (l1 == null) {
                System.out.println(ConsoleTheme.SUCCESS + "+ (linha " + (i+1) + "): " + l2 + ConsoleTheme.RESET);
                hasDifferences = true;
            } else if (l2 == null) {
                System.out.println(ConsoleTheme.ERROR + "- (linha " + (i+1) + "): " + l1 + ConsoleTheme.RESET);
                hasDifferences = true;
            } else if (!l1.equals(l2)) {
                System.out.println(ConsoleTheme.ERROR + "- (linha " + (i+1) + "): " + l1 + ConsoleTheme.RESET);
                System.out.println(ConsoleTheme.SUCCESS + "+ (linha " + (i+1) + "): " + l2 + ConsoleTheme.RESET);
                hasDifferences = true;
            }
        }

        if (!hasDifferences) {
            System.out.println(ConsoleTheme.SUCCESS + "✅ Os ficheiros são exatamente iguais." + ConsoleTheme.RESET);
        }

        return currentDirectory;
    }
}