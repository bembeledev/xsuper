package com.dic.xsuper.dom.properties;


import com.dic.xsuper.dom.properties.cssunit.CssContext;
import com.dic.xsuper.dom.properties.cssunit.CssParser;
import com.dic.xsuper.dom.properties.cssunit.CssValue;
import com.dic.xsuper.dom.properties.style.XplColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa uma única sombra (box-shadow ou text-shadow).
 * Suporta valores com unidades (px, em, %, etc.)
 */
public class BoxShadow {
    private final CssValue offsetX;
    private final CssValue offsetY;
    private final CssValue blurRadius;
    private final CssValue spreadRadius; // apenas box-shadow
    private final int color;
    private final boolean inset;

    // ─── Construtor ─────────────────────────────────────────────────

    public BoxShadow(CssValue offsetX, CssValue offsetY, CssValue blurRadius,
                     CssValue spreadRadius, int color, boolean inset) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.blurRadius = blurRadius;
        this.spreadRadius = spreadRadius;
        this.color = color;
        this.inset = inset;
    }

    // ─── Getters ──────────────────────────────────────────────────

    public CssValue getOffsetX() { return offsetX; }
    public CssValue getOffsetY() { return offsetY; }
    public CssValue getBlurRadius() { return blurRadius; }
    public CssValue getSpreadRadius() { return spreadRadius; }
    public int getColor() { return color; }
    public boolean isInset() { return inset; }

    // ─── Conversão para pixels (com contexto) ──────────────────

    public float getOffsetXPixels(CssContext context) {
        return (float) offsetX.toPixels(context);
    }
    public float getOffsetYPixels(CssContext context) {
        return (float) offsetY.toPixels(context);
    }
    public float getBlurPixels(CssContext context) {
        return (float) blurRadius.toPixels(context);
    }
    public float getSpreadPixels(CssContext context) {
        return (float) spreadRadius.toPixels(context);
    }

    // ─── Parser estático ─────────────────────────────────────────

    /**
     * Parseia uma string CSS de sombra (ex: "2px 2px 5px rgba(0,0,0,0.3)")
     * e devolve uma lista de BoxShadow (pode ser múltipla separada por vírgula).
     */
    public static List<BoxShadow> parseList(String shadowStr) {
        List<BoxShadow> shadows = new ArrayList<>();
        if (shadowStr == null || shadowStr.trim().isEmpty()) return shadows;

        // Divide por vírgula (cuidado com vírgulas dentro de rgba())
        String[] parts = splitShadows(shadowStr);
        for (String part : parts) {
            BoxShadow shadow = parseSingle(part.trim());
            if (shadow != null) shadows.add(shadow);
        }
        return shadows;
    }

    private static BoxShadow parseSingle(String part) {
        if (part.isEmpty()) return null;

        boolean inset = false;
        List<String> tokens = new ArrayList<>();
        String[] raw = part.split("\\s+");
        for (String token : raw) {
            if (token.equalsIgnoreCase("inset")) {
                inset = true;
            } else {
                tokens.add(token);
            }
        }

        // Ordem: offsetX offsetY [blur] [spread] [color]
        CssValue offsetX = CssValue.zero();
        CssValue offsetY = CssValue.zero();
        CssValue blur = CssValue.zero();
        CssValue spread = CssValue.zero();
        int color = 0xFF000000; // preto opaco

        int idx = 0;
        if (idx < tokens.size()) offsetX = CssParser.parseValue(tokens.get(idx++));
        if (idx < tokens.size()) offsetY = CssParser.parseValue(tokens.get(idx++));
        if (idx < tokens.size()) {
            // Pode ser blur ou spread ou cor
            String next = tokens.get(idx);
            if (isColorToken(next)) {
                color = parseColorToken(next);
            } else {
                blur = CssParser.parseValue(next);
                idx++;
                if (idx < tokens.size()) {
                    String next2 = tokens.get(idx);
                    if (isColorToken(next2)) {
                        color = parseColorToken(next2);
                    } else {
                        spread = CssParser.parseValue(next2);
                        idx++;
                        if (idx < tokens.size()) {
                            color = parseColorToken(tokens.get(idx));
                        }
                    }
                }
            }
        }

        return new BoxShadow(offsetX, offsetY, blur, spread, color, inset);
    }

    private static boolean isColorToken(String token) {
        // Verifica se parece uma cor: hex, rgb, rgba, nome
        return token.startsWith("#") || token.startsWith("rgb") ||
                token.startsWith("rgba") || XplColor.fromWebName(token) != null;
    }

    private static int parseColorToken(String token) {
        // Tenta resolver via XplColor ou hex
        XplColor color = XplColor.fromWebName(token);
        if (color != null) return color.toArgb();
        return parseHexColor(token, 0xFF000000);
    }

    private static int parseHexColor(String hexStr, int defaultColor) {
        if (hexStr == null || hexStr.trim().isEmpty()) return defaultColor;
        try {
            String clean = hexStr.replace("#", "").trim();
            if (clean.length() == 6) clean = "FF" + clean;
            return (int) Long.parseLong(clean, 16);
        } catch (NumberFormatException e) {
            return defaultColor;
        }
    }

    // ─── Suporte a múltiplas sombras (split por vírgula) ────────

    private static String[] splitShadows(String input) {
        // Divide por vírgula, mas ignora vírgulas dentro de parênteses (ex: rgba)
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) {
                parts.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        if (current.length() > 0) parts.add(current.toString().trim());
        return parts.toArray(new String[0]);
    }
}