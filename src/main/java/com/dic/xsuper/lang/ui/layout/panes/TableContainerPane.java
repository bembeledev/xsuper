package com.dic.xsuper.lang.ui.layout.panes;

import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.properties.cssunit.CssContext;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import java.util.List;
import java.util.Map;

public class TableContainerPane extends GridPane implements CustomLayoutPane {

    private final Map<String, String> style;

    public TableContainerPane(Map<String, String> style) {
        this.style = style != null ? style : Map.of();

        if (this.style.containsKey("border-spacing")) {
            try {
                double gap = Double.parseDouble(this.style.get("border-spacing").replace("px", "").trim());
                setHgap(gap);
                setVgap(gap);
            } catch (Exception ignored) {}
        } else {
            // Espaçamento padrão de tabelas HTML (2px)
            setHgap(2);
            setVgap(2);
        }
    }

    @Override
    public void populateChildren(List<NativeTag> children, CssContext context) {
        // Inicia a mineração recursiva das linhas a começar na coordenada Y=0
        extractAndBuildRows(children, context, 0);
    }

    private int extractAndBuildRows(List<NativeTag> nodes, CssContext context, int startRowIndex) {
        int rowIndex = startRowIndex;

        for (NativeTag node : nodes) {
            String tagName = node.getSourceNode().tag.toLowerCase();
            String display = node.getResolvedStyles().display.toLowerCase().trim();

            // ⭐ 1. Se for um Grupo (thead/tbody/tfoot) ou uma caixa fantasma (contents)
            // Abrimos o nó e procuramos as TRs que estão lá dentro!
            if ("tbody".equals(tagName) || "thead".equals(tagName) || "tfoot".equals(tagName) || "contents".equals(display)) {
                rowIndex = extractAndBuildRows(node.getChildren(), context, rowIndex);
            }
            // ⭐ 2. Se for uma linha autêntica (tr / table-row), montamos as células (td/th)
            else if ("table-row".equals(display) || "tr".equals(tagName)) {
                int colIndex = 0;
                for (NativeTag cellTag : node.getChildren()) {
                    Node fxCell = cellTag.build();

                    // As células esticam-se para preencher a sua célula da grelha
                    GridPane.setFillWidth(fxCell, true);
                    GridPane.setFillHeight(fxCell, true);
                    applyMargins(fxCell, cellTag, context);

                    this.add(fxCell, colIndex++, rowIndex);
                }
                rowIndex++;
            }
            // 3. Qualquer outro elemento intruso (ex: caption) ocupa toda a largura
            else {
                Node fxNode = node.build();
                this.add(fxNode, 0, rowIndex++);
                GridPane.setColumnSpan(fxNode, GridPane.REMAINING);
            }
        }
        return rowIndex;
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