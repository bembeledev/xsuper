package com.dic.xsuper.cli.commands.standard;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

import java.io.File;
import java.nio.file.Path;

public class ChmodCmd implements Command {
    @Override
    public String getName() { return "chmod"; }

    @Override
    public String getDescription() { return "Altera as permissões de um ficheiro. Uso: chmod <+r|-r|+w|-w|+x|-x> <ficheiro>"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 3) {
            System.out.println(ConsoleTheme.WARNING + "⚠️ Uso: chmod <modificador> <ficheiro> (Ex: chmod +x script.sh ou chmod -w segredo.txt)" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        String mode = args[1].toLowerCase();
        File target = currentDirectory.resolve(args[2]).normalize().toFile();

        if (!target.exists()) {
            System.out.println(ConsoleTheme.ERROR + "❌ Ficheiro não encontrado." + ConsoleTheme.RESET);
            return currentDirectory;
        }

        boolean success = false;
        try {
            switch (mode) {
                case "+r": success = target.setReadable(true, false); break;
                case "-r": success = target.setReadable(false, false); break;
                case "+w": success = target.setWritable(true, false); break;
                case "-w": success = target.setWritable(false, false); break; // Torna Read-Only
                case "+x": success = target.setExecutable(true, false); break;
                case "-x": success = target.setExecutable(false, false); break;
                default:
                    System.out.println(ConsoleTheme.ERROR + "❌ Modificador inválido. Usa +r, -r, +w, -w, +x, -x." + ConsoleTheme.RESET);
                    return currentDirectory;
            }

            if (success) {
                System.out.println(ConsoleTheme.SUCCESS + "✅ Permissões atualizadas para '" + target.getName() + "'." + ConsoleTheme.RESET);
            } else {
                System.out.println(ConsoleTheme.ERROR + "❌ O Sistema Operativo recusou a alteração de permissões." + ConsoleTheme.RESET);
            }
        } catch (SecurityException e) {
            System.out.println(ConsoleTheme.ERROR + "❌ Acesso negado pelo sistema." + ConsoleTheme.RESET);
        }

        return currentDirectory;
    }
}