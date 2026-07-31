package com.dic.xsuper.cli.commands.standard;
import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.cli.model.XFileIO;
import java.nio.file.Path;

public class CatCmd implements Command {
    @Override public String getName() { return "cat"; }
    @Override public String getDescription() { return "Exibe o conteúdo completo do ficheiro."; }
    @Override public Path execute(String[] args, Path currentDir) throws Exception {
        if (args.length < 2) { System.out.println("Uso: cat <nome>"); return currentDir; }
        System.out.println(XFileIO.readAllText(currentDir.resolve(args[1]).normalize().toFile()));
        return currentDir;
    }
}