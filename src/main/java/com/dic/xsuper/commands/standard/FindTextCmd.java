package com.dic.xsuper.commands.standard;
import com.dic.xsuper.core.Command;
import com.dic.xsuper.controller.XFileController;
import java.nio.file.Path;
import java.io.File;
import java.util.List;

public class FindTextCmd implements Command {
    @Override public String getName() { return "findtext"; }
    @Override public String getDescription() { return "Procura texto dentro de ficheiros. Uso: findtext <texto> <dir>"; }
    @Override public Path execute(String[] args, Path currentDir) throws Exception {
        if (args.length < 3) { System.out.println("Uso: findtext <padrão> <diretório>"); return currentDir; }
        File searchDir = currentDir.resolve(args[2]).normalize().toFile();
        List<File> found = XFileController.searchTextInFiles(searchDir, args[1]);
        if (found == null || found.isEmpty()) System.out.println("Nenhuma ocorrência encontrada.");
        else found.forEach(f -> System.out.println(f.getAbsolutePath()));
        return currentDir;
    }
}