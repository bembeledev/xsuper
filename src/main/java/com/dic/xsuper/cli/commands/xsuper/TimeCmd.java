package com.dic.xsuper.cli.commands.xsuper;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.cli.core.CommandRegistry;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Path;

public class TimeCmd implements Command {

    private final CommandRegistry registry;

    public TimeCmd(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() { return "time"; }

    @Override
    public String getDescription() { return "Mede o tempo de execução de um comando. Uso: time <comando>"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 2) {
            System.out.println(ConsoleTheme.WARNING + "⚠️ Uso: time <comando> [argumentos...]" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        // Reconstrói o comando a ser cronometrado
        StringBuilder commandToRun = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            commandToRun.append(args[i]).append(" ");
        }

        System.out.println(ConsoleTheme.TEXT + "⏱️ A iniciar cronómetro para: " + commandToRun.toString().trim() + ConsoleTheme.RESET);

        long startTime = System.nanoTime();

        // Executa o comando internamente no motor
        Path newDir = registry.executeCommand(commandToRun.toString().trim(), currentDirectory);

        long endTime = System.nanoTime();
        long durationInMillis = (endTime - startTime) / 1_000_000;

        System.out.println(ConsoleTheme.HEADER + "========================================" + ConsoleTheme.RESET);
        System.out.printf(ConsoleTheme.SUCCESS + "⏱️ Tempo de Execução Real: %d ms (%.3f s)%n" + ConsoleTheme.RESET,
                durationInMillis, durationInMillis / 1000.0);
        System.out.println(ConsoleTheme.HEADER + "========================================" + ConsoleTheme.RESET);

        return newDir;
    }
}