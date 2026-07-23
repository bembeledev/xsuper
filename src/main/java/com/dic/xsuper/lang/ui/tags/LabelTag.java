package com.dic.xsuper.lang.ui.tags;

import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.Map;

public class LabelTag extends NativeTag {

    private Label fxLabel;

    public LabelTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        fxLabel = new Label();
        fxLabel.setWrapText(true);
        applyDefaultSemanticStyles();
        return fxLabel;
    }

    @Override
    protected void applyTagSpecificStyles() {

    }

    private void applyDefaultSemanticStyles() {
        String tagName = sourceNode.tag.toLowerCase();
        double defaultFontSize = 14.0;
        FontWeight defaultWeight = FontWeight.NORMAL;

        switch (tagName) {
            case "h1" -> { defaultFontSize = 32.0; defaultWeight = FontWeight.BOLD; }
            case "h2" -> { defaultFontSize = 24.0; defaultWeight = FontWeight.BOLD; }
            case "h3" -> { defaultFontSize = 18.72; defaultWeight = FontWeight.BOLD; }
            case "h4" -> { defaultFontSize = 16.0; defaultWeight = FontWeight.BOLD; }
            case "h5" -> { defaultFontSize = 13.28; defaultWeight = FontWeight.BOLD; }
            case "h6" -> { defaultFontSize = 10.72; defaultWeight = FontWeight.BOLD; }
            case "b", "strong" -> defaultWeight = FontWeight.BOLD;
            case "i", "em" -> fxLabel.setStyle("-fx-font-style: italic;");
            case "p" -> {
                defaultFontSize = 16.0;
                fxLabel.setWrapText(true);
            }
            case "span" -> {
                fxLabel.setWrapText(false);
            }
            case "a" -> {
                fxLabel.setWrapText(false);
                fxLabel.setTextFill(Color.BLUE);
                fxLabel.setUnderline(true);
            }
        }

        fxLabel.setFont(Font.font("System", defaultWeight, defaultFontSize));
    }

    @Override
    protected void applyCommonStyles() {
        super.applyCommonStyles();

        Map<String, String> styles = getRawStyles();
        StringBuilder fxCss = new StringBuilder(fxLabel.getStyle() != null ? fxLabel.getStyle() : "");

        // ─── 1. Conteúdo com transformação de texto ──────────────────────────
        String textContent = sourceNode.textContent != null ? sourceNode.textContent : "";
        if (styles.containsKey("text-transform")) {
            String transform = styles.get("text-transform").toLowerCase().trim();
            textContent = switch (transform) {
                case "uppercase" -> textContent.toUpperCase();
                case "lowercase" -> textContent.toLowerCase();
                case "capitalize" -> capitalizeWords(textContent);
                default -> textContent;
            };
        }
        fxLabel.setText(textContent);

        // ─── 2. CSS Web → JavaFX ────────────────────────────────────────────

        // Font Family
        if (styles.containsKey("font-family")) {
            String family = styles.get("font-family").split(",")[0].replace("'", "").replace("\"", "").trim();
            fxCss.append("-fx-font-family: '").append(family).append("'; ");
        }

        // ⭐ REMOVIDOS: Font-Size, Font-Weight e Color!
        // A classe pai (NativeTag) já os aplicou corretamente no super.applyCommonStyles()

        // Font Style
        if (styles.containsKey("font-style")) {
            fxCss.append("-fx-font-style: ").append(styles.get("font-style")).append("; ");
        }

        // Text Decoration
        if (styles.containsKey("text-decoration")) {
            String dec = styles.get("text-decoration").toLowerCase();
            if (dec.contains("underline")) fxCss.append("-fx-underline: true; ");
            if (dec.contains("line-through")) fxCss.append("-fx-strikethrough: true; ");
        }

        // Line Height (Web) → JavaFX (-fx-line-spacing)
        if (styles.containsKey("line-height")) {
            try {
                double fontSize = 14.0;
                if (styles.containsKey("font-size")) {
                    fontSize = Double.parseDouble(styles.get("font-size").replace("px", "").replace("rem", "").trim());
                }
                double lh = Double.parseDouble(styles.get("line-height").replace("px", "").trim());
                double spacing = lh - fontSize;
                fxCss.append("-fx-line-spacing: ").append(spacing).append("px; ");
            } catch (Exception ignored) {}
        }

        // Opacity
        if (styles.containsKey("opacity")) {
            fxCss.append("-fx-opacity: ").append(styles.get("opacity")).append("; ");
        }

        // White Space
        if (styles.containsKey("white-space")) {
            String ws = styles.get("white-space").toLowerCase().trim();
            if (ws.equals("nowrap")) {
                fxLabel.setWrapText(false);
            } else if (ws.equals("pre-wrap") || ws.equals("pre-line")) {
                fxLabel.setWrapText(true);
            }
        }

        // Aplica o CSS acumulado
        fxLabel.setStyle(fxCss.toString());

        // ─── 3. Alinhamentos e posicionamento ───────────────────────────────

        // Text Align
        if (styles.containsKey("text-align")) {
            String align = styles.get("text-align").toLowerCase().trim();
            switch (align) {
                case "center" -> { fxLabel.setTextAlignment(TextAlignment.CENTER); fxLabel.setAlignment(Pos.CENTER); }
                case "right" -> { fxLabel.setTextAlignment(TextAlignment.RIGHT); fxLabel.setAlignment(Pos.CENTER_RIGHT); }
                case "justify" -> fxLabel.setTextAlignment(TextAlignment.JUSTIFY);
                default -> { fxLabel.setTextAlignment(TextAlignment.LEFT); fxLabel.setAlignment(Pos.CENTER_LEFT); }
            }
        }

        // Vertical Alignment (align-items: center, flex-start, flex-end)
        if (styles.containsKey("vertical-align") || styles.containsKey("align-items")) {
            String val = styles.getOrDefault("vertical-align", styles.get("align-items")).toLowerCase().trim();
            switch (val) {
                case "center" -> fxLabel.setAlignment(Pos.CENTER);
                case "flex-end", "end", "bottom" -> fxLabel.setAlignment(Pos.BOTTOM_CENTER);
                case "flex-start", "start", "top" -> fxLabel.setAlignment(Pos.TOP_CENTER);
                default -> fxLabel.setAlignment(Pos.CENTER_LEFT);
            }
        }

        // ─── 4. Sombras no texto ────────────────────────────────────────────

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

                    fxLabel.setEffect(dropShadow);
                }
            } catch (Exception ignored) {}
        }

        // ─── 5. Background, Padding e Border-Radius ───
        if (styles.containsKey("background-color")) {
            String bgColor = styles.get("background-color").trim();
            if (!bgColor.isEmpty()) {
                fxLabel.setStyle(fxLabel.getStyle() + "-fx-background-color: " + bgColor + "; ");
            }
        }
        if (styles.containsKey("padding")) {
            fxLabel.setStyle(fxLabel.getStyle() + "-fx-padding: " + styles.get("padding") + "; ");
        }
        if (styles.containsKey("border-radius")) {
            fxLabel.setStyle(fxLabel.getStyle() + "-fx-background-radius: " + styles.get("border-radius") + "; ");
        }

        // ─── 6. Cor do link (se for <a>) ────────────────────────────────────
        if (sourceNode.tag.equalsIgnoreCase("a") && !styles.containsKey("color")) {
            fxLabel.setTextFill(Color.BLUE);
            fxLabel.setUnderline(true);
        }
    }

    // ─── Utilitário para text-transform: capitalize ────────────────────────

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
}