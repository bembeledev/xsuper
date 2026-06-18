package com.dic.xsuper.services;

import com.dic.xsuper.utils.ConsoleTheme;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

public class FileSystemService {

    public static void listContents(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            stream.sorted((p1, p2) -> {
                boolean dir1 = Files.isDirectory(p1);
                boolean dir2 = Files.isDirectory(p2);
                if (dir1 && !dir2) return -1;
                if (!dir1 && dir2) return 1;
                return p1.getFileName().compareTo(p2.getFileName());
            }).forEach(path -> {
                try {
                    BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
                    String name = path.getFileName().toString();
                    if (attr.isDirectory()) {
                        System.out.printf(ConsoleTheme.DIRECTORY + "[DIR]  %-30s" + ConsoleTheme.RESET + " %n", name);
                    } else {
                        System.out.printf(ConsoleTheme.TEXT + "[FILE] %-30s %,10d bytes" + ConsoleTheme.RESET + " %n", name, attr.size());
                    }
                } catch (IOException ignored) {}
            });
        }
    }
}
