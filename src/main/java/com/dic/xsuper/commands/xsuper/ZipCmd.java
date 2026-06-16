package com.dic.xsuper.commands.xsuper;

import com.dic.xsuper.core.Command;
import com.dic.xsuper.utils.ConsoleTheme;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipCmd implements Command {
    @Override
    public String getName() { return "zip"; }

    @Override
    public String getDescription() { return "Comprime ficheiros ou pastas num arquivo .zip. Uso: zip <arquivo.zip> <origem>"; }

    @Override
    public Path execute(String[] args, Path currentDirectory) throws Exception {
        if (args.length < 3) {
            System.out.println(ConsoleTheme.WARNING + "⚠️ Uso: zip <destino.zip> <pasta_ou_ficheiro_origem>" + ConsoleTheme.RESET);
            return currentDirectory;
        }

        Path zipFile = currentDirectory.resolve(args[1]).normalize();
        Path source = currentDirectory.resolve(args[2]).normalize();

        if (!Files.exists(source)) {
            System.out.println(ConsoleTheme.ERROR + "❌ A origem não existe: " + args[2] + ConsoleTheme.RESET);
            return currentDirectory;
        }

        System.out.println(ConsoleTheme.TEXT + "A comprimir '" + source.getFileName() + "' para '" + zipFile.getFileName() + "'..." + ConsoleTheme.RESET);

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // Calcula o caminho relativo dentro do ZIP
                    Path targetFile = source.getParent() == null ? file : source.getParent().relativize(file);
                    zos.putNextEntry(new ZipEntry(targetFile.toString().replace("\\", "/")));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!dir.equals(source.getParent())) {
                        Path targetDir = source.getParent() == null ? dir : source.getParent().relativize(dir);
                        String dirName = targetDir.toString().replace("\\", "/") + "/";
                        zos.putNextEntry(new ZipEntry(dirName));
                        zos.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            System.out.println(ConsoleTheme.SUCCESS + "✅ Compressão concluída com sucesso!" + ConsoleTheme.RESET);
        } catch (IOException e) {
            System.out.println(ConsoleTheme.ERROR + "❌ Erro ao comprimir: " + e.getMessage() + ConsoleTheme.RESET);
        }

        return currentDirectory;
    }
}