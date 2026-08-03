package com.dic.xsuper.render.javafx.tags.texts;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.Map;

public class TextNodeTag extends NativeTag {

    public TextNodeTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        String content = this.sourceNode.textContent != null ? this.sourceNode.textContent : "";

        // ⭐ 1. LÊ OS ESTILOS QUE O PAI INJETOU (Top-Down Perfeito)
        Map<String, String> styles = getRawStyles();

        // ⭐ 2. TRANSFORMAÇÕES DE TEXTO
        String transform = styles.getOrDefault("text-transform", "none").toLowerCase();
        content = switch (transform) {
            case "uppercase" -> content.toUpperCase();
            case "lowercase" -> content.toLowerCase();
            case "capitalize" -> capitalizeWords(content);
            default -> content;
        };

        Text textNode = new Text(content);

        // ⭐ 3. COR PRINCIPAL
        String color = styles.getOrDefault("color", styles.getOrDefault("fill", "#1e293b"));
        try {
            textNode.setFill(Color.web(color));
        } catch (Exception e) {
            textNode.setFill(Color.BLACK);
        }

        // ⭐ 4. CONTORNO DO TEXTO (Strokes W3C)
        String strokeColor = styles.getOrDefault("-webkit-text-stroke-color",
                styles.getOrDefault("text-stroke-color", styles.get("stroke")));

        String strokeWidth = styles.getOrDefault("-webkit-text-stroke-width",
                styles.getOrDefault("text-stroke-width", styles.get("stroke-width")));

        if (strokeColor != null) {
            try { textNode.setStroke(Color.web(strokeColor)); } catch (Exception ignored) {}
        }
        if (strokeWidth != null) {
            try { textNode.setStrokeWidth(Double.parseDouble(strokeWidth.replaceAll("[^0-9.]", ""))); } catch (Exception ignored) {}
        }

        // ⭐ 5. MOTOR DE FONTES BLINDADO
        String fontFamily = styles.getOrDefault("font-family", "System").replace("\"", "").replace("'", "").split(",")[0];

        double fontSize = 16.0;
        if (styles.containsKey("font-size")) {
            String fsStr = styles.get("font-size").trim().toLowerCase();
            try {
                // Extração super segura: garante que só pega a parte numérica
                String numberOnly = fsStr.replaceAll("[^0-9.]", "");
                if (!numberOnly.isEmpty()) {
                    double rawVal = Double.parseDouble(numberOnly);

                    // O Tradutor que JavaFX nunca teve!
                    if (fsStr.endsWith("em") || fsStr.endsWith("rem")) {
                        fontSize = rawVal * 16.0;
                    } else if (fsStr.endsWith("%")) {
                        fontSize = (rawVal / 100.0) * 16.0;
                    } else {
                        fontSize = rawVal;
                    }
                }
            } catch (Exception ignored) {
                System.err.println("[TextNodeTag] Falha ao ler font-size: " + fsStr);
            }
        }

        FontWeight weight = FontWeight.NORMAL;
        String fwStr = styles.getOrDefault("font-weight", "normal").toLowerCase();
        if (fwStr.equals("bold") || fwStr.equals("700") || fwStr.equals("800") || fwStr.equals("900") || fwStr.equals("bolder")) {
            weight = FontWeight.BOLD;
        } else if (fwStr.equals("light") || fwStr.equals("300") || fwStr.equals("100")) {
            weight = FontWeight.LIGHT;
        }

        FontPosture posture = FontPosture.REGULAR;
        if ("italic".equals(styles.get("font-style")) || "oblique".equals(styles.get("font-style"))) {
            posture = FontPosture.ITALIC;
        }

        textNode.setFont(Font.font(fontFamily, weight, posture, fontSize));

        // ⭐ 6. QUALIDADE DE RENDERIZAÇÃO
        String smoothing = styles.getOrDefault("-webkit-font-smoothing", "auto");
        if (smoothing.equals("antialiased")) {
            textNode.setFontSmoothingType(FontSmoothingType.GRAY);
        } else {
            textNode.setFontSmoothingType(FontSmoothingType.LCD);
        }

        // ⭐ 7. DECORAÇÕES W3C
        String decoration = styles.getOrDefault("text-decoration", "");
        textNode.setUnderline(decoration.contains("underline"));
        textNode.setStrikethrough(decoration.contains("line-through"));



        // ⭐ 8. SOMBRAS (DropShadow)
        if (styles.containsKey("text-shadow")) {
            try {
                String[] shadowParts = styles.get("text-shadow").trim().split("\\s+");
                if (shadowParts.length >= 3) {
                    double offsetX = Double.parseDouble(shadowParts[0].replace("px", "").trim());
                    double offsetY = Double.parseDouble(shadowParts[1].replace("px", "").trim());
                    double blur = Double.parseDouble(shadowParts[2].replace("px", "").trim());
                    String colorHex = shadowParts.length == 4 ? shadowParts[3] : "#000000";

                    DropShadow dropShadow = new DropShadow();
                    dropShadow.setOffsetX(offsetX);
                    dropShadow.setOffsetY(offsetY);
                    dropShadow.setRadius(blur);
                    dropShadow.setColor(Color.web(colorHex));
                    textNode.setEffect(dropShadow);
                }
            } catch (Exception ignored) {}
        }

        return textNode;
    }

    private String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) return str;
        StringBuilder result = new StringBuilder(str.length());
        boolean capitalizeNext = true;
        for (char c : str.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toTitleCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }

    // =====================================================================
    // ⭐ A CURA DEFINITIVA: BLOQUEIA A APLICAÇÃO DE CSS DO JAVAFX
    // =====================================================================
    @Override
    protected void applyCommonStyles() {
        // Deixamos vazio intencionalmente!
        // Os nós Text nativos não devem receber backgrounds, borders ou paddings,
        // e o CSS do JavaFX tentaria aplicar o 'font-size' de forma imperfeita,
        // corrompendo o Font que já injetámos matematicamente no createNode()!
    }

    @Override
    protected void applyTagSpecificStyles() {}
}