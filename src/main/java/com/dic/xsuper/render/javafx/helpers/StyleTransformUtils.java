package com.dic.xsuper.render.javafx.helpers;

import javafx.scene.Node;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tradutor de estilos W3C Transform (CSS) para propriedades nativas JavaFX.
 */
public class StyleTransformUtils {

    // Regex Mágica para capturar funções de transform, ex: translateX(50px) ou rotate(45deg)
    private static final Pattern TRANSFORM_PATTERN = Pattern.compile("([a-zA-Z0-9]+)\\(([^)]+)\\)");

    public static void applyTransforms(Node fxNode, Map<String, String> style) {
        if (fxNode == null || style == null || style.isEmpty()) return;

        // ==========================================================
        // 1. SUPORTE A PROPRIEDADES INDIVIDUAIS (Ex: translatey: 100px)
        // ==========================================================
        if (style.containsKey("translatex")) fxNode.setTranslateX(parseSize(style.get("translatex")));
        if (style.containsKey("translatey")) fxNode.setTranslateY(parseSize(style.get("translatey")));
        if (style.containsKey("scalex")) fxNode.setScaleX(parseDouble(style.get("scalex"), 1.0));
        if (style.containsKey("scaley")) fxNode.setScaleY(parseDouble(style.get("scaley"), 1.0));
        if (style.containsKey("scale")) {
            double scale = parseDouble(style.get("scale"), 1.0);
            fxNode.setScaleX(scale);
            fxNode.setScaleY(scale);
        }
        if (style.containsKey("rotate")) fxNode.setRotate(parseAngle(style.get("rotate")));
        if (style.containsKey("opacity")) fxNode.setOpacity(parseDouble(style.get("opacity"), 1.0));

        // ==========================================================
        // 2. SUPORTE À PROPRIEDADE COMPOSTA (Ex: transform: translateX(10px) rotate(45deg))
        // ==========================================================
        if (style.containsKey("transform")) {
            String transformCss = style.get("transform");
            Matcher matcher = TRANSFORM_PATTERN.matcher(transformCss);

            // Varre todos os comandos dentro da string "transform"
            while (matcher.find()) {
                String function = matcher.group(1).toLowerCase();
                String value = matcher.group(2).trim();

                switch (function) {
                    case "translatex":
                        fxNode.setTranslateX(parseSize(value));
                        break;
                    case "translatey":
                        fxNode.setTranslateY(parseSize(value));
                        break;
                    case "translate":
                        // Suporta: translate(X) ou translate(X, Y)
                        String[] transParts = value.split(",");
                        if (transParts.length > 0) fxNode.setTranslateX(parseSize(transParts[0].trim()));
                        if (transParts.length > 1) fxNode.setTranslateY(parseSize(transParts[1].trim()));
                        break;
                    case "scalex":
                        fxNode.setScaleX(parseDouble(value, 1.0));
                        break;
                    case "scaley":
                        fxNode.setScaleY(parseDouble(value, 1.0));
                        break;
                    case "scale":
                        // Suporta: scale(Uniforme) ou scale(X, Y)
                        String[] scaleParts = value.split(",");
                        double sx = parseDouble(scaleParts[0].trim(), 1.0);
                        double sy = scaleParts.length > 1 ? parseDouble(scaleParts[1].trim(), 1.0) : sx;
                        fxNode.setScaleX(sx);
                        fxNode.setScaleY(sy);
                        break;
                    case "rotate":
                        fxNode.setRotate(parseAngle(value));
                        break;
                }
            }
        }
    }

    // ==========================================================
    // 🛠️ HELPERS DE PARSING BLINDADOS (Evitam Crash de NumberFormatException)
    // ==========================================================

    private static double parseSize(String value) {
        if (value == null || value.isEmpty()) return 0.0;
        // Limpa tudo o que seja letras (px, em, rem) e deixa só o número e o sinal de menos (-)
        String clean = value.replaceAll("[^0-9.\\-]", "");
        try {
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static double parseDouble(String value, double defaultValue) {
        if (value == null || value.isEmpty()) return defaultValue;
        String clean = value.replaceAll("[^0-9.\\-]", "");
        try {
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static double parseAngle(String value) {
        if (value == null || value.isEmpty()) return 0.0;
        // Deteta se o utilizador escreveu em radianos (ex: 3.14rad)
        boolean isRad = value.toLowerCase().contains("rad");

        String clean = value.replaceAll("[^0-9.\\-]", "");
        try {
            double parsed = Double.parseDouble(clean);
            // O JavaFX roda em Graus (Degrees) nativamente.
            return isRad ? Math.toDegrees(parsed) : parsed;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}