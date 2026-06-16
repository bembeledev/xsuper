package com.dic.xsuper.commands.standard;
import com.dic.xsuper.core.Command;
import com.dic.xsuper.model.XFileIO;
import com.dic.xsuper.utils.ConsoleTheme;
import java.nio.file.Path;
import java.io.File;

public class MoveCmd implements Command {
    @Override public String getName() { return "mv"; }
    @Override public String getDescription() { return "Move ou renomeia. Uso: mv <origem> <destino>"; }
    @Override public Path execute(String[] args, Path currentDir) throws Exception {
        if (args.length < 3) { System.out.println(ConsoleTheme.WARNING + "Uso: mv <origem> <destino>" + ConsoleTheme.RESET); return currentDir; }
        File src = currentDir.resolve(args[1]).normalize().toFile();
        File dest = currentDir.resolve(args[2]).normalize().toFile();
        XFileIO.move(src, dest);
        System.out.println(ConsoleTheme.SUCCESS + "✅ Movido." + ConsoleTheme.RESET);
        return currentDir;
    }
}