package com.dic.xsuper.render.javafx.tags.controls.date;

import com.dic.xsuper.dom.node.XplNode;
import com.dic.xsuper.render.javafx.tags.controls.TextInputTag;
import javafx.scene.Node;
import javafx.scene.control.TextField;

public class TimeInputTag extends TextInputTag {

    public TimeInputTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        TextField tf = new TextField();
        tf.setPromptText(placeholder.isEmpty() ? "HH:mm" : placeholder);
        tf.setText(value);
        fxControl = tf;

        applyCommonAttributes(fxControl);
        if (readonly) tf.setEditable(false);
        applyAttributeStyles(fxControl);

        // Validação de hora (simples)
        tf.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty() && !newVal.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                tf.setStyle("-fx-border-color: red;");
            } else {
                tf.setStyle("");
            }
        });

        return fxControl;
    }
}