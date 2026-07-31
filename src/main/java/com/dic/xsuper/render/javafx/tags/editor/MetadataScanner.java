package com.dic.xsuper.render.javafx.tags.editor;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetadataScanner {
    // Regex para pegar cores HEX e RGB
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})\\b");

    public static void scanLinesAsync(String text, int cursorLine) {
        new Thread(() -> {
            try {
                String[] lines = text.split("\\R");
                if (cursorLine >= 0 && cursorLine < lines.length) {
                    String currentLineText = lines[cursorLine];
                    Matcher matcher = HEX_COLOR_PATTERN.matcher(currentLineText);

                    boolean colorChanged = false; // ⭐ A Flag de Segurança

                    if (matcher.find()) {
                        try {
                            Color color = Color.web(matcher.group());
                            colorChanged = MetadataCache.getInstance().setColorForLine(cursorLine, color);
                        } catch (IllegalArgumentException ignored) {}
                    } else {
                        colorChanged = MetadataCache.getInstance().clearColorForLine(cursorLine);
                    }

                    // ⭐ A CURA DOS SALTITOS: Só engasga a UI se uma cor real apareceu ou sumiu!
                    if (colorChanged) {
                        Platform.runLater(() -> EditorEventBus.getInstance().fireMetadataChanged(cursorLine));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}