package com.dic.xsuper.lang.ui.cursor;

import javafx.scene.Cursor;
import javafx.scene.Node;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilitário para aplicar a propriedade CSS 'cursor' a um nó JavaFX.
 * Suporta todos os cursores nativos do JavaFX e URLs de imagem.
 */
public class StyleCursorUtils {

    // Mapeamento explícito para valores CSS comuns (para fallback)
    private static final Map<String, String> CSS_TO_NATIVE = new HashMap<>();

    static {
        // Mapeia nomes CSS para os nomes das constantes em maiúsculas
        CSS_TO_NATIVE.put("default", "DEFAULT");
        CSS_TO_NATIVE.put("auto", "DEFAULT");
        CSS_TO_NATIVE.put("pointer", "HAND");
        CSS_TO_NATIVE.put("hand", "HAND");
        CSS_TO_NATIVE.put("text", "TEXT");
        CSS_TO_NATIVE.put("wait", "WAIT");
        CSS_TO_NATIVE.put("move", "MOVE");
        CSS_TO_NATIVE.put("crosshair", "CROSSHAIR");
        CSS_TO_NATIVE.put("help", "HELP"); // HELP não existe nativamente? Verificar
        CSS_TO_NATIVE.put("grab", "OPEN_HAND");
        CSS_TO_NATIVE.put("grabbing", "CLOSED_HAND");
        CSS_TO_NATIVE.put("not-allowed", "NONE"); // ou DEFAULT
        CSS_TO_NATIVE.put("no-drop", "NONE");
        CSS_TO_NATIVE.put("progress", "WAIT");
        CSS_TO_NATIVE.put("n-resize", "N_RESIZE");
        CSS_TO_NATIVE.put("s-resize", "S_RESIZE");
        CSS_TO_NATIVE.put("e-resize", "E_RESIZE");
        CSS_TO_NATIVE.put("w-resize", "W_RESIZE");
        CSS_TO_NATIVE.put("ne-resize", "NE_RESIZE");
        CSS_TO_NATIVE.put("nw-resize", "NW_RESIZE");
        CSS_TO_NATIVE.put("se-resize", "SE_RESIZE");
        CSS_TO_NATIVE.put("sw-resize", "SW_RESIZE");
        CSS_TO_NATIVE.put("ew-resize", "E_RESIZE");
        CSS_TO_NATIVE.put("ns-resize", "N_RESIZE");
        CSS_TO_NATIVE.put("nesw-resize", "NE_RESIZE");
        CSS_TO_NATIVE.put("nwse-resize", "NW_RESIZE");
        CSS_TO_NATIVE.put("col-resize", "E_RESIZE");
        CSS_TO_NATIVE.put("row-resize", "N_RESIZE");
        CSS_TO_NATIVE.put("all-scroll", "MOVE");
        CSS_TO_NATIVE.put("zoom-in", "DEFAULT");
        CSS_TO_NATIVE.put("zoom-out", "DEFAULT");
        // Adiciona mais conforme necessidade
    }

    /**
     * Aplica o cursor ao nó, com base no valor CSS.
     * Suporta nomes CSS comuns e URLs de imagem.
     * @param node O nó JavaFX
     * @param cursorValue O valor da propriedade 'cursor' (ex: "pointer", "text", "url(cursor.png)")
     */
    public static void applyCursor(Node node, String cursorValue) {
        if (node == null || cursorValue == null || cursorValue.trim().isEmpty()) {
            return;
        }

        String normalized = cursorValue.trim().toLowerCase();
        Cursor cursor = null;

        // 1. Tenta mapear diretamente via Cursor.cursor() (nomes em maiúsculas)
        try {
            String nativeName = CSS_TO_NATIVE.getOrDefault(normalized, normalized.toUpperCase());
            // tenta como nome de constante (ex: "HAND")
            cursor = Cursor.cursor(nativeName);
        } catch (IllegalArgumentException e) {
            // 2. Se falhar, tenta como URL de imagem (ex: "url(cursor.png)")
            if (normalized.startsWith("url(") && normalized.endsWith(")")) {
                String url = normalized.substring(4, normalized.length() - 1).trim();
                if (!url.isEmpty()) {
                    try {
                        cursor = Cursor.cursor(url);
                    } catch (Exception ignored) {}
                }
            }
        }

        // 3. Fallback: cursor default
        if (cursor == null) {
            cursor = Cursor.DEFAULT;
        }

        node.setCursor(cursor);
    }
}