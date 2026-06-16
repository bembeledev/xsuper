package com.dic.xsuper.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class XFileProperties {

    private final File file;
    private final Path path;

    public XFileProperties(File file) {
        this.file = file;
        this.path = file.toPath();
    }

    public void printProperties() {
        if (!file.exists()) {
            System.out.println("[ERRO] O ficheiro ou diretório não existe: " + file.getAbsolutePath());
            return;
        }

        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("  SYSTEM PROPERTIES REPORT : " + file.getName().toUpperCase());
        System.out.println("--------------------------------------------------------------------------------");

        printSectionHeader("GERAL & IDENTIFICAÇÃO");
        printField("Nome", file.getName());
        printField("Caminho Absoluto", file.getAbsolutePath());
        if (file.isFile()) {
            printField("Extensão", getExtension(file.getName()));
            printField("Tipo MIME", getMimeType());
            printField("Codificação / Tipo", detectEncoding());
        }
        printField("Tipo de Registo", file.isDirectory() ? "DIRETÓRIO" : "FICHEIRO");
        printField("Link Simbólico", boolToStr(Files.isSymbolicLink(path)));

        printSectionHeader("ARMAZENAMENTO & DISCO");
        if (file.isFile()) {
            printField("Tamanho Físico", formatSize(file.length()) + " (" + file.length() + " bytes)");
        } else {
            printField("Itens Contidos", countFiles() + " ficheiros, " + countDirectories() + " pastas");
            printField("Estado", isDirectoryEmpty() ? "Vazio" : "Com conteúdo");
        }
        printFileStoreDetails();

        printSectionHeader("DATAS & CICLO DE VIDA");
        printTimestamps();

        printSectionHeader("SEGURANÇA, DONOS & PARTILHA (ACL)");
        printSecurityAndOwnership();

        printSectionHeader("ATRIBUTOS DO SISTEMA");
        printField("Leitura (Read)", boolToStr(file.canRead()));
        printField("Escrita (Write)", boolToStr(file.canWrite()));
        printField("Execução (Exec)", boolToStr(file.canExecute()));
        printField("Oculto (Hidden)", boolToStr(file.isHidden()));
        printAdvancedAttributes();

        if (file.isFile()) {
            printSectionHeader("INTEGRIDADE CRIPTOGRÁFICA");
            if (file.length() < 100 * 1024 * 1024) {
                printField("MD5 Checksum", calculateHash("MD5"));
                printField("SHA-256 Checksum", calculateHash("SHA-256"));
            } else {
                printField("Hashes", "Ignorado (Ficheiro superior a 100MB)");
            }
        }

        if (file.isFile() && file.canRead() && file.length() < 2_000_000) {
            printContentPreview();
        }

        System.out.println("--------------------------------------------------------------------------------\n");
    }

    // ============ MÉTODOS DE FORMATAÇÃO VISUAL ============

    private void printSectionHeader(String title) {
        System.out.println("\n  :: " + title);
    }

    private void printField(String label, Object value) {
        System.out.printf("     %-25s : %s%n", label, value != null ? value.toString() : "N/A");
    }

    private String boolToStr(boolean value) {
        return value ? "[SIM]" : "[NAO]";
    }

    // ============ LEITURA DE DADOS FORENSES ============

    private String detectEncoding() {
        if (!file.isFile() || !file.canRead() || file.length() == 0) return "Desconhecido / Vazio";
        try (InputStream is = Files.newInputStream(path)) {
            byte[] bom = new byte[4];
            int n = is.read(bom, 0, bom.length);

            // Deteção de Byte Order Mark (BOM)
            if (n >= 3 && bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) return "UTF-8 (com BOM)";
            if (n >= 2 && bom[0] == (byte) 0xFE && bom[1] == (byte) 0xFF) return "UTF-16 BE";
            if (n >= 2 && bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE) return "UTF-16 LE";
            if (n >= 4 && bom[0] == 0x00 && bom[1] == 0x00 && bom[2] == (byte) 0xFE && bom[3] == (byte) 0xFF) return "UTF-32 BE";

            // Heurística simples: se tiver bytes nulos nas primeiras posições, costuma ser binário
            for (int i = 0; i < n; i++) {
                if (bom[i] == 0) return "Binário (Executável/Dados)";
            }

            String mime = getMimeType();
            return mime.startsWith("text") ? "UTF-8 / ASCII (Sem BOM)" : "Binário genérico";
        } catch (Exception e) {
            return "Erro ao analisar codificação";
        }
    }

    private void printFileStoreDetails() {
        try {
            FileStore store = Files.getFileStore(path);
            printField("Sistema de Ficheiros", store.type().toUpperCase());
            printField("Drive / Partição", store.name());
            printField("Espaço Total Drive", formatSize(store.getTotalSpace()));
            printField("Espaço Livre Drive", formatSize(store.getUsableSpace()));
        } catch (IOException e) {
            printField("Volume", "Inacessível");
        }
    }

    private void printSecurityAndOwnership() {
        try {
            // Tenta obter o Dono do ficheiro (Autor no SO)
            FileOwnerAttributeView ownerView = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
            if (ownerView != null && ownerView.getOwner() != null) {
                printField("Dono (Owner/Autor)", ownerView.getOwner().getName());
            }

            // Tenta obter as regras de segurança avançadas e partilha (ACLs do Windows/Unix)
            AclFileAttributeView aclView = Files.getFileAttributeView(path, AclFileAttributeView.class);
            if (aclView != null && aclView.getAcl() != null) {
                List<AclEntry> acls = aclView.getAcl();
                printField("Regras de Acesso (ACL)", acls.size() + " regras definidas");
                int count = 1;
                for (AclEntry acl : acls) {
                    if (count <= 3) { // Mostra as 3 principais para não inundar o terminal
                        System.out.printf("     %25s   -> %s [%s]%n", "", acl.principal().getName(), acl.type());
                    }
                    count++;
                }
                if (acls.size() > 3) System.out.printf("     %25s   -> ... e mais %d regras%n", "", (acls.size() - 3));
            } else {
                printField("Partilha (ACL)", "Sem regras restritas específicas.");
            }
        } catch (Exception e) {
            printField("Segurança", "Sem permissão de leitura a nível de Administrador");
        }
    }

    private void printTimestamps() {
        try {
            BasicFileAttributes basic = Files.readAttributes(path, BasicFileAttributes.class);
            printField("Data de Criação", formatDate(basic.creationTime().toMillis()));
            printField("Última Modificação", formatDate(basic.lastModifiedTime().toMillis()));
            printField("Último Acesso", formatDate(basic.lastAccessTime().toMillis()));
        } catch (IOException e) {
            printField("Datas", "Inacessível");
        }
    }

    private void printAdvancedAttributes() {
        try {
            // Atributos DOS/Windows
            try {
                DosFileAttributes dos = Files.readAttributes(path, DosFileAttributes.class);
                printField("Ficheiro de Sistema", boolToStr(dos.isSystem()));
                printField("Ficheiro de Arquivo", boolToStr(dos.isArchive()));
                printField("Apenas Leitura", boolToStr(dos.isReadOnly()));
            } catch (UnsupportedOperationException ignored) {}

            // Atributos POSIX (Unix/Linux/Mac)
            try {
                PosixFileAttributes posix = Files.readAttributes(path, PosixFileAttributes.class);
                printField("Grupo (Group)", posix.group().getName());
                printField("Permissões POSIX", PosixFilePermissions.toString(posix.permissions()));
            } catch (UnsupportedOperationException ignored) {}

            // Atributos Extendidos (Metadata Oculta)
            try {
                UserDefinedFileAttributeView userView = Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
                if (userView != null) {
                    List<String> attrs = userView.list();
                    if (!attrs.isEmpty()) {
                        printField("Meta-Dados Ocultos", attrs.size() + " tags (ex: " + String.join(", ", attrs) + ")");
                    }
                }
            } catch (UnsupportedOperationException | IOException ignored) {}

        } catch (IOException ignored) {}
    }

    private void printContentPreview() {
        String mime = getMimeType();
        boolean isText = mime.startsWith("text") || mime.contains("json") || mime.contains("xml") ||
                mime.contains("javascript") || mime.contains("csv") || mime.contains("sql");

        String name = file.getName().toLowerCase();
        if (!isText) {
            isText = name.endsWith(".txt") || name.endsWith(".log") || name.endsWith(".ini") ||
                    name.endsWith(".cfg") || name.endsWith(".properties") || name.endsWith(".yml") ||
                    name.endsWith(".md") || name.endsWith(".bat") || name.endsWith(".sh");
        }

        if (isText) {
            System.out.println("\n  :: PRÉ-VISUALIZAÇÃO DE CONTEÚDO (MAX 10 LINHAS)");
            System.out.println("     -------------------------------------------------------");
            try (Stream<String> lines = Files.lines(path)) {
                List<String> firstLines = lines.limit(10).collect(Collectors.toList());
                for (int i = 0; i < firstLines.size(); i++) {
                    System.out.printf("     %3d | %s%n", (i + 1), firstLines.get(i));
                }
                if (firstLines.size() == 10) {
                    System.out.println("         | ... (ficheiro truncado para visualização)");
                }
            } catch (IOException e) {
                System.out.println("     [Aviso] Ficheiro ilegível como texto plano.");
            }
            System.out.println("     -------------------------------------------------------");
        }
    }

    private String calculateHash(String algorithm) {
        try (InputStream is = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] block = new byte[8192];
            int length;
            while ((length = is.read(block)) > 0) {
                digest.update(block, 0, length);
            }
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest.digest()) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            return "Erro ao calcular";
        }
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1 || dotIndex == 0) ? "Nenhuma" : fileName.substring(dotIndex + 1).toUpperCase();
    }

    private String getMimeType() {
        try {
            String mime = Files.probeContentType(path);
            return mime != null ? mime : "application/octet-stream";
        } catch (IOException e) {
            return "Desconhecido";
        }
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        int exp = (int) (Math.log(size) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.2f %cB", size / Math.pow(1024, exp), pre);
    }

    private String formatDate(long millis) {
        if (millis == 0) return "N/A";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(millis));
    }

    private long countFiles() {
        if (!file.isDirectory()) return 0;
        File[] files = file.listFiles();
        return files == null ? 0 : Stream.of(files).filter(File::isFile).count();
    }

    private long countDirectories() {
        if (!file.isDirectory()) return 0;
        File[] files = file.listFiles();
        return files == null ? 0 : Stream.of(files).filter(File::isDirectory).count();
    }

    private boolean isDirectoryEmpty() {
        if (!file.isDirectory()) return false;
        File[] files = file.listFiles();
        return files == null || files.length == 0;
    }

    public static void showProperties(File file) {
        new XFileProperties(file).printProperties();
    }

    public static void showProperties(String path) {
        showProperties(new File(path));
    }
}