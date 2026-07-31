package com.dic.xsuper.render.javafx.layout.grid;

import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;

public class MinMaxDimension implements GridDimension {
    private final GridDimension min;
    private final GridDimension max;

    public MinMaxDimension(GridDimension min, GridDimension max) {
        this.min = min;
        this.max = max;
    }

    public GridDimension getMin() { return min; }
    public GridDimension getMax() { return max; }

    // ⭐ A CURA 1: O Painel agora sabe que há um 'fr' aqui dentro!
    @Override
    public boolean isFr() {
        return max.isFr() || min.isFr();
    }

    @Override
    public double getFrValue() {
        return max.isFr() ? max.getFrValue() : (min.isFr() ? min.getFrValue() : 0);
    }

    @Override
    public void applyToColumn(ColumnConstraints cc, double availableWidth, int totalColumns) {
        // ⭐ A CURA 2: No momento do build a largura é 0.
        // Lemos diretamente o valor do min se for em pixels!
        if (min instanceof FixedDimension fd) {
            cc.setMinWidth(fd.getValue());
        } else {
            cc.setMinWidth(Region.USE_COMPUTED_SIZE);
        }
        // O max será tratado pelo setPercentWidth no GridContainerPane!
    }

    @Override
    public void applyToRow(RowConstraints rc, double availableHeight, int totalRows) {
        if (min instanceof FixedDimension fd) {
            rc.setMinHeight(fd.getValue());
        } else {
            rc.setMinHeight(Region.USE_COMPUTED_SIZE);
        }
    }

    @Override
    public String toString() { return "minmax(" + min + ", " + max + ")"; }
}