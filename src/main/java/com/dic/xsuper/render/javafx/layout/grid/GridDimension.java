package com.dic.xsuper.render.javafx.layout.grid;

import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;

import java.util.List;

public interface GridDimension {
    void applyToColumn(ColumnConstraints cc, double availableWidth, int totalColumns);
    void applyToRow(RowConstraints rc, double availableHeight, int totalRows);

    default boolean isAuto() { return false; }
    default boolean isFr() { return false; }
    default double getFrValue() { return 0; }
    default boolean isRepeat() { return false; }
    default List<GridDimension> expand() { return List.of(this); }
}