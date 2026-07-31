package com.dic.xsuper.cli.commands.standard;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.cli.controller.XFileProperties;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Path;
import java.io.File;

public class PropsCmd implements Command {
    @Override
    public String getName() { return "props"; }

    @Override
    public String getDescription() { return "Mostra metadados e propriedades avançadas de um ficheiro."; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 2) {
            System.out.println(ConsoleTheme.WARNING + "Uso: props <nome_do_ficheiro>" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        File propFile = currentDirectory.resolve(args[1]).toFile();
        if (!propFile.exists()) {
            System.out.println(ConsoleTheme.ERROR + "❌ Ficheiro não existe: " + args[1] + ConsoleTheme.RESET);
        } else {
            XFileProperties.showProperties(propFile); // Reutiliza a tua classe de UI antiga perfeitamente!
        }
        return currentDirectory;
    }
}