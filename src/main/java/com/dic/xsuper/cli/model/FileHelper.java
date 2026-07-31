package com.dic.xsuper.cli.model;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class FileHelper {

    /**
     * Verifica se um arquivo está bloqueado por outro processo.
     * Tenta abrir o arquivo em modo read-only com RandomAccessFile.
     * Se conseguir, não está bloqueado; se falhar, está bloqueado.
     */
    public static boolean isFileLocked(File file) {
        if (!file.exists() || !file.isFile()) return false;
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            // tenta ler o primeiro byte – se conseguir, não está bloqueado
            raf.read();
            return false;
        } catch (IOException e) {
            // erro ao aceder – provavelmente bloqueado
            return true;
        }
    }

    /**
     * Obtém PIDs de processos que estão a usar o arquivo (Windows/Linux).
     */
    public static List<Long> getProcessesUsingFile(File file) {
        List<Long> pids = new ArrayList<>();
        String os = System.getProperty("os.name").toLowerCase();
        long currentPid = ProcessHandle.current().pid(); // PID do próprio explorador
        try {
            if (os.contains("win")) {
                String path = file.getAbsolutePath().replace("\\", "\\\\");
                ProcessBuilder pb = new ProcessBuilder(
                        "powershell", "-Command",
                        "Get-Process | Where-Object { $_.Modules.FileName -eq '" + path + "' } | Select-Object -ExpandProperty Id"
                );
                Process p = pb.start();
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            try {
                                long pid = Long.parseLong(line);
                                if (pid != currentPid) pids.add(pid); // evitar matar a si próprio
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
                p.waitFor();
            } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
                String path = file.getAbsolutePath();
                ProcessBuilder pb = new ProcessBuilder("lsof", "-t", path);
                Process p = pb.start();
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            try {
                                long pid = Long.parseLong(line);
                                if (pid != currentPid) pids.add(pid);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
                p.waitFor();
            }
        } catch (Exception e) {
            // Falha silenciosa – não conseguimos obter PIDs
        }
        return pids;
    }

    /**
     * Tenta matar um processo pelo PID (forçado).
     */
    public static boolean killProcess(long pid) {
        try {
            ProcessHandle.of(pid).ifPresent(ph -> ph.destroyForcibly());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Elimina o arquivo forçadamente – tenta matar processos que o bloqueiam (exceto o próprio).
     */
    public static boolean forceDelete(File file) {
        if (!file.exists()) return false;

        // 1. Tenta eliminar normalmente
        if (file.delete()) return true;

        // 2. Se falhou e está bloqueado, tenta matar processos que o usam
        if (isFileLocked(file)) {
            List<Long> pids = getProcessesUsingFile(file);
            for (long pid : pids) {
                killProcess(pid);
            }
            // Espera um pouco para os processos terminarem
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            // Tenta novamente
            return file.delete();
        }
        return false;
    }
}