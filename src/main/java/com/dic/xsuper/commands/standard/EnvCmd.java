package com.dic.xsuper.commands.standard;

import com.dic.xsuper.core.Command;
import com.dic.xsuper.core.SessionManager;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class EnvCmd implements Command {
    @Override
    public String getName() { return "env"; }

    @Override
    public String getDescription() { return "Carrega e lista variáveis de ambiente (ficheiros .env). Uso: env [ficheiro]"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length == 1) {
            // Lista variáveis atuais carregadas
            System.out.println(ConsoleTheme.HEADER + "--- Variáveis de Sessão Carregadas ---" + ConsoleTheme.RESET);
            SessionManager.ENV_VARS.forEach((k, v) ->
                    System.out.println(ConsoleTheme.SUCCESS + k + ConsoleTheme.TEXT + "=" + v + ConsoleTheme.RESET)
            );
            return currentDirectory;
        }

        Path envFile = currentDirectory.resolve(args[1]).normalize();

        if (!Files.exists(envFile)) {
            System.out.println(ConsoleTheme.ERROR + "❌ Ficheiro não encontrado: " + args[1] + ConsoleTheme.RESET);
            return currentDirectory;
        }

        int count = 0;
        List<String> lines = Files.readAllLines(envFile);
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue; // Ignora comentários

            if (line.contains("=")) {
                String[] parts = line.split("=", 2);
                SessionManager.ENV_VARS.put(parts[0].trim(), parts[1].trim());
                count++;
            }
        }

        System.out.println(ConsoleTheme.SUCCESS + "✅ " + count + " variáveis carregadas de " + envFile.getFileName() + ConsoleTheme.RESET);
        return currentDirectory;
    }
}