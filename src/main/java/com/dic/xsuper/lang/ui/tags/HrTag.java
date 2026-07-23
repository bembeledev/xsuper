package com.dic.xsuper.lang.ui.tags;

import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.layout.Region;

import java.util.Map;

/**
 * Tag HTML <hr> (Horizontal Rule) com suporte a orientação vertical/horizontal.
 * Exemplo: <hr orientation="vertical" style="height: 200px;"/>
 */
public class HrTag extends NativeTag {

    public HrTag(XplNode node) {
        super(node);
    }

    @Override
    protected Node createNode() {
        Separator separator = new Separator();

        // Lê a orientação do atributo "orientation" ou do estilo "flex-direction" (fallback)
        String orientation = (String) sourceNode.attributes.getOrDefault("orientation", "horizontal");
        if (orientation == null || orientation.trim().isEmpty()) {
            orientation = "horizontal";
        }

        // Suporte a flex-direction: column -> vertical
        Map<String, String> styles = getRawStyles();
        if (styles.containsKey("flex-direction") && styles.get("flex-direction").toLowerCase().contains("column")) {
            orientation = "vertical";
        }

        separator.setOrientation(orientation.equalsIgnoreCase("vertical") ?
                Orientation.VERTICAL : Orientation.HORIZONTAL);

        // Configuração de tamanhos via CSS (width/height) serão aplicados no applyCommonStyles
        // Separator herda de Region, então podemos definir prefWidth/prefHeight lá.

        return separator;
    }

    @Override
    protected void applyTagSpecificStyles() {
        // Nada específico, tudo já é tratado no applyCommonStyles
    }

    @Override
    protected void applyCommonStyles() {
        // Chamamos o pai para aplicar estilos comuns (padding, margin, background, etc.)
        super.applyCommonStyles();

        if (fxNode instanceof Separator sep) {
            Map<String, String> styles = getRawStyles();

            // Se for vertical, usamos a altura (height) como prefHeight
            if (sep.getOrientation() == Orientation.VERTICAL) {
                if (styles.containsKey("height")) {
                    try {
                        double h = parseSize(styles.get("height"));
                        if (h > 0) sep.setPrefHeight(h);
                    } catch (Exception ignored) {}
                }
                // Largura padrão para separador vertical (pode ser ajustada via width)
                if (styles.containsKey("width")) {
                    try {
                        double w = parseSize(styles.get("width"));
                        if (w > 0) sep.setPrefWidth(w);
                    } catch (Exception ignored) {}
                } else {
                    // Valor padrão para largura (ex: 2px)
                    sep.setPrefWidth(2);
                }
            } else {
                // Horizontal: usa largura (width) como prefWidth
                if (styles.containsKey("width")) {
                    try {
                        double w = parseSize(styles.get("width"));
                        if (w > 0) sep.setPrefWidth(w);
                    } catch (Exception ignored) {}
                } else {
                    // Valor padrão para largura (ex: 100%)
                    sep.setPrefWidth(Region.USE_COMPUTED_SIZE);
                }
                if (styles.containsKey("height")) {
                    try {
                        double h = parseSize(styles.get("height"));
                        if (h > 0) sep.setPrefHeight(h);
                    } catch (Exception ignored) {}
                } else {
                    // Altura padrão para separador horizontal (ex: 2px)
                    sep.setPrefHeight(2);
                }
            }

            // Cor do separador: usa a propriedade CSS 'color' ou 'background-color'
            String color = styles.getOrDefault("color", styles.get("background-color"));
            if (color != null && !color.isEmpty()) {
                // Aplica a cor ao separador via CSS
                sep.setStyle("-fx-background-color: " + color + ";");
            }
        }
    }

    private double parseSize(String value) {
        if (value == null) return -1;
        value = value.trim().replace("px", "").replace("%", "");
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}