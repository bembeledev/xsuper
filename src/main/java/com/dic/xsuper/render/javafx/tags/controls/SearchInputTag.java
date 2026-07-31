package com.dic.xsuper.render.javafx.tags.controls;

import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.Node;
import javafx.scene.control.TextField;

public class SearchInputTag extends TextInputTag {

    public SearchInputTag(XplNode sourceNode) {
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
            if (!newVal.isEmpty()) {
                tf.setStyle(baseStyle + " -fx-padding: 0 25 0 10;");
            } else {
                tf.setStyle(baseStyle);
            }
        });

        return fxControl;
    }
}