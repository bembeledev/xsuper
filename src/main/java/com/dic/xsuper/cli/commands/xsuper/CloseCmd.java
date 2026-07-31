package com.dic.xsuper.cli.commands.xsuper;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.cli.model.FileHelper;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class CloseCmd implements Command {

    @Override
    public String getName() {
        return "close";
    }

    @Override
    public String getDescription() {
        return "Superpoder: Força a libertação de um ficheiro fechando os programas que o estão a usar.";
    }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 2) {
            System.out.println("⚠️ Uso correto: close <nome_do_ficheiro> (ex: close pom.xml)");
            return currentDirectory;
        }

        File targetFile = currentDirectory.resolve(args[1]).normalize().toFile();

        if (!targetFile.exists()) {
            System.out.println("❌ O ficheiro '" + args[1] + "' não existe.");
            return currentDirectory;
        }

        System.out.println("🔍 A analisar o estado do ficheiro: " + targetFile.getName() + "...");

        // 1. Verifica se está realmente bloqueado usando a tua lógica do FileHelper
        if (!FileHelper.isFileLocked(targetFile)) {
            System.out.println("✅ O ficheiro já se encontra livre. Não está a ser bloqueado por nenhum processo.");
            return currentDirectory;
        }

        System.out.println("⚠️ O ficheiro está BLOQUEADO! A procurar os processos culpados a nível de Sistema Operativo...");

        // 2. Caça os PIDs usando PowerShell ou lsof
        List<Long> pids = FileHelper.getProcessesUsingFile(targetFile);

        if (pids.isEmpty()) {
            System.out.println("❌ Ficheiro bloqueado, mas não foi possível identificar o processo. (Dica: Executa o Xplorer como Administrador)");
            return currentDirectory;
        }

        // 3. Executa a destruição forçada dos processos
        for (long pid : pids) {
            System.out.println("🔫 A neutralizar o processo com PID: " + pid + "...");
            boolean killed = FileHelper.killProcess(pid);
            if (killed) {
                System.out.println("   -> Processo " + pid + " terminado com sucesso.");
            } else {
                System.out.println("   -> Falha ao terminar o processo " + pid + ". Acesso negado.");
            }
        }

        // 4. Aguarda meio segundo para o Sistema Operativo limpar o 'handle' da memória
        Thread.sleep(500);

        // 5. Verifica se a libertação teve sucesso
        if (!FileHelper.isFileLocked(targetFile)) {
            System.out.println("✅ SUCESSO: O ficheiro '" + targetFile.getName() + "' foi totalmente libertado e está pronto a usar!");
        } else {
            System.out.println("❌ O ficheiro ainda se encontra bloqueado. Provavelmente está a ser usado pelo Kernel do sistema.");
        }

        return currentDirectory;
    }
}