package com.dic.xsuper.cli.commands.standard;
import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class FindTextCmd implements Command {

    @Override
    public String getName() {
        return "findtext";
    }

    @Override
    public String getDescription() {
        return "Procura texto ou Regex em ficheiros. Uso: findtext <regex> <dir> [-f padrao] [-l] [-c] [--link]";
    }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 2) {
            System.out.println(ConsoleTheme.WARNING + " Uso: findtext <texto/regex> [diretório] [-f *.java] [-l] [-c] [--link]" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        // ─── 1. Parsing Manual Blindado e Seguro ────────────────────────────
        String searchPatternStr = null;
        Path searchDir = currentDirectory;
        boolean showLine = false;
        boolean showCol = false;
        boolean showLink = false;
        String fileFilter = null;

        int i = 1;
        List<String> patternParts = new ArrayList<>();

        // Coleta o padrão de pesquisa limpando as aspas preventivamente
        while (i < args.length) {
            String token = args[i];

            // A. Verifica se atingiu as flags conhecidas
            if (token.startsWith("-") && (token.equals("-f") || token.equals("-l") || token.equals("-line") ||
                    token.equals("-c") || token.equals("-col") || token.equals("-link") || token.equals("--link"))) {
                break;
            }

            // ⭐ SEGURANÇA: Limpa as aspas do token antes de fazer qualquer teste de diretório ou validação!
            String cleanToken = token.replace("\"", "").replace("'", "").trim();

            // B. Se o token original continha aspas no início, ele faz OBRIGATORIAMENTE parte do texto de pesquisa.
            // Não tentamos resolvê-lo como diretório para evitar o erro de caractere ilegal.
            if (!token.startsWith("\"") && !token.startsWith("'")) {
                try {
                    Path potentialDir = currentDirectory.resolve(cleanToken).normalize();
                    if (Files.exists(potentialDir) && Files.isDirectory(potentialDir)) {
                        if (patternParts.isEmpty()) {
                            searchDir = potentialDir;
                            i++;
                            break;
                        } else {
                            searchDir = potentialDir;
                            i++;
                            break;
                        }
                    }
                } catch (Exception ignored) {
                    // Ignora falhas de parsing de paths estranhos e assume que é texto de pesquisa
                }
            }

            // Se não for um diretório válido e limpo, adiciona às partes do padrão
            if (!cleanToken.isEmpty()) {
                patternParts.add(cleanToken);
            }
            i++;
        }

        // Junta as partes do padrão mantendo os espaços originais intactos
        searchPatternStr = String.join(" ", patternParts).trim();
        if (searchPatternStr.isEmpty()) {
            System.out.println(ConsoleTheme.ERROR + " Padrão de pesquisa vazio." + ConsoleTheme.RESET);
            return currentDirectory;
        }

        // ─── 2. Processa as flags restantes ────────────────────────────────
        while (i < args.length) {
            String arg = args[i].toLowerCase();
            switch (arg) {
                case "-l":
                case "-line":
                    showLine = true;
                    break;
                case "-c":
                case "-col":
                    showCol = true;
                    break;
                case "-link":
                case "--link":
                    showLink = true;
                    break;
                case "-f":
                    if (i + 1 < args.length) {
                        fileFilter = args[++i];
                        // Remove aspas do filtro, se existirem
                        fileFilter = fileFilter.replace("\"", "").replace("'", "");
                    } else {
                        System.out.println(ConsoleTheme.ERROR + "  A flag '-f' requer um padrão de ficheiro." + ConsoleTheme.RESET);
                        return currentDirectory;
                    }
                    break;
                default:
                    // Ignora argumentos desconhecidos
                    break;
            }
            i++;
        }

        // ─── 3. Compilação do padrão Regex ────────────────────────────────
        Pattern pattern;
        try {
            // Tenta compilar como Regex (case-insensitive)
            pattern = Pattern.compile(searchPatternStr, Pattern.CASE_INSENSITIVE);
        } catch (Exception e) {
            System.out.println(ConsoleTheme.ERROR + "  Expressão regular inválida: " + searchPatternStr + ConsoleTheme.RESET);
            return currentDirectory;
        }

        // ─── 4. Filtro de ficheiros (glob) ────────────────────────────────
        PathMatcher fileMatcher = null;
        if (fileFilter != null) {
            String glob = fileFilter.contains("*") ? fileFilter : "*" + fileFilter + "*";
            fileMatcher = FileSystems.getDefault().getPathMatcher("glob:**/" + glob);
        }

        System.out.println(ConsoleTheme.TEXT + "A pesquisar por '" + searchPatternStr + "' em " + searchDir.getFileName() + "..." + ConsoleTheme.RESET);
        System.out.println("-------------------------------------------------------");

        int totalMatches = 0;
        int filesMatched = 0;

        try (Stream<Path> paths = Files.walk(searchDir)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                if (!Files.isRegularFile(path)) continue;

                // Ignora pastas comuns
                String pathString = path.toString();
                if (pathString.contains(".git") || pathString.contains("target") ||
                        pathString.contains("build") || pathString.contains("node_modules") ||
                        pathString.contains("__pycache__")) {
                    continue;
                }

                if (fileMatcher != null && !fileMatcher.matches(path)) {
                    continue;
                }

                int matchesInFile = searchInFile(path, pattern, showLine, showCol, showLink, currentDirectory);
                if (matchesInFile > 0) {
                    filesMatched++;
                    totalMatches += matchesInFile;
                }
            }
        } catch (Exception e) {
            System.out.println(ConsoleTheme.ERROR + "  Erro ao percorrer diretórios: " + e.getMessage() + ConsoleTheme.RESET);
        }

        System.out.println("-------------------------------------------------------");
        System.out.println(ConsoleTheme.SUCCESS + "-> " + totalMatches + " ocorrência(s) em " + filesMatched + " ficheiro(s)." + ConsoleTheme.RESET);

        return currentDirectory;
    }

    private int searchInFile(Path file, Pattern pattern, boolean showLine, boolean showCol, boolean showLink, Path currentDirectory) {
        int matchCount = 0;
        try (Stream<String> lines = Files.lines(file)) {
            int lineNum = 1;
            for (String line : (Iterable<String>) lines::iterator) {
                Matcher matcher = pattern.matcher(line);

                while (matcher.find()) {
                    StringBuilder output = new StringBuilder("  ");

                    if (showLink) {
                        Path relPath = currentDirectory.relativize(file);
                        output.append(ConsoleTheme.DIRECTORY)
                                .append(relPath)
                                .append(":")
                                .append(lineNum)
                                .append(showCol ? ":" + (matcher.start() + 1) : "")
                                .append(ConsoleTheme.RESET)
                                .append(" -> ");
                    } else {
                        if (matchCount == 0) {
                            System.out.println(ConsoleTheme.HEADER + "■ " + currentDirectory.relativize(file) + ConsoleTheme.RESET);
                        }
                        if (showLine) output.append(ConsoleTheme.TEXT).append("[L:").append(lineNum).append("] ");
                        if (showCol) output.append(ConsoleTheme.TEXT).append("[C:").append(matcher.start() + 1).append("] ");
                    }

                    String highlightedLine = line.substring(0, matcher.start())
                            + ConsoleTheme.WARNING + matcher.group() + ConsoleTheme.TEXT
                            + line.substring(matcher.end());

                    output.append(ConsoleTheme.TEXT).append(highlightedLine.trim()).append(ConsoleTheme.RESET);

                    System.out.println(output);
                    matchCount++;
                }
                lineNum++;
            }
        } catch (Exception ignored) {
        }
        return matchCount;
    }
}