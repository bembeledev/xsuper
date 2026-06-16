package com.dic.xsuper.commands.xsuper;

import com.dic.xsuper.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

public class PsCmd implements Command {
    @Override
    public String getName() { return "ps"; }

    @Override
    public String getDescription() { return "Lista os processos em execução no sistema. Uso: ps [filtro_nome]"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        String filter = args.length > 1 ? args[1].toLowerCase() : "";

        System.out.println(ConsoleTheme.HEADER + "  PID   | USER             | CPU TIME | COMMAND" + ConsoleTheme.RESET);
        System.out.println("----------------------------------------------------------------------");

        ProcessHandle.allProcesses().forEach(ph -> {
            ProcessHandle.Info info = ph.info();
            String cmd = info.command().orElse("");

            // Ignorar processos vazios do kernel
            if (cmd.isEmpty()) return;

            // Extrair apenas o nome do executável para ficar limpo
            String shortCmd = cmd.substring(cmd.lastIndexOf("\\") + 1);
            shortCmd = shortCmd.substring(shortCmd.lastIndexOf("/") + 1);

            if (filter.isEmpty() || shortCmd.toLowerCase().contains(filter)) {
                String user = info.user().orElse("SYSTEM");
                if (user.contains("\\")) user = user.substring(user.lastIndexOf("\\") + 1); // Limpa domínio no Windows

                long cpuMillis = info.totalCpuDuration().orElse(Duration.ZERO).toMillis();
                String cpuTime = String.format("%02d.%03ds", cpuMillis / 1000, cpuMillis % 1000);

                System.out.printf(ConsoleTheme.SUCCESS + "%7d" + ConsoleTheme.TEXT + " | %-16s | %-8s | %s%n" + ConsoleTheme.RESET,
                        ph.pid(),
                        user.length() > 16 ? user.substring(0, 15) + "…" : user,
                        cpuTime,
                        shortCmd);
            }
        });

        return currentDirectory;
    }
}