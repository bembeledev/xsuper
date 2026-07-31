package com.dic.xsuper.render.javafx.layout.panes;

import com.dic.xsuper.render.javafx.tags.NativeTag;
import com.dic.xsuper.dom.properties.cssunit.CssContext;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import java.util.List;
import java.util.Map;

public class InlineGridContainerPane extends GridPane implements CustomLayoutPane {

    private final Map<String, String> style;

    public InlineGridContainerPane(Map<String, String> style) {
        this.style = style != null ? style : Map.of();

        // ⭐ A GRANDE DIFERENÇA: Um Inline-Grid NÃO cresce!
        // Fica restrito ao tamanho exato do seu conteúdo.
        setMaxWidth(Region.USE_PREF_SIZE);
        setMaxHeight(Region.USE_PREF_SIZE);
    }

    @Override
    public void populateChildren(List<NativeTag> children, CssContext context) {
        int row = 0, col = 0;
        int maxCols = 1;

        if (style.containsKey("grid-template-columns")) {
            maxCols = style.get("grid-template-columns").trim().split("\\s+").length;
        }

        for (NativeTag child : children) {
            Node fxChild = child.build();

            // Num inline-grid, os filhos não preenchem automaticamente o ecrã todo
            GridPane.setFillWidth(fxChild, false);
            GridPane.setFillHeight(fxChild, false);

            applyMargins(fxChild, child, context);

            this.add(fxChild, col++, row);
            if (col >= maxCols) {
                col = 0; row++;
            }
        }
    }

    private void applyMargins(Node targetNode, NativeTag child, CssContext context) {
        if (!child.getResolvedStyles().margin.isZero()) {
            Insets m = new Insets(
                    child.getResolvedStyles().margin.getTopPixels(context),
                    child.getResolvedStyles().margin.getRightPixels(context),
                    child.getResolvedStyles().margin.getBottomPixels(context),
                    child.getResolvedStyles().margin.getLeftPixels(context)
            );
            GridPane.setMargin(targetNode, m);
        }
    }
}