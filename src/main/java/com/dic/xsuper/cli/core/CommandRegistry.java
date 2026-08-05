package com.dic.xsuper.cli.core;

import com.dic.xsuper.utils.ConsoleTheme;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CommandRegistry {
    private final Map<String, Command> commands = new TreeMap<>();

    public void register(Command command) {
        commands.put(command.getName(), command);
    }

    public Path executeCommand(String input, Path currentDirectory) {
        if (input == null || input.trim().isEmpty()) return currentDirectory;


        // 1. Grava o comando no Histórico (Sprint 5)
        SessionManager.HISTORY.add(input);

        // 2. Resolve Aliases (Sprint 5) - Se digitares "ll", ele transforma em "ls"
        String resolvedInput = input.trim();
        for (Map.Entry<String, String> alias : SessionManager.ALIASES.entrySet()) {
            if (resolvedInput.equals(alias.getKey()) || resolvedInput.startsWith(alias.getKey() + " ")) {
                resolvedInput = resolvedInput.replaceFirst(alias.getKey(), alias.getValue());
                break;
            }
        }


        // Continua a usar o input resolvido a partir daqui
        String[] rawTokens = resolvedInput.trim().split("\\s+");
        List<String> cleanArgsList = new ArrayList<>();

        String redirectTarget = null;
        boolean appendMode = false;
        boolean redirectDetected = false;

        // Analisador para detetar o operador de redirecionamento ( > ou >> )
        for (int i = 0; i < rawTokens.length; i++) {
            if (rawTokens[i].equals(">") || rawTokens[i].equals(">>")) {
                redirectDetected = true;
                appendMode = rawTokens[i].equals(">>");
                if (i + 1 < rawTokens.length) {
                    redirectTarget = rawTokens[i + 1];
                }
                break;
            } else {
                cleanArgsList.add(rawTokens[i]);
            }
        }

        if (cleanArgsList.isEmpty()) return currentDirectory;

        String commandName = cleanArgsList.get(0).toLowerCase();
        String[] args = cleanArgsList.toArray(new String[0]);

        Command command = commands.get(commandName);
        if (command != null) {
            PrintStream originalOut = System.out;
            PrintStream fileOut = null;

            try {
                // Ativa o Redirecionamento Limpo (Sem cores ANSI)
                if (redirectDetected && redirectTarget != null) {
                    Path outPath = currentDirectory.resolve(redirectTarget).normalize();
                    File outFile = outPath.toFile();

                    File parent = outFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }

                    // Cria o canal para o ficheiro e embrulha-o no nosso filtro removedor de cores
                    FileOutputStream fos = new FileOutputStream(outFile, appendMode);
                    AnsiFilterOutputStream cleanStream = new AnsiFilterOutputStream(fos);

                    fileOut = new PrintStream(cleanStream, true, "UTF-8");
                    System.setOut(fileOut); // Altera System.out temporariamente
                }

                // Executa o comando
                currentDirectory = command.execute(args, currentDirectory);

            } catch (Exception e) {
                System.setOut(originalOut);
                System.out.println(ConsoleTheme.ERROR + "[ERRO] Falha crítica: " + e.getMessage() + ConsoleTheme.RESET);
            } finally {
                if (fileOut != null) {
                    fileOut.flush();
                    fileOut.close();
                }
                System.setOut(originalOut); // Restaura o terminal sempre!
            }
        } else {
            System.out.println(ConsoleTheme.WARNING + "Comando não encontrado: '" + commandName + "'. Digita 'help'." + ConsoleTheme.RESET);
        }
        return currentDirectory;
    }

    public void printHelp() {
        System.out.println("\n" + ConsoleTheme.HEADER + "=== XPLORER SUPER POWERS ===" + ConsoleTheme.RESET);
        commands.values().forEach(cmd ->
                System.out.printf(ConsoleTheme.COMMAND + "  %-15s" + ConsoleTheme.RESET + " : %s%n", cmd.getName(), cmd.getDescription())
        );
        System.out.println();
    }

    // Adiciona este método se ainda não existir
    public Command getCommand(String name) {
        return commands.get(name.toLowerCase()); // Assumindo que 'commands' é o teu Map<String, Command>
    }

    // =================================================================================
    // SUPERPODER FORENSE: Filtro de Máquina de Estados para limpar Códigos de Cor ANSI
    // =================================================================================
    private static class AnsiFilterOutputStream extends FilterOutputStream {
        private boolean inEscapeSequence = false;

        public AnsiFilterOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            if (inEscapeSequence) {
                // Códigos ANSI terminam sempre com uma letra (geralmente 'm' ou 'K')
                if (Character.isLetter(b)) {
                    inEscapeSequence = false;
                }
                return; // Ignora e não escreve no ficheiro enquanto for código de cor
            }

            // 27 em decimal é o caracter 'ESC' (Início de um código de cor)
            if (b == 27) {
                inEscapeSequence = true;
                return;
            }

            super.write(b); // Escreve o caracter limpo
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            for (int i = off; i < off + len; i++) {
                write(b[i]);
            }
        }
    }
}