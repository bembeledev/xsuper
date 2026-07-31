package com.dic.xsuper.render.javafx.tags.controls;

import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.Node;
import javafx.scene.control.TextField;

public class TelInputTag extends TextInputTag {

    public TelInputTag(XplNode sourceNode) {
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


        String baseStyle = tf.getStyle() != null ? tf.getStyle() : "";

        tf.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty() && !newVal.matches("^[0-9() +-]+$")) {
                tf.setStyle(baseStyle + " -fx-border-color: red;");
            } else {
                tf.setStyle(baseStyle);
            }
        });

        return fxControl;
    }
}