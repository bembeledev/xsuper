package com.dic.xsuper.cli.commands.standard;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.cli.controller.XFileController;
import com.dic.xsuper.utils.ConsoleTheme;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ListCmd implements Command {

    private final XFileController controller = new XFileController();

    @Override
    public String getName() { return "ls"; }

    @Override
    public String getDescription() { return "Lista ficheiros suportando Wildcards e Globbing (**, *). Uso: ls [-l] [caminho_ou_padrao]"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        boolean detailed = false;
        List<String> pathParts = new ArrayList<>();

        for (int i = 1; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-l")) {
                detailed = true;
            } else if (!args[i].startsWith(">") && !args[i].startsWith("-")) {
                pathParts.add(args[i]);
            }
        }

        // Se não houver partes, targetPattern é null
        String targetPattern = pathParts.isEmpty() ? null : String.join(" ", pathParts);

        // Remover aspas se existirem
        if (targetPattern != null) {
            targetPattern = targetPattern.replaceAll("^\"|\"$", "").trim();
            if (targetPattern.isEmpty()) targetPattern = null;
        }

        // Se nenhum alvo for fornecido ou for ".", lista a diretoria atual
        if (targetPattern == null || targetPattern.equals(".") || targetPattern.equals(".\\") || targetPattern.equals("./")) {
            printNormalList(currentDirectory, currentDirectory.toFile(), detailed);
            return currentDirectory;
        }

        // Deteção do caminho absoluto vs relativo
        Path targetPath;
        try {
            Path maybeAbsolute = Paths.get(targetPattern);
            if (maybeAbsolute.isAbsolute()) {
                targetPath = maybeAbsolute.normalize();
            } else {
                targetPath = currentDirectory.resolve(targetPattern).normalize();
            }
        } catch (InvalidPathException e) {
            System.out.println(ConsoleTheme.ERROR + "❌ Caminho inválido: " + targetPattern + ConsoleTheme.RESET);
            return currentDirectory;
        }

        // Se for um padrão com asteriscos, usar Globbing
        if (targetPattern.contains("*") || targetPattern.contains("?")) {
            handleGlobbingSearch(targetPattern, currentDirectory, detailed);
            return currentDirectory;
        }

        // Verificar existência e listar
        if (!Files.exists(targetPath)) {
            System.out.println(ConsoleTheme.ERROR + "❌ Caminho não encontrado: " + targetPath.toAbsolutePath() + ConsoleTheme.RESET);
        } else {
            printNormalList(currentDirectory, targetPath.toFile(), detailed);
        }

        return currentDirectory;
    }

    /**
     * O Motor de Globbing para resolver padrões complexos como .\\**\\data\\**\\23jungo\\*
     */
    private void handleGlobbingSearch(String pattern, Path currentDirectory, boolean detailed) {
        System.out.println(ConsoleTheme.HEADER + "🔍 A aplicar motor Glob: " + pattern + ConsoleTheme.RESET);
        System.out.println("-------------------------------------------------------");

        // Limpa sintaxes como ".\\" que confundem o motor de Globbing
        if (pattern.startsWith(".\\") || pattern.startsWith("./")) {
            pattern = pattern.substring(2);
        }

        // O Java GlobMatcher prefere '/' como separador universal, mesmo no Windows
        String globStr = pattern.replace("\\", "/");

        // Se a pessoa digitar uma pasta no fim (ex: 23jungo\), assume-se que ela quer tudo lá dentro (23jungo\*)
        if (globStr.endsWith("/")) {
            globStr += "*";
        }

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + globStr);

        List<XFileController.FileInfo> results = new ArrayList<>();
        List<Path> relativePaths = new ArrayList<>();

        // Abre uma stream de alto desempenho para percorrer todas as ramificações de pastas
        try (Stream<Path> walk = Files.walk(currentDirectory)) {
            walk.forEach(path -> {
                if (path.equals(currentDirectory)) return; // Ignora a própria raiz

                Path relative = currentDirectory.relativize(path);
                // Normaliza as barras para a verificação do Padrão
                Path normalizedForMatch = Path.of(relative.toString().replace("\\", "/"));

                if (matcher.matches(normalizedForMatch) || matcher.matches(relative)) {
                    try {
                        BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
                        results.add(new XFileController.FileInfo(path.toFile(), attr));
                        relativePaths.add(relative);
                    } catch (IOException ignored) {}
                }
            });
        } catch (IOException e) {
            System.out.println(ConsoleTheme.ERROR + "❌ Erro ao percorrer diretórios: " + e.getMessage() + ConsoleTheme.RESET);
            return;
        }

        if (results.isEmpty()) {
            System.out.println(ConsoleTheme.TEXT + "   (Nenhum ficheiro ou pasta corresponde ao padrão)" + ConsoleTheme.RESET);
            return;
        }

        // Impressão dos Resultados com o Caminho Completo Relativo para o utilizador saber ONDE encontrou
        for (int i = 0; i < results.size(); i++) {
            XFileController.FileInfo info = results.get(i);
            Path relPath = relativePaths.get(i);

            if (detailed) {
                System.out.println(ConsoleTheme.TEXT + "   " + String.format("%-25s | %,8d bytes | %s", relPath.toString(), info.size, info.isDirectory ? "[DIR]" : "[FILE]") + ConsoleTheme.RESET);
            } else {
                String color = info.isDirectory ? ConsoleTheme.DIRECTORY : ConsoleTheme.TEXT;
                System.out.println(color + "   " + relPath.toString() + ConsoleTheme.RESET);
            }
        }
    }

    private void printNormalList(Path currentDir, File targetFile, boolean detailed) {
        if (!targetFile.isDirectory()) {
            System.out.println(ConsoleTheme.WARNING + "⚠️ O caminho aponta para um ficheiro. Usa 'props' ou 'cat' para o inspecionar." + ConsoleTheme.RESET);
            return;
        }

        List<XFileController.FileInfo> list = controller.listDirectory(targetFile);

        System.out.println(ConsoleTheme.HEADER + "📁 " + targetFile.getAbsolutePath() + ConsoleTheme.RESET);
        System.out.println("-------------------------------------------------------");

        if (list.isEmpty()) {
            System.out.println(ConsoleTheme.TEXT + "   (Diretório vazio)" + ConsoleTheme.RESET);
            return;
        }

        for (XFileController.FileInfo info : list) {
            if (detailed) {
                System.out.println(ConsoleTheme.TEXT + "   " + info.toString() + ConsoleTheme.RESET);
            } else {
                String color = info.isDirectory ? ConsoleTheme.DIRECTORY : ConsoleTheme.TEXT;
                String prefix = info.isDirectory ? "[DIR]  " : "[FILE] ";
                System.out.println(color + "   " + prefix + info.name + ConsoleTheme.RESET);
            }
        }
    }
}