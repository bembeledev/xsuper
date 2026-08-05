package com.dic.xsuper.cli.commands.standard;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.cli.core.CommandRegistry;
import com.dic.xsuper.cli.core.XatEngine;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Files;
import java.nio.file.Path;

public class CallCmd implements Command {

    private final CommandRegistry registry;

    public CallCmd(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "call";
    }

    @Override
    public String getDescription() {
        return "Executa um script compilado de automação .xat. Uso: call <script.xat>";
    }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 2) {
            System.out.println(ConsoleTheme.WARNING + "  Uso: call <nome_do_script.xat>" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        Path targetFile = currentDirectory.resolve(args[1]).normalize();

        if (!Files.exists(targetFile)) {
            System.out.println(ConsoleTheme.ERROR + "  Ficheiro de automação não encontrado: " + targetFile.getFileName() + ConsoleTheme.RESET);
            return currentDirectory;
        }

        // Lê o código completo e atira-o para o Mini-Motor
        String sourceCode = Files.readString(targetFile);
        XatEngine engine = new XatEngine(registry, currentDirectory);

        return engine.run(sourceCode);
    }
}