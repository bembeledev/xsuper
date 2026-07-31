package com.dic.xsuper.cli.commands.standard;
import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;
import java.nio.file.Path;
import java.io.File;

public class EditCmd implements Command {
    @Override public String getName() { return "edit"; }
    @Override public String getDescription() { return "Abre o ficheiro num editor nativo."; }
    @Override public Path execute(String[] args, Path currentDir) throws Exception {
        if (args.length < 2) { System.out.println("Uso: edit <nome>"); return currentDir; }
        File editFile = currentDir.resolve(args[1]).normalize().toFile();
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb = os.contains("win") ? new ProcessBuilder("notepad", editFile.getAbsolutePath()) :
                os.contains("mac") ? new ProcessBuilder("open", "-t", editFile.getAbsolutePath()) :
                        new ProcessBuilder("gedit", editFile.getAbsolutePath());
        pb.start();
        System.out.println(ConsoleTheme.SUCCESS + "✅ Editor aberto." + ConsoleTheme.RESET);
        return currentDir;
    }
}