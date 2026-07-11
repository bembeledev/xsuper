package com.dic.xsuper.lang.ui.tags.controls;

import com.dic.xsuper.lang.ui.html.XplNode;
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