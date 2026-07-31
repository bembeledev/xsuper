package com.dic.xsuper.cli.commands.standard;
import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;
import java.nio.file.Path;

public class PwdCmd implements Command {
    @Override public String getName() { return "pwd"; }
    @Override public String getDescription() { return "Mostra o caminho do diretório atual."; }
    @Override public Path execute(String[] args, Path currentDir) {
        System.out.println(ConsoleTheme.DIRECTORY + currentDir.toAbsolutePath() + ConsoleTheme.RESET);
        return currentDir;
    }
}