package com.dic.xsuper.render.javafx.tags.controls.date;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.controls.FormControlTag;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;

import java.time.Month;
import java.util.stream.Collectors;

public class MonthInputTag extends FormControlTag {

    private ComboBox<String> combo;

    public MonthInputTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        combo = new ComboBox<>();
        combo.getItems().addAll(
                java.util.Arrays.stream(Month.values())
                        .map(m -> String.format("%02d", m.getValue()) + " - " + m.name())
                        .collect(Collectors.toList())
        );

        // Tenta selecionar o mês do value
        if (!value.isEmpty()) {
            try {
                int month = Integer.parseInt(value);
                combo.getSelectionModel().select(month - 1);
            } catch (NumberFormatException ignored) {}
        }

        applyCommonAttributes(combo);
        return combo;
    }

    @Override
    protected void applyTagSpecificStyles() {

    }

    @Override
    protected void addChildren() {
        // void
    }
}