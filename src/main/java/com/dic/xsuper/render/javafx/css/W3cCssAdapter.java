package com.dic.xsuper.render.javafx.css;

import javafx.scene.Node;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor de Adaptação W3C -> JavaFX.
 * Garante que a SuperUI suporta animações, transformações, gradientes e cores RGBA modernas.
 */
public class W3cCssAdapter {

    public static void applyW3cToNative(Node fxNode, Map<String, String> w3cStyles, StringBuilder currentFxCss) {
        if (fxNode == null || w3cStyles == null) return;

        // ─── 1. Suporte Nativo a Opacidade ──────────────────────────────────────
        if (w3cStyles.containsKey("opacity")) {
            try { fxNode.setOpacity(Double.parseDouble(w3cStyles.get("opacity"))); } catch (Exception ignored) {}
        }

        // ─── 2. Suporte Nativo a Transformações W3C (translate, scale) ──────────
        if (w3cStyles.containsKey("transform")) {
            String transform = w3cStyles.get("transform").toLowerCase();

            if (transform.contains("translatey")) {
                try {
                    String val = transform.substring(transform.indexOf("translatey(") + 11);
                    fxNode.setTranslateY(Double.parseDouble(val.substring(0, val.indexOf(")")).replace("px", "").trim()));
                } catch (Exception ignored) {}
            }
            if (transform.contains("translatex")) {
                try {
                    String val = transform.substring(transform.indexOf("translatex(") + 11);
                    fxNode.setTranslateX(Double.parseDouble(val.substring(0, val.indexOf(")")).replace("px", "").trim()));
                } catch (Exception ignored) {}
            }
            if (transform.contains("scale(")) {
                try {
                    String val = transform.substring(transform.indexOf("scale(") + 6);
                    double scale = Double.parseDouble(val.substring(0, val.indexOf(")")).trim());
                    fxNode.setScaleX(scale); fxNode.setScaleY(scale);
                } catch (Exception ignored) {}
            }
        }

        // ─── 3. Suporte a Gradientes, Malhas Livres e Cores ─────────────────────
        if (w3cStyles.containsKey("background") || w3cStyles.containsKey("background-color") || w3cStyles.containsKey("background-image")) {

            String bgImage = w3cStyles.get("background-image");
            String bgColor = w3cStyles.containsKey("background-color") ? w3cStyles.get("background-color") :
                    (w3cStyles.containsKey("background") && !w3cStyles.get("background").contains("gradient") ? w3cStyles.get("background") : null);

            String bg = bgImage != null ? bgImage.trim() : (bgColor != null ? bgColor.trim() : "");

            if (bg.equalsIgnoreCase("none")) bg = "transparent";

            if (bg.contains("custom-gradient") || bg.contains("costum-gradient")) {
                Matcher m = Pattern.compile("(?s)(custom-gradient|costum-gradient)\\s*\\((.*)\\)").matcher(bg);
                if (m.find()) {
                    String content = m.group(2);
                    Matcher tupleMatcher = Pattern.compile("\\(([^)]+)\\)").matcher(content);
                    List<String> radials = new ArrayList<>();
                    if (bgColor != null) radials.add(bgColor.trim());

                    while (tupleMatcher.find()) {
                        String[] params = tupleMatcher.group(1).split(",");
                        if (params.length >= 3) {
                            String ptX = params[0].trim();
                            if (!ptX.endsWith("%")) ptX = ptX.replace("px", "") + "%";
                            String ptY = params[1].trim();
                            if (!ptY.endsWith("%")) ptY = ptY.replace("px", "") + "%";
                            String color = params[2].trim();
                            String radius = "50%";
                            if (params.length > 5) {
                                try {
                                    double rVal = Double.parseDouble(params[5].trim().replace("px", "").replace("%", ""));
                                    if (rVal > 100) rVal = rVal / 10;
                                    radius = rVal + "%";
                                } catch (Exception e) { radius = "50%"; }
                            }
                            radials.add(String.format("radial-gradient(center %s %s, radius %s, %s 0%%, transparent 100%%)", ptX, ptY, radius, color));
                        }
                    }
                    if (!radials.isEmpty()) bg = String.join(", ", radials);
                }
            }
            else if (bg.contains("linear-gradient")) {
                bg = bg.replaceAll("135deg", "to bottom right").replaceAll("45deg", "to top right")
                        .replaceAll("225deg", "to bottom left").replaceAll("315deg", "to top left")
                        .replaceAll("90deg", "to right").replaceAll("180deg", "to bottom")
                        .replaceAll("270deg", "to left").replaceAll("0deg", "to top")
                        .replaceAll("360deg", "to top").replaceAll("(?i)([0-9]+)deg", "to bottom right");
            }

            bg = bg.replace(";", "");
            String safeCss = currentFxCss.toString().replaceAll("-fx-background-color\\s*:[^;]+;", "")
                    .replaceAll("-fx-background-image\\s*:[^;]+;", "");
            currentFxCss.setLength(0);
            currentFxCss.append(safeCss);
            currentFxCss.append("-fx-background-color: ").append(bg).append("; ");
        }

        // ─── 4. Cura Definitiva para Box-Shadow com RGBA ────────────────────────
        if (w3cStyles.containsKey("box-shadow")) {
            String shadow = w3cStyles.get("box-shadow").trim();
            if (!shadow.equalsIgnoreCase("none")) {

                String color = "black";
                // Interceta a cor: rgba(...), rgb(...) ou #HEX
                Matcher colorMatcher = Pattern.compile("(rgba?\\([^)]+\\)|#[0-9a-fA-F]{3,8})").matcher(shadow);

                if (colorMatcher.find()) {
                    color = colorMatcher.group(1);
                    shadow = shadow.replace(color, "").trim();
                } else {
                    // Fallback para palavras-chave de cores (ex: "red", "black") no final do box-shadow
                    String[] tokens = shadow.split("\\s+");
                    if (tokens.length > 0 && !tokens[tokens.length - 1].matches(".*\\d.*")) {
                        color = tokens[tokens.length - 1];
                        shadow = shadow.substring(0, shadow.lastIndexOf(color)).trim();
                    }
                }

                shadow = shadow.replace("px", ""); // Limpa unidades
                String[] parts = shadow.split("\\s+");

                String offsetX = parts.length > 0 ? parts[0] : "0";
                String offsetY = parts.length > 1 ? parts[1] : "0";
                String blur    = parts.length > 2 ? parts[2] : "0";
                String spread  = parts.length > 3 ? parts[3] : "0";

                // Converte W3C para sintaxe estrita do JavaFX: dropshadow(blur-type, color, radius, spread, offsetX, offsetY)
                String fxShadow = String.format("dropshadow(gaussian, %s, %s, %s, %s, %s)", color, blur, spread, offsetX, offsetY);

                String safeCss = currentFxCss.toString().replaceAll("-fx-effect\\s*:[^;]+;", "");
                currentFxCss.setLength(0);
                currentFxCss.append(safeCss);
                currentFxCss.append("-fx-effect: ").append(fxShadow).append("; ");
            }
        }

        // ─── 5. Escudo para Cores de Texto e Bordas em RGBA ─────────────────────
        if (w3cStyles.containsKey("color") && w3cStyles.get("color").contains("rgba(")) {
            String safeCss = currentFxCss.toString()
                    .replaceAll("-fx-text-fill\\s*:[^;]+;", "")
                    .replaceAll("-fx-text-inner-color\\s*:[^;]+;", "");
            currentFxCss.setLength(0);
            currentFxCss.append(safeCss);
            currentFxCss.append("-fx-text-fill: ").append(w3cStyles.get("color")).append("; ");
        }

        if (w3cStyles.containsKey("border-color") && w3cStyles.get("border-color").contains("rgba(")) {
            String safeCss = currentFxCss.toString().replaceAll("-fx-border-color\\s*:[^;]+;", "");
            currentFxCss.setLength(0);
            currentFxCss.append(safeCss);
            currentFxCss.append("-fx-border-color: ").append(w3cStyles.get("border-color")).append("; ");
        }
    }
}