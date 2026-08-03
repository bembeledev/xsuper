package com.dic.xsuper.cli.commands.xsuper;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class JoinfCmd implements Command {

    @Override
    public String getName() {
        return "joinf";
    }

    @Override
    public String getDescription() {
        return "Junta o conteúdo de vários ficheiros. Uso: joinf [-d dir1, dir2] -f <filtros> [--header] [-df <separador>]";
    }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        List<Path> searchDirs = new ArrayList<>();
        List<String> filePatterns = new ArrayList<>();
        boolean useHeader = false;
        String separator = "======================="; // Separador padrão

        // 1. Analisador de Argumentos
        for (int i = 1; i < args.length; i++) {
            String arg = args[i].toLowerCase();

            // ⭐ AGORA SUPORTA MÚLTIPLOS DIRETÓRIOS (ex: -d src, views, config)
            if (arg.equals("-d")) {
                StringBuilder dirBuilder = new StringBuilder();
                while (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    dirBuilder.append(args[++i]).append(" ");
                }

                String[] splits = dirBuilder.toString().split(",");
                for (String s : splits) {
                    String trimmed = s.trim().replace("\"", "").replace("'", "");
                    if (!trimmed.isEmpty()) {
                        searchDirs.add(currentDirectory.resolve(trimmed).normalize());
                    }
                }
            }
            else if (arg.equals("--header") || arg.equals("--h") || arg.equals("-h")) {
                useHeader = true;
            }
            else if (arg.equals("-df") && i + 1 < args.length) {
                separator = args[++i].replace("\"", "").replace("'", "");
            }
            else if (arg.equals("-f")) {
                StringBuilder patternsBuilder = new StringBuilder();
                while (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    patternsBuilder.append(args[++i]).append(" ");
                }

                String[] splits = patternsBuilder.toString().split(",");
                for (String s : splits) {
                    String trimmed = s.trim();
                    if (!trimmed.isEmpty()) {
                        filePatterns.add(trimmed);
                    }
                }
            }
        }

        // 2. Validações Iniciais
        if (searchDirs.isEmpty()) {
            // Se o utilizador não passar -d, assume a diretoria atual por defeito
            searchDirs.add(currentDirectory);
        }

        if (filePatterns.isEmpty()) {
            System.out.println(ConsoleTheme.WARNING + "  Aviso: Nenhum filtro especificado. Usa: -f *.java, *.txt" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        // 3. Compila os Padrões de Pesquisa (Glob Matchers)
        List<PathMatcher> matchers = new ArrayList<>();
        for (String pattern : filePatterns) {
            matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + pattern));
        }

        final boolean finalUseHeader = useHeader;
        final String finalSeparator = separator;

        // 4. ⭐ O LOOP MULTI-DIRETÓRIOS ⭐
        for (Path searchDir : searchDirs) {

            // Valida se cada diretório específico existe
            if (!Files.exists(searchDir) || !Files.isDirectory(searchDir)) {
                System.out.println(ConsoleTheme.WARNING + "  [Aviso] Diretório ignorado (não encontrado): " + searchDir + ConsoleTheme.RESET);
                continue;
            }

            // Executa a Pesquisa dentro do diretório atual da iteração
            try (Stream<Path> walk = Files.walk(searchDir)) {
                walk.filter(Files::isRegularFile).forEach(path -> {

                    // Verifica se o ficheiro atual bate com algum dos filtros (ex: *.java)
                    boolean matches = false;
                    for (PathMatcher matcher : matchers) {
                        if (matcher.matches(path.getFileName())) {
                            matches = true;
                            break;
                        }
                    }

                    if (matches) {
                        // Imprime o Cabeçalho se a flag --header foi ativada
                        if (finalUseHeader) {
                            // Caminho relativo ao diretório principal atual (para ficar limpo no texto)
                            Path relativePath = searchDir.relativize(path);
                            System.out.println(ConsoleTheme.HEADER + finalSeparator + ConsoleTheme.RESET);
                            // Se for da diretoria local mostra o nome, se não mostra o nome do diretório base também
                            String prefix = searchDir.equals(currentDirectory) ? "" : searchDir.getFileName() + "/";
                            System.out.println(ConsoleTheme.DIRECTORY + " FILE: " + prefix + relativePath + ConsoleTheme.RESET);
                            System.out.println(ConsoleTheme.HEADER + finalSeparator + ConsoleTheme.RESET);
                        }

                        // Imprime o conteúdo do ficheiro linha a linha
                        try (Stream<String> lines = Files.lines(path)) {
                            lines.forEach(line -> System.out.println(ConsoleTheme.TEXT + line + ConsoleTheme.RESET));
                        } catch (MalformedInputException e) {
                            System.out.println(ConsoleTheme.WARNING + "  [Ignorado: Ficheiro Binário - " + path.getFileName() + "]" + ConsoleTheme.RESET);
                        } catch (IOException e) {
                            System.out.println(ConsoleTheme.ERROR + "  [Erro ao ler: " + path.getFileName() + "]" + ConsoleTheme.RESET);
                        }

                        System.out.println(); // Linha em branco separadora
                    }
                });
            } catch (IOException e) {
                System.out.println(ConsoleTheme.ERROR + "  Erro ao percorrer a diretoria '" + searchDir.getFileName() + "': " + e.getMessage() + ConsoleTheme.RESET);
            }
        }

        return currentDirectory;
    }
}