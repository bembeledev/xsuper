package com.dic.xsuper.cli.commands.standard;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.cli.controller.XFileController;
import com.dic.xsuper.utils.ConsoleTheme;

import java.io.File;
import java.nio.file.Path;

public class DeleteCmd implements Command {

    private final XFileController controller = new XFileController();

    @Override
    public String getName() {
        return "rm"; // O comando que o utilizador vai digitar no terminal
    }

    @Override
    public String getDescription() {
        return "Elimina ficheiro/pasta de forma segura. Uso: rm [-f] <nome>";
    }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 2) {
            System.out.println(ConsoleTheme.WARNING + "⚠️ Uso: rm [-f] <nome_do_alvo>" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        // Verifica se o utilizador quer forçar a eliminação (matando processos)
        boolean force = args.length > 2 && args[1].equals("-f");
        String targetName = force ? args[2] : args[1];

        File toDelete = currentDirectory.resolve(targetName).normalize().toFile();

        if (!toDelete.exists()) {
            System.out.println(ConsoleTheme.ERROR + "❌ Arquivo ou pasta não encontrado(a): " + targetName + ConsoleTheme.RESET);
            return currentDirectory;
        }

        boolean deleted;
        if (force) {
            System.out.println(ConsoleTheme.WARNING + "Atenção: Modo Forçado ativado. A tentar desbloquear e eliminar..." + ConsoleTheme.RESET);
            deleted = controller.forceDelete(toDelete);
        } else {
            // O segundo parâmetro 'true' significa que ele vai apagar diretórios com conteúdo recursivamente
            deleted = controller.delete(toDelete, true);
        }

        if (deleted) {
            System.out.println(ConsoleTheme.SUCCESS + "✅ '" + targetName + "' eliminado com sucesso." + ConsoleTheme.RESET);
        } else {
            System.out.println(ConsoleTheme.ERROR + "❌ Falha ao eliminar '" + targetName + "'." + ConsoleTheme.RESET);
            if (!force) {
                System.out.println(ConsoleTheme.TEXT + "Dica: Pode estar a ser usado por outro programa. Tenta usar 'rm -f " + targetName + "' para forçar." + ConsoleTheme.RESET);
            }
        }

        return currentDirectory;
    }
}