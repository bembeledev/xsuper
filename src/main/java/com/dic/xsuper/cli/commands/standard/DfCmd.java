package com.dic.xsuper.cli.commands.standard;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class DfCmd implements Command {
    @Override
    public String getName() { return "df"; }

    @Override
    public String getDescription() { return "Mostra o espaço livre e usado nos discos do sistema."; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        System.out.println(ConsoleTheme.HEADER + String.format("%-20s %-15s %-12s %-12s %-12s", "SISTEMA", "TIPO", "TOTAL", "USADO", "LIVRE") + ConsoleTheme.RESET);
        System.out.println("-------------------------------------------------------------------------------");

        for (FileStore store : FileSystems.getDefault().getFileStores()) {
            try {
                long total = store.getTotalSpace();
                long livre = store.getUsableSpace();
                long usado = total - livre;

                if (total > 0) { // Ignora partições virtuais vazias do sistema
                    System.out.printf(ConsoleTheme.TEXT + "%-20s %-15s %-12s %-12s %-12s%n" + ConsoleTheme.RESET,
                            store.name().isEmpty() ? store.toString() : store.name(),
                            store.type(),
                            formatSize(total),
                            formatSize(usado),
                            ConsoleTheme.SUCCESS + formatSize(livre) + ConsoleTheme.TEXT);
                }
            } catch (Exception ignored) {
                // Algumas partições podem negar acesso
            }
        }
        return currentDirectory;
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        int exp = (int) (Math.log(size) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %cB", size / Math.pow(1024, exp), pre);
    }
}