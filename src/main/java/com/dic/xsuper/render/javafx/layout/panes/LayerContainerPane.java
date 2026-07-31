package com.dic.xsuper.render.javafx.layout.panes;

import com.dic.xsuper.render.javafx.tags.NativeTag;
import com.dic.xsuper.dom.properties.cssunit.CssContext;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import java.util.List;
import java.util.Map;

public class LayerContainerPane extends StackPane implements CustomLayoutPane {

    public LayerContainerPane() {
        setAlignment(Pos.CENTER);
    }

    @Override
    public void populateChildren(List<NativeTag> children, CssContext context) {
        for (NativeTag child : children) {
            Node fxChild = child.build();
            Map<String, String> styles = child.getRawStyles();

            // ⭐ 0. APLICAR DIMENSÕES RÍGIDAS (Width / Height)
            applyExplicitDimensions(fxChild, styles);

            // 1. O SISTEMA MAGNÉTICO (8 Posições + Center)
            String pos = styles.getOrDefault("pos", "center").toLowerCase().trim();
            Pos alignment = switch (pos) {
                case "top-left" -> Pos.TOP_LEFT;
                case "top-right" -> Pos.TOP_RIGHT;
                case "bottom-left" -> Pos.BOTTOM_LEFT;
                case "bottom-right" -> Pos.BOTTOM_RIGHT;
                case "top" -> Pos.TOP_CENTER;
                case "bottom" -> Pos.BOTTOM_CENTER;
                case "left" -> Pos.CENTER_LEFT;
                case "right" -> Pos.CENTER_RIGHT;
                default -> Pos.CENTER;
            };
            StackPane.setAlignment(fxChild, alignment);

            // 2. O SISTEMA DE TRANSLAÇÃO (Ajuste cirúrgico em Pixels)
            double transX = 0;
            double transY = 0;

            if (styles.containsKey("x")) transX = parsePixel(styles.get("x"));
            else if (styles.containsKey("translate-x")) transX = parsePixel(styles.get("translate-x"));

            if (styles.containsKey("y")) transY = parsePixel(styles.get("y"));
            else if (styles.containsKey("translate-y")) transY = parsePixel(styles.get("translate-y"));

            if (transX != 0) fxChild.setTranslateX(transX);
            if (transY != 0) fxChild.setTranslateY(transY);

            getChildren().add(fxChild);
        }
    }

    private void applyExplicitDimensions(Node fxChild, Map<String, String> styles) {
        if (styles.containsKey("width")) {
            double w = parsePixel(styles.get("width"));
            if (w > 0) {
                if (fxChild instanceof Region r) {
                    r.setPrefWidth(w);
                    r.setMaxWidth(Region.USE_PREF_SIZE); // Trava o esticamento automático!
                    r.setMinWidth(w);
                } else if (fxChild instanceof Control c) {
                    c.setPrefWidth(w);
                    c.setMaxWidth(Control.USE_PREF_SIZE);
                    c.setMinWidth(w);
                }
            }
        }

        if (styles.containsKey("height")) {
            double h = parsePixel(styles.get("height"));
            if (h > 0) {
                if (fxChild instanceof Region r) {
                    r.setPrefHeight(h);
                    r.setMaxHeight(Region.USE_PREF_SIZE);
                    r.setMinHeight(h);
                } else if (fxChild instanceof Control c) {
                    c.setPrefHeight(h);
                    c.setMaxHeight(Control.USE_PREF_SIZE);
                    c.setMinHeight(h);
                }
            }
        }
    }

    private double parsePixel(String value) {
        try {
            return Double.parseDouble(value.replace("px", "").trim());
        } catch (Exception e) {
            return 0;
        }
    }
}