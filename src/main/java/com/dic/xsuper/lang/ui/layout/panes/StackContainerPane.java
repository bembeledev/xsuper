package com.dic.xsuper.lang.ui.layout.panes;

import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.properties.cssunit.CssContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import java.util.List;
import java.util.Map;

public class StackContainerPane extends StackPane implements CustomLayoutPane {

    public StackContainerPane() {
        // Na web, os elementos começam no topo esquerdo por defeito.
        // O JavaFX centraliza tudo no StackPane nativamente, por isso corrigimos aqui!
        setAlignment(Pos.TOP_LEFT);
    }

    @Override
    public void populateChildren(List<NativeTag> children, CssContext context) {
        for (NativeTag child : children) {
            Node fxChild = child.build();
            Map<String, String> childStyles = child.getRawStyles();

            // ⭐ Lógica de Position Absolute
            String position = childStyles.getOrDefault("position", "static").toLowerCase().trim();

            if ("absolute".equals(position)) {
                Pos alignment = Pos.TOP_LEFT; // Padrão

                boolean hasTop = childStyles.containsKey("top");
                boolean hasBottom = childStyles.containsKey("bottom");
                boolean hasLeft = childStyles.containsKey("left");
                boolean hasRight = childStyles.containsKey("right");

                // Mapeia o CSS (top/bottom/left/right) para os alinhamentos magnéticos do JavaFX
                if (hasTop && hasLeft) alignment = Pos.TOP_LEFT;
                else if (hasTop && hasRight) alignment = Pos.TOP_RIGHT;
                else if (hasBottom && hasLeft) alignment = Pos.BOTTOM_LEFT;
                else if (hasBottom && hasRight) alignment = Pos.BOTTOM_RIGHT;
                else if (hasTop) alignment = Pos.TOP_CENTER;
                else if (hasBottom) alignment = Pos.BOTTOM_CENTER;
                else if (hasLeft) alignment = Pos.CENTER_LEFT;
                else if (hasRight) alignment = Pos.CENTER_RIGHT;

                StackPane.setAlignment(fxChild, alignment);
            }

            applyMargins(fxChild, child, context);
            getChildren().add(fxChild);
        }
    }

    private void applyMargins(Node targetNode, NativeTag child, CssContext context) {
        Map<String, String> styles = child.getRawStyles();

        // Puxa as margens normais que o teu StyleResolver calculou
        double mt = child.getResolvedStyles().margin.getTopPixels(context);
        double mr = child.getResolvedStyles().margin.getRightPixels(context);
        double mb = child.getResolvedStyles().margin.getBottomPixels(context);
        double ml = child.getResolvedStyles().margin.getLeftPixels(context);

        // Se for absoluto, os valores 'top', 'left', etc., são injetados como margem
        // para dar o afastamento exato do canto!
        if ("absolute".equals(styles.getOrDefault("position", "").toLowerCase().trim())) {
            if (styles.containsKey("top")) mt += parsePixel(styles.get("top"));
            if (styles.containsKey("bottom")) mb += parsePixel(styles.get("bottom"));
            if (styles.containsKey("left")) ml += parsePixel(styles.get("left"));
            if (styles.containsKey("right")) mr += parsePixel(styles.get("right"));
        }

        if (mt != 0 || mr != 0 || mb != 0 || ml != 0) {
            StackPane.setMargin(targetNode, new Insets(mt, mr, mb, ml));
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