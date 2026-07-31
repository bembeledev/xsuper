package com.dic.xsuper.cli.commands.xsuper;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.cli.core.CommandRegistry;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Path;

public class WatchCmd implements Command {

    private final CommandRegistry registry;

    public WatchCmd(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() { return "watch"; }

    @Override
    public String getDescription() { return "Executa um comando repetidamente. Uso: watch [-n segundos] <comando>"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 2) {
            System.out.println(ConsoleTheme.WARNING + "⚠️ Uso: watch [-n segundos] <comando>" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        int intervalSeconds = 2; // Padrão: 2 segundos
        int cmdStartIndex = 1;

        if (args[1].equals("-n") && args.length > 3) {
            try {
                intervalSeconds = Integer.parseInt(args[2]);
                cmdStartIndex = 3;
            } catch (NumberFormatException e) {
                System.out.println(ConsoleTheme.ERROR + "❌ Intervalo inválido." + ConsoleTheme.RESET);
                return currentDirectory;
            }
        }

        StringBuilder commandToRun = new StringBuilder();
        for (int i = cmdStartIndex; i < args.length; i++) {
            commandToRun.append(args[i]).append(" ");
        }
        String cmdStr = commandToRun.toString().trim();

        System.out.println(ConsoleTheme.TEXT + "A monitorizar '" + cmdStr + "' a cada " + intervalSeconds + "s. (Pressiona Ctrl+C para sair)" + ConsoleTheme.RESET);

        try {
            while (true) {
                // Limpa o ecrã simulando o comando 'clear'
                System.out.print("\033[H\033[2J");
                System.out.flush();

                System.out.println(ConsoleTheme.HEADER + "Monitorização Ativa | Comando: " + cmdStr + " | Atualizado a cada " + intervalSeconds + "s" + ConsoleTheme.RESET);
                System.out.println("------------------------------------------------------------------");

                registry.executeCommand(cmdStr, currentDirectory);

                Thread.sleep(intervalSeconds * 1000L);
            }
        } catch (InterruptedException e) {
            System.out.println(ConsoleTheme.WARNING + "\nMonitorização interrompida." + ConsoleTheme.RESET);
        }

        return currentDirectory;
    }
}