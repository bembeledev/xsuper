package com.dic.xsuper.model;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class XFileRun {

    /**
     * Abre um ficheiro com o programa padrão do sistema.
     * Se o ficheiro for um diretório, abre-o no explorador de ficheiros.
     */
    public static void openWithDefault(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IOException("Ficheiro não existe: " + (file != null ? file.getAbsolutePath() : "null"));
        }
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(file);
        } else {
            // Fallback por SO
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", "start", "", file.getAbsolutePath());
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", file.getAbsolutePath());
            } else {
                pb = new ProcessBuilder("xdg-open", file.getAbsolutePath());
            }
            pb.start();
        }
    }

    /**
     * Executa um ficheiro executável (pode ser absoluto, relativo ou no PATH).
     * @param file O executável (pode ser apenas o nome, ex: "notepad")
     * @param args Lista de argumentos (pode ser null)
     * @return Processo criado
     */
    public static Process execute(File file, List<String> args) throws IOException {
        if (file == null) throw new IOException("Ficheiro nulo.");

        // Se o ficheiro não existe ou não é executável, tenta resolver no PATH
        File resolved = resolveExecutable(file);
        if (!resolved.exists() || !resolved.canExecute()) {
            throw new IOException("Ficheiro não existe ou não é executável: " + file.getAbsolutePath());
        }

        List<String> command = new ArrayList<>();
        command.add(resolved.getAbsolutePath());
        if (args != null) command.addAll(args);
        ProcessBuilder pb = new ProcessBuilder(command);
        return pb.start();
    }

    /**
     * Executa um ficheiro com um programa específico.
     * @param program O programa (ex: "notepad", "code")
     * @param target O ficheiro alvo
     * @param args Argumentos adicionais (opcional)
     * @return Processo criado
     */
    public static Process executeWithProgram(File program, File target, List<String> args) throws IOException {
        if (program == null) throw new IOException("Programa nulo.");
        if (target == null || !target.exists()) {
            throw new IOException("Alvo não existe: " + (target != null ? target.getAbsolutePath() : "null"));
        }

        File resolvedProgram = resolveExecutable(program);
        if (!resolvedProgram.exists() || !resolvedProgram.canExecute()) {
            throw new IOException("Programa não encontrado ou não executável: " + program.getAbsolutePath());
        }

        List<String> command = new ArrayList<>();
        command.add(resolvedProgram.getAbsolutePath());
        command.add(target.getAbsolutePath());
        if (args != null) command.addAll(args);
        ProcessBuilder pb = new ProcessBuilder(command);
        return pb.start();
    }

    /**
     * Executa um script/ficheiro no terminal (shell).
     * @param script O script (pode ser .sh, .bat, etc.)
     * @param args Argumentos
     * @return Processo criado
     */
    public static Process executeInShell(File script, List<String> args) throws IOException {
        if (script == null || !script.exists()) {
            throw new IOException("Script não existe: " + (script != null ? script.getAbsolutePath() : "null"));
        }
        if (!script.canExecute()) {
            // Tenta tornar executável (no Linux/Mac)
            if (!System.getProperty("os.name").toLowerCase().contains("win")) {
                script.setExecutable(true);
            }
        }

        String os = System.getProperty("os.name").toLowerCase();
        List<String> command = new ArrayList<>();
        if (os.contains("win")) {
            command.add("cmd");
            command.add("/c");
            command.add(script.getAbsolutePath());
        } else {
            command.add("/bin/sh");
            command.add("-c");
            command.add(script.getAbsolutePath());
        }
        if (args != null) command.addAll(args);
        ProcessBuilder pb = new ProcessBuilder(command);
        return pb.start();
    }

    /**
     * Executa um comando do sistema (ex: "dir", "ls -la").
     * @param commandString O comando completo (ex: "ls -la")
     * @return Processo criado
     */
    public static Process runCommand(String commandString) throws IOException {
        if (commandString == null || commandString.trim().isEmpty()) {
            throw new IOException("Comando vazio.");
        }
        String os = System.getProperty("os.name").toLowerCase();
        List<String> command = new ArrayList<>();
        if (os.contains("win")) {
            command.add("cmd");
            command.add("/c");
            command.add(commandString);
        } else {
            command.add("/bin/sh");
            command.add("-c");
            command.add(commandString);
        }
        ProcessBuilder pb = new ProcessBuilder(command);
        return pb.start();
    }

    // ============ MÉTODOS AUXILIARES ============

    /**
     * Resolve um executável: se for caminho absoluto ou relativo, verifica a existência.
     * Se não existir, procura no PATH (variável de ambiente).
     * @param file O ficheiro a resolver
     * @return O ficheiro encontrado (pode ser o mesmo ou um no PATH)
     */
    private static File resolveExecutable(File file) {
        if (file == null) return null;
        if (file.exists() && file.canExecute()) return file;

        // Se for apenas um nome (sem separadores), procura no PATH
        if (file.getParent() == null || file.getParent().isEmpty()) {
            String name = file.getName();
            String pathEnv = System.getenv("PATH");
            if (pathEnv != null) {
                String[] dirs = pathEnv.split(File.pathSeparator);
                for (String dir : dirs) {
                    File candidate = new File(dir, name);
                    if (candidate.exists() && candidate.canExecute()) {
                        return candidate;
                    }
                }
            }
        }
        return file; // retorna o original (mesmo que não exista)
    }
}