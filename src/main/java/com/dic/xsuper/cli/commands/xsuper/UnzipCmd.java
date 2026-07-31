package com.dic.xsuper.cli.commands.xsuper;

import com.dic.xsuper.cli.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

import java.io.IOException;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipCmd implements Command {
    @Override
    public String getName() { return "unzip"; }

    @Override
    public String getDescription() { return "Extrai ficheiros de um arquivo .zip. Uso: unzip <arquivo.zip> [pasta_destino]"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 2) {
            System.out.println(ConsoleTheme.WARNING + "⚠️ Uso: unzip <arquivo.zip> [pasta_destino]" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        Path zipFile = currentDirectory.resolve(args[1]).normalize();
        Path destDir = args.length > 2 ? currentDirectory.resolve(args[2]).normalize() : currentDirectory;

        if (!Files.exists(zipFile)) {
            System.out.println(ConsoleTheme.ERROR + "❌ O arquivo ZIP não foi encontrado: " + args[1] + ConsoleTheme.RESET);
            return currentDirectory;
        }

        if (!Files.exists(destDir)) {
            Files.createDirectories(destDir);
        }

        System.out.println(ConsoleTheme.TEXT + "A extrair para '" + destDir.toAbsolutePath() + "'..." + ConsoleTheme.RESET);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                Path newPath = zipSlipProtect(zipEntry, destDir);

                if (zipEntry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    if (newPath.getParent() != null) {
                        Files.createDirectories(newPath.getParent());
                    }
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
            System.out.println(ConsoleTheme.SUCCESS + "✅ Extração concluída!" + ConsoleTheme.RESET);
        } catch (IOException e) {
            System.out.println(ConsoleTheme.ERROR + "❌ Erro ao extrair: " + e.getMessage() + ConsoleTheme.RESET);
        }

        return currentDirectory;
    }

    // Zip Slip Vulnerability Protection
    private Path zipSlipProtect(ZipEntry zipEntry, Path targetDir) throws IOException {
        Path targetDirResolved = targetDir.resolve(zipEntry.getName()).normalize();
        if (!targetDirResolved.startsWith(targetDir.normalize())) {
            throw new IOException("Entrada de ZIP maliciosa tentou escapar do diretório: " + zipEntry.getName());
        }
        return targetDirResolved;
    }
}