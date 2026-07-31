package com.dic.xsuper.cli.commands.xsuper;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Path;
import java.util.Optional;

public class KillCmd implements Command {
    @Override
    public String getName() { return "kill"; }

    @Override
    public String getDescription() { return "Força o fecho de um processo pelo seu PID. Uso: kill <PID>"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 2) {
            System.out.println(ConsoleTheme.WARNING + "⚠️ Uso: kill <PID>" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        try {
            long pid = Long.parseLong(args[1].replace("-9", "").trim()); // Ignora o -9 se o user o escrever por hábito do Linux
            Optional<ProcessHandle> process = ProcessHandle.of(pid);

            if (process.isPresent()) {
                System.out.println(ConsoleTheme.WARNING + "A enviar sinal SIGKILL para o PID " + pid + "..." + ConsoleTheme.RESET);
                boolean success = process.get().destroyForcibly();

                if (success) {
                    System.out.println(ConsoleTheme.SUCCESS + "✅ Processo " + pid + " terminado com sucesso." + ConsoleTheme.RESET);
                } else {
                    System.out.println(ConsoleTheme.ERROR + "❌ Acesso Negado. O processo pode pertencer ao Kernel ou a outro utilizador." + ConsoleTheme.RESET);
                }
            } else {
                System.out.println(ConsoleTheme.ERROR + "❌ PID não encontrado: " + pid + ConsoleTheme.RESET);
            }
        } catch (NumberFormatException e) {
            System.out.println(ConsoleTheme.ERROR + "❌ PID inválido. Deve ser um número." + ConsoleTheme.RESET);
        }

        return currentDirectory;
    }
}