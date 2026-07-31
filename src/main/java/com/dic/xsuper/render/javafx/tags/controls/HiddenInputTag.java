package com.dic.xsuper.render.javafx.tags.controls;

import com.dic.xsuper.dom.node.XplNode;
import javafx.scene.Node;
import javafx.scene.control.TextField;

public class HiddenInputTag extends FormControlTag {

    public HiddenInputTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        TextField tf = new TextField(value);
        tf.setVisible(false);
        applyCommonAttributes(tf);

        return tf;
    }

    @Override
    protected void applyTagSpecificStyles() {

    }
}