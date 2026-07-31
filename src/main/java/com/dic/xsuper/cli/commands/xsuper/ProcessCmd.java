package com.dic.xsuper.cli.commands.xsuper;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.cli.services.ProcessService;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Path;

public class ProcessCmd implements Command {
    @Override
    public String getName() { return "force-unlock"; }

    @Override
    public String getDescription() { return "Superpower: Kills system processes blocking a specific file."; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 2) {
            System.out.println(ConsoleTheme.WARNING + "Usage: force-unlock <file_name>" + ConsoleTheme.RESET);
            return currentDirectory;
        }
        Path target = currentDirectory.resolve(args[1]).normalize();
        boolean success = ProcessService.killProcessBlockingFile(target);

        if(success) {
            System.out.println(ConsoleTheme.SUCCESS + "✔ File is now unlocked." + ConsoleTheme.RESET);
        }
        return currentDirectory;
    }
}