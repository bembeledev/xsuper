package com.dic.xsuper.lang.ui.tags.controls.date;

import com.dic.xsuper.lang.ui.html.XplNode;
import com.dic.xsuper.lang.ui.tags.controls.TextInputTag;
import javafx.scene.Node;
import javafx.scene.control.TextField;

public class WeekInputTag extends TextInputTag {

    public WeekInputTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        TextField tf = new TextField();
        tf.setPromptText(placeholder.isEmpty() ? "YYYY-Www" : placeholder);
        tf.setText(value);
        fxControl = tf;

        applyCommonAttributes(fxControl);
        if (readonly) tf.setEditable(false);
        applyAttributeStyles(fxControl);

        // Validação de semana (YYYY-Www)
        tf.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty() && !newVal.matches("^\\d{4}-W[0-5][0-9]$")) {
                tf.setStyle("-fx-border-color: red;");
            } else {
                tf.setStyle("");
            }
        });

        return fxControl;
    }
}