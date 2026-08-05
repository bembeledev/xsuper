package com.dic.xsuper.cli.commands.standard;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;
import java.nio.file.Path;

public class EchoCmd implements Command {

    @Override
    public String getName() {
        return "echo";
    }

    @Override
    public String getDescription() {
        return "Imprime texto no ecrã. Uso: echo <mensagem>";
    }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 2) {
            System.out.println();
            return currentDirectory;
        }

        int startIndex = 1;
        String color = ConsoleTheme.TEXT;

        String firstArg = args[1].toLowerCase();

        // ⭐ NOVA LÓGICA: Verifica se é a flag -hex
        if (firstArg.equals("-hex") || firstArg.equals("-h")) {
            // Garante que o utilizador passou o código HEX a seguir à flag
            if (args.length > 2) {
                String hexCode = args[2].replace("\"", "").replace("'", "");
                color = hexToAnsi(hexCode);
                startIndex = 3; // O texto da mensagem só começa no índice 3
            }
        }
        // Lógica anterior para as cores padrão
        else {
            switch (firstArg) {
                case "-red", "-r" -> { color = ConsoleTheme.ERROR; startIndex = 2; }
                case "-green", "-g" -> { color = ConsoleTheme.SUCCESS; startIndex = 2; }
                case "-yellow", "-y" -> { color = ConsoleTheme.WARNING; startIndex = 2; }
                case "-blue", "-b" -> { color = ConsoleTheme.DIRECTORY; startIndex = 2; }
                case "-purple", "-p" -> { color = ConsoleTheme.HEADER; startIndex = 2; }
            }
        }

        if (startIndex >= args.length) {
            System.out.println();
            return currentDirectory;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            sb.append(args[i]).append(" ");
        }

        String output = sb.toString().trim();
        if (output.startsWith("\"") && output.endsWith("\"") && output.length() > 1) {
            output = output.substring(1, output.length() - 1);
        } else if (output.startsWith("'") && output.endsWith("'") && output.length() > 1) {
            output = output.substring(1, output.length() - 1);
        }

        // Imprime usando a cor calculada (seja do tema ou o HEX convertido)
        System.out.println(color + output + ConsoleTheme.RESET);

        return currentDirectory;
    }

    // ⭐ NOVO MÉTODO AUXILIAR: Converte HEX para RGB ANSI
    private String hexToAnsi(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }

        // Valida se tem os 6 caracteres corretos (ex: FF0000)
        if (hex.length() != 6) return ConsoleTheme.TEXT;

        try {
            int r = Integer.valueOf(hex.substring(0, 2), 16);
            int g = Integer.valueOf(hex.substring(2, 4), 16);
            int b = Integer.valueOf(hex.substring(4, 6), 16);
            // Formato ANSI 24-bit para True Color
            return String.format("\033[38;2;%d;%d;%dm", r, g, b);
        } catch (Exception e) {
            return ConsoleTheme.TEXT; // Se houver erro (ex: letras inválidas), usa a cor padrão
        }
    }
}