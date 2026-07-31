package com.dic.xsuper.render.javafx.tags.controls;

import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.Node;
import javafx.scene.control.TextField;

public class EmailInputTag extends TextInputTag {

    public EmailInputTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        TextField tf = new TextField();
        tf.setPromptText(placeholder);
        tf.setText(value);
        fxControl = tf;

        applyCommonAttributes(fxControl);
        if (readonly) tf.setEditable(false);
        applyAttributeStyles(fxControl);


        // ⭐ Captura o estilo injetado pelo motor CSS antes dos listeners
        String baseStyle = tf.getStyle() != null ? tf.getStyle() : "";

        tf.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty() && !newVal.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                tf.setStyle(baseStyle + " -fx-border-color: red;");
            } else {
                tf.setStyle(baseStyle); // Repõe o estilo original em vez de ""
            }
        });

        return fxControl;
    }
}