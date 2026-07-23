package com.dic.xsuper.lang.ui.layout.panes;

import com.dic.xsuper.lang.ui.layout.grid.*;
import com.dic.xsuper.lang.ui.tags.NativeTag;
import com.dic.xsuper.lang.ui.properties.cssunit.CssContext;
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

    // Cache para o Auto-Fit Responsivo W3C
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

        // 1. Pré-constrói os nós do JavaFX APENAS UMA VEZ para poupar CPU
        for (NativeTag child : children) {
            builtNodes.add(child.build());
        }

        // 2. O Layout pede responsividade (auto-fit/auto-fill)?
        boolean hasAutoFit = columns.stream().anyMatch(d -> d.isRepeat() &&
                (((RepeatDimension)d).isAutoFit() || ((RepeatDimension)d).isAutoFill()));

        if (hasAutoFit) {
            // ⭐ A FÍSICA W3C: Ouve a largura da Janela e recalcula as colunas!
            this.widthProperty().addListener((obs, oldV, newV) -> {
                double w = newV.doubleValue();
                if (w > 0) buildResponsiveGrid(w);
            });
        } else {
            // Grelha Estática Clássica (ex: 1fr 1fr)
            buildStaticGrid();
        }
    }

    private void buildResponsiveGrid(double containerWidth) {
        // Descobre a largura mínima requerida pelo teu Parser (ex: os 200px do minmax)
        double minWidth = 200; // Fallback
        for (GridDimension d : columns) {
            if (d.isRepeat()) {
                GridDimension inner = ((RepeatDimension) d).getDimensions().getFirst();
                if (inner instanceof MinMaxDimension minmax && minmax.getMin() instanceof FixedDimension fd) {
                    minWidth = fd.getValue();
                }
            }
        }

        // A Matemática Responsiva: Quantas colunas cabem com os gaps?
        double gap = this.getHgap();
        int colsThatFit = Math.max(1, (int) ((containerWidth + gap) / (minWidth + gap)));

        // Não criar colunas fantasma se houver poucos cartões (Regra auto-fit)
        colsThatFit = Math.min(colsThatFit, builtNodes.size());

        // Se o número de colunas for igual à renderização anterior, evita recalcular!
        if (colsThatFit == this.currentCols) return;
        this.currentCols = colsThatFit;

        // 1. Limpar a grelha atual
        this.getColumnConstraints().clear();
        this.getChildren().clear();

        // 2. Distribuir larguras perfeitamente (ex: se cabem 3, divide por 33.33%)
        for (int i = 0; i < colsThatFit; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / colsThatFit);
            this.getColumnConstraints().add(cc);
        }

        // 3. Posicionar os nós usando as coordenadas de grelha "Auto-Flow" W3C
        int col = 0, row = 0;
        for (int i = 0; i < builtNodes.size(); i++) {
            Node node = builtNodes.get(i);
            applyMargins(node, cachedTags.get(i), this.context);

            this.add(node, col, row);

            col++;
            if (col >= colsThatFit) {
                col = 0; // Quebra de linha!
                row++;
            }
        }
    }

    private void buildStaticGrid() {
        // A tua lógica original mantida e limpa
        List<GridDimension> expandedColumns = expandDimensions(columns, builtNodes.size());
        List<GridDimension> expandedRows = expandDimensions(rows, builtNodes.size());

        applyColumnConstraints(expandedColumns);
        applyRowConstraints(expandedRows);

        int col = 0, row = 0;
        for (int i = 0; i < builtNodes.size(); i++) {
            Node node = builtNodes.get(i);
            applyMargins(node, cachedTags.get(i), context);
            this.add(node, col, row);

            col++;
            if (col >= expandedColumns.size()) {
                col = 0;
                row++;
            }
        }
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
        for (GridDimension d : dims) {
            if (d.isFr()) totalFr += d.getFrValue();
        }
        for (GridDimension d : dims) {
            ColumnConstraints cc = new ColumnConstraints();
            d.applyToColumn(cc, 0, dims.size());
            if (d.isFr() && totalFr > 0) {
                cc.setPercentWidth((d.getFrValue() / totalFr) * 100);
            }
            getColumnConstraints().add(cc);
        }
    }

    private void applyRowConstraints(List<GridDimension> dims) {
        // Oculto por simplicidade, mantém o teu código original...
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