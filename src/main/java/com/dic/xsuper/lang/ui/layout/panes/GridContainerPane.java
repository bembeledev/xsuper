package com.dic.xsuper.lang.ui.layout.panes;

import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.properties.cssunit.CssContext;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import java.util.List;
import java.util.Map;

public class GridContainerPane extends GridPane implements CustomLayoutPane {

    private final Map<String, String> style;

    public GridContainerPane(Map<String, String> style) {
        this.style = style != null ? style : Map.of();
    }

    @Override
    public void populateChildren(List<NativeTag> children, CssContext context) {
        int row = 0, col = 0;
        int maxCols = 1; // Fallback

        if (style.containsKey("grid-template-columns")) {
            maxCols = style.get("grid-template-columns").trim().split("\\s+").length;
        }

        for (NativeTag child : children) {
            Node builtNode = child.build();

            // Diz ao elemento filho para se esticar e preencher a célula (Coluna) a 100%
            GridPane.setFillWidth(builtNode, true);
            GridPane.setFillHeight(builtNode, true);

            applyMargins(builtNode, child, context);

            // Adiciona na grelha nativa do JavaFX e move o cursor da tabela
            this.add(builtNode, col++, row);

            if (col >= maxCols) {
                col = 0;
                row++;
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