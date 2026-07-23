package com.dic.xsuper.lang.ui.css;

import javafx.scene.Node;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor de Adaptação W3C -> JavaFX.
 * Garante que a SuperUI suporta animações, transformações e gradientes modernos.
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

            // Separa a imagem/gradiente da cor base sólida
            String bgImage = w3cStyles.get("background-image");
            String bgColor = w3cStyles.containsKey("background-color") ? w3cStyles.get("background-color") :
                    (w3cStyles.containsKey("background") && !w3cStyles.get("background").contains("gradient") ? w3cStyles.get("background") : null);

            String bg = bgImage != null ? bgImage.trim() : (bgColor != null ? bgColor.trim() : "");

            // ⭐ ESCUDO 1: 'none' bloqueia o JavaFX.
            if (bg.equalsIgnoreCase("none")) bg = "transparent";

            // ⭐ ESCUDO 2: Gradientes Livres (Adobe Illustrator Mesh Gradients)
            if (bg.contains("custom-gradient") || bg.contains("costum-gradient")) {

                // O '(?s)' permite que a regex leia quebras de linha caso o CSS venha identado
                Matcher m = Pattern.compile("(?s)(custom-gradient|costum-gradient)\\s*\\((.*)\\)").matcher(bg);

                if (m.find()) {
                    String content = m.group(2);
                    Matcher tupleMatcher = Pattern.compile("\\(([^)]+)\\)").matcher(content);
                    List<String> radials = new ArrayList<>();

                    // 1º CAMADA: A Cor Base. Sem isto, as malhas desvanecem para o vazio!
                    if (bgColor != null) {
                        radials.add(bgColor.trim());
                    }

                    while (tupleMatcher.find()) {
                        String[] params = tupleMatcher.group(1).split(",");
                        if (params.length >= 3) {
                            // Coordenadas: Força o uso de % para respeitar a geometria do JavaFX
                            String ptX = params[0].trim();
                            if (!ptX.endsWith("%")) ptX = ptX.replace("px", "") + "%";

                            String ptY = params[1].trim();
                            if (!ptY.endsWith("%")) ptY = ptY.replace("px", "") + "%";

                            String color = params[2].trim();

                            // Raio: Se o utilizador escrever "450px", dividimos para "45%"
                            // para evitar o colapso estelar no ecrã.
                            String radius = "50%";
                            if (params.length > 5) {
                                String rRaw = params[5].trim().replace("px", "").replace("%", "");
                                try {
                                    double rVal = Double.parseDouble(rRaw);
                                    if (rVal > 100) rVal = rVal / 10;
                                    radius = rVal + "%";
                                } catch (Exception e) {
                                    radius = "50%";
                                }
                            }

                            // Constroi a mancha esbatida da malha
                            radials.add(String.format("radial-gradient(center %s %s, radius %s, %s 0%%, transparent 100%%)", ptX, ptY, radius, color));
                        }
                    }
                    if (!radials.isEmpty()) {
                        bg = String.join(", ", radials); // Aglutina tudo no formato nativo
                    }
                }
            }

            // ⭐ ESCUDO 3: O JavaFX não entende 'deg'. Convertemos para direções W3C.
            else if (bg.contains("linear-gradient")) {
                bg = bg.replaceAll("135deg", "to bottom right")
                        .replaceAll("45deg", "to top right")
                        .replaceAll("225deg", "to bottom left")
                        .replaceAll("315deg", "to top left")
                        .replaceAll("90deg", "to right")
                        .replaceAll("180deg", "to bottom")
                        .replaceAll("270deg", "to left")
                        .replaceAll("0deg", "to top")
                        .replaceAll("360deg", "to top")
                        .replaceAll("(?i)([0-9]+)deg", "to bottom right");
            }

            bg = bg.replace(";", "");

            // Limpa lixo residual do NativeTag
            String safeCss = currentFxCss.toString().replaceAll("-fx-background-color\\s*:[^;]+;", "")
                    .replaceAll("-fx-background-image\\s*:[^;]+;", "");

            currentFxCss.setLength(0);
            currentFxCss.append(safeCss);
            // O JavaFX usa -fx-background-color para agrupar as "Layers" de cor + radiais
            currentFxCss.append("-fx-background-color: ").append(bg).append("; ");
        }
    }
}