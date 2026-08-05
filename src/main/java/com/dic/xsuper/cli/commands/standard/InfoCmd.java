package com.dic.xsuper.cli.commands.standard;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.cli.core.CommandRegistry;
import com.dic.xsuper.utils.ConsoleTheme;
import java.nio.file.Path;

public class InfoCmd implements Command {

    private final CommandRegistry registry;

    // Recebe o Registry no construtor
    public InfoCmd(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "Exibe manual detalhado e exemplos de um ou mais comandos. Uso: info <cmd1> [cmd2]";
    }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 2) {
            System.out.println(ConsoleTheme.WARNING + "  Uso: info <nome_do_comando> [outros_comandos...]" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        System.out.println(); // Espaçamento

        // Itera por todos os argumentos (info mkdir echo ls)
        for (int i = 1; i < args.length; i++) {
            String cmdName = args[i].toLowerCase();
            Command cmd = registry.getCommand(cmdName);

            System.out.println(ConsoleTheme.HEADER + "========================================" + ConsoleTheme.RESET);

            if (cmd != null) {
                System.out.println(ConsoleTheme.WARNING + " 📖 COMANDO: " + ConsoleTheme.SUCCESS + cmd.getName().toUpperCase());
                System.out.println(ConsoleTheme.DIRECTORY + " RESUMO:    " + ConsoleTheme.TEXT + cmd.getDescription());
                System.out.println(ConsoleTheme.DIRECTORY + " DETALHES:\n" + ConsoleTheme.TEXT + cmd.getDetailedInfo());
            } else {
                System.out.println(ConsoleTheme.ERROR + " ❌ Comando '" + cmdName + "' não encontrado no sistema." + ConsoleTheme.RESET);
            }

            System.out.println(ConsoleTheme.HEADER + "========================================\n" + ConsoleTheme.RESET);
        }

        return currentDirectory;
    }
}