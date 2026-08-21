package com.dic.xsuper.cli.commands.xsuper;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.stream.Stream;
import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

public class AwkCmd implements Command {
    @Override
    public String getName() { return "awk"; }

    @Override
    public String getDescription() { return "Extrai colunas de um ficheiro estruturado. Uso: awk <1,3,4> <ficheiro> [-d delimitador]"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 3) {
            System.out.println(ConsoleTheme.WARNING + "⚠️ Uso: awk <colunas> <ficheiro> [-d delimitador]\n   Exemplo: awk 1,3 dados.csv -d ," + ConsoleTheme.RESET);
            return currentDirectory;
        }

        String[] colsToExtract = args[1].split(",");
        Path target = currentDirectory.resolve(args[2]).normalize();
        String delimiter; // Por padrão, divide por espaços

        if (args.length > 4 && args[3].equals("-d")) {
            delimiter = args[4];
        } else {
            delimiter = "\\s+";
        }

        if (!Files.exists(target)) {
            System.out.println(ConsoleTheme.ERROR + "❌ Ficheiro não encontrado." + ConsoleTheme.RESET);
            return currentDirectory;
        }

        try (Stream<String> lines = Files.lines(target)) {
            lines.forEach(line -> {
                String[] columns = line.split(delimiter);
                StringBuilder output = new StringBuilder();

                for (String colIndex : colsToExtract) {
                    try {
                        int idx = Integer.parseInt(colIndex.trim()) - 1; // AWK começa na coluna 1, arrays em 0
                        if (idx >= 0 && idx < columns.length) {
                            output.append(columns[idx]).append("\t");
                        }
                    } catch (NumberFormatException ignored) {}
                }

                if (!output.isEmpty()) {
                    System.out.println(ConsoleTheme.TEXT + output.toString().trim() + ConsoleTheme.RESET);
                }
            });
        } catch (Exception e) {
            System.out.println(ConsoleTheme.ERROR + "❌ Erro ao ler colunas: " + e.getMessage() + ConsoleTheme.RESET);
        }

        return currentDirectory;
    }
}