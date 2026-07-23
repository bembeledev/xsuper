package com.dic.xsuper.lang.ui.layout.grid;

import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;

public class PercentDimension implements GridDimension {
    private final double percent; // 0-100
    public PercentDimension(double percent) { this.percent = percent; }
    public double getPercent() { return percent; }

    @Override
    public void applyToColumn(ColumnConstraints cc, double availableWidth, int totalColumns) {
        cc.setPercentWidth(percent);
    }
    @Override
    public void applyToRow(RowConstraints rc, double availableHeight, int totalRows) {
        rc.setPercentHeight(percent);
    }
    @Override
    public String toString() { return percent + "%"; }
}