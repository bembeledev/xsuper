package com.dic.xsuper.ui;
import com.dic.xsuper.controller.XFileController;
import com.dic.xsuper.controller.XFileProperties;
import com.dic.xsuper.model.XFileIO;
import com.dic.xsuper.model.XFileReader;
import com.dic.xsuper.model.XFileRun;
import com.dic.xsuper.model.XFileWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class FileExplorerCLI {

    private final Scanner scanner;
    private final XFileController controller;
    private File currentDir;

    public FileExplorerCLI() {
        scanner = new Scanner(System.in);
        currentDir = new File(System.getProperty("user.dir"));
        controller = new XFileController();
    }

    public static void main(String[] args) {
        new FileExplorerCLI().start();
    }

    public void start() {
        System.out.println("=== XSuper File Explorer ===");
        System.out.println("Digite 'help' para comandos.");

        while (true) {
            System.out.print("\n" + currentDir.getAbsolutePath() + "> ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            String[] parts = input.split("\\s+");
            String cmd = parts[0].toLowerCase();

            try {
                switch (cmd) {
                    case "help":
                        showHelp();
                        break;
                    case "ls":
                        listFiles(parts.length > 1 && parts[1].equals("-l"));
                        break;
                    case "cd":
                        if (parts.length < 2) {
                            System.out.println("Uso: cd <diretório>");
                        } else {
                            changeDirectory(parts[1]);
                        }
                        break;
                    case "pwd":
                        System.out.println(currentDir.getAbsolutePath());
                        break;
                    case "mkdir":
                        if (parts.length < 2) {
                            System.out.println("Uso: mkdir <nome>");
                        } else {
                            controller.createDirectory(currentDir, parts[1]);
                        }
                        break;
                    case "touch":
                        if (parts.length < 2) {
                            System.out.println("Uso: touch <nome>");
                        } else {
                            controller.createFile(currentDir, parts[1]);
                        }
                        break;
                    case "rm":
                        if (parts.length < 2) {
                            System.out.println("Uso: rm [-f] <nome>");
                            break;
                        }
                        boolean force = parts.length > 2 && parts[1].equals("-f");
                        String target = force ? parts[2] : parts[1];
                        File toDelete = new File(currentDir, target);
                        if (!toDelete.exists()) {
                            System.out.println("Arquivo/pasta não encontrado.");
                            break;
                        }
                        boolean deleted;
                        if (force) {
                            deleted = controller.forceDelete(toDelete);
                        } else {
                            deleted = controller.delete(toDelete, false);
                            if (!deleted && toDelete.isDirectory()) {
                                System.out.println("Pasta não vazia. Use rm -f para forçar.");
                                break;
                            }
                        }
                        System.out.println(deleted ? "Eliminado com sucesso." : "Falha ao eliminar.");
                        break;
                    case "rename":
                        if (parts.length < 3) {
                            System.out.println("Uso: rename <antigo> <novo>");
                        } else {
                            File oldFile = new File(currentDir, parts[1]);
                            if (!oldFile.exists()) {
                                System.out.println("Arquivo não encontrado.");
                            } else {
                                controller.rename(oldFile, parts[2]);
                            }
                        }
                        break;
                    case "copy":
                        if (parts.length < 3) {
                            System.out.println("Uso: copy <origem> <destino>");
                        } else {
                            File src = resolveFile(parts[1]);
                            File dest = resolveFile(parts[2]);
                            try {
                                XFileIO.copy(src, dest);
                                System.out.println("✅ Copiado.");
                            } catch (Exception e) {
                                System.err.println("❌ Erro: " + e.getMessage());
                            }
                        }
                        break;

                    case "mv":
                        if (parts.length < 3) {
                            System.out.println("Uso: mv <origem> <destino>");
                        } else {
                            File srcMv = resolveFile(parts[1]);
                            File destMv = resolveFile(parts[2]);
                            try {
                                XFileIO.move(srcMv, destMv);
                                System.out.println("✅ Movido.");
                            } catch (Exception e) {
                                System.err.println("❌ Erro: " + e.getMessage());
                            }
                        }
                        break;
                    case "info":
                        if (parts.length < 2) {
                            System.out.println("Uso: info <nome>");
                        } else {
                            File infoFile = new File(currentDir, parts[1]);
                            if (!infoFile.exists()) {
                                System.out.println("Não existe.");
                            } else {
                                System.out.println(controller.listDirectory(infoFile.getParentFile())
                                        .stream().filter(f -> f.name.equals(infoFile.getName()))
                                        .findFirst().orElse(null));
                            }
                        }
                        break;
                    case "search":
                        if (parts.length < 2) {
                            System.out.println("Uso: search <padrão>");
                        } else {
                            List<File> found = controller.search(currentDir, parts[1]);
                            if (found.isEmpty()) {
                                System.out.println("Nenhum resultado.");
                            } else {
                                found.forEach(f -> System.out.println(f.getAbsolutePath()));
                            }
                        }
                        break;
                    // Dentro do switch no FileExplorerCLI
                    case "props":
                        if (parts.length < 2) {
                            System.out.println("Uso: props <nome>");
                        } else {
                            File propFile = new File(currentDir, parts[1]);
                            if (!propFile.exists()) {
                                System.out.println("Ficheiro não existe.");
                            } else {
                                XFileProperties.showProperties(propFile);
                            }
                        }
                        break;
                    case "open":
                        if (parts.length < 2) {
                            System.out.println("Uso: open <nome>");
                        } else {
                            File openFile = resolveFile(parts[1]);
                            try {
                                XFileRun.openWithDefault(openFile);
                                System.out.println("✅ Aberto com programa padrão.");
                            } catch (Exception e) {
                                System.err.println("❌ Erro: " + e.getMessage());
                            }
                        }
                        break;

                    case "run":
                        if (parts.length < 2) {
                            System.out.println("Uso: run <executável> [args...]");
                        } else {
                            File execFile = resolveFile(parts[1]);
                            List<String> args = new ArrayList<>();
                            args.addAll(Arrays.asList(parts).subList(2, parts.length));
                            try {
                                Process p = XFileRun.execute(execFile, args);
                                System.out.println("✅ Processo iniciado (PID: " + p.pid() + ").");
                            } catch (Exception e) {
                                System.err.println("❌ Erro: " + e.getMessage());
                            }
                        }
                        break;

                    case "runwith":
                        if (parts.length < 3) {
                            System.out.println("Uso: runwith <programa> <alvo> [args...]");
                        } else {
                            File program = resolveFile(parts[1]);
                            File targett = resolveFile(parts[2]);
                            List<String> extra = new ArrayList<>();
                            extra.addAll(Arrays.asList(parts).subList(3, parts.length));
                            try {
                                Process p = XFileRun.executeWithProgram(program, targett, extra);
                                System.out.println("✅ Processo iniciado (PID: " + p.pid() + ").");
                            } catch (Exception e) {
                                System.err.println("❌ Erro: " + e.getMessage());
                            }
                        }
                        break;

                    case "runsh":
                        if (parts.length < 2) {
                            System.out.println("Uso: runsh <script> [args...]");
                        } else {
                            File scriptFile = resolveFile(parts[1]);
                            List<String> argsSh = new ArrayList<>();
                            argsSh.addAll(Arrays.asList(parts).subList(2, parts.length));
                            try {
                                Process p = XFileRun.executeInShell(scriptFile, argsSh);
                                System.out.println("✅ Script executado (PID: " + p.pid() + ").");
                            } catch (Exception e) {
                                System.err.println("❌ Erro: " + e.getMessage());
                            }
                        }
                        break;

                    case "cmd":
                        if (parts.length < 2) {
                            System.out.println("Uso: cmd <comando> (ex: cmd 'dir' ou cmd 'ls -la')");
                        } else {
                            StringBuilder cmdw = new StringBuilder();
                            for (int i = 1; i < parts.length; i++) {
                                cmdw.append(parts[i]).append(" ");
                            }
                            try {
                                Process p = XFileRun.runCommand(cmdw.toString().trim());
                                System.out.println("✅ Comando executado (PID: " + p.pid() + ").");
                            } catch (Exception e) {
                                System.err.println("❌ Erro: " + e.getMessage());
                            }
                        }
                        break;


                    case "read":
                        if (parts.length < 2) {
                            System.out.println("Uso: read <ficheiro> [lines|all] (ex: read meu.txt lines)");
                        } else {
                            File readFile = new File(currentDir, parts[1]);
                            if (!readFile.exists()) {
                                System.out.println("Ficheiro não existe.");
                            } else {
                                try {
                                    if (parts.length >= 3 && parts[2].equalsIgnoreCase("lines")) {
                                        List<String> lines = XFileReader.readAllLines(readFile);
                                        for (int i = 0; i < lines.size(); i++) {
                                            System.out.printf("%4d: %s%n", i + 1, lines.get(i));
                                        }
                                    } else {
                                        System.out.println(XFileReader.readAll(readFile));
                                    }
                                } catch (Exception e) {
                                    System.err.println("❌ Erro: " + e.getMessage());
                                }
                            }
                        }
                        break;

                    case "write":
                        if (parts.length < 3) {
                            System.out.println("Uso: write <ficheiro> <conteúdo> (ex: write teste.txt 'Hello World')");
                        } else {
                            File writeFile = new File(currentDir, parts[1]);
                            try {
                                // Reconstruir conteúdo com o resto dos argumentos
                                StringBuilder content = new StringBuilder();
                                for (int i = 2; i < parts.length; i++) {
                                    if (i > 2) content.append(" ");
                                    content.append(parts[i]);
                                }
                                XFileWriter.write(writeFile, content.toString());
                                System.out.println("✅ Ficheiro escrito.");
                            } catch (Exception e) {
                                System.err.println("❌ Erro: " + e.getMessage());
                            }
                        }
                        break;

                    case "append":
                        if (parts.length < 3) {
                            System.out.println("Uso: append <ficheiro> <conteúdo>");
                        } else {
                            File appendFile = new File(currentDir, parts[1]);
                            try {
                                StringBuilder content = new StringBuilder();
                                for (int i = 2; i < parts.length; i++) {
                                    if (i > 2) content.append(" ");
                                    content.append(parts[i]);
                                }
                                XFileWriter.write(appendFile, content.toString(), StandardCharsets.UTF_8, true);
                                System.out.println("✅ Conteúdo adicionado.");
                            } catch (Exception e) {
                                System.err.println("❌ Erro: " + e.getMessage());
                            }
                        }
                        break;

                    case "cat":
                        if (parts.length < 2) {
                            System.out.println("Uso: cat <nome>");
                        } else {
                            File catFile = resolveFile(parts[1]);
                            try {
                                String content = XFileIO.readAllText(catFile);
                                System.out.println(content);
                            } catch (Exception e) {
                                System.err.println("❌ Erro: " + e.getMessage());
                            }
                        }
                        break;

                    case "head":
                        if (parts.length < 2) {
                            System.out.println("Uso: head <nome> [n] (padrão 10)");
                        } else {
                            File headFile = resolveFile(parts[1]);
                            int n = parts.length > 2 ? Integer.parseInt(parts[2]) : 10;
                            try {
                                List<String> lines = XFileIO.readAllLines(headFile);
                                lines.stream().limit(n).forEach(System.out::println);
                            } catch (Exception e) {
                                System.err.println("❌ Erro: " + e.getMessage());
                            }
                        }
                        break;

                    case "tail":
                        if (parts.length < 2) {
                            System.out.println("Uso: tail <nome> [n] (padrão 10)");
                        } else {
                            File tailFile = resolveFile(parts[1]);
                            int n = parts.length > 2 ? Integer.parseInt(parts[2]) : 10;
                            try {
                                List<String> lines = XFileIO.readAllLines(tailFile);
                                int start = Math.max(0, lines.size() - n);
                                for (int i = start; i < lines.size(); i++) {
                                    System.out.println(lines.get(i));
                                }
                            } catch (Exception e) {
                                System.err.println("❌ Erro: " + e.getMessage());
                            }
                        }
                        break;

                    case "edit":
                        if (parts.length < 2) {
                            System.out.println("Uso: edit <nome>");
                        } else {
                            File editFile = resolveFile(parts[1]);
                            try {
                                // Tenta abrir com editor padrão (Windows: notepad, Linux: gedit, Mac: open -t)
                                String os = System.getProperty("os.name").toLowerCase();
                                ProcessBuilder pb;
                                if (os.contains("win")) {
                                    pb = new ProcessBuilder("notepad", editFile.getAbsolutePath());
                                } else if (os.contains("mac")) {
                                    pb = new ProcessBuilder("open", "-t", editFile.getAbsolutePath());
                                } else {
                                    pb = new ProcessBuilder("gedit", editFile.getAbsolutePath());
                                }
                                pb.start();
                                System.out.println("✅ Editor aberto.");
                            } catch (Exception e) {
                                System.err.println("❌ Erro: " + e.getMessage());
                            }
                        }
                        break;

                    case "findtext":
                        if (parts.length < 3) {
                            System.out.println("Uso: findtext <padrão> <diretório> (ex: findtext 'TODO' .)");
                        } else {
                            String pattern = parts[1];
                            File searchDir = resolveFile(parts[2]);
                            try {
                                List<File> found = XFileController.searchTextInFiles(searchDir, pattern);
                                if (found.isEmpty()) {
                                    System.out.println("Nenhuma ocorrência encontrada.");
                                } else {
                                    found.forEach(f -> System.out.println(f.getAbsolutePath()));
                                }
                            } catch (Exception e) {
                                System.err.println("❌ Erro: " + e.getMessage());
                            }
                        }
                        break;

                    case "exit":
                        System.out.println("Saindo...");
                        scanner.close();
                        return;
                    default:
                        System.out.println("Comando desconhecido. Digite 'help'.");
                }
            } catch (Exception e) {
                System.err.println("Erro: " + e.getMessage());
            }
        }
    }

    private void listFiles(boolean detailed) {
        List<XFileController.FileInfo> list = controller.listDirectory(currentDir);
        if (list.isEmpty()) {
            System.out.println("(vazio)");
            return;
        }
        for (XFileController.FileInfo info : list) {
            if (detailed) {
                System.out.println(info);
            } else {
                System.out.print(info.isDirectory ? "[DIR] " : "[FILE] ");
                System.out.println(info.name);
            }
        }
    }

    private void changeDirectory(String path) {
        File newDir;
        if (path.equals("..")) {
            newDir = currentDir.getParentFile();
            if (newDir == null) return;
        } else if (path.startsWith("/") || path.matches("^[A-Za-z]:.*")) {
            newDir = new File(path);
        } else {
            newDir = new File(currentDir, path);
        }
        if (newDir.exists() && newDir.isDirectory()) {
            currentDir = newDir;
        } else {
            System.out.println("Diretório inválido.");
        }
    }

    private File resolveFile(String name) {
        // Se for caminho absoluto ou relativo com separadores, tenta diretamente
        if (name.contains(File.separator) || name.startsWith(".") || name.startsWith("/") || name.matches("^[A-Za-z]:.*")) {
            return new File(currentDir, name);
        }
        // Senão, procura no diretório atual
        File inCurrent = new File(currentDir, name);
        if (inCurrent.exists()) return inCurrent;
        // Se não existir, retorna o ficheiro no currentDir (será tratado como erro depois)
        return inCurrent;
    }

    private void showHelp() {
        System.out.println("""
                Comandos disponíveis:
                  ls [-l]              - Listar conteúdo (detalhado com -l)
                  cd <dir>             - Mudar diretório (.. para subir)
                  pwd                  - Mostrar caminho atual
                  mkdir <nome>         - Criar pasta
                  touch <nome>         - Criar arquivo vazio
                  rm [-f] <nome>       - Eliminar (forçar com -f)
                  rename <antigo> <novo> - Renomear
                  copy <origem> <dest> - Copiar arquivo/pasta
                  mv <origem> <dest>   - Mover/renomear
                  info <nome>          - Mostrar metadados resumidos
                  props <nome>         - Mostrar todas as propriedades do ficheiro
                  search <padrão>      - Buscar arquivos/pastas
                  open <nome>          - Abrir com programa padrão
                  run <executável> [args...] - Executar programa
                  runwith <prog> <alvo> [args...] - Executar alvo com programa específico
                  help                 - Este menu
                  exit                 - Sair
                """);
    }
}