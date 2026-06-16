package com.dic.xsuper.commands.standard;

import com.dic.xsuper.core.Command;

import java.nio.file.Path;

public class MakeDirCmd implements Command {
    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        return null;
    }
}
