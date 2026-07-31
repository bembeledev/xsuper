package com.dic.xsuper.render.javafx.layout.grid;

import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;

import java.util.ArrayList;
import java.util.List;

public class RepeatDimension implements GridDimension {
    private final int count; // -1 para auto-fit/fill, >0 para fixo
    private final List<GridDimension> dimensions;
    private final boolean isAutoFit;
    private final boolean isAutoFill;

    public RepeatDimension(int count, List<GridDimension> dimensions) {
        this.count = count;
        this.dimensions = dimensions;
        this.isAutoFit = false;
        this.isAutoFill = false;
    }

    public RepeatDimension(boolean autoFit, List<GridDimension> dimensions) {
        this.count = -1;
        this.dimensions = dimensions;
        this.isAutoFit = autoFit;
        this.isAutoFill = !autoFit;
    }

    public int getCount() { return count; }
    public List<GridDimension> getDimensions() { return dimensions; }
    public boolean isAutoFit() { return isAutoFit; }
    public boolean isAutoFill() { return isAutoFill; }

    @Override
    public boolean isRepeat() { return true; }

    @Override
    public List<GridDimension> expand() {
        if (count == -1) {
            // Auto-fit/fill será resolvido fora com base no número de filhos
            return dimensions; // placeholder, será expandido depois
        }
        List<GridDimension> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.addAll(dimensions);
        }
        return result;
    }

    @Override
    public void applyToColumn(ColumnConstraints cc, double availableWidth, int totalColumns) {
        // Não se aplica diretamente, pois será expandido
    }
    @Override
    public void applyToRow(RowConstraints rc, double availableHeight, int totalRows) {
        // Não se aplica diretamente
    }
    @Override
    public String toString() {
        return "repeat(" + (count == -1 ? (isAutoFit ? "auto-fit" : "auto-fill") : count) + ", " + dimensions + ")";
    }
}