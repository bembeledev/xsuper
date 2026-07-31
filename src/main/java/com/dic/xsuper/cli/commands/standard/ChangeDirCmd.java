package com.dic.xsuper.cli.commands.standard;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Files;
import java.nio.file.Path;

public class ChangeDirCmd implements Command {
    @Override
    public String getName() { return "cd"; }

    @Override
    public String getDescription() { return "Changes the current directory (e.g., cd .. ou cd folder)."; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 2) return currentDirectory;

        Path newPath = currentDirectory.resolve(args[1]).normalize();
        if (Files.exists(newPath) && Files.isDirectory(newPath)) {
            return newPath;
        } else {
            System.out.println(ConsoleTheme.ERROR + "Path not found: " + args[1] + ConsoleTheme.RESET);
            return currentDirectory;
        }
    }
}