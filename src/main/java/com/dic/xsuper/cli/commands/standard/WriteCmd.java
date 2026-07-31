package com.dic.xsuper.cli.commands.standard;
import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.cli.model.XFileWriter;
import com.dic.xsuper.utils.ConsoleTheme;
import java.nio.file.Path;

public class WriteCmd implements Command {
    @Override public String getName() { return "write"; }
    @Override public String getDescription() { return "Escreve/sobrescreve texto. Uso: write <ficheiro> <conteúdo>"; }
    @Override public Path execute(String[] args, Path currentDir) throws Exception {
        if (args.length < 3) { System.out.println("Uso: write <ficheiro> <conteúdo>"); return currentDir; }
        StringBuilder content = new StringBuilder();
        for (int i = 2; i < args.length; i++) { if (i > 2) content.append(" "); content.append(args[i]); }
        XFileWriter.write(currentDir.resolve(args[1]).normalize().toFile(), content.toString());
        System.out.println(ConsoleTheme.SUCCESS + "✅ Ficheiro escrito." + ConsoleTheme.RESET);
        return currentDir;
    }
}