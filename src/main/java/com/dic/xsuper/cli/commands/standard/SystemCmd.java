package com.dic.xsuper.cli.commands.standard;
import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.cli.model.XFileRun;
import com.dic.xsuper.utils.ConsoleTheme;
import java.nio.file.Path;

public class SystemCmd implements Command {
    @Override public String getName() { return "cmd"; }
    @Override public String getDescription() { return "Executa comando do SO. Uso: cmd <comando>"; }
    @Override public Path execute(String[] args, Path currentDir) throws Exception {
        if (args.length < 2) { System.out.println("Uso: cmd <comando>"); return currentDir; }
        StringBuilder cmdStr = new StringBuilder();
        for (int i = 1; i < args.length; i++) cmdStr.append(args[i]).append(" ");
        Process p = XFileRun.runCommand(cmdStr.toString().trim());
        System.out.println(ConsoleTheme.SUCCESS + "✅ Comando executado (PID: " + p.pid() + ")." + ConsoleTheme.RESET);
        return currentDir;
    }
}