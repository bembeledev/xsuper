package com.dic.xsuper.cli.commands.standard;
import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.cli.controller.XFileController;
import com.dic.xsuper.utils.ConsoleTheme;
import java.nio.file.Path;

public class TouchCmd implements Command {
    private final XFileController controller = new XFileController();
    @Override public String getName() { return "touch"; }
    @Override public String getDescription() { return "Cria um arquivo vazio. Uso: touch <nome>"; }
    @Override public Path execute(String[] args, Path currentDir) throws Exception {
        if (args.length < 2) { System.out.println(ConsoleTheme.WARNING + "Uso: touch <nome>" + ConsoleTheme.RESET); return currentDir; }
        boolean success = controller.createFile(currentDir.toFile(), args[1]);
        System.out.println(success ? ConsoleTheme.SUCCESS + "✅ Arquivo criado." + ConsoleTheme.RESET : ConsoleTheme.ERROR + "❌ Falha ao criar." + ConsoleTheme.RESET);
        return currentDir;
    }
}