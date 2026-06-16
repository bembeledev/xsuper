package com.dic.xsuper.commands.standard;

import com.dic.xsuper.core.Command;
import java.nio.file.Path;

public class ClearCmd implements Command {
    @Override
    public String getName() { return "clear"; }

    @Override
    public String getDescription() { return "Limpa o ecrã do terminal."; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        // Envia o código ANSI oficial para limpar o ecrã e repor o cursor no topo
        System.out.print("\033[H\033[2J");
        System.out.flush();
        return currentDirectory;
    }
}