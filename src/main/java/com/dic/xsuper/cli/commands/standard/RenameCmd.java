package com.dic.xsuper.cli.commands.standard;
import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.cli.controller.XFileController;
import com.dic.xsuper.utils.ConsoleTheme;
import java.nio.file.Path;
import java.io.File;

public class RenameCmd implements Command {
    private final XFileController controller = new XFileController();
    @Override public String getName() { return "rename"; }
    @Override public String getDescription() { return "Renomeia um ficheiro. Uso: rename <antigo> <novo>"; }
    @Override public Path execute(String[] args, Path currentDir) {
        if (args.length < 3) { System.out.println(ConsoleTheme.WARNING + "Uso: rename <antigo> <novo>" + ConsoleTheme.RESET); return currentDir; }
        File oldFile = currentDir.resolve(args[1]).normalize().toFile();
        if (!oldFile.exists()) { System.out.println(ConsoleTheme.ERROR + "❌ Arquivo não encontrado." + ConsoleTheme.RESET); }
        else {
            boolean success = controller.rename(oldFile, args[2]);
            System.out.println(success ? ConsoleTheme.SUCCESS + "✅ Renomeado." + ConsoleTheme.RESET : ConsoleTheme.ERROR + "❌ Falha ao renomear." + ConsoleTheme.RESET);
        }
        return currentDir;
    }
}