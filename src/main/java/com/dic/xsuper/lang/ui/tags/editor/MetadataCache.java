package com.dic.xsuper.lang.ui.tags.editor;

import javafx.scene.paint.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;

// --- Cache Inteligente ---
public class MetadataCache {
    private static final MetadataCache INSTANCE = new MetadataCache();
    private final Map<Integer, Color> lineColors = new ConcurrentHashMap<>();

    private MetadataCache() {}
    public static MetadataCache getInstance() { return INSTANCE; }

    // ⭐ Retorna TRUE se a cor registada for nova ou diferente da anterior
    public boolean setColorForLine(int line, Color color) {
        Color oldColor = lineColors.put(line, color);
        return !Objects.equals(oldColor, color);
    }

    public Color getColorForLine(int line) { return lineColors.get(line); }

    // ⭐ Retorna TRUE se apagou uma cor que existia (e avisa a UI para limpar o quadrado)
    public boolean clearColorForLine(int line) {
        return lineColors.remove(line) != null;
    }
}