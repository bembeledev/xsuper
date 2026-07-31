package com.dic.xsuper.render.javafx.layout.panes;

import com.dic.xsuper.render.javafx.layout.grid.*;
import com.dic.xsuper.render.javafx.tags.NativeTag;
import com.dic.xsuper.dom.properties.cssunit.CssContext;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GridContainerPane extends GridPane implements CustomLayoutPane {
    private final List<GridDimension> columns;
    private final List<GridDimension> rows;
    private final Map<String, String> style;

    private List<NativeTag> cachedTags;
    private final List<Node> builtNodes = new ArrayList<>();
    private CssContext context;
    private int currentCols = -1;

    public GridContainerPane(Map<String, String> style, List<GridDimension> columns, List<GridDimension> rows) {
        this.style = style != null ? style : Map.of();
        this.columns = columns != null ? columns : List.of();
        this.rows = rows != null ? rows : List.of();
    }

    @Override
    public void populateChildren(List<NativeTag> children, CssContext context) {
        this.cachedTags = children;
        this.context = context;

        for (NativeTag child : children) {
            builtNodes.add(child.build());
        }

        boolean hasAutoFit = columns.stream().anyMatch(d -> d.isRepeat() &&
                (((RepeatDimension)d).isAutoFit() || ((RepeatDimension)d).isAutoFill()));

        if (hasAutoFit) {
            this.widthProperty().addListener((obs, oldV, newV) -> {
                double w = newV.doubleValue();
                if (w > 0) buildResponsiveGrid(w);
            });
        } else {
            buildStaticGrid();
        }
    }

    private void buildResponsiveGrid(double containerWidth) {
        double minWidth = 200;
        for (GridDimension d : columns) {
            if (d.isRepeat()) {
                GridDimension inner = ((RepeatDimension) d).getDimensions().getFirst();
                if (inner instanceof MinMaxDimension minmax && minmax.getMin() instanceof FixedDimension fd) {
                    minWidth = fd.getValue();
                }
            }
        }

        double gap = this.getHgap();
        int colsThatFit = Math.max(1, (int) ((containerWidth + gap) / (minWidth + gap)));
        colsThatFit = Math.min(colsThatFit, builtNodes.size());

        if (colsThatFit == this.currentCols) return;
        this.currentCols = colsThatFit;

        this.getColumnConstraints().clear();
        this.getChildren().clear();

        for (int i = 0; i < colsThatFit; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / colsThatFit);
            cc.setMinWidth(0); // ⭐ CURA WEB: Liberta a compressão!
            cc.setFillWidth(true);
            cc.setHalignment(javafx.geometry.HPos.CENTER);
            cc.setHgrow(javafx.scene.layout.Priority.SOMETIMES);
            this.getColumnConstraints().add(cc);
        }

        int col = 0, row = 0;
        for (int i = 0; i < builtNodes.size(); i++) {
            Node node = builtNodes.get(i);
            NativeTag tag = cachedTags.get(i);
            applyMargins(node, tag, this.context);

            int colSpan = getSpan(tag, "grid-column");
            int rowSpan = getSpan(tag, "grid-row");

            if (colSpan > colsThatFit) colSpan = colsThatFit;
            if (col + colSpan > colsThatFit) { col = 0; row++; }

            this.add(node, col, row, colSpan, rowSpan);

            col += colSpan;
            if (col >= colsThatFit) { col = 0; row++; }
        }
    }

    private void buildStaticGrid() {
        List<GridDimension> expandedColumns = expandDimensions(columns, builtNodes.size());
        List<GridDimension> expandedRows = expandDimensions(rows, builtNodes.size());

        applyColumnConstraints(expandedColumns);
        applyRowConstraints(expandedRows);

        int totalCols = expandedColumns.size();
        if (totalCols == 0) totalCols = 1;

        int col = 0, row = 0;
        for (int i = 0; i < builtNodes.size(); i++) {
            Node node = builtNodes.get(i);
            NativeTag tag = cachedTags.get(i);
            applyMargins(node, tag, context);

            // ⭐ A INTELIGÊNCIA ESPACIAL (Lê o "span 2")
            int colSpan = getSpan(tag, "grid-column");
            int rowSpan = getSpan(tag, "grid-row");

            if (colSpan > totalCols) colSpan = totalCols;

            // Se não couber na linha, quebra para a de baixo!
            if (col + colSpan > totalCols) {
                col = 0;
                row++;
            }

            this.add(node, col, row, colSpan, rowSpan);

            col += colSpan;
            if (col >= totalCols) {
                col = 0;
                row++;
            }
        }
    }

    // ⭐ NOVO: O Extrator de Span CSS
    private int getSpan(NativeTag tag, String property) {
        Map<String, String> styles = tag.getRawStyles();
        if (styles.containsKey(property)) {
            String val = styles.get(property).toLowerCase();
            if (val.contains("span")) {
                try {
                    return Integer.parseInt(val.replace("span", "").trim());
                } catch (Exception ignored) {}
            }
        }
        return 1; // Padrão é 1 célula
    }

    private List<GridDimension> expandDimensions(List<GridDimension> dims, int childCount) {
        List<GridDimension> result = new ArrayList<>();
        for (GridDimension dim : dims) {
            if (dim.isRepeat()) {
                RepeatDimension rep = (RepeatDimension) dim;
                int repeatCount = rep.getCount() > 0 ? rep.getCount() : childCount;
                for (int i = 0; i < repeatCount; i++) {
                    result.addAll(rep.getDimensions());
                }
            } else {
                result.add(dim);
            }
        }
        return result;
    }

    private void applyColumnConstraints(List<GridDimension> dims) {
        getColumnConstraints().clear();
        double totalFr = 0;
        boolean isMixedLayout = false;

        for (GridDimension d : dims) {
            if (d.isFr()) totalFr += d.getFrValue();
            else isMixedLayout = true;
        }

        for (GridDimension d : dims) {
            ColumnConstraints cc = new ColumnConstraints();
            d.applyToColumn(cc, 0, dims.size());

            if (d.isFr() && totalFr > 0) {
                if (isMixedLayout) {
                    cc.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                    cc.setMinWidth(0); // ⭐ CURA WEB: Liberta a compressão!
                } else {
                    cc.setPercentWidth((d.getFrValue() / totalFr) * 100);
                    cc.setMinWidth(0); // ⭐ CURA WEB: Liberta a compressão!
                }
            }
            cc.setFillWidth(true);
            cc.setHalignment(javafx.geometry.HPos.CENTER);
            getColumnConstraints().add(cc);
        }
    }

    private void applyRowConstraints(List<GridDimension> dims) {}

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