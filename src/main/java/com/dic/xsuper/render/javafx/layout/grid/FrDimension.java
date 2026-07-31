package com.dic.xsuper.render.javafx.layout.grid;

import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;

public class FrDimension implements GridDimension {
    private final double value;
    public FrDimension(double value) { this.value = value; }
    public double getValue() { return value; }

    @Override
    public void applyToColumn(ColumnConstraints cc, double availableWidth, int totalColumns) {
        // Será aplicado depois de saber o total de fr (guardamos para uso posterior)
        // Não aplicamos aqui, pois a percentagem depende do total
    }
    @Override
    public void applyToRow(RowConstraints rc, double availableHeight, int totalRows) {
        // similar
    }
    @Override
    public boolean isFr() { return true; }
    @Override
    public double getFrValue() { return value; }
    @Override
    public String toString() { return value + "fr"; }
}