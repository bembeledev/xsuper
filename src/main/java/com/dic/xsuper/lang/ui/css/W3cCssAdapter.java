package com.dic.xsuper.lang.ui.css;

import javafx.scene.Node;
import java.util.Map;

/**
 * Motor de Adaptação W3C -> JavaFX.
 * Garante que a SuperUI suporta animações, transformações e gradientes modernos
 * protegendo o motor interno de envenenamentos de cache.
 */
public class W3cCssAdapter {

    public static void applyW3cToNative(Node fxNode, Map<String, String> w3cStyles, StringBuilder currentFxCss) {
        if (fxNode == null || w3cStyles == null) return;

        // ─── 1. Suporte Nativo a Opacidade W3C ──────────────────────────────────
        if (w3cStyles.containsKey("opacity")) {
            try {
                fxNode.setOpacity(Double.parseDouble(w3cStyles.get("opacity")));
            } catch (Exception ignored) {}
        }

        // ─── 2. Suporte Nativo a Transformações W3C (translate, scale) ──────────
        if (w3cStyles.containsKey("transform")) {
            String transform = w3cStyles.get("transform").toLowerCase();

            if (transform.contains("translatey")) {
                try {
                    String val = transform.substring(transform.indexOf("translatey(") + 11);
                    val = val.substring(0, val.indexOf(")")).replace("px", "").trim();
                    fxNode.setTranslateY(Double.parseDouble(val));
                } catch (Exception ignored) {}
            }
            if (transform.contains("translatex")) {
                try {
                    String val = transform.substring(transform.indexOf("translatex(") + 11);
                    val = val.substring(0, val.indexOf(")")).replace("px", "").trim();
                    fxNode.setTranslateX(Double.parseDouble(val));
                } catch (Exception ignored) {}
            }
            if (transform.contains("scale(")) {
                try {
                    String val = transform.substring(transform.indexOf("scale(") + 6);
                    val = val.substring(0, val.indexOf(")")).trim();
                    double scale = Double.parseDouble(val);
                    fxNode.setScaleX(scale);
                    fxNode.setScaleY(scale);
                } catch (Exception ignored) {}
            }
        }

        // ─── 3. Suporte a Gradientes e Cores (Escudo Anti-ClassCastException)
        if (w3cStyles.containsKey("background") || w3cStyles.containsKey("background-color")) {
            String bg = w3cStyles.containsKey("background") ? w3cStyles.get("background") : w3cStyles.get("background-color");

            bg = bg.trim();

            // ⭐ ESCUDO 1: 'none' bloqueia o JavaFX. Convertemos para 'transparent'.
            if (bg.equalsIgnoreCase("none")) {
                bg = "transparent";
            }

            // ⭐ ESCUDO 2: O JavaFX não entende 'deg'. Convertemos para direções W3C.
            if (bg.contains("linear-gradient")) {
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

            // Removemos pontos-e-vírgulas acidentais que possam vir do parser inline
            bg = bg.replace(";", "");

            // ⭐ ESCUDO 3: Apagamos qualquer cor residual (ex: transparent) injetada antes pelo NativeTag
            // Assim garantimos que o JavaFX só recebe UMA propriedade de cor, evitando erros.
            String safeCss = currentFxCss.toString().replaceAll("-fx-background-color\\s*:[^;]+;", "");

            // Reconstruímos o CSS apenas com a cor W3C convertida e limpa
            currentFxCss.setLength(0);
            currentFxCss.append(safeCss);
            currentFxCss.append("-fx-background-color: ").append(bg).append("; ");
        }
    }
}