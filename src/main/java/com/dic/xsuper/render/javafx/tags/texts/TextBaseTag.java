package com.dic.xsuper.render.javafx.tags.texts;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class TextBaseTag extends NativeTag {

    public TextBaseTag(XplNode sourceNode) {
        super(sourceNode);
    }

    protected static void injectDefaultStyle(XplNode node, String property, String value) {
        if (node.attributes == null) return;
        String currentStyle = (String) node.attributes.getOrDefault("style", "");
        if (!currentStyle.toLowerCase().contains(property + ":")) {
            node.attributes.put("style", property + ": " + value + "; " + currentStyle);
        }
    }

    @Override
    protected Node createNode() {
        TextFlow textFlow = new TextFlow();

        textFlow.setMinWidth(0);
        textFlow.setMaxWidth(Double.MAX_VALUE);
        textFlow.setMinHeight(Region.USE_PREF_SIZE);

        String align = getRawStyles().getOrDefault("text-align", "left").toLowerCase();
        switch (align) {
            case "center" -> textFlow.setTextAlignment(TextAlignment.CENTER);
            case "right" -> textFlow.setTextAlignment(TextAlignment.RIGHT);
            case "justify" -> textFlow.setTextAlignment(TextAlignment.JUSTIFY);
        }

        return textFlow;
    }

    @Override
    protected void applyTagSpecificStyles() {}

    @Override
    protected void addChildren() {
        if (fxNode instanceof TextFlow textFlow) {
            Map<String, String> myStyles = this.getRawStyles();

            String[] inheritableProps = {
                    "color", "fill", "font-family", "font-size", "font-weight",
                    "font-style", "text-decoration", "text-shadow", "text-transform",
                    "-webkit-text-stroke-width", "-webkit-text-stroke-color", "stroke", "stroke-width",
                    "-webkit-font-smoothing"
            };

            for (NativeTag child : children) {
                Map<String, String> childStyles = child.getRawStyles();

                // Cascata Top-Down W3C
                for (String prop : inheritableProps) {
                    if (myStyles.containsKey(prop) && !childStyles.containsKey(prop)) {
                        childStyles.put(prop, myStyles.get(prop));
                    }
                }

                Node childNode = child.build();

                // =========================================================================
                // ⭐ A MAGIA DO INLINE WRAP (FLATTENING) ⭐
                // Se a tag for um <a>, <b> ou <span>, ela vem como um TextFlow fechado.
                // Extraímos os textos dela e aplicamos na mesma linha do parágrafo pai!
                // =========================================================================
                if (childNode instanceof TextFlow childFlow) {
                    List<Node> extractedNodes = new ArrayList<>(childFlow.getChildren());

                    for (Node extracted : extractedNodes) {

                        // 1. Transfere a Mãozinha do Cursor (Dos Links)
                        if (childFlow.getCursor() != null) {
                            extracted.setCursor(childFlow.getCursor());
                        }

                        // 2. Transfere os eventos de Clique (Para navegar)
                        if (childFlow.getOnMouseClicked() != null) {
                            extracted.setOnMouseClicked(childFlow.getOnMouseClicked());
                        }

                        // 3. Efeitos de Hover interativos
                        if (childFlow.getOnMouseEntered() != null) {
                            extracted.setOnMouseEntered(childFlow.getOnMouseEntered());
                        }
                        if (childFlow.getOnMouseExited() != null) {
                            extracted.setOnMouseExited(childFlow.getOnMouseExited());
                        }

                        // 4. Passa a âncora de reatividade (Para não perdermos a tag na RAM)
                        extracted.getProperties().putAll(childFlow.getProperties());

                        textFlow.getChildren().add(extracted);
                    }
                } else if (childNode != null) {
                    // Nós normais, como texto simples (<#text>)
                    textFlow.getChildren().add(childNode);
                }
            }
        }
    }
}