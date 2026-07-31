package com.dic.xsuper.cli.commands.standard;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

public class TreeCmd implements Command {
    @Override
    public String getName() { return "tree"; }

    @Override
    public String getDescription() { return "Desenha a árvore visual de diretórios. Uso: tree [pasta]"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        Path target = args.length > 1 ? currentDirectory.resolve(args[1]).normalize() : currentDirectory;

        File rootDir = target.toFile();
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            System.out.println(ConsoleTheme.ERROR + "❌ Diretório inválido." + ConsoleTheme.RESET);
            return currentDirectory;
        }

        System.out.println(ConsoleTheme.DIRECTORY + rootDir.getName() + ConsoleTheme.RESET);
        printTree(rootDir, "", true);

        return currentDirectory;
    }

    private void printTree(File folder, String prefix, boolean isTail) {
        File[] files = folder.listFiles();
        if (files == null) return;

        // Ordena para que as pastas apareçam primeiro
        Arrays.sort(files, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });

        for (int i = 0; i < files.length; i++) {
            boolean isLast = (i == files.length - 1);
            File file = files[i];

            // Ignora pastas pesadas para não prender o terminal (ex: node_modules, .git)
            if (file.getName().equals("node_modules") || file.getName().equals(".git")) continue;

            System.out.print(ConsoleTheme.TEXT + prefix + (isLast ? "└── " : "├── ") + ConsoleTheme.RESET);

            if (file.isDirectory()) {
                System.out.println(ConsoleTheme.DIRECTORY + file.getName() + ConsoleTheme.RESET);
                printTree(file, prefix + (isLast ? "    " : "│   "), isLast);
            } else {
                System.out.println(ConsoleTheme.TEXT + file.getName() + ConsoleTheme.RESET);
            }
        }
    }
}