package com.dic.xsuper.render.javafx.tags.controls;

import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.Node;
import javafx.scene.control.ColorPicker;
import javafx.scene.paint.Color;

public class ColorInputTag extends FormControlTag {

    private ColorPicker fxColorPicker;

    public ColorInputTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        fxColorPicker = new ColorPicker();
        if (!value.isEmpty()) {
            try { fxColorPicker.setValue(Color.web(value)); } catch (Exception ignored) {}
        }
        applyCommonAttributes(fxColorPicker);

        bindEvents();
        return fxColorPicker;
    }

    @Override
    protected void applyTagSpecificStyles() {

    }

    public void bindEvents() {
        fxColorPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            String hex = String.format("#%02X%02X%02X",
                    (int) (newVal.getRed() * 255),
                    (int) (newVal.getGreen() * 255),
                    (int) (newVal.getBlue() * 255));
            //dispatchEvent("change", sourceNode.id, hex);
            //dispatchEvent("input", sourceNode.id, hex);
        });
    }
}