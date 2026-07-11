package com.dic.xsuper.lang.ui.tags.controls.date;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.controls.FormControlTag;
import javafx.scene.Node;
import javafx.scene.control.DatePicker;

import java.time.LocalDate;

public class DateInputTag extends FormControlTag {

    private DatePicker fxDatePicker;

    public DateInputTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        fxDatePicker = new DatePicker();
        if (!value.isEmpty()) {
            try { fxDatePicker.setValue(LocalDate.parse(value)); } catch (Exception ignored) {}
        }
        applyCommonAttributes(fxDatePicker);
        if (!placeholder.isEmpty()) fxDatePicker.setPromptText(placeholder);
        bindEvents();
        return fxDatePicker;
    }

    @Override
    protected void applyTagSpecificStyles() {

    }

    public void bindEvents() {
        fxDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            String iso = newVal != null ? newVal.toString() : "";
            //dispatchEvent("change", sourceNode.id, iso);
            //dispatchEvent("input", sourceNode.id, iso);
        });
    }
}