package com.dic.xsuper.commands.standard;

import com.dic.xsuper.core.Command;
import com.dic.xsuper.core.SessionManager;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Path;
import java.util.Map;

public class AliasCmd implements Command {
    @Override
    public String getName() { return "alias"; }

    @Override
    public String getDescription() { return "Cria atalhos para comandos. Uso: alias <nome>='<comando>'"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        // Se digitar apenas "alias", lista os atalhos existentes
        if (args.length == 1) {
            if (SessionManager.ALIASES.isEmpty()) {
                System.out.println(ConsoleTheme.TEXT + "Nenhum alias definido nesta sessão." + ConsoleTheme.RESET);
            } else {
                for (Map.Entry<String, String> entry : SessionManager.ALIASES.entrySet()) {
                    System.out.println(ConsoleTheme.SUCCESS + "alias " + entry.getKey() + ConsoleTheme.TEXT + "='" + entry.getValue() + "'" + ConsoleTheme.RESET);
                }
            }
            return currentDirectory;
        }

        // Junta tudo para processar ex: alias ll="ls -l"
        StringBuilder fullArgs = new StringBuilder();
        for (int i = 1; i < args.length; i++) fullArgs.append(args[i]).append(" ");
        String expression = fullArgs.toString().trim();

        if (expression.contains("=")) {
            String[] parts = expression.split("=", 2);
            String key = parts[0].trim();
            String value = parts[1].trim().replace("'", "").replace("\"", ""); // Remove aspas

            SessionManager.ALIASES.put(key, value);
            System.out.println(ConsoleTheme.SUCCESS + "✅ Alias criado: " + key + " -> " + value + ConsoleTheme.RESET);
        } else {
            System.out.println(ConsoleTheme.ERROR + "❌ Sintaxe inválida. Usa: alias ll='ls -l'" + ConsoleTheme.RESET);
        }

        return currentDirectory;
    }
}