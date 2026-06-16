package com.dic.xsuper.commands.standard;

import com.dic.xsuper.core.Command;
import com.dic.xsuper.services.FileSystemService;
import java.nio.file.Path;

public class ListCmd implements Command {
    @Override
    public String getName() { return "ls"; }

    @Override
    public String getDescription() { return "Lists directory contents with details."; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        FileSystemService.listContents(currentDirectory);
        return currentDirectory;
    }
}