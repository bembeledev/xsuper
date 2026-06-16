package com.dic.xsuper.commands.standard;

import com.dic.xsuper.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

import java.nio.file.Files;
import java.nio.file.Path;

public class LnCmd implements Command {
    @Override
    public String getName() { return "ln"; }

    @Override
    public String getDescription() { return "Cria um link simbólico (atalho). Uso: ln -s <alvo_real> <nome_do_link>"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 4 || !args[1].equals("-s")) {
            System.out.println(ConsoleTheme.WARNING + "⚠️ Uso: ln -s <alvo_existente> <novo_link>" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        Path target = currentDirectory.resolve(args[2]).normalize();
        Path link = currentDirectory.resolve(args[3]).normalize();

        if (!Files.exists(target)) {
            System.out.println(ConsoleTheme.ERROR + "❌ O alvo real não existe: " + args[2] + ConsoleTheme.RESET);
            return currentDirectory;
        }

        try {
            Files.createSymbolicLink(link, target);
            System.out.println(ConsoleTheme.SUCCESS + "✅ Link simbólico criado: " + link.getFileName() + " -> " + target.getFileName() + ConsoleTheme.RESET);
        } catch (UnsupportedOperationException e) {
            System.out.println(ConsoleTheme.ERROR + "❌ O sistema operativo ou partição não suporta links simbólicos." + ConsoleTheme.RESET);
        } catch (java.nio.file.AccessDeniedException e) {
            System.out.println(ConsoleTheme.ERROR + "❌ Acesso Negado. No Windows, criar links simbólicos requer executar o Xplorer como Administrador." + ConsoleTheme.RESET);
        } catch (Exception e) {
            System.out.println(ConsoleTheme.ERROR + "❌ Erro ao criar o link: " + e.getMessage() + ConsoleTheme.RESET);
        }

        return currentDirectory;
    }
}