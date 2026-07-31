package com.dic.xsuper.cli.commands.standard;
import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.cli.model.XFileRun;
import com.dic.xsuper.utils.ConsoleTheme;
import java.nio.file.Path;

public class OpenCmd implements Command {
    @Override public String getName() { return "open"; }
    @Override public String getDescription() { return "Abre com o programa padrão. Uso: open <nome>"; }
    @Override public Path execute(String[] args, Path currentDir) throws Exception {
        if (args.length < 2) { System.out.println("Uso: open <nome>"); return currentDir; }
        XFileRun.openWithDefault(currentDir.resolve(args[1]).normalize().toFile());
        System.out.println(ConsoleTheme.SUCCESS + "✅ Aberto com programa padrão." + ConsoleTheme.RESET);
        return currentDir;
    }
}