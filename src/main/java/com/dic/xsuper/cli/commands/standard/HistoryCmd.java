package com.dic.xsuper.cli.commands.standard;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.cli.core.SessionManager;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Path;
import java.util.List;

public class HistoryCmd implements Command {
    @Override
    public String getName() { return "history"; }

    @Override
    public String getDescription() { return "Mostra o histórico de comandos. Uso: history [-c] [-n num]"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length > 1 && args[1].equals("-c")) {
            SessionManager.HISTORY.clear();
            System.out.println(ConsoleTheme.SUCCESS + "✅ Histórico de sessão limpo." + ConsoleTheme.RESET);
            return currentDirectory;
        }

        int limit = SessionManager.HISTORY.size();
        if (args.length > 2 && args[1].equals("-n")) {
            try {
                limit = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {}
        }

        List<String> history = SessionManager.HISTORY;
        int start = Math.max(0, history.size() - limit);

        for (int i = start; i < history.size(); i++) {
            System.out.printf(ConsoleTheme.DIRECTORY + "%5d" + ConsoleTheme.TEXT + "  %s%n" + ConsoleTheme.RESET, (i + 1), history.get(i));
        }

        return currentDirectory;
    }
}