package com.dic.xsuper.lang.ui.layout.grid;

import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;

public class FixedDimension implements GridDimension {
    private final double value; // em pixels
    public FixedDimension(double value) { this.value = value; }
    public double getValue() { return value; }

    @Override
    public void applyToColumn(ColumnConstraints cc, double availableWidth, int totalColumns) {
        cc.setPrefWidth(value);
        cc.setMinWidth(value);
        cc.setMaxWidth(value);
    }
    @Override
    public void applyToRow(RowConstraints rc, double availableHeight, int totalRows) {
        rc.setPrefHeight(value);
        rc.setMinHeight(value);
        rc.setMaxHeight(value);
    }
    @Override
    public String toString() { return value + "px"; }
}