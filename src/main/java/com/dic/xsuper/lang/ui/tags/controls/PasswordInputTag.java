package com.dic.xsuper.lang.ui.tags.controls;

import com.dic.xsuper.lang.ui.html.XplNode;
import javafx.scene.Node;
import javafx.scene.control.PasswordField;

public class PasswordInputTag extends TextInputTag {

    public PasswordInputTag(XplNode sourceNode) {
        super(sourceNode);
    }

    @Override
    protected Node createNode() {
        PasswordField pf = new PasswordField();
        pf.setPromptText(placeholder);
        pf.setText(value);
        fxControl = pf;

        applyCommonAttributes(fxControl);
        if (readonly) pf.setEditable(false);
        applyAttributeStyles(fxControl);


        return fxControl;
    }
}