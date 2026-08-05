package com.dic.xsuper.cli.commands.standard;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;
import java.nio.file.Files;
import java.nio.file.Path;

public class MakeDirCmd implements Command {

    @Override
    public String getName() {
        return "mkdir";
    }

    @Override
    public String getDescription() {
        return "Cria um ou mais diretórios. Uso: mkdir <pasta1> [pasta2] [pasta3...]";
    }

    @Override
    public String getDetailedInfo() {
        return """
                 Cria novos diretórios no sistema de ficheiros atual.
                 Possui capacidade de criar diretórios em lote (vários ao mesmo tempo)
                 e suporta a criação automática de caminhos aninhados.

                 Exemplos de uso prático:
                   > mkdir pasta_simples
                   > mkdir pasta1 pasta2 pasta3
                   > mkdir src/main/java

                 No motor .XAT:
                   run "mkdir build/dist"
               """;
    }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 2) {
            System.out.println(ConsoleTheme.WARNING + "  Uso: mkdir <pasta1> [pasta2] [pasta3...]" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        // Itera por todos os argumentos a partir de index 1 para criar múltiplos diretórios
        for (int i = 1; i < args.length; i++) {
            String rawArg = args[i].replace("\"", "").replace("'", "");
            if (rawArg.isEmpty()) continue;

            Path target = currentDirectory.resolve(rawArg).normalize();

            if (Files.exists(target)) {
                System.out.println(ConsoleTheme.WARNING + "  O diretório já existe: " + target.getFileName() + ConsoleTheme.RESET);
                continue;
            }

            try {
                // Cria a pasta (e subpastas se necessário, ex: pai/filho)
                Files.createDirectories(target);
                System.out.println(ConsoleTheme.SUCCESS + "  Diretório criado: " + target.getFileName() + ConsoleTheme.RESET);
            } catch (Exception e) {
                System.out.println(ConsoleTheme.ERROR + "  Falha ao criar o diretório '" + target.getFileName() + "': " + e.getMessage() + ConsoleTheme.RESET);
            }
        }

        return currentDirectory;
    }
}