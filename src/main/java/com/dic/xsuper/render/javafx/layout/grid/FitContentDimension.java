package com.dic.xsuper.render.javafx.layout.grid;

import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;

public class FitContentDimension implements GridDimension {
    private final GridDimension value;
    public FitContentDimension(GridDimension value) { this.value = value; }

    @Override
    public void applyToColumn(ColumnConstraints cc, double availableWidth, int totalColumns) {
        double val = dimensionToPixels(value, availableWidth);
        cc.setPrefWidth(Region.USE_COMPUTED_SIZE);
        cc.setMaxWidth(val);
        cc.setMinWidth(Region.USE_COMPUTED_SIZE);
    }
    @Override
    public void applyToRow(RowConstraints rc, double availableHeight, int totalRows) {
        double val = dimensionToPixels(value, availableHeight);
        rc.setPrefHeight(Region.USE_COMPUTED_SIZE);
        rc.setMaxHeight(val);
        rc.setMinHeight(Region.USE_COMPUTED_SIZE);
    }
    private double dimensionToPixels(GridDimension dim, double available) {
        if (dim instanceof FixedDimension f) return f.getValue();
        if (dim instanceof PercentDimension p) return (p.getPercent() / 100.0) * available;
        return available;
    }
    @Override
    public String toString() { return "fit-content(" + value + ")"; }
}