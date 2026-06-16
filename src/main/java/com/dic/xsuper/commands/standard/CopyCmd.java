package com.dic.xsuper.commands.standard;

import com.dic.xsuper.core.Command;
import com.dic.xsuper.controller.XFileController;
import com.dic.xsuper.utils.ConsoleTheme;

import java.io.File;
import java.nio.file.Path;

public class CopyCmd implements Command {

    private final XFileController controller = new XFileController();

    @Override
    public String getName() {
        return "copy";
    }

    @Override
    public String getDescription() {
        return "Copia um ficheiro ou diretório recursivamente. Uso: copy <origem> <destino>";
    }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 3) {
            System.out.println(ConsoleTheme.WARNING + "⚠️ Uso: copy <origem> <destino>" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        // Resolve os caminhos tendo como base o diretório atual
        File src = currentDirectory.resolve(args[1]).normalize().toFile();
        File dest = currentDirectory.resolve(args[2]).normalize().toFile();

        if (!src.exists()) {
            System.out.println(ConsoleTheme.ERROR + "❌ O ficheiro/diretório de origem não existe: " + args[1] + ConsoleTheme.RESET);
            return currentDirectory;
        }

        System.out.println(ConsoleTheme.TEXT + "A copiar de '" + src.getName() + "' para '" + dest.getName() + "'..." + ConsoleTheme.RESET);

        try {
            boolean success = controller.copy(src, dest);
            if (success) {
                System.out.println(ConsoleTheme.SUCCESS + "✅ Cópia concluída com sucesso." + ConsoleTheme.RESET);
            } else {
                System.out.println(ConsoleTheme.ERROR + "❌ Falha ao realizar a cópia." + ConsoleTheme.RESET);
            }
        } catch (Exception e) {
            System.out.println(ConsoleTheme.ERROR + "❌ Erro durante a cópia: " + e.getMessage() + ConsoleTheme.RESET);
        }

        return currentDirectory;
    }
}