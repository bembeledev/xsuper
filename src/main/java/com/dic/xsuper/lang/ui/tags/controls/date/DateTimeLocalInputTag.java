package com.dic.xsuper.lang.ui.tags.controls.date;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.controls.FormControlTag;
import javafx.scene.Node;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class DateTimeLocalInputTag extends FormControlTag {

    private HBox container;

    public DateTimeLocalInputTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        container = new HBox(5);

        DatePicker datePicker = new DatePicker();
        TextField timeField = new TextField();
        timeField.setPromptText("HH:mm");
        timeField.setText("12:00");

        if (!value.isEmpty() && value.contains("T")) {
            String[] parts = value.split("T");
            try { datePicker.setValue(java.time.LocalDate.parse(parts[0])); } catch (Exception ignored) {}
            if (parts.length > 1) timeField.setText(parts[1]);
        }

        container.getChildren().addAll(datePicker, timeField);
        //applyCommonAttributes(container);

        return container;
    }

    @Override
    protected void applyTagSpecificStyles() {

    }

    @Override
    protected void addChildren() {
        // void
    }
}