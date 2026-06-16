package com.dic.xsuper.commands.standard;
import com.dic.xsuper.core.Command;
import com.dic.xsuper.controller.XFileController;
import java.nio.file.Path;
import java.io.File;
import java.util.List;

public class SearchCmd implements Command {
    private final XFileController controller = new XFileController();
    @Override public String getName() { return "search"; }
    @Override public String getDescription() { return "Busca arquivos por nome. Uso: search <padrão>"; }
    @Override public Path execute(String[] args, Path currentDir) {
        if (args.length < 2) { System.out.println("Uso: search <padrão>"); return currentDir; }
        List<File> found = controller.search(currentDir.toFile(), args[1]);
        if (found.isEmpty()) System.out.println("Nenhum resultado.");
        else found.forEach(f -> System.out.println(f.getAbsolutePath()));
        return currentDir;
    }
}