package com.dic.xsuper.render.javafx.layout.grid;

import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;

public class AutoDimension implements GridDimension {
    public static final AutoDimension INSTANCE = new AutoDimension();
    private AutoDimension() {}

    @Override
    public void applyToColumn(ColumnConstraints cc, double availableWidth, int totalColumns) {
        cc.setPrefWidth(Region.USE_COMPUTED_SIZE);
        cc.setMinWidth(Region.USE_COMPUTED_SIZE);
        cc.setMaxWidth(Region.USE_PREF_SIZE);
    }
    @Override
    public void applyToRow(RowConstraints rc, double availableHeight, int totalRows) {
        rc.setPrefHeight(Region.USE_COMPUTED_SIZE);
        rc.setMinHeight(Region.USE_COMPUTED_SIZE);
        rc.setMaxHeight(Region.USE_PREF_SIZE);
    }
    @Override
    public boolean isAuto() { return true; }
    @Override
    public String toString() { return "auto"; }
}